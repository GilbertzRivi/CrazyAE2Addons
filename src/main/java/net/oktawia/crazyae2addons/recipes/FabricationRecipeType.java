package net.oktawia.crazyae2addons.recipes;

import net.minecraft.world.item.crafting.RecipeType;

public final class FabricationRecipeType implements RecipeType<FabricationRecipe> {
    public static final FabricationRecipeType INSTANCE = new FabricationRecipeType();
    private FabricationRecipeType() {}
    @Override public String toString() { return "crazyae2addons:fabrication"; }
}
