package net.oktawia.crazyae2addons.recipes;

import lombok.Getter;
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
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
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
    @Getter
    private final List<Entry> inputs;
    @Getter
    private final int inputCount;

    private final ItemStack output;
    private final @Nullable String requiredKey;

    private final FluidStack fluidInput;
    private final FluidStack fluidOutput;

    public FabricationRecipe(
            ResourceLocation id,
            List<Entry> inputs,
            ItemStack output,
            @Nullable String requiredKey
    ) {
        this(id, inputs, output, requiredKey, FluidStack.EMPTY, FluidStack.EMPTY);
    }

    public FabricationRecipe(
            ResourceLocation id,
            List<Entry> inputs,
            ItemStack output,
            @Nullable String requiredKey,
            FluidStack fluidInput,
            FluidStack fluidOutput
    ) {
        this.id = id;
        this.inputs = Collections.unmodifiableList(new ArrayList<>(inputs));

        int total = 0;
        for (Entry entry : inputs) {
            total += entry.count();
        }
        this.inputCount = Math.max(1, total);

        this.output = output == null ? ItemStack.EMPTY : output.copy();
        this.requiredKey = requiredKey;

        this.fluidInput = fluidInput == null ? FluidStack.EMPTY : fluidInput.copy();
        this.fluidOutput = fluidOutput == null ? FluidStack.EMPTY : fluidOutput.copy();
    }

    public ItemStack getOutput() {
        return this.output.copy();
    }

    public @Nullable String getRequiredKey() {
        return this.requiredKey;
    }

    public FluidStack getFluidInput() {
        return this.fluidInput.copy();
    }

    public FluidStack getFluidOutput() {
        return this.fluidOutput.copy();
    }

    public boolean hasItemOutput() {
        return !this.output.isEmpty();
    }

    public boolean hasFluidInput() {
        return !this.fluidInput.isEmpty();
    }

    public boolean hasFluidOutput() {
        return !this.fluidOutput.isEmpty();
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        if (this.inputs.isEmpty()) {
            return false;
        }

        int[] remaining = new int[container.getContainerSize()];
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            remaining[i] = stack.isEmpty() ? 0 : stack.getCount();
        }

        for (Entry entry : this.inputs) {
            int needed = entry.count();

            for (int i = 0; i < container.getContainerSize() && needed > 0; i++) {
                if (remaining[i] <= 0) {
                    continue;
                }

                ItemStack stack = container.getItem(i);
                if (!stack.isEmpty() && entry.ingredient().test(stack)) {
                    int taken = Math.min(remaining[i], needed);
                    remaining[i] -= taken;
                    needed -= taken;
                }
            }

            if (needed > 0) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return this.output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return this.output.copy();
    }

    @Override
    public ResourceLocation getId() {
        return this.id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return CrazyRecipes.FABRICATION_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return CrazyRecipes.FABRICATION_TYPE.get();
    }
}