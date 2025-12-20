package net.oktawia.crazyae2addons.misc;

import appeng.api.crafting.IPatternDetails;
import appeng.api.crafting.IPatternDetails.IInput;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class PatternDetailsSerializer {

    public static CompoundTag serialize(IPatternDetails details) {
        CompoundTag root = new CompoundTag();
        root.put("def", details.getDefinition().toTag());

        ListTag inputsList = new ListTag();
        for (IInput input : details.getInputs()) {
            ListTag inputTag = new ListTag();
            for (GenericStack stack : input.getPossibleInputs()) {
                inputTag.add(GenericStack.writeTag(stack));
            }
            inputsList.add(inputTag);
        }
        root.put("input", inputsList);

        ListTag outputsList = new ListTag();
        for (GenericStack stack : details.getOutputs()) {
            outputsList.add(GenericStack.writeTag(stack));
        }
        root.put("output", outputsList);

        root.putBoolean("push", details.supportsPushInputsToExternalInventory());
        return root;
    }

    public static IPatternDetails deserialize(CompoundTag tag) {
        AEItemKey def = AEItemKey.fromTag(tag.getCompound("def"));

        ListTag inputsList = tag.getList("input", Tag.TAG_LIST);
        IInput[] inputs = new IInput[inputsList.size()];
        for (int i = 0; i < inputsList.size(); i++) {
            Tag entry = inputsList.get(i);
            if (!(entry instanceof ListTag)) continue;
            ListTag stacksTag = (ListTag) entry;

            List<GenericStack> stacks = new ArrayList<>();
            for (int j = 0; j < stacksTag.size(); j++) {
                stacks.add(GenericStack.readTag(stacksTag.getCompound(j)));
            }
            inputs[i] = new DeserializedInput(stacks.toArray(new GenericStack[0]));
        }

        ListTag outputsTag = tag.getList("output", Tag.TAG_COMPOUND);
        List<GenericStack> outStacks = new ArrayList<>();
        for (int i = 0; i < outputsTag.size(); i++) {
            outStacks.add(GenericStack.readTag(outputsTag.getCompound(i)));
        }
        GenericStack[] outputs = outStacks.toArray(new GenericStack[0]);

        boolean push = tag.getBoolean("push");
        return new PatternDetails(def, inputs, outputs, push);
    }

    private static class DeserializedInput implements IPatternDetails.IInput {
        private final GenericStack[] possible;

        DeserializedInput(GenericStack[] possible) {
            this.possible = possible;
        }

        @Override public GenericStack[] getPossibleInputs() {
            return possible;
        }

        @Override public long getMultiplier() {
            return 1;
        }

        @Override
        public boolean isValid(AEKey input, Level level) {
            for (GenericStack stack : possible) {
                if (stack.what().equals(input)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public AEKey getRemainingKey(AEKey template) {
            return template;
        }
    }

    public static class PatternDetails implements IPatternDetails {
        private final AEItemKey definition;
        private final IInput[] inputs;
        private final GenericStack[] outputs;
        private final boolean supportsPush;

        PatternDetails(AEItemKey definition, IInput[] inputs, GenericStack[] outputs, boolean supportsPush) {
            this.definition = definition;
            this.inputs = inputs;
            this.outputs = outputs;
            this.supportsPush = supportsPush;
        }

        @Override public AEItemKey getDefinition()                     { return definition; }
        @Override public IInput[] getInputs()                          { return inputs; }
        @Override public GenericStack[] getOutputs()                   { return outputs; }

        @Override public boolean supportsPushInputsToExternalInventory() { return supportsPush; }

    }
}
