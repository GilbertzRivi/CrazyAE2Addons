package net.oktawia.crazyae2addons.recipes;

import net.minecraft.world.item.crafting.RecipeType;
import net.oktawia.crazyae2addons.CrazyAddons;

public class CradleRecipeType implements RecipeType<CradleRecipe> {
    public static final CradleRecipeType INSTANCE = new CradleRecipeType();
    private CradleRecipeType() {}
    @Override public String toString() { return CrazyAddons.makeId("cradle").toString(); }
}
