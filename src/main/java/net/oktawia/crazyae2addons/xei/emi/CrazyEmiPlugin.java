package net.oktawia.crazyae2addons.xei.emi;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.network.chat.Component;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
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

        if (!CrazyConfig.COMMON.DISPLAY_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.DISPLAY.get()));
        }
        if (!CrazyConfig.COMMON.EMITTER_TERMINAL_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.EMITTER_TERMINAL.get()));
        }
        if (!CrazyConfig.COMMON.EMITTER_TERMINAL_ENABLED.get()
                || !CrazyConfig.COMMON.WIRELESS_EMITTER_TERMINAL_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get()));
        }
        if (!CrazyConfig.COMMON.WIRELESS_NOTIFICATION_TERMINAL_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.WIRELESS_NOTIFICATION_TERMINAL.get()));
        }
        if (!CrazyConfig.COMMON.MULTI_LEVEL_EMITTER_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.MULTI_LEVEL_EMITTER.get()));
        }
        if (!CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.TAG_LEVEL_EMITTER.get()));
        }
        if (!CrazyConfig.COMMON.REDSTONE_EMITTER_TERMINAL_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.REDSTONE_TERMINAL.get()));
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.REDSTONE_EMITTER.get()));
        }
        if (!CrazyConfig.COMMON.REDSTONE_EMITTER_TERMINAL_ENABLED.get()
                || !CrazyConfig.COMMON.WIRELESS_REDSTONE_TERMINAL_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get()));
        }
        if (!CrazyConfig.COMMON.WORMHOLE_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.WORMHOLE.get()));
        }
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.CPU_PRIO_TUNER.get()));
        }
        if (!CrazyConfig.COMMON.RR_ITEM_P2P_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.RR_ITEM_P2P.get()));
        }
        if (!CrazyConfig.COMMON.RR_FLUID_P2P_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.RR_FLUID_P2P.get()));
        }
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.CPU_PRIO_TUNER.get()));
        }
        if (!CrazyConfig.COMMON.TAG_VIEW_CELL_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.TAG_VIEW_CELL.get()));
        }
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.PATTERN_MULTIPLIER.get()));
        }
        if (!CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_PART_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get()));
        }
        if (!CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get()));
        }
        if (!CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED.get()
                && !CrazyConfig.COMMON.CRAZY_PATTERN_PROVIDER_PART_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.CRAZY_UPGRADE.get()));
        }
        if (!CrazyConfig.COMMON.EJECTOR_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyBlockRegistrar.EJECTOR_BLOCK.get()));
        }
        if (!CrazyConfig.COMMON.PORTABLE_SPATIAL_STORAGE_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.PORTABLE_SPATIAL_STORAGE.get()));
        }
        if (!CrazyConfig.COMMON.PORTABLE_SPATIAL_CLONER_ENABLED.get()) {
            registry.removeEmiStacks(EmiStack.of(CrazyItemRegistrar.PORTABLE_SPATIAL_CLONER.get()));
        }
    }
}