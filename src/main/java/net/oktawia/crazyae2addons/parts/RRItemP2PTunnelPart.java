package net.oktawia.crazyae2addons.parts;

import appeng.api.config.Actionable;
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
import appeng.parts.PartModel;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.util.InteractionUtil;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentMap.Builder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addons.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;

public class RRItemP2PTunnelPart extends CapabilityP2PTunnelPart<RRItemP2PTunnelPart, IItemHandler> {

    private static final ResourceLocation MODEL_STATUS_OFF = AppEng.makeId("part/p2p/p2p_tunnel_status_off");
    private static final ResourceLocation MODEL_STATUS_ON = AppEng.makeId("part/p2p/p2p_tunnel_status_on");
    private static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = AppEng.makeId("part/p2p/p2p_tunnel_status_has_channel");
    private static final ResourceLocation MODEL_FREQUENCY = AppEng.makeId("part/p2p/p2p_tunnel_frequency");
    private static final ResourceLocation FRONT_MODEL = AppEng.makeId("part/p2p/round_robin_item_p2p_tunnel");
    private static final IPartModel MODELS_OFF = new PartModel(MODEL_STATUS_OFF, MODEL_FREQUENCY, FRONT_MODEL);
    private static final IPartModel MODELS_ON = new PartModel(MODEL_STATUS_ON, MODEL_FREQUENCY, FRONT_MODEL);
    private static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_STATUS_HAS_CHANNEL, MODEL_FREQUENCY, FRONT_MODEL);
    private static final IItemHandler NULL_ITEM_HANDLER = new NullItemHandler();
    private int containerIndex;

    @PartModels
    public static List<IPartModel> getModels() {
        return List.of(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public RRItemP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, Capabilities.ItemHandler.BLOCK);
        inputHandler = new InputItemHandler();
        outputHandler = new OutputItemHandler();
        emptyHandler = NULL_ITEM_HANDLER;
        containerIndex = 0;
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
    public boolean onUseItemOn(ItemStack heldItem, Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide() || hand == InteractionHand.OFF_HAND) {
            return false;
        }

        if (heldItem.getItem() instanceof IMemoryCard mc) {
            if (InteractionUtil.isInAlternateUseMode(player)) {
                heldItem.get(AEComponents.EXPORTED_P2P_FREQUENCY);
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
                    || !heldItem.has(CrazyDataComponents.RR_ITEM_P2P_TYPE)) {
                mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                return false;
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
            output.set(CrazyDataComponents.RR_ITEM_P2P_TYPE, getPartItem().asItem().getDescription().getString());
            if (getFrequency() != 0) {
                output.set(AEComponents.EXPORTED_P2P_FREQUENCY, getFrequency());
                var colors = Platform.p2p().toColors(getFrequency());
                output.set(AEComponents.MEMORY_CARD_COLORS,
                        new MemoryCardColors(colors[0], colors[0], colors[1], colors[1],
                                colors[2], colors[2], colors[3], colors[3]));
            }
        }
    }

    @Override
    public void readFromNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.readFromNBT(data, registries);
        data.putInt("containerIndex", containerIndex);
    }

    @Override
    public void writeToNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        data.putInt("containerIndex", containerIndex);
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        int oldIndex = containerIndex;
        containerIndex = data.readInt();
        return changed || oldIndex != containerIndex;
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeInt(containerIndex);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1.0f;
    }

    private class InputItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            int remainder = stack.getCount();

            final int outputTunnels = RRItemP2PTunnelPart.this.getOutputs().size();
            final int amount = stack.getCount();

            if (outputTunnels == 0 || amount == 0) {
                return stack;
            }

            final int amountPerOutput = amount / outputTunnels;
            int overflow = amountPerOutput == 0 ? amount : amount % amountPerOutput;

            List<RRItemP2PTunnelPart> outputs = Utils.rotate(RRItemP2PTunnelPart.this.getOutputs(), containerIndex);

            for (RRItemP2PTunnelPart target : outputs) {
                try (CapabilityGuard capabilityGuard = target.getAdjacentCapability()) {
                    final IItemHandler output = capabilityGuard.get();
                    final int toSend = amountPerOutput + overflow;

                    if (toSend <= 0) {
                        break;
                    }

                    ItemStack stackCopy = stack.copy();
                    stackCopy.setCount(toSend);
                    final int sent = toSend - ItemHandlerHelper.insertItem(output, stackCopy, simulate).getCount();

                    overflow = toSend - sent;
                    remainder -= sent;
                }
            }

            if (!simulate) {
                deductTransportCost(amount - remainder, AEKeyType.items());
                containerIndex += 1;
                if (containerIndex >= outputTunnels) {
                    containerIndex = 0;
                }
            }

            if (remainder == stack.getCount()) {
                return stack;
            } else if (remainder == 0) {
                return ItemStack.EMPTY;
            } else {
                ItemStack copy = stack.copy();
                copy.setCount(remainder);
                return copy;
            }
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return true;
        }
    }

    private class OutputItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getSlots();
            }
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getStackInSlot(slot);
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            try (CapabilityGuard input = getInputCapability()) {
                ItemStack result = input.get().extractItem(slot, amount, simulate);

                if (!simulate) {
                    deductTransportCost(result.getCount(), AEKeyType.items());
                }

                return result;
            }
        }

        @Override
        public int getSlotLimit(int slot) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().getSlotLimit(slot);
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            try (CapabilityGuard input = getInputCapability()) {
                return input.get().isItemValid(slot, stack);
            }
        }
    }

    private static class NullItemHandler implements IItemHandler {

        @Override
        public int getSlots() {
            return 0;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 0;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
