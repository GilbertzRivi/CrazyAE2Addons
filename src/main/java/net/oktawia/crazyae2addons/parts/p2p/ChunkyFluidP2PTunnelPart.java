package net.oktawia.crazyae2addons.parts.p2p;

import appeng.api.ids.AEComponents;
import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardColors;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.api.util.AECableType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.items.tools.MemoryCardItem;
import appeng.me.service.P2PService;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.part.ChunkyFluidP2PTunnelMenu;
import net.oktawia.crazyae2addons.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ChunkyFluidP2PTunnelPart extends CapabilityP2PTunnelPart<ChunkyFluidP2PTunnelPart, IFluidHandler> implements MenuProvider {

    private static final ResourceLocation MODEL_STATUS_OFF = AppEng.makeId("part/p2p/p2p_tunnel_status_off");
    private static final ResourceLocation MODEL_STATUS_ON = AppEng.makeId("part/p2p/p2p_tunnel_status_on");
    private static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = AppEng.makeId("part/p2p/p2p_tunnel_status_has_channel");
    private static final ResourceLocation MODEL_FREQUENCY = AppEng.makeId("part/p2p/p2p_tunnel_frequency");
    private static final ResourceLocation FRONT_MODEL = AppEng.makeId("part/p2p/chunky_fluid_p2p_tunnel");

    private static final IPartModel MODELS_OFF = new PartModel(MODEL_STATUS_OFF, MODEL_FREQUENCY, FRONT_MODEL);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_STATUS_ON, MODEL_FREQUENCY, FRONT_MODEL);
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_STATUS_HAS_CHANNEL, MODEL_FREQUENCY, FRONT_MODEL);

    private static final IFluidHandler NULL_FLUID_HANDLER = new NullFluidHandler();

    private final PartState persisted = new PartState();

    @PartModels
    public static List<IPartModel> getModels() {
        return List.of(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public ChunkyFluidP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, Capabilities.FluidHandler.BLOCK);
        inputHandler = new InputFluidHandler();
        outputHandler = new OutputFluidHandler();
        emptyHandler = NULL_FLUID_HANDLER;
    }

    public int getUnitSize() {
        return persisted.unitSize;
    }

    public void setUnitSize(int unitSize) {
        persisted.unitSize = unitSize;
    }

    @Override
    public IPartModel getStaticModels() {
        if (isPowered() && isActive()) {
            return MODELS_HAS_CHANNEL;
        }
        if (isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ChunkyFluidP2PTunnelMenu(containerId, playerInventory, this);
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(
                    CrazyMenuRegistrar.CHUNKY_FLUID_P2P_TUNNEL_MENU.get(),
                    player,
                    MenuLocators.forPart(this)
            );
            return true;
        }
        return true;
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide() || hand == InteractionHand.OFF_HAND) {
            return false;
        }

        if (heldItem.getItem() instanceof IMemoryCard mc) {
            if (InteractionUtil.isInAlternateUseMode(player)) {
                short newFreq = getFrequency();
                boolean wasOutput = isOutput();

                ((P2PTunnelPartAccessor) this).setOutputField(false);

                boolean needsNewFrequency = wasOutput || newFreq == 0;
                IGrid grid = getMainNode().getGrid();
                if (grid != null) {
                    P2PService p2p = P2PService.get(grid);
                    if (needsNewFrequency) {
                        newFreq = p2p.newFrequency();
                    }
                    p2p.updateFreq(this, newFreq);
                }

                onTunnelConfigChange();

                MemoryCardItem.clearCard(heldItem);
                heldItem.set(AEComponents.EXPORTED_SETTINGS_SOURCE, getPartItem().asItem().getDescription());
                heldItem.applyComponents(exportSettings(SettingsFrom.MEMORY_CARD));

                if (needsNewFrequency) {
                    mc.notifyUser(player, MemoryCardMessages.SETTINGS_RESET);
                } else {
                    mc.notifyUser(player, MemoryCardMessages.SETTINGS_SAVED);
                }
                return true;
            }

            if (heldItem.get(AEComponents.EXPORTED_P2P_TYPE) != null
                    || !heldItem.has(CrazyDataComponents.CHUNKY_FLUID_P2P_TYPE)) {
                mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                return true;
            }

            importSettings(SettingsFrom.MEMORY_CARD, heldItem.getComponents(), player);
            mc.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
            return true;
        }

        return false;
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            var freq = input.get(AEComponents.EXPORTED_P2P_FREQUENCY);
            if (freq instanceof Short frequency) {
                ((P2PTunnelPartAccessor) this).setOutputField(true);
                var grid = getMainNode().getGrid();
                if (grid != null) {
                    P2PService.get(grid).updateFreq(this, frequency);
                } else {
                    setFrequency(frequency);
                    onTunnelNetworkChange();
                }
            }
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, Builder output) {
        if (mode == SettingsFrom.MEMORY_CARD) {
            output.set(CrazyDataComponents.CHUNKY_FLUID_P2P_TYPE, getPartItem().asItem().getDescription().getString());

            if (getFrequency() != 0) {
                output.set(AEComponents.EXPORTED_P2P_FREQUENCY, getFrequency());
                var colors = Platform.p2p().toColors(getFrequency());
                output.set(
                        AEComponents.MEMORY_CARD_COLORS,
                        new MemoryCardColors(
                                colors[0], colors[0],
                                colors[1], colors[1],
                                colors[2], colors[2],
                                colors[3], colors[3]
                        )
                );
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.readFromNBT(data, registries);

        if (data.contains("crazy_state", Tag.TAG_COMPOUND)) {
            persisted.deserializeNBT(registries, data.getCompound("crazy_state"));
        }
    }

    @Override
    public void writeToNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        data.put("crazy_state", persisted.serializeNBT(registries));
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);

        int oldContainerIndex = persisted.containerIndex;
        int oldUnitSize = persisted.unitSize;

        persisted.readFromBuff(data);

        return changed
                || oldContainerIndex != persisted.containerIndex
                || oldUnitSize != persisted.unitSize;
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        persisted.writeToBuff(data);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1.0f;
    }

    private final class InputFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return true;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            int total = 0;
            final int outputTunnels = ChunkyFluidP2PTunnelPart.this.getOutputs().size();
            final int amount = resource.getAmount();
            final int unit = persisted.unitSize;

            if (outputTunnels == 0 || amount < unit) {
                return 0;
            }

            int availableUnits = amount / unit;
            final int unitsPerOutput = availableUnits / outputTunnels;
            int overflowUnits = unitsPerOutput == 0 ? availableUnits : availableUnits % unitsPerOutput;

            List<ChunkyFluidP2PTunnelPart> outputs =
                    Utils.rotate(ChunkyFluidP2PTunnelPart.this.getOutputs(), persisted.containerIndex);

            for (ChunkyFluidP2PTunnelPart target : outputs) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IFluidHandler output = capabilityGuard.get();
                    final int toSendUnits = unitsPerOutput + overflowUnits;
                    if (toSendUnits <= 0) {
                        break;
                    }

                    FluidStack fillStack = resource.copy();
                    fillStack.setAmount(toSendUnits * unit);

                    final int received = output.fill(fillStack, action);
                    int transferredUnits = received / unit;
                    overflowUnits = toSendUnits - transferredUnits;
                    total += received;
                }
            }

            if (action == FluidAction.EXECUTE) {
                deductTransportCost(total, AEKeyType.fluids());
                persisted.containerIndex++;
                if (persisted.containerIndex >= outputTunnels) {
                    persisted.containerIndex = 0;
                }
            }

            return total;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class OutputFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getTanks();
            }
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getFluidInTank(tank);
            }
        }

        @Override
        public int getTankCapacity(int tank) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getTankCapacity(tank);
            }
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().isFluidValid(tank, stack);
            }
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            try (CapabilityGuard input = getInputCapability()) {
                FluidStack result = input.get().drain(resource, action);

                if (action.execute()) {
                    deductTransportCost(result.getAmount(), AEKeyType.fluids());
                }

                return result;
            }
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            try (CapabilityGuard input = getInputCapability()) {
                FluidStack result = input.get().drain(maxDrain, action);

                if (action.execute()) {
                    deductTransportCost(result.getAmount(), AEKeyType.fluids());
                }

                return result;
            }
        }
    }

    private static final class NullFluidHandler implements IFluidHandler {

        @Override
        public int getTanks() {
            return 0;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private static final class PartState implements IPersistedSerializable {
        @Persisted
        private int containerIndex = 0;

        @Persisted
        private int unitSize = 1000;
    }
}