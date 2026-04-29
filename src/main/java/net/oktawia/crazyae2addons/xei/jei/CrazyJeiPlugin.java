package net.oktawia.crazyae2addons.xei.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.xei.common.CrazyRecipes;

import java.util.List;

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

        if (!CrazyConfig.COMMON.DISPLAY_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.DISPLAY.get()))
            );
        }
        if (!CrazyConfig.COMMON.EMITTER_TERMINAL_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.EMITTER_TERMINAL.get()))
            );
        }
        if (!CrazyConfig.COMMON.EMITTER_TERMINAL_ENABLED.get()
                || !CrazyConfig.COMMON.WIRELESS_EMITTER_TERMINAL_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get()))
            );
        }
        if (!CrazyConfig.COMMON.WIRELESS_NOTIFICATION_TERMINAL_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get()))
            );
        }
        if (!CrazyConfig.COMMON.MULTI_LEVEL_EMITTER_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.MULTI_LEVEL_EMITTER.get()))
            );
        }
        if (!CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.TAG_LEVEL_EMITTER.get()))
            );
        }
        if (!CrazyConfig.COMMON.REDSTONE_EMITTER_TERMINAL_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.REDSTONE_TERMINAL.get()))
            );
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.REDSTONE_EMITTER.get()))
            );
        }
        if (!CrazyConfig.COMMON.REDSTONE_EMITTER_TERMINAL_ENABLED.get()
                || !CrazyConfig.COMMON.WIRELESS_REDSTONE_TERMINAL_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get()))
            );
        }
        if (!CrazyConfig.COMMON.WORMHOLE_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.WORMHOLE.get()))
            );
        }
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.CPU_PRIO_TUNER.get()))
            );
        }
        if (!CrazyConfig.COMMON.RR_ITEM_P2P_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.RR_ITEM_P2P.get()))
            );
        }
        if (!CrazyConfig.COMMON.RR_FLUID_P2P_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.RR_FLUID_P2P.get()))
            );
        }
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.CPU_PRIO_TUNER.get()))
            );
        }
        if (!CrazyConfig.COMMON.TAG_VIEW_CELL_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.TAG_VIEW_CELL.get()))
            );
        }
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.PATTERN_MULTIPLIER.get()))
            );
        }
        if (!CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_PART_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get()))
            );
        }
        if (!CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get()))
            );
        }
        if (!CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED.get()
                && !CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_PART_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyItemRegistrar.CRAZY_UPGRADE.get()))
            );
        }
        if (!CrazyConfig.COMMON.EJECTOR_ENABLED.get()) {
            registration.getIngredientManager().removeIngredientsAtRuntime(
                    VanillaTypes.ITEM_STACK,
                    List.of(new ItemStack(CrazyBlockRegistrar.EJECTOR_BLOCK.get()))
            );
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(
                CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get(),
                FabricationCategory.TYPE
        );
    }
}