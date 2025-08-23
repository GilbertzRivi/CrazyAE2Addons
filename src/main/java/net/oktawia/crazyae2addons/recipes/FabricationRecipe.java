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
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;

public class FabricationRecipe implements Recipe<SimpleContainer> {

    private final ResourceLocation id;
    final Ingredient input;
    final int inputCount;
    final ItemStack output;
    final String requiredKey;

    public FabricationRecipe(ResourceLocation id, Ingredient input, int inputCount,
                             ItemStack output, String requiredKey) {
        this.id = id;
        this.input = input;
        this.inputCount = Math.max(1, inputCount);
        this.output = output;
        this.requiredKey = requiredKey;
    }

    // === GETTERS ===
    public Ingredient getInput() { return input; }
    public int getInputCount() { return inputCount; }
    public ItemStack getOutput() { return output; }
    public String getRequiredKey() { return requiredKey; }

    // === Recipe API ===
    @Override
    public boolean matches(SimpleContainer container, Level level) {
        ItemStack st = container.getItem(0);
        return !st.isEmpty() && input.test(st);
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess ra) {
        return output.copy();
    }

    @Override
    public boolean canCraftInDimensions(int w, int h) { return true; }

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
