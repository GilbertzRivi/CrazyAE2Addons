package net.oktawia.crazyae2addons.logic.patternmultiplier;

import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.GenericStack;
import appeng.core.definitions.AEItems;
import appeng.crafting.pattern.AEProcessingPattern;
import appeng.crafting.pattern.EncodedPatternItem;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.util.inv.AppEngInternalInventory;
import lombok.experimental.UtilityClass;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;

@UtilityClass
public class PatternMultiplierLogic {

    public boolean applyToInterface(InterfaceLogicHost host, double multiplier, int limit) {
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            return false;
        }

        var storage = host.getStorage();
        boolean changed = false;

        for (int i = 0; i < storage.size(); i++) {
            var stack = storage.getStack(i);
            if (stack == null || !(stack.what() instanceof AEItemKey key)) {
                continue;
            }

            ItemStack original = key.toStack();
            ItemStack modified = modify(original, multiplier, limit, host.getBlockEntity().getLevel());

            if (!ItemStack.matches(original, modified)) {
                storage.setStack(i, GenericStack.fromItemStack(modified));
                changed = true;
            }
        }

        if (changed) {
            host.getBlockEntity().setChanged();
        }

        return changed;
    }

    public boolean applyToPatternProvider(PatternProviderLogicHost host, double multiplier, int limit) {
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            return false;
        }

        var inv = host.getTerminalPatternInventory();
        boolean changed = false;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack original = inv.getStackInSlot(i);
            ItemStack modified = modify(original, multiplier, limit, host.getBlockEntity().getLevel());

            if (!ItemStack.matches(original, modified)) {
                inv.setItemDirect(i, modified);
                changed = true;
            }
        }

        if (changed) {
            host.getLogic().updatePatterns();
            host.getBlockEntity().setChanged();
        }

        return changed;
    }

    public boolean applyToContainer(Container container, double multiplier, int limit, Level level) {
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            return false;
        }

        boolean changed = false;

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack original = container.getItem(i);
            ItemStack modified = modify(original, multiplier, limit, level);

            if (!ItemStack.matches(original, modified)) {
                container.setItem(i, modified);
                changed = true;
            }
        }

        if (changed) {
            container.setChanged();
        }

        return changed;
    }

    public boolean applyToInventory(AppEngInternalInventory inv, double multiplier, int limit, Level level) {
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            return false;
        }

        boolean changed = false;

        for (int i = 0; i < inv.size(); i++) {
            ItemStack original = inv.getStackInSlot(i);
            ItemStack modified = modify(original, multiplier, limit, level);

            if (!ItemStack.matches(original, modified)) {
                inv.setItemDirect(i, modified);
                changed = true;
            }
        }

        return changed;
    }

    public ItemStack modify(ItemStack stack, double multiplier, int limit, Level level) {
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            return stack;
        }

        if (!(stack.getItem() instanceof EncodedPatternItem patternItem)) {
            return stack;
        }

        var details = patternItem.decode(stack, level, false);
        if (!(details instanceof AEProcessingPattern processingPattern)) {
            return stack;
        }

        GenericStack[] inputs = processingPattern.getSparseInputs();
        GenericStack[] outputs = processingPattern.getOutputs();

        if (limit > 0) {
            int totalOutput = 0;

            for (GenericStack output : outputs) {
                if (output != null) {
                    totalOutput += (int) output.amount();
                }
            }

            if (totalOutput > 0) {
                double maxMultiplier = Math.floor((double) limit / totalOutput);
                if (maxMultiplier < multiplier) {
                    multiplier = maxMultiplier;
                }
            }
        }

        if (multiplier <= 0) {
            return stack;
        }

        if (multiplier < 1.0D) {
            double inverse = 1.0D / multiplier;
            int divisor = (int) Math.round(inverse);

            if (Math.abs(inverse - divisor) > 1.0E-9D) {
                return stack;
            }

            for (GenericStack input : inputs) {
                if (input == null) {
                    continue;
                }

                if (input.what() instanceof AEFluidKey) {
                    continue;
                }

                long amount = input.amount();
                if (amount % divisor != 0) {
                    return stack;
                }
            }

            for (GenericStack output : outputs) {
                if (output == null) {
                    continue;
                }

                if (output.what() instanceof AEFluidKey) {
                    continue;
                }

                long amount = output.amount();
                if (amount % divisor != 0) {
                    return stack;
                }
            }
        }

        GenericStack[] newInputs = new GenericStack[inputs.length];
        GenericStack[] newOutputs = new GenericStack[outputs.length];

        for (int i = 0; i < inputs.length; i++) {
            if (inputs[i] != null) {
                int amount = (int) Math.max(Math.floor(inputs[i].amount() * multiplier), 1);
                newInputs[i] = new GenericStack(inputs[i].what(), amount);
            }
        }

        for (int i = 0; i < outputs.length; i++) {
            if (outputs[i] != null) {
                int amount = (int) Math.max(Math.floor(outputs[i].amount() * multiplier), 1);
                newOutputs[i] = new GenericStack(outputs[i].what(), amount);
            }
        }

        ItemStack result = PatternDetailsHelper.encodeProcessingPattern(newInputs, newOutputs);
        return result.isEmpty() ? stack : result;
    }
}