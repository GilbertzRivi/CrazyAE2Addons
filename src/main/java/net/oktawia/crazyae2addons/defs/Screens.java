package net.oktawia.crazyae2addons.defs;

import appeng.init.client.InitScreens;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.*;
import net.oktawia.crazyae2addons.client.screens.*;
import net.oktawia.crazyae2addons.client.screens.AmpereMeterScreen;
import net.oktawia.crazyae2addons.client.screens.ChunkyFluidP2PTunnelScreen;
import net.oktawia.crazyae2addons.client.screens.EjectorScreen;
import net.oktawia.crazyae2addons.client.screens.EmitterTerminalScreen;
import net.oktawia.crazyae2addons.items.wireless.WirelessEmitterTerminalMenu;

public final class Screens {

    public static void register(RegisterMenuScreensEvent event) {
        InitScreens.register(event, CrazyMenuRegistrar.DISPLAY_MENU.get(),
                DisplayScreen<DisplayMenu>::new,
                "/screens/display.json");

        InitScreens.register(event, CrazyMenuRegistrar.DISPLAY_SUBMENU.get(),
                DisplaySubScreen::new,
                "/screens/display_subscreen.json");

        InitScreens.register(event, CrazyMenuRegistrar.EMITTER_TERMINAL_MENU.get(),
                EmitterTerminalScreen<EmitterTerminalMenu>::new,
                "/screens/emitter_terminal.json");

        InitScreens.register(event, WirelessEmitterTerminalMenu.TYPE,
                EmitterTerminalScreen<WirelessEmitterTerminalMenu>::new,
                "/screens/emitter_terminal.json");

        InitScreens.register(event, CrazyMenuRegistrar.EJECTOR_MENU.get(),
                EjectorScreen<EjectorMenu>::new,
                "/screens/ejector.json");

        InitScreens.register(event, CrazyMenuRegistrar.CHUNKY_FLUID_P2P_TUNNEL_MENU.get(),
                ChunkyFluidP2PTunnelScreen<ChunkyFluidP2PTunnelMenu>::new,
                "/screens/chunky_fluid_p2p_tunnel.json");

        InitScreens.register(event, CrazyMenuRegistrar.AMPERE_METER_MENU.get(),
                AmpereMeterScreen<AmpereMeterMenu>::new,
                "/screens/ampere_meter.json");

        InitScreens.register(event, CrazyMenuRegistrar.CRAFTING_SCHEDULER_MENU.get(),
                CraftingSchedulerScreen<CraftingSchedulerMenu>::new,
                "/screens/crafting_scheduler.json");

        InitScreens.register(event, CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(),
                AutoBuilderScreen<AutoBuilderMenu>::new,
                "/screens/auto_builder.json");

        InitScreens.register(event, CrazyMenuRegistrar.BUILDER_PATTERN_MENU.get(),
                BuilderPatternScreen<BuilderPatternMenu>::new,
                "/screens/builder_pattern.json");

        InitScreens.register(event, CrazyMenuRegistrar.BUILDER_PATTERN_SUBMENU.get(),
                BuilderPatternSubScreen<BuilderPatternSubMenu>::new,
                "/screens/builder_pattern_subscreen.json");

        InitScreens.register(event, CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(),
                CrazyPatternProviderScreen<CrazyPatternProviderMenu>::new,
                "/screens/crazy_pattern_provider.json");

        InitScreens.register(event, CrazyMenuRegistrar.BROKEN_PATTERN_PROVIDER_MENU.get(),
                BrokenPatternProviderScreen<BrokenPatternProviderMenu>::new,
                "/screens/broken_pattern_provider.json");
    }

    private Screens() {}
}