package net.oktawia.crazyae2addons.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.recipes.FabricationRecipes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class FabricationRecipeProvider implements DataProvider {

    private final PackOutput output;

    public FabricationRecipeProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        FabricationRecipes.registerRecipes();

        var pathResolver = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes/fabrication");
        List<CompletableFuture<?>> futures = new ArrayList<>();

        for (var recipe : FabricationRecipes.getRecipes()) {
            JsonObject json = new JsonObject();
            json.addProperty("type", CrazyAddons.MODID + ":fabrication");

            JsonArray inputArray = new JsonArray();
            for (var entry : recipe.inputs()) {
                JsonObject inputObj = new JsonObject();
                inputObj.addProperty("item", entry.item());
                inputObj.addProperty("count", entry.count());
                inputArray.add(inputObj);
            }
            json.add("input", inputArray);

            if (recipe.outputItem() != null) {
                JsonObject outputObj = new JsonObject();
                outputObj.addProperty("item", recipe.outputItem());
                outputObj.addProperty("count", recipe.outputCount());
                json.add("output", outputObj);
            }

            if (recipe.fluidInput() != null) {
                JsonObject fluidInputObj = new JsonObject();
                fluidInputObj.addProperty("fluid", recipe.fluidInput().fluid());
                fluidInputObj.addProperty("amount", recipe.fluidInput().amount());
                json.add("fluid_input", fluidInputObj);
            }

            if (recipe.fluidOutput() != null) {
                JsonObject fluidOutputObj = new JsonObject();
                fluidOutputObj.addProperty("fluid", recipe.fluidOutput().fluid());
                fluidOutputObj.addProperty("amount", recipe.fluidOutput().amount());
                json.add("fluid_output", fluidOutputObj);
            }


            var path = pathResolver.json(CrazyAddons.makeId(recipe.id()));
            futures.add(DataProvider.saveStable(cache, json, path));
        }

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Crazy AE2 Addons Fabrication Recipes";
    }
}
