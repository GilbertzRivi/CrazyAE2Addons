package net.oktawia.crazyae2addons.datagen;

import appeng.core.definitions.AEBlocks;
import com.mojang.logging.LogUtils;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.BlockDefs;
import net.oktawia.crazyae2addons.defs.ItemDefs;

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
        int recipeIndex1 = 0;
        for (var entry : BlockDefs.getBlockRecipes().entrySet()){
            if (entry.getValue().getKey().trim().length() == 1) {
                ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, entry.getKey());
                builder.requires(entry.getValue().getValue().entrySet().stream().findAny().get().getValue());
                builder.unlockedBy(getHasName(AEBlocks.CONTROLLER.asItem()), has(AEBlocks.CONTROLLER.asItem()));
                ResourceLocation recipeId = CrazyAddons.makeId(
                ForgeRegistries.BLOCKS.getKey(entry.getKey()).getPath() + (recipeIndex1 == 0 ? "" : "_alt" + recipeIndex1)
                );
                builder.save(pWriter, recipeId);
            } else {
                ShapedRecipeBuilder builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, entry.getKey());
                for (var recipe : entry.getValue().getKey().split("/")) {
                    builder.pattern(recipe);
                }
                for (Map.Entry<String, Item> e : entry.getValue().getValue().entrySet()) {
                    builder.define(e.getKey().charAt(0), e.getValue());
                }
                builder.unlockedBy(getHasName(AEBlocks.CONTROLLER.asItem()), has(AEBlocks.CONTROLLER.asItem()));

                ResourceLocation recipeId = CrazyAddons.makeId(
                        ForgeRegistries.BLOCKS.getKey(entry.getKey()).getPath() + (recipeIndex1 == 0 ? "" : "_alt" + recipeIndex1)
                );
                builder.save(pWriter, recipeId);
            }
            recipeIndex1++;
        }
        for (var entry : ItemDefs.getItemRecipes().entrySet()) {
            int recipeIndex = 0;
            for (var recipeEntry : entry.getValue()) {
                if (entry.getValue().stream().findFirst().get().getKey().trim().length() == 1) {
                    ShapelessRecipeBuilder builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, entry.getKey());
                    builder.requires(entry.getValue().stream().findFirst().get().getValue().entrySet().stream().findFirst().get().getValue());
                    builder.unlockedBy(getHasName(AEBlocks.CONTROLLER.asItem()), has(AEBlocks.CONTROLLER.asItem()));
                    ResourceLocation recipeId = CrazyAddons.makeId(
                            ForgeRegistries.ITEMS.getKey(entry.getKey()).getPath() + (recipeIndex1 == 0 ? "" : "_alt" + recipeIndex1)
                    );
                    builder.save(pWriter, recipeId);
                } else {
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
                }
                recipeIndex++;
            }
        }
    }
}
