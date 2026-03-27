package net.oktawia.crazyae2addons.recipes;

import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;

import java.util.Collections;
import java.util.List;

public class FabricationRecipe implements Recipe<SimpleContainer> {

    public record Entry(Ingredient ingredient, int count) {
        public Entry(Ingredient ingredient, int count) {
            this.ingredient = ingredient;
            this.count = Math.max(1, count);
        }
    }

    private final ResourceLocation id;
    private final List<Entry> inputs;
    private final int inputCount;

    private final ItemStack output;

    private final String requiredKey;

    private final FluidStack fluidInput;
    private final FluidStack fluidOutput;

    public FabricationRecipe(ResourceLocation id,
                             List<Entry> inputs,
                             ItemStack output,
                             String requiredKey) {
        this(id, inputs, output, requiredKey, FluidStack.EMPTY, FluidStack.EMPTY);
    }

    public FabricationRecipe(ResourceLocation id,
                             List<Entry> inputs,
                             ItemStack output,
                             String requiredKey,
                             FluidStack fluidInput,
                             FluidStack fluidOutput) {
        this.id = id;
        this.inputs = Collections.unmodifiableList(inputs);

        int total = 0;
        for (Entry e : inputs) total += e.count();
        this.inputCount = Math.max(1, total);

        this.output = output == null ? ItemStack.EMPTY : output;
        this.requiredKey = requiredKey;

        this.fluidInput = fluidInput == null ? FluidStack.EMPTY : fluidInput.copy();
        this.fluidOutput = fluidOutput == null ? FluidStack.EMPTY : fluidOutput.copy();
    }

    public List<Entry> getInputs() { return inputs; }
    public int getInputCount() { return inputCount; }

    public ItemStack getOutput() { return output; }
    public String getRequiredKey() { return requiredKey; }

    public FluidStack getFluidInput() { return fluidInput.copy(); }
    public FluidStack getFluidOutput() { return fluidOutput.copy(); }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (inputs.isEmpty()) return false;

        for (Entry entry : inputs) {
            int needed = entry.count();
            int found = 0;

            for (int i = 0; i < container.getContainerSize(); i++) {
                ItemStack st = container.getItem(i);
                if (st.isEmpty()) continue;

                if (entry.ingredient().test(st)) {
                    found += st.getCount();
                    if (found >= needed) break;
                }
            }

            if (found < needed) return false;
        }

        return true;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess ra) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess ra) {
        return output.copy();
    }

    @Override
    public ResourceLocation getId() { return id; }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CrazyRecipes.FABRICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CrazyRecipes.FABRICATION_TYPE.get();
    }
}
