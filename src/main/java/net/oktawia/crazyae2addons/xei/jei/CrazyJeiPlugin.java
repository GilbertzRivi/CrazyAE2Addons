package net.oktawia.crazyae2addons.xei.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.xei.common.CrazyRecipes;

@JeiPlugin
public class CrazyJeiPlugin implements IModPlugin {

    private static final ResourceLocation ID = CrazyAddons.makeId("jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new FabricationCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var fabricationWrapped = CrazyRecipes.getFabricationEntries().stream()
                .map(FabricationWrapper::new)
                .toList();

        registration.addRecipes(FabricationCategory.TYPE, fabricationWrapped);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get(),
                FabricationCategory.TYPE
        );
    }
}