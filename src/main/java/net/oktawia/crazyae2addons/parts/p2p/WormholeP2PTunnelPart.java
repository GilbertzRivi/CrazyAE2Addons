package net.oktawia.crazyae2addons.parts.p2p;

import appeng.api.config.PowerUnits;
import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import lombok.Getter;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.wormhole.WormholeP2PCapabilityProxy;
import net.oktawia.crazyae2addons.logic.wormhole.WormholeP2PConnectionManager;
import net.oktawia.crazyae2addons.logic.wormhole.WormholeP2PInteractionLogic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.function.Function;

public class WormholeP2PTunnelPart extends P2PTunnelPart<WormholeP2PTunnelPart> implements IGridTickable, ICapabilityProvider {

    public interface WormholeCapabilityExtension {
        boolean handles(Capability<?> cap);
        <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side);
        void onRemoveFromWorld();
    }

    private static final List<Function<WormholeP2PTunnelPart, WormholeCapabilityExtension>> EXTENSION_FACTORIES = new ArrayList<>();

    public static void registerExtension(Function<WormholeP2PTunnelPart, WormholeCapabilityExtension> factory) {
        EXTENSION_FACTORIES.add(factory);
    }

    private static final P2PModels MODELS = new P2PModels(CrazyAddons.makeId("part/wormhole"));

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    @Getter
    private final WormholeP2PConnectionManager connectionManager = new WormholeP2PConnectionManager(this);

    @Getter
    private final WormholeP2PCapabilityProxy capabilityProxy = new WormholeP2PCapabilityProxy(this);

    @Getter
    private final WormholeP2PInteractionLogic interactionLogic = new WormholeP2PInteractionLogic(this);

    private final List<WormholeCapabilityExtension> extensions;

    @Getter
    protected final IManagedGridNode outerNode = CrazyConfig.COMMON.WORMHOLE_NESTED_P2PS_ENABLED.get()
            ? GridHelper.createManagedNode(this, NodeListener.INSTANCE)
                    .setTagName("outer")
                    .setInWorldNode(true)
                    .setFlags(GridFlags.DENSE_CAPACITY)
            : GridHelper.createManagedNode(this, NodeListener.INSTANCE)
                    .setTagName("outer")
                    .setInWorldNode(true)
                    .setFlags(GridFlags.DENSE_CAPACITY, GridFlags.CANNOT_CARRY_COMPRESSED);

    public WormholeP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
        this.extensions = EXTENSION_FACTORIES.stream().map(f -> f.apply(this)).toList();
        this.getMainNode()
                .setFlags(
                        GridFlags.REQUIRE_CHANNEL,
                        CrazyConfig.COMMON.WORMHOLE_NESTED_P2PS_ENABLED.get() ? GridFlags.DENSE_CAPACITY : GridFlags.COMPRESSED_CHANNEL
                )
                .addService(IGridTickable.class, this);
    }

    public void drainPower(double amount, PowerUnits unit) {
        deductEnergyCost(amount, unit);
    }

    @Override
    public boolean onPartActivate(Player player, net.minecraft.world.InteractionHand hand, net.minecraft.world.phys.Vec3 pos) {
        return interactionLogic.onPartActivate(player, hand, pos);
    }

    @Override
    public void importSettings(appeng.util.SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        interactionLogic.importSettings(mode, input, player);
    }

    @Override
    public void exportSettings(appeng.util.SettingsFrom mode, CompoundTag output) {
        interactionLogic.exportSettings(mode, output);
    }

    @Override
    protected float getPowerDrainPerTick() {
        return 2.0f;
    }

    @Override
    public void readFromNBT(CompoundTag extra) {
        super.readFromNBT(extra);
        this.outerNode.loadFromNBT(extra);
    }

    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        this.outerNode.saveToNBT(extra);
    }

    @Override
    public void onTunnelNetworkChange() {
        super.onTunnelNetworkChange();

        if (!this.isOutput() || connectionManager.hasConnections()) {
            getMainNode().ifPresent((grid, node) -> grid.getTickManager().wakeDevice(node));
        }

        connectionManager.sendBlockUpdateToOppositeSide();
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.outerNode.destroy();
        connectionManager.disconnectAll();
        for (var ext : extensions) {
            ext.onRemoveFromWorld();
        }
    }

    @Override
    public void addToWorld() {
        super.addToWorld();
        this.outerNode.create(getLevel(), getBlockEntity().getBlockPos());
    }

    @Override
    public void setPartHostInfo(Direction side, IPartHost host, BlockEntity blockEntity) {
        super.setPartHostInfo(side, host, blockEntity);
        this.outerNode.setExposedOnSides(EnumSet.of(side));
    }

    @Override
    public IGridNode getExternalFacingNode() {
        return this.outerNode.getNode();
    }

    @Override
    public void onPlacement(Player player) {
        super.onPlacement(player);
        this.outerNode.setOwningPlayer(player);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, true, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        connectionManager.schedule(node.isOnline());

        if (getLevel() != null) {
            TickHandler.instance().addCallable(getLevel(), connectionManager::updateConnections);
        }

        return TickRateModulation.SLEEP;
    }

    @Override
    public void onNeighborChanged(net.minecraft.world.level.BlockGetter level, net.minecraft.core.BlockPos pos, net.minecraft.core.BlockPos neighbor) {
        super.onNeighborChanged(level, pos, neighbor);
        connectionManager.sendBlockUpdateToOppositeSide();
    }

    @Override
    public AECableType getExternalCableConnectionType() {
        return AECableType.DENSE_SMART;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(5, 5, 11.5, 11, 11, 12.5);
        bch.addBox(3, 3, 12.5, 13, 13, 13.5);
        bch.addBox(2, 2, 13.5, 14, 14, 15.5);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        for (var ext : extensions) {
            if (ext.handles(cap)) {
                return ext.getCapability(cap, side);
            }
        }
        return capabilityProxy.getCapability(cap, side);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capabilityClass) {
        for (var ext : extensions) {
            if (ext.handles(capabilityClass)) {
                return ext.getCapability(capabilityClass, getSide());
            }
        }
        return capabilityProxy.getCapability(capabilityClass, getSide());
    }
}
