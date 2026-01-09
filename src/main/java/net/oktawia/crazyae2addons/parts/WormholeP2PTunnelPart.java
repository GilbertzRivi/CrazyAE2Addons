package net.oktawia.crazyae2addons.parts;

import java.util.*;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.me.service.P2PService;
import appeng.menu.MenuOpener;
import appeng.parts.p2p.P2PModels;
import appeng.parts.p2p.P2PTunnelPart;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

import appeng.api.networking.GridFlags;
import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartHost;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.PartModels;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.misc.CombinedEnergyStorage;
import net.oktawia.crazyae2addons.misc.CombinedFluidHandlerItem;
import net.oktawia.crazyae2addons.misc.FluidHandlerConcatenate;
import net.oktawia.crazyae2addons.misc.WormholeAnchor;
import net.oktawia.crazyae2addons.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class WormholeP2PTunnelPart extends P2PTunnelPart<WormholeP2PTunnelPart> implements IGridTickable, ICapabilityProvider {

    private static final P2PModels MODELS = new P2PModels(CrazyAddons.makeId("part/wormhole_p2p_tunnel"));
    private static final Set<BlockPos> wormholeUpdateBlacklist = new HashSet<>();
    private int redstonePower = 0;
    private boolean redstoneRecursive = false;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }
    private ConnectionUpdate pendingUpdate = ConnectionUpdate.NONE;
    private final Map<WormholeP2PTunnelPart, IGridConnection> connections = new IdentityHashMap<>();

    protected final IManagedGridNode outerNode = CrazyConfig.COMMON.NestedP2PWormhole.get() ?
            GridHelper
            .createManagedNode(this, NodeListener.INSTANCE)
            .setTagName("outer")
            .setInWorldNode(true)
            .setFlags(GridFlags.DENSE_CAPACITY)
            :
            GridHelper
                    .createManagedNode(this, NodeListener.INSTANCE)
            .setTagName("outer")
            .setInWorldNode(true)
            .setFlags(GridFlags.DENSE_CAPACITY, GridFlags.CANNOT_CARRY_COMPRESSED);

    public WormholeP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL, CrazyConfig.COMMON.NestedP2PWormhole.get() ? GridFlags.DENSE_CAPACITY : GridFlags.COMPRESSED_CHANNEL)
                .addService(IGridTickable.class, this);
    }

    private void readRedstoneInput() {
        var targetPos = getBlockEntity().getBlockPos().relative(getSide());
        var state = getLevel().getBlockState(targetPos);
        var block = state.getBlock();

        Direction inputSide = block instanceof RedStoneWireBlock ? Direction.UP : getSide();
        int newPower = block.getSignal(state, getLevel(), targetPos, inputSide);
        sendRedstoneToOutput(newPower);
    }

    private void sendRedstoneToOutput(int power) {
        int reducedPower = Math.max(0, power - 1);

        for (var output : getOutputs()) {
            output.receiveRedstoneInput(reducedPower);
        }
    }


    private void receiveRedstoneInput(int power) {
        if (redstoneRecursive) return;
        redstoneRecursive = true;

        if (isOutput() && getMainNode().isActive()) {
            if (this.redstonePower != power) {
                this.redstonePower = power;
                notifyRedstoneUpdate();
            }
        }

        redstoneRecursive = false;
    }

    @Override
    public boolean canConnectRedstone() {
        return true;
    }

    @Override
    public int isProvidingStrongPower() {
        return isOutput() ? redstonePower : 0;
    }

    @Override
    public int isProvidingWeakPower() {
        return isOutput() ? redstonePower : 0;
    }

    private void notifyRedstoneUpdate() {
        var world = getLevel();
        var pos = getBlockEntity().getBlockPos();
        if (world != null) {
            Platform.notifyBlocksOfNeighbors(world, pos);
            Platform.notifyBlocksOfNeighbors(world, pos.relative(getSide()));
        }
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) return true;
        if (hand == InteractionHand.OFF_HAND) return false;

        var is = player.getItemInHand(hand);
        if (!is.isEmpty() && is.getItem() instanceof IMemoryCard mc) {
            var configData = mc.getData(is);
            if (configData.contains("p2pType") || configData.contains("p2pFreq") || !configData.contains("wormhole")) {
                mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                return false;
            } else {
                this.importSettings(SettingsFrom.MEMORY_CARD, configData, player);
                mc.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                return true;
            }
        }

        var level = getLevel();
        if (!(player instanceof ServerPlayer sp) || level == null) return false;

        BlockPos targetPos = null;
        Direction hitFace = null;
        ServerLevel targetWorld = null;

        if (isOutput()) {
            var input = getInput();
            if (input != null && input.getHost() != null) {
                var remoteHost = input.getHost().getBlockEntity();
                targetPos = remoteHost.getBlockPos().relative(input.getSide());
                hitFace = input.getSide().getOpposite();
                targetWorld = (ServerLevel) remoteHost.getLevel();
            }
        } else {
            var outs = getOutputs();
            if (!outs.isEmpty()) {
                var out = outs.iterator().next();
                var remoteHost = out.getHost().getBlockEntity();
                targetPos = remoteHost.getBlockPos().relative(out.getSide());
                hitFace = out.getSide().getOpposite();
                targetWorld = (ServerLevel) remoteHost.getLevel();
            }
        }

        if (targetPos == null || targetWorld == null) return false;

        long chunkKey = new ChunkPos(targetPos).toLong();
        if (!targetWorld.getChunkSource().isPositionTicking(chunkKey)) return false;

        if (is.is(Items.ENDER_PEARL) && CrazyConfig.COMMON.P2PWormholeTeleportation.get()){
            is.shrink(1);
            sp.teleportTo(targetWorld, targetPos.getX() + 0.5D, targetPos.getY() + 0.1D, targetPos.getZ() + 0.5D, hitFace.getOpposite().toYRot(), sp.getXRot());
            return true;
        }

        var state = targetWorld.getBlockState(targetPos);
        var hit = new BlockHitResult(Vec3.atCenterOf(targetPos), hitFace, targetPos, false);

        var provider = state.getMenuProvider(targetWorld, targetPos);
        if (provider != null && targetWorld != getLevel()) return false;

        WormholeAnchor.set(sp, targetPos, targetWorld);
        var containerBefore = sp.containerMenu;

        var result = state.use(targetWorld, sp, hand, hit);

        if (sp.containerMenu == containerBefore) {
            WormholeAnchor.clear(sp);
        }

        return result.consumesAction();
    }


    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (input.contains("myFreq")) {
            var freq = input.getShort("myFreq");
            var grid = getMainNode().getGrid();
            
            if (grid != null){
                ((P2PTunnelPartAccessor)this).setOutput(true);
                P2PService.get(grid).updateFreq(this, freq);
                sendBlockUpdateToOppositeSide();
            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            if (!output.getAllKeys().isEmpty()) {
                var iterator = output.getAllKeys().iterator();
                while (iterator.hasNext()){
                    iterator.next();
                    iterator.remove();
                }
            };
            output.putString("myType", IPartItem.getId(getPartItem()).toString());
            output.putBoolean("wormhole", true);

            if (getFrequency() != 0) {
                output.putShort("myFreq", getFrequency());

                var colors = Platform.p2p().toColors(getFrequency());
                var colorCode = new int[] { colors[0].ordinal(), colors[0].ordinal(), colors[1].ordinal(),
                        colors[1].ordinal(), colors[2].ordinal(), colors[2].ordinal(), colors[3].ordinal(),
                        colors[3].ordinal(), };
                output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
            }
        }
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
        if (!this.isOutput() || !connections.isEmpty()) {
            getMainNode().ifPresent((grid, node) -> {
                grid.getTickManager().wakeDevice(node);
            });
        }
        sendBlockUpdateToOppositeSide();
    }

    private void sendBlockUpdateToOppositeSide() {
        var world = getLevel();
        if (world == null || world.isClientSide) return;

        if (isOutput()) {
            var input = getInput();
            if (input != null && input.getHost() != null) {
                var be = input.getHost().getBlockEntity();
                var pos = be.getBlockPos().relative(input.getSide());
                sendNeighborUpdatesAt(be.getLevel(), pos, input.getSide());
            }
        } else {
            for (var out : getOutputs()) {
                if (out.getHost() != null) {
                    var be = out.getHost().getBlockEntity();
                    var pos = be.getBlockPos().relative(out.getSide());
                    sendNeighborUpdatesAt(be.getLevel(), pos, out.getSide());
                }
            }
        }
    }

    private void sendNeighborUpdatesAt(Level level, BlockPos pos, Direction facing) {
        if (level == null || level.isClientSide) return;
        if (wormholeUpdateBlacklist.contains(pos)) return;

        wormholeUpdateBlacklist.add(pos);
        TickHandler.instance().addCallable(level, wormholeUpdateBlacklist::clear);

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
        level.updateNeighborsAt(pos, state.getBlock());

        var neighbor = pos.relative(facing.getOpposite());
        BlockState neighborState = level.getBlockState(neighbor);
        level.updateNeighborsAt(neighbor, neighborState.getBlock());
    }

    @Override
    public void onNeighborChanged(BlockGetter level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChanged(level, pos, neighbor);
        sendBlockUpdateToOppositeSide();
        if (!isOutput()) {
            readRedstoneInput();
        }
    }

    @Override
    public AECableType getExternalCableConnectionType() {
        return AECableType.DENSE_SMART;
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.outerNode.destroy();
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
        if (!node.isOnline()) {
            pendingUpdate = ConnectionUpdate.DISCONNECT;
        } else {
            pendingUpdate = ConnectionUpdate.CONNECT;
        }

        TickHandler.instance().addCallable(getLevel(), this::updateConnections);
        return TickRateModulation.SLEEP;
    }

    private void updateConnections() {
        var operation = pendingUpdate;
        pendingUpdate = ConnectionUpdate.NONE;

        var mainGrid = getMainNode().getGrid();

        if (isOutput()) {
            operation = ConnectionUpdate.DISCONNECT;
        } else if (mainGrid == null) {
            operation = ConnectionUpdate.DISCONNECT;
        }

        if (operation == ConnectionUpdate.DISCONNECT) {
            for (var cw : connections.values()) {
                cw.destroy();
            }
            connections.clear();
        } else if (operation == ConnectionUpdate.CONNECT) {
            var outputs = getOutputs();

            Iterator<Map.Entry<WormholeP2PTunnelPart, IGridConnection>> it = connections.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<WormholeP2PTunnelPart, IGridConnection> entry = it.next();
                WormholeP2PTunnelPart output = entry.getKey();
                var connection = entry.getValue();

                if (output.getMainNode().getGrid() != mainGrid
                        || !output.getMainNode().isOnline()
                        || !outputs.contains(output)) {
                    connection.destroy();
                    it.remove();
                }
            }

            for (var output : outputs) {
                if (!output.getMainNode().isOnline() || connections.containsKey(output)) {
                    continue;
                }

                var connection = GridHelper.createConnection(getExternalFacingNode(),
                        output.getExternalFacingNode());
                connections.put(output, connection);
            }
        }
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!isActive()) return LazyOptional.empty();
        var world = getLevel();
        if (world == null) return LazyOptional.empty();

        if (isOutput()) {
            var input = getInput();
            if (input == null) return LazyOptional.empty();

            var remoteHost = input.getHost().getBlockEntity();
            var targetPos = remoteHost.getBlockPos().relative(input.getSide());
            var targetBE = remoteHost.getLevel().getBlockEntity(targetPos);
            if (targetBE == null) return LazyOptional.empty();

            return targetBE.getCapability(cap, input.getSide().getOpposite());
        }

        var outputs = getOutputs();
        if (outputs.isEmpty()) return LazyOptional.empty();

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            List<IItemHandlerModifiable> handlers = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.ITEM_HANDLER, output.getSide().getOpposite());
                    opt.ifPresent(handler -> {
                        if (handler instanceof IItemHandlerModifiable modifiable) {
                            handlers.add(modifiable);
                        }
                    });
                }
            }
            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedInvWrapper(handlers.toArray(new IItemHandlerModifiable[0])));
            }
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            List<IFluidHandler> handlers = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.FLUID_HANDLER, output.getSide().getOpposite());
                    opt.ifPresent(handlers::add);
                }
            }
            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new FluidHandlerConcatenate(handlers));
            }
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
            List<IFluidHandlerItem> handlers = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM, output.getSide().getOpposite());
                    opt.ifPresent(handlers::add);
                }
            }
            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedFluidHandlerItem(handlers));
            }
        }

        if (cap == ForgeCapabilities.ENERGY) {
            List<IEnergyStorage> storages = new ArrayList<>();
            for (var output : outputs) {
                var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
                if (be != null) {
                    var opt = be.getCapability(ForgeCapabilities.ENERGY, output.getSide().getOpposite());
                    opt.ifPresent(storages::add);
                }
            }
            if (!storages.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedEnergyStorage(storages));
            }
        }

        for (var output : outputs) {
            var be = world.getBlockEntity(output.getHost().getBlockEntity().getBlockPos().relative(output.getSide()));
            if (be != null) {
                var result = be.getCapability(cap, output.getSide().getOpposite());
                if (result.isPresent()) {
                    return result;
                }
            }
        }

        return LazyOptional.empty();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> capabilityClass) {
        return getCapability(capabilityClass, getSide());
    }

    private enum ConnectionUpdate {
        NONE,
        DISCONNECT,
        CONNECT
    }
}
