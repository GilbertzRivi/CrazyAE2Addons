package net.oktawia.crazyae2addonslite.datagen;

import appeng.core.definitions.AEBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.defs.BlockDefs;
import net.oktawia.crazyae2addonslite.defs.ItemDefs;

import java.util.Map;
import java.util.function.Consumer;

public class CrazyRecipeProvider extends RecipeProvider implements IConditionBuilder {
    public CrazyRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> pWriter) {
        ItemDefs.registerRecipes();
        BlockDefs.registerRecipes();
        for (var entry : BlockDefs.getBlockRecipes().entrySet()){
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, entry.getKey());
            for (var recipe : entry.getValue().getKey().split("/")) {
                builder.pattern(recipe);
            }
            for (Map.Entry<String, Item> e : entry.getValue().getValue().entrySet()) {
                builder.define(e.getKey().charAt(0), e.getValue());
            }
            builder.unlockedBy(getHasName(AEBlocks.CONTROLLER.asItem()), has(AEBlocks.CONTROLLER.asItem()));
            builder.save(pWriter);
        }
        for (var entry : ItemDefs.getItemRecipes().entrySet()) {
            int recipeIndex = 0;
            for (var recipeEntry : entry.getValue()) {
                ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, entry.getKey());
                for (var recipe : recipeEntry.getKey().split("/")) {
                    builder.pattern(recipe);
                }
                for (Map.Entry<String, Item> e : recipeEntry.getValue().entrySet()) {
                    builder.define(e.getKey().charAt(0), e.getValue());
                }
                builder.unlockedBy(getHasName(AEBlocks.CONTROLLER.asItem()), has(AEBlocks.CONTROLLER.asItem()));

                ResourceLocation recipeId = CrazyAddons.makeId(
                        ForgeRegistries.ITEMS.getKey(entry.getKey()).getPath() + (recipeIndex == 0 ? "" : "_alt" + recipeIndex)
                );

                builder.save(pWriter, recipeId);
                recipeIndex++;
            }
        }
    }
}
