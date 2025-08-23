package net.oktawia.crazyae2addons.recipes;

import net.minecraft.world.item.crafting.RecipeType;
import net.oktawia.crazyae2addons.CrazyAddons;

public class ResearchRecipeType implements RecipeType<ResearchRecipe> {
    public static final ResearchRecipeType INSTANCE = new ResearchRecipeType();
    private ResearchRecipeType() {}
    @Override public String toString() { return CrazyAddons.makeId("research").toString(); }
}
