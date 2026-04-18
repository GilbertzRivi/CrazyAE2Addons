package net.oktawia.crazyae2addons.defs;

import appeng.init.client.InitScreens;
import net.oktawia.crazyae2addons.client.screens.CrazyPatternProviderScreen;
import net.oktawia.crazyae2addons.client.screens.block.AmpereMeterScreen;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.menus.block.AmpereMeterMenu;

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
    }

    private Screens() {}
}