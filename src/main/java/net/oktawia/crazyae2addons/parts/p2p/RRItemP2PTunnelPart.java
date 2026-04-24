package net.oktawia.crazyae2addons.parts.p2p;

import appeng.api.implementations.items.IMemoryCard;
import appeng.api.implementations.items.MemoryCardMessages;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKeyType;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.me.service.P2PService;
import appeng.parts.p2p.CapabilityP2PTunnelPart;
import appeng.parts.p2p.P2PModels;
import appeng.util.Platform;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.oktawia.crazyae2addons.mixins.accessors.P2PTunnelPartAccessor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RRItemP2PTunnelPart extends CapabilityP2PTunnelPart<RRItemP2PTunnelPart, IItemHandler> {

    private static final P2PModels MODELS = new P2PModels(AppEng.makeId("part/p2p/p2p_tunnel_items"));
    private static final IItemHandler NULL_ITEM_HANDLER = new NullItemHandler();

    private int containerIndex = 0;

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public RRItemP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem, ForgeCapabilities.ITEM_HANDLER);
        this.inputHandler = new InputItemHandler();
        this.outputHandler = new OutputItemHandler();
        this.emptyHandler = NULL_ITEM_HANDLER;
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (isClientSide()) {
            return true;
        }

        if (hand == InteractionHand.OFF_HAND) {
            return false;
        }

        var heldItem = player.getItemInHand(hand);
        if (heldItem.isEmpty() || !(heldItem.getItem() instanceof IMemoryCard memoryCard)) {
            return false;
        }

        var data = memoryCard.getData(heldItem);
        if (isInvalidMemoryCardData(data, "RRItem")) {
            memoryCard.notifyUser(player, MemoryCardMessages.INVALID_MACHINE);
            return false;
        }

        importSettings(SettingsFrom.MEMORY_CARD, data, player);
        memoryCard.notifyUser(player, MemoryCardMessages.SETTINGS_LOADED);
        return true;
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (!input.contains("myFreq")) {
            return;
        }

        short freq = input.getShort("myFreq");
        ((P2PTunnelPartAccessor) this).setOutput(true);

        var grid = getMainNode().getGrid();
        if (grid != null) {
            P2PService.get(grid).updateFreq(this, freq);
        } else {
            setFrequency(freq);
            onTunnelNetworkChange();
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }

        clearTag(output);
        output.putString("myType", IPartItem.getId(getPartItem()).toString());
        output.putBoolean("RRItem", true);
        output.putShort("myFreq", getFrequency());
        output.putIntArray(IMemoryCard.NBT_COLOR_CODE, createColorCode());
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(isPowered(), isActive());
    }

    private boolean isInvalidMemoryCardData(CompoundTag data, String markerKey) {
        return data.contains("p2pType") || data.contains("p2pFreq") || !data.contains(markerKey);
    }

    private void clearTag(CompoundTag tag) {
        for (String key : List.copyOf(tag.getAllKeys())) {
            tag.remove(key);
        }
    }

    private int[] createColorCode() {
        var colors = Platform.p2p().toColors(getFrequency());
        return new int[] {
                colors[0].ordinal(), colors[0].ordinal(),
                colors[1].ordinal(), colors[1].ordinal(),
                colors[2].ordinal(), colors[2].ordinal(),
                colors[3].ordinal(), colors[3].ordinal()
        };
    }

    private List<RRItemP2PTunnelPart> getRotatedOutputs() {
        var outputs = new ArrayList<>(getOutputs());
        if (outputs.isEmpty()) {
            return outputs;
        }

        int startIndex = Math.floorMod(containerIndex, outputs.size());
        Collections.rotate(outputs, -startIndex);
        return outputs;
    }

    private void advanceContainerIndex(int outputCount) {
        if (outputCount <= 0) {
            containerIndex = 0;
            return;
        }

        containerIndex++;
        if (containerIndex >= outputCount) {
            containerIndex = 0;
        }
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
            int amount = stack.getCount();
            if (amount <= 0) {
                return stack;
            }

            var outputs = getRotatedOutputs();
            int outputCount = outputs.size();
            if (outputCount == 0) {
                return stack;
            }

            int basePerOutput = amount / outputCount;
            int remainderToDistribute = amount % outputCount;

            int totalSent = 0;
            int carry = 0;

            for (int i = 0; i < outputCount; i++) {
                int planned = basePerOutput + (i < remainderToDistribute ? 1 : 0) + carry;
                if (planned <= 0) {
                    continue;
                }

                var target = outputs.get(i);
                try (CapabilityGuard guard = target.getAdjacentCapability()) {
                    IItemHandler output = guard.get();

                    ItemStack toInsert = stack.copy();
                    toInsert.setCount(planned);

                    int sent = planned - ItemHandlerHelper.insertItem(output, toInsert, simulate).getCount();
                    totalSent += sent;
                    carry = planned - sent;
                }
            }

            if (!simulate && totalSent > 0) {
                deductTransportCost(totalSent, AEKeyType.items());
                advanceContainerIndex(outputCount);
            }

            if (totalSent <= 0) {
                return stack;
            }

            if (totalSent >= amount) {
                return ItemStack.EMPTY;
            }

            ItemStack remainder = stack.copy();
            remainder.setCount(amount - totalSent);
            return remainder;
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

                if (!simulate && !result.isEmpty()) {
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