package net.oktawia.crazyae2addons.xei.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.xei.common.CrazyEntry;
import net.oktawia.crazyae2addons.xei.common.CrazyRecipes;

import java.util.List;

@JeiPlugin
public class CrazyJeiPlugin implements IModPlugin {
    private static final ResourceLocation ID = CrazyAddons.makeId("jei_plugin");
    public static CrazyEntry currentEntry;

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new CrazyCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ReinforcedCondenserCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new CradleCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ResearchCategory(registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new FabricationCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var wrapped = CrazyRecipes.getCrazyEntries().stream().map(CrazyWrapper::new).toList();
        registration.addRecipes(CrazyCategory.TYPE, wrapped);

        registration.addRecipes(ReinforcedCondenserCategory.TYPE, CrazyRecipes.getCondenserEntried());

        var cradleWrapped = CrazyRecipes.getCradleEntries().stream().map(CradleWrapper::new).toList();
        registration.addRecipes(CradleCategory.TYPE, cradleWrapped);

        var researchWrapped = CrazyRecipes.getResearchEntries().stream().map(ResearchWrapper::new).toList();
        registration.addRecipes(ResearchCategory.TYPE, researchWrapped);

        var fabricationWrapped = CrazyRecipes.getFabricationEntries().stream()
                .map(FabricationWrapper::new)
                .toList();
        registration.addRecipes(FabricationCategory.TYPE, fabricationWrapped);
    }
}
