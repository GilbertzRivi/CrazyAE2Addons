package net.oktawia.crazyae2addons.xei.jei;

import appeng.api.integrations.jei.IngredientConverters;
import appeng.api.stacks.GenericStack;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IModIngredientRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IIngredientManager;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.mobstorage.MobKey;
import net.oktawia.crazyae2addons.mobstorage.MobKeyIng;
import net.oktawia.crazyae2addons.mobstorage.MobKeyIngType;
import net.oktawia.crazyae2addons.mobstorage.MobKeyItem;
import net.oktawia.crazyae2addons.renderer.MobKeyIngDelegatingRenderer;
import net.oktawia.crazyae2addons.xei.common.CrazyEntry;
import net.oktawia.crazyae2addons.xei.common.CrazyRecipes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    @Override
    public void registerIngredients(IModIngredientRegistration reg) {
        List<MobKeyIng> all = new ArrayList<>();
        for (EntityType<?> type : ForgeRegistries.ENTITY_TYPES.getValues()) {
            if (type != null && type.canSummon() && type.getCategory() != MobCategory.MISC) {
                var key = MobKey.of(type);
                all.add(MobKeyIng.of(key));
            }
        }
        reg.register(MobKeyIngType.TYPE, all, new MobKeyIngHelper(), new MobKeyIngDelegatingRenderer());
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime jei) {
        IngredientConverters.register(new MobKeyIngredientConverter());
    }
}
