package net.oktawia.crazyae2addons.defs;

import appeng.init.client.InitScreens;
import net.oktawia.crazyae2addons.client.screens.CrazyPatternProviderScreen;
import net.oktawia.crazyae2addons.client.screens.block.*;
import net.oktawia.crazyae2addons.client.screens.item.BuilderPatternScreen;
import net.oktawia.crazyae2addons.client.screens.item.BuilderPatternSubScreen;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.menus.block.*;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternMenu;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternSubMenu;

public final class Screens {

    public static void register() {
        InitScreens.register(
                CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(),
                CrazyPatternProviderScreen<CrazyPatternProviderMenu>::new,
                "/screens/crazy_pattern_provider.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.AMPERE_METER_MENU.get(),
                AmpereMeterScreen<AmpereMeterMenu>::new,
                "/screens/ampere_meter.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(),
                AutoBuilderScreen<AutoBuilderMenu>::new,
                "/screens/auto_builder.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.BUILDER_PATTERN_MENU.get(),
                BuilderPatternScreen<BuilderPatternMenu>::new,
                "/screens/builder_pattern.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.BUILDER_PATTERN_SUBMENU.get(),
                BuilderPatternSubScreen<BuilderPatternSubMenu>::new,
                "/screens/builder_pattern_subscreen.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.BROKEN_PATTERN_PROVIDER_MENU.get(),
                BrokenPatternProviderScreen<BrokenPatternProviderMenu>::new,
                "/screens/broken_pattern_provider.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.EJECTOR_MENU.get(),
                EjectorScreen<EjectorMenu>::new,
                "/screens/ejector.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.CRAFTING_SCHEDULER_MENU.get(),
                CraftingSchedulerScreen<CraftingSchedulerMenu>::new,
                "/screens/crafting_scheduler.json"
        );
    }

    private Screens() {}
}