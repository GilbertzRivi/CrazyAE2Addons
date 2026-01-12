package net.oktawia.crazyae2addonslite.defs.regs;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.compat.GregTech.GTAmpereMeterBE;
import net.oktawia.crazyae2addonslite.logic.*;
import net.oktawia.crazyae2addonslite.parts.*;
import net.oktawia.crazyae2addonslite.entities.*;
import net.oktawia.crazyae2addonslite.menus.*;

public class CrazyMenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, CrazyAddons.MODID);

    private static <C extends AEBaseMenu, I> RegistryObject<MenuType<C>> reg(
            String id, MenuTypeBuilder.MenuFactory<C, I> factory, Class<I> host) {

        return MENU_TYPES.register(id,
                () -> MenuTypeBuilder.create(factory, host).build(id));
    }

    public static final RegistryObject<MenuType<WirelessRedstoneTerminalMenu>> WIRELESS_REDSTONE_TERMINAL_MENU =
            MENU_TYPES.register(id("wireless_redstone_terminal"), () -> WirelessRedstoneTerminalMenu.TYPE);

    public static final RegistryObject<MenuType<WirelessNotificationTerminalMenu>> WIRELESS_NOTIFICATION_TERMINAL_MENU =
            MENU_TYPES.register(id("wireless_notification_terminal"), () -> WirelessNotificationTerminalMenu.TYPE);

    private static String id(String s) { return s; }

    public static final RegistryObject<MenuType<NBTExportBusMenu>> NBT_EXPORT_BUS_MENU =
            reg(id("nbt_export_bus"), NBTExportBusMenu::new, NBTExportBusPart.class);

    public static final RegistryObject<MenuType<NBTStorageBusMenu>> NBT_STORAGE_BUS_MENU =
            reg(id("nbt_storage_bus"), NBTStorageBusMenu::new, NBTStorageBusPart.class);

    public static final RegistryObject<MenuType<DisplayMenu>> DISPLAY_MENU =
            reg(id("display"), DisplayMenu::new, DisplayPart.class);

    public static final RegistryObject<MenuType<AmpereMeterMenu>> AMPERE_METER_MENU =
            ModList.get().isLoaded("gtceu")
                    ? reg(id("ampere_meter"), AmpereMeterMenu::new, GTAmpereMeterBE.class)
                    : reg(id("ampere_meter"), AmpereMeterMenu::new, AmpereMeterBE.class);

    public static final RegistryObject<MenuType<CrazyPatternMultiplierMenu>> CRAZY_PATTERN_MULTIPLIER_MENU =
            reg(id("crazy_pattern_multiplier"), CrazyPatternMultiplierMenu::new, CrazyPatternMultiplierHost.class);

    public static final RegistryObject<MenuType<CrazyEmitterMultiplierMenu>> CRAZY_EMITTER_MULTIPLIER_MENU =
            reg(id("crazy_emitter_multiplier"), CrazyEmitterMultiplierMenu::new, CrazyEmitterMultiplierHost.class);

    public static final RegistryObject<MenuType<EjectorMenu>> EJECTOR_MENU =
            reg(id("ejector"), EjectorMenu::new, EjectorBE.class);

    public static final RegistryObject<MenuType<CraftingSchedulerMenu>> CRAFTING_SCHEDULER_MENU =
            reg(id("crafting_scheduler"), CraftingSchedulerMenu::new, CraftingSchedulerBE.class);

    public static final RegistryObject<MenuType<RedstoneEmitterMenu>> REDSTONE_EMITTER_MENU =
            reg(id("redstone_emitter"), RedstoneEmitterMenu::new, RedstoneEmitterPart.class);

    public static final RegistryObject<MenuType<RedstoneTerminalMenu>> REDSTONE_TERMINAL_MENU =
            reg(id("redstone_terminal"), RedstoneTerminalMenu::new, RedstoneTerminalPart.class);

    public static final RegistryObject<MenuType<CrazyPatternProviderMenu>> CRAZY_PATTERN_PROVIDER_MENU =
            reg(id("crazy_pattern_provider"), CrazyPatternProviderMenu::new, PatternProviderLogicHost.class);

    public static final RegistryObject<MenuType<BrokenPatternProviderMenu>> BROKEN_PATTERN_PROVIDER_MENU =
            reg(id("broken_pattern_provider"), BrokenPatternProviderMenu::new, PatternProviderLogicHost.class);

    public static final RegistryObject<MenuType<NbtViewCellMenu>> NBT_VIEW_CELL_MENU =
            reg(id("nbt_view_cell_menu"), NbtViewCellMenu::new, ViewCellHost.class);

    public static final RegistryObject<MenuType<TagViewCellMenu>> TAG_VIEW_CELL_MENU =
            reg(id("tag_view_cell_menu"), TagViewCellMenu::new, ViewCellHost.class);

    public static final RegistryObject<MenuType<CpuPrioMenu>> CPU_PRIO_MENU =
            reg(id("cpu_prio_menu"), CpuPrioMenu::new, CpuPrioHost.class);

    public static final RegistryObject<MenuType<MultiLevelEmitterMenu>> MULTI_LEVEL_EMITTER_MENU =
            reg(id("multi_level_emitter_menu"), MultiLevelEmitterMenu::new, MultiStorageLevelEmitterPart.class);

    private CrazyMenuRegistrar() {}
}