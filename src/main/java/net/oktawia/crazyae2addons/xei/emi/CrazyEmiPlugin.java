package net.oktawia.crazyae2addons.xei.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.xei.common.CrazyRecipes;

@EmiEntrypoint
public class CrazyEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        EmiRecipeCategory fabricationCategory = new EmiRecipeCategory(
                CrazyAddons.makeId("fabrication_recipes"),
                EmiStack.of(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get().asItem())
        ) {
            @Override
            public Component getName() {
                return Component.translatable(LangDefs.FABRICATION_CATEGORY.getTranslationKey());
            }
        };

        registry.addCategory(fabricationCategory);

        for (var entry : CrazyRecipes.getFabricationEntries()) {
            registry.addRecipe(new FabricationEmiRecipe(entry, fabricationCategory));
        }

        registry.addWorkstation(
                fabricationCategory,
                EmiStack.of(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get().asItem())
        );

        registry.addRecipeHandler(null, new FabricationEmiRecipeHandler());
    }
}