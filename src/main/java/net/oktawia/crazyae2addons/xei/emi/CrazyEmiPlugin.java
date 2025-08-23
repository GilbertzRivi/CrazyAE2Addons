package net.oktawia.crazyae2addons.xei.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.xei.common.CradleEntry;
import net.oktawia.crazyae2addons.xei.common.CrazyEntry;
import net.oktawia.crazyae2addons.xei.common.CrazyRecipes;
import net.oktawia.crazyae2addons.xei.common.ResearchEntry;

import java.util.List;

@EmiEntrypoint
public class CrazyEmiPlugin implements EmiPlugin {
    @Override
    public void register(EmiRegistry registry) {
        EmiRecipeCategory multiblocksCategory = new EmiRecipeCategory(
                CrazyAddons.makeId("emi_multiblocks"),
                EmiStack.of(CrazyItemRegistrar.SUPER_SINGULARITY.get())
        ){
            @Override public Component getName(){ return Component.literal("Crazy Multiblocks"); }
        };
        registry.addCategory(multiblocksCategory);

        List<CrazyEntry> recipes = CrazyRecipes.getCrazyEntries();
        for (CrazyEntry recipe : recipes) {
            registry.addRecipe(new CrazyEmiRecipe(recipe, multiblocksCategory));
            registry.addWorkstation(multiblocksCategory, EmiStack.of(recipe.output()));
        }
        registry.addRecipeHandler(null, new CrazyEmiRecipeHandler());

        EmiRecipeCategory cradleCategory = new EmiRecipeCategory(
                CrazyAddons.makeId("cradle_recipes"),
                EmiStack.of(CrazyBlockRegistrar.ENTROPY_CRADLE.get().asItem().getDefaultInstance())
        ){
            @Override public Component getName(){ return Component.literal("Entropy Cradle Recipes"); }
        };
        registry.addCategory(cradleCategory);

        List<CradleEntry> cradleRecipes = CrazyRecipes.getCradleEntries();
        for (CradleEntry recipe : cradleRecipes) {
            registry.addRecipe(new CradleEmiRecipe(recipe, cradleCategory));
        }
        registry.addWorkstation(cradleCategory, EmiStack.of(CrazyBlockRegistrar.ENTROPY_CRADLE_CONTROLLER.get().asItem()));
        registry.addRecipeHandler(null, new CradleEmiRecipeHandler());

        EmiRecipeCategory researchCategory = new EmiRecipeCategory(
                CrazyAddons.makeId("research_recipes"),
                EmiStack.of(CrazyItemRegistrar.DATA_DRIVE.get())
        ){
            @Override public Component getName() { return Component.literal("Research Station"); }
        };
        registry.addCategory(researchCategory);

        List<ResearchEntry> researchEntries = CrazyRecipes.getResearchEntries();
        for (ResearchEntry entry : researchEntries) {
            registry.addRecipe(new ResearchEmiRecipe(entry, researchCategory));
        }
        registry.addWorkstation(researchCategory, EmiStack.of(CrazyBlockRegistrar.RESEARCH_STATION.get().asItem()));
        registry.addRecipeHandler(null, new ResearchEmiRecipeHandler());

        // NEW: Fabrication
        EmiRecipeCategory fabricationCategory = new EmiRecipeCategory(
                CrazyAddons.makeId("fabrication_recipes"),
                EmiStack.of(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get().asItem())
        ){
            @Override public Component getName() { return Component.literal("Recipe Fabricator"); }
        };
        registry.addCategory(fabricationCategory);

        var fabricationEntries = CrazyRecipes.getFabricationEntries();
        for (var entry : fabricationEntries) {
            registry.addRecipe(new FabricationEmiRecipe(entry, fabricationCategory));
        }
        registry.addWorkstation(fabricationCategory,
                EmiStack.of(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get().asItem()));
        registry.addRecipeHandler(null, new FabricationEmiRecipeHandler());
    }
}
