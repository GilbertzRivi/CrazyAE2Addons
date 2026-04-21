package net.oktawia.crazyae2addons.defs;

import appeng.init.client.InitScreens;
import net.oktawia.crazyae2addons.client.screens.CrazyPatternProviderScreen;
import net.oktawia.crazyae2addons.client.screens.block.EjectorScreen;
import net.oktawia.crazyae2addons.client.screens.item.PortableSpatialStorageScreen;
import net.oktawia.crazyae2addons.client.screens.item.WirelessEmitterTerminalScreen;
import net.oktawia.crazyae2addons.client.screens.item.WirelessNotificationTerminalScreen;
import net.oktawia.crazyae2addons.client.screens.item.WirelessRedstoneTerminalScreen;
import net.oktawia.crazyae2addons.client.screens.part.DisplayImagesSubScreen;
import net.oktawia.crazyae2addons.client.screens.part.DisplayScreen;
import net.oktawia.crazyae2addons.client.screens.part.DisplayTokenSubScreen;
import net.oktawia.crazyae2addons.client.screens.part.EmitterTerminalScreen;
import net.oktawia.crazyae2addons.client.screens.part.MultiLevelEmitterScreen;
import net.oktawia.crazyae2addons.client.screens.part.RedstoneEmitterScreen;
import net.oktawia.crazyae2addons.client.screens.part.RedstoneTerminalScreen;
import net.oktawia.crazyae2addons.client.screens.part.TagLevelEmitterScreen;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.menus.block.EjectorMenu;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialStorageMenu;
import net.oktawia.crazyae2addons.menus.item.WirelessEmitterTerminalMenu;
import net.oktawia.crazyae2addons.menus.item.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addons.menus.item.WirelessRedstoneTerminalMenu;
import net.oktawia.crazyae2addons.menus.part.DisplayMenu;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;
import net.oktawia.crazyae2addons.menus.part.MultiLevelEmitterMenu;
import net.oktawia.crazyae2addons.menus.part.RedstoneEmitterMenu;
import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.menus.part.TagLevelEmitterMenu;

public final class Screens {

    public static void register() {
        InitScreens.register(
                CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(),
                CrazyPatternProviderScreen<CrazyPatternProviderMenu>::new,
                "/screens/crazy_pattern_provider.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.EJECTOR_MENU.get(),
                EjectorScreen<EjectorMenu>::new,
                "/screens/ejector.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.DISPLAY_MENU.get(),
                DisplayScreen<DisplayMenu>::new,
                "/screens/display.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.DISPLAY_IMAGES_SUBMENU.get(),
                DisplayImagesSubScreen::new,
                "/screens/display_images_subscreen.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.DISPLAY_TOKEN_SUBMENU.get(),
                DisplayTokenSubScreen::new,
                "/screens/display_token_subscreen.json"
        );
        InitScreens.register(
                WirelessNotificationTerminalMenu.TYPE,
                WirelessNotificationTerminalScreen<WirelessNotificationTerminalMenu>::new,
                "/screens/wireless_notification_terminal.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.EMITTER_TERMINAL_MENU.get(),
                EmitterTerminalScreen<EmitterTerminalMenu>::new,
                "/screens/emitter_terminal.json"
        );
        InitScreens.register(
                WirelessEmitterTerminalMenu.TYPE,
                WirelessEmitterTerminalScreen::new,
                "/screens/wireless_emitter_terminal.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.MULTI_LEVEL_EMITTER_MENU.get(),
                MultiLevelEmitterScreen<MultiLevelEmitterMenu>::new,
                "/screens/multi_level_emitter.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.TAG_LEVEL_EMITTER_MENU.get(),
                TagLevelEmitterScreen<TagLevelEmitterMenu>::new,
                "/screens/tag_level_emitter.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.REDSTONE_TERMINAL_MENU.get(),
                RedstoneTerminalScreen<RedstoneTerminalMenu>::new,
                "/screens/redstone_terminal.json"
        );
        InitScreens.register(
                WirelessRedstoneTerminalMenu.TYPE,
                WirelessRedstoneTerminalScreen::new,
                "/screens/wireless_redstone_terminal.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.REDSTONE_EMITTER_MENU.get(),
                RedstoneEmitterScreen<RedstoneEmitterMenu>::new,
                "/screens/redstone_emitter.json"
        );
        InitScreens.register(
                CrazyMenuRegistrar.PORTABLE_SPATIAL_STORAGE_MENU.get(),
                PortableSpatialStorageScreen<PortableSpatialStorageMenu>::new,
                "/screens/portable_spatial_storage.json"
        );
    }

    private Screens() {}
}