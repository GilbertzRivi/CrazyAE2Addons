package net.oktawia.crazyae2addons.datagen;

import appeng.core.definitions.AEBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.recipes.BlockRecipes;
import net.oktawia.crazyae2addons.defs.recipes.ItemRecipes;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class CrazyRecipeProvider extends RecipeProvider implements IConditionBuilder {

    public CrazyRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> writer) {
        BlockRecipes.registerRecipes();
        ItemRecipes.registerRecipes();

        for (var recipe : BlockRecipes.getRecipes()) {
            save(writer, recipe.id(), recipe.pattern(), recipe.keys(), recipe.shapelessIngredients(), recipe.output(), recipe.count());
        }
        for (var recipe : ItemRecipes.getRecipes()) {
            save(writer, recipe.id(), recipe.pattern(), recipe.keys(), recipe.shapelessIngredients(), recipe.output(), recipe.count());
        }
    }

    private void save(Consumer<FinishedRecipe> writer, String id, String pattern,
                      Map<Character, Item> keys, List<Item> shapeless, Item output, int count) {
        var unlock = has(AEBlocks.CONTROLLER.asItem());
        var unlockName = getHasName(AEBlocks.CONTROLLER.asItem());
        var recipeId = CrazyAddons.makeId(id);

        if (pattern == null) {
            var builder = ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, output, count);
            shapeless.forEach(builder::requires);
            builder.unlockedBy(unlockName, unlock);
            builder.save(writer, recipeId);
        } else {
            var builder = ShapedRecipeBuilder.shaped(RecipeCategory.MISC, output, count);
            for (var row : pattern.split("/")) builder.pattern(row);
            keys.forEach(builder::define);
            builder.unlockedBy(unlockName, unlock);
            builder.save(writer, recipeId);
        }
    }
}
