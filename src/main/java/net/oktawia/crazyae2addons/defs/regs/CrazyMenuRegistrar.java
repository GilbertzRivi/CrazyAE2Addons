package net.oktawia.crazyae2addons.defs.regs;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import net.oktawia.crazyae2addons.entities.RecipeFabricatorBE;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPrioHost;
import net.oktawia.crazyae2addons.logic.patternmultiplier.PatternMultiplierHost;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolHost;
import net.oktawia.crazyae2addons.logic.viewcell.TagViewCellHost;
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.menus.PatternMultiplierMenu;
import net.oktawia.crazyae2addons.menus.block.EjectorMenu;
import net.oktawia.crazyae2addons.menus.block.RecipeFabricatorMenu;
import net.oktawia.crazyae2addons.menus.item.*;
import net.oktawia.crazyae2addons.menus.part.DisplayImagesSubMenu;
import net.oktawia.crazyae2addons.menus.part.DisplayMenu;
import net.oktawia.crazyae2addons.menus.part.DisplayTokenSubMenu;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;
import net.oktawia.crazyae2addons.menus.part.MultiLevelEmitterMenu;
import net.oktawia.crazyae2addons.menus.part.RedstoneEmitterMenu;
import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.menus.part.TagLevelEmitterMenu;
import net.oktawia.crazyae2addons.parts.Display;
import net.oktawia.crazyae2addons.parts.EmitterTerminal;
import net.oktawia.crazyae2addons.parts.MultiLevelEmitter;
import net.oktawia.crazyae2addons.parts.RedstoneEmitter;
import net.oktawia.crazyae2addons.parts.RedstoneTerminal;
import net.oktawia.crazyae2addons.parts.TagLevelEmitter;

public class CrazyMenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CrazyAddons.MODID);

    private static <C extends AEBaseMenu, I> RegistryObject<MenuType<C>> reg(
            String id,
            MenuTypeBuilder.MenuFactory<C, I> factory,
            Class<I> host
    ) {
        return MENU_TYPES.register(
                id,
                () -> MenuTypeBuilder.create(factory, host).build(id)
        );
    }

    public static final RegistryObject<MenuType<WirelessNotificationTerminalMenu>> WIRELESS_NOTIFICATION_TERMINAL_MENU =
            MENU_TYPES.register("wireless_notification_terminal_menu", () -> WirelessNotificationTerminalMenu.TYPE);

    public static final RegistryObject<MenuType<WirelessEmitterTerminalMenu>> WIRELESS_EMITTER_TERMINAL_MENU =
            MENU_TYPES.register("wireless_emitter_terminal_menu", () -> WirelessEmitterTerminalMenu.TYPE);

    public static final RegistryObject<MenuType<WirelessRedstoneTerminalMenu>> WIRELESS_REDSTONE_TERMINAL_MENU =
            MENU_TYPES.register("wireless_redstone_terminal_menu", () -> WirelessRedstoneTerminalMenu.TYPE);

    public static final RegistryObject<MenuType<CrazyPatternProviderMenu>> CRAZY_PATTERN_PROVIDER_MENU =
            reg("crazy_pattern_provider_menu", CrazyPatternProviderMenu::new, PatternProviderLogicHost.class);

    public static final RegistryObject<MenuType<EjectorMenu>> EJECTOR_MENU =
            reg("ejector_menu", EjectorMenu::new, EjectorBE.class);

    public static final RegistryObject<MenuType<DisplayMenu>> DISPLAY_MENU =
            reg("display_menu", DisplayMenu::new, Display.class);

    public static final RegistryObject<MenuType<DisplayTokenSubMenu>> DISPLAY_TOKEN_SUBMENU =
            reg("display_token_submenu", DisplayTokenSubMenu::new, Display.class);

    public static final RegistryObject<MenuType<DisplayImagesSubMenu>> DISPLAY_IMAGES_SUBMENU =
            reg("display_images_submenu", DisplayImagesSubMenu::new, Display.class);

    public static final RegistryObject<MenuType<EmitterTerminalMenu>> EMITTER_TERMINAL_MENU =
            reg("emitter_terminal_menu", EmitterTerminalMenu::new, EmitterTerminal.class);

    public static final RegistryObject<MenuType<MultiLevelEmitterMenu>> MULTI_LEVEL_EMITTER_MENU =
            reg("multi_level_emitter_menu", MultiLevelEmitterMenu::new, MultiLevelEmitter.class);

    public static final RegistryObject<MenuType<TagLevelEmitterMenu>> TAG_LEVEL_EMITTER_MENU =
            reg("tag_level_emitter_menu", TagLevelEmitterMenu::new, TagLevelEmitter.class);

    public static final RegistryObject<MenuType<RedstoneTerminalMenu>> REDSTONE_TERMINAL_MENU =
            reg("redstone_terminal_menu", RedstoneTerminalMenu::new, RedstoneTerminal.class);

    public static final RegistryObject<MenuType<RedstoneEmitterMenu>> REDSTONE_EMITTER_MENU =
            reg("redstone_emitter_menu", RedstoneEmitterMenu::new, RedstoneEmitter.class);

    public static final RegistryObject<MenuType<PortableSpatialStorageMenu>> PORTABLE_SPATIAL_STORAGE_MENU =
            reg("portable_spatial_storage_menu", PortableSpatialStorageMenu::new, StructureToolHost.class);

    public static final RegistryObject<MenuType<PortableSpatialClonerMenu>> PORTABLE_SPATIAL_CLONER_MENU =
            reg("portable_spatial_cloner_menu", PortableSpatialClonerMenu::new, StructureToolHost.class);

    public static final RegistryObject<MenuType<PatternMultiplierMenu>> PATTERN_MULTIPLIER_MENU =
            reg("pattern_multiplier_menu", PatternMultiplierMenu::new, PatternMultiplierHost.class);

    public static final RegistryObject<MenuType<CpuPrioMenu>> CPU_PRIO_MENU =
            reg("cpu_priority_menu", CpuPrioMenu::new, CpuPrioHost.class);

    public static final RegistryObject<MenuType<TagViewCellMenu>> TAG_VIEW_CELL_MENU =
            reg("tag_view_cell_menu", TagViewCellMenu::new, TagViewCellHost.class);

    public static final RegistryObject<MenuType<RecipeFabricatorMenu>> RECIPE_FABRICATOR_MENU =
            reg("recipe_fabricator_menu", RecipeFabricatorMenu::new, RecipeFabricatorBE.class);

    private CrazyMenuRegistrar() {}
}