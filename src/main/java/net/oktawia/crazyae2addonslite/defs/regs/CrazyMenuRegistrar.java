package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.entities.*;
import net.oktawia.crazyae2addonslite.menus.*;

public class CrazyMenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CrazyAddonslite.MODID);

    private static <C extends AEBaseMenu, I> DeferredHolder<MenuType<?>, MenuType<C>> reg(
            String id, MenuTypeBuilder.MenuFactory<C, I> factory, Class<I> host) {
        return MENU_TYPES.register(id, () -> MenuTypeBuilder.create(factory, host).build(id));
    }

//    public static final DeferredHolder<MenuType<?>, MenuType<WirelessRedstoneTerminalMenu>> WIRELESS_REDSTONE_TERMINAL_MENU =
//            MENU_TYPES.register("wireless_redstone_terminal", () -> WirelessRedstoneTerminalMenu.TYPE);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<WirelessNotificationTerminalMenu>> WIRELESS_NOTIFICATION_TERMINAL_MENU =
//            MENU_TYPES.register("wireless_notification_terminal", () -> WirelessNotificationTerminalMenu.TYPE);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<NBTExportBusMenu>> NBT_EXPORT_BUS_MENU =
//            reg("nbt_export_bus", NBTExportBusMenu::new, NBTExportBusPart.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<NBTStorageBusMenu>> NBT_STORAGE_BUS_MENU =
//            reg("nbt_storage_bus", NBTStorageBusMenu::new, NBTStorageBusPart.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<DisplayMenu>> DISPLAY_MENU =
//            reg("display", DisplayMenu::new, DisplayPart.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<AmpereMeterMenu>> AMPERE_METER_MENU =
//            reg("ampere_meter", AmpereMeterMenu::new, AmpereMeterBE.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<CrazyPatternMultiplierMenu>> CRAZY_PATTERN_MULTIPLIER_MENU =
//            reg("crazy_pattern_multiplier", CrazyPatternMultiplierMenu::new, CrazyPatternMultiplierHost.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<CrazyEmitterMultiplierMenu>> CRAZY_EMITTER_MULTIPLIER_MENU =
//            reg("crazy_emitter_multiplier", CrazyEmitterMultiplierMenu::new, CrazyEmitterMultiplierHost.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<EjectorMenu>> EJECTOR_MENU =
//            reg("ejector", EjectorMenu::new, EjectorBE.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<CraftingSchedulerMenu>> CRAFTING_SCHEDULER_MENU =
//            reg("crafting_scheduler", CraftingSchedulerMenu::new, CraftingSchedulerBE.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneEmitterMenu>> REDSTONE_EMITTER_MENU =
//            reg("redstone_emitter", RedstoneEmitterMenu::new, RedstoneEmitterPart.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneTerminalMenu>> REDSTONE_TERMINAL_MENU =
//            reg("redstone_terminal", RedstoneTerminalMenu::new, RedstoneTerminalPart.class);
//
    public static final DeferredHolder<MenuType<?>, MenuType<CrazyPatternProviderMenu>> CRAZY_PATTERN_PROVIDER_MENU =
            reg("crazy_pattern_provider", CrazyPatternProviderMenu::new, PatternProviderLogicHost.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<BrokenPatternProviderMenu>> BROKEN_PATTERN_PROVIDER_MENU =
//            reg("broken_pattern_provider", BrokenPatternProviderMenu::new, PatternProviderLogicHost.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<NbtViewCellMenu>> NBT_VIEW_CELL_MENU =
//            reg("nbt_view_cell_menu", NbtViewCellMenu::new, ViewCellHost.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<TagViewCellMenu>> TAG_VIEW_CELL_MENU =
//            reg("tag_view_cell_menu", TagViewCellMenu::new, ViewCellHost.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<CpuPrioMenu>> CPU_PRIO_MENU =
//            reg("cpu_prio_menu", CpuPrioMenu::new, CpuPrioHost.class);
//
//    public static final DeferredHolder<MenuType<?>, MenuType<MultiLevelEmitterMenu>> MULTI_LEVEL_EMITTER_MENU =
//            reg("multi_level_emitter_menu", MultiLevelEmitterMenu::new, MultiStorageLevelEmitterPart.class);

    private CrazyMenuRegistrar() {}
}
