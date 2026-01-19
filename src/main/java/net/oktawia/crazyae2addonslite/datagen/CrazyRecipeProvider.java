package net.oktawia.crazyae2addonslite.datagen;

import appeng.core.definitions.AEBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.conditions.IConditionBuilder;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.defs.BlockDefs;
import net.oktawia.crazyae2addonslite.defs.ItemDefs;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class CrazyRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public CrazyRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        ItemDefs.registerRecipes();
        BlockDefs.registerRecipes();

        for (var entry : BlockDefs.getBlockRecipes().entrySet()) {
            ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, entry.getKey());
            for (var row : entry.getValue().getKey().split("/")) {
                builder.pattern(row);
            }
            for (Map.Entry<String, Item> e : entry.getValue().getValue().entrySet()) {
                builder.define(e.getKey().charAt(0), e.getValue());
            }
            builder.unlockedBy(getHasName(AEBlocks.CONTROLLER.asItem()), has(AEBlocks.CONTROLLER.asItem()));
            builder.save(output);
        }

        for (var entry : ItemDefs.getItemRecipes().entrySet()) {
            int recipeIndex = 0;
            for (var recipeEntry : entry.getValue()) {
                ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, entry.getKey());
                for (var row : recipeEntry.getKey().split("/")) {
                    builder.pattern(row);
                }
                for (Map.Entry<String, Item> e : recipeEntry.getValue().entrySet()) {
                    builder.define(e.getKey().charAt(0), e.getValue());
                }
                builder.unlockedBy(getHasName(AEBlocks.CONTROLLER.asItem()), has(AEBlocks.CONTROLLER.asItem()));

                var itemKey = BuiltInRegistries.ITEM.getKey(entry.getKey());
                ResourceLocation recipeId = CrazyAddonslite.makeId(
                        itemKey.getPath() + (recipeIndex == 0 ? "" : "_alt" + recipeIndex)
                );

                builder.save(output, recipeId);
                recipeIndex++;
            }
        }
    }
}
