package net.oktawia.crazyae2addonslite.parts;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.me.service.P2PService;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.parts.p2p.P2PModels;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.oktawia.crazyae2addonslite.Utils;
import net.oktawia.crazyae2addonslite.mixins.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class RRItemP2PTunnelPart extends CapabilityP2PTunnelPart<RRItemP2PTunnelPart, IItemHandler> {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_items"));
    private static final IItemHandler NULL_ITEM_HANDLER = new NullItemHandler();
    private int ContainerIndex;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public RRItemP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, ForgeCapabilities.ITEM_HANDLER);
        inputHandler = new InputItemHandler();
        outputHandler = new OutputItemHandler();
        emptyHandler = NULL_ITEM_HANDLER;
        ContainerIndex = 0;
    }


    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) {
            return true;
        }

        if (hand == InteractionHand.OFF_HAND) {
            return false;
        }

        var is = player.getItemInHand(hand);

        if (!is.isEmpty() && is.getItem() instanceof IMemoryCard mc) {
            var configData = mc.getData(is);
            if (configData.contains("p2pType") || configData.contains("p2pFreq") || !configData.contains("RRItem")) {
                mc.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
                return false;
            } else {
                this.importSettings(SettingsFrom.MEMORY_CARD, configData, player);
                mc.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
                return true;
            }
        }
        return false;
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (input.contains("myFreq")) {
            var freq = input.getShort("myFreq");

            ((P2PTunnelPartAccessor)this).setOutput(true);
            var grid = getMainNode().getGrid();
            if (grid != null) {
                P2PService.get(grid).updateFreq(this, freq);
            } else {
                setFrequency(freq);
                onTunnelNetworkChange();
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
            output.putBoolean("RRItem", true);
            output.putShort("myFreq", getFrequency());

            var colors = Platform.p2p().toColors(getFrequency());
            var colorCode = new int[] { colors[0].ordinal(), colors[0].ordinal(), colors[1].ordinal(),
                    colors[1].ordinal(), colors[2].ordinal(), colors[2].ordinal(), colors[3].ordinal(),
                    colors[3].ordinal(), };
            output.putIntArray(IMemoryCard.NBT_COLOR_CODE, colorCode);
        }
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
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

            List<RRItemP2PTunnelPart> outputs = Utils.rotate(RRItemP2PTunnelPart.this.getOutputs(), ContainerIndex);

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
                ContainerIndex += 1;
                if (ContainerIndex >= outputTunnels){
                    ContainerIndex = 0;
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