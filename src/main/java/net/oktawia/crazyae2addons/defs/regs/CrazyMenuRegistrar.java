package net.oktawia.crazyae2addons.defs.regs;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.AEBaseMenu;
import appeng.menu.implementations.MenuTypeBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.CC.CCDataExtractorPart;
import net.oktawia.crazyae2addons.compat.GregTech.GTAmpereMeterBE;
import net.oktawia.crazyae2addons.compat.GregTech.GTEnergyExporterPart;
import net.oktawia.crazyae2addons.items.CpuPrioTunerItem;
import net.oktawia.crazyae2addons.items.TagViewCellItem;
import net.oktawia.crazyae2addons.logic.*;
import net.oktawia.crazyae2addons.mobstorage.MobExportBus;
import net.oktawia.crazyae2addons.mobstorage.MobExportBusMenu;
import net.oktawia.crazyae2addons.mobstorage.MobFormationPlane;
import net.oktawia.crazyae2addons.mobstorage.MobFormationPlaneMenu;
import net.oktawia.crazyae2addons.parts.*;
import net.oktawia.crazyae2addons.entities.*;
import net.oktawia.crazyae2addons.menus.*;

public class CrazyMenuRegistrar {

    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, CrazyAddons.MODID);

    private static <C extends AEBaseMenu, I> DeferredHolder<MenuType<?>, MenuType<C>> reg(
            String id, MenuTypeBuilder.MenuFactory<C, I> factory, Class<I> host) {

        return MENU_TYPES.register(id,
                () -> MenuTypeBuilder.create(factory, host).build(id));
    }

    public static final DeferredHolder<MenuType<?>, MenuType<WirelessRedstoneTerminalMenu>> WIRELESS_REDSTONE_TERMINAL_MENU =
            MENU_TYPES.register(id("wireless_redstone_terminal"), () -> WirelessRedstoneTerminalMenu.TYPE);

    private static String id(String s) { return s; }

    public static final DeferredHolder<MenuType<?>, MenuType<EntityTickerMenu>> ENTITY_TICKER_MENU =
            reg(id("entity_ticker"), EntityTickerMenu::new, EntityTickerPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<NBTExportBusMenu>> NBT_EXPORT_BUS_MENU =
            reg(id("nbt_export_bus"), NBTExportBusMenu::new, NBTExportBusPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<NBTStorageBusMenu>> NBT_STORAGE_BUS_MENU =
            reg(id("nbt_storage_bus"), NBTStorageBusMenu::new, NBTStorageBusPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CrazyPatternModifierMenu>> CRAZY_PATTERN_MODIFIER_MENU =
            reg(id("crazy_pattern_modifier"), CrazyPatternModifierMenu::new, CrazyPatternModifierHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<DisplayMenu>> DISPLAY_MENU =
            reg(id("display"), DisplayMenu::new, DisplayPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<MEDataControllerMenu>> ME_DATA_CONTROLLER_MENU =
            reg(id("me_data_controller"), MEDataControllerMenu::new, MEDataControllerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<DataExtractorMenu>> DATA_EXTRACTOR_MENU =
            IsModLoaded.isCCLoaded()
                    ? reg(id("data_extractor"), DataExtractorMenu::new, CCDataExtractorPart.class)
                    : reg(id("data_extractor"), DataExtractorMenu::new, DataExtractorPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<ChunkyFluidP2PTunnelMenu>> CHUNKY_FLUID_P2P_TUNNEL_MENU =
            reg(id("chunky_p2p"), ChunkyFluidP2PTunnelMenu::new, ChunkyFluidP2PTunnelPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyExporterMenu>> ENERGY_EXPORTER_MENU =
            ModList.get().isLoaded("gtceu")
                    ? reg(id("energy_exporter"), EnergyExporterMenu::new, GTEnergyExporterPart.class)
                    : reg(id("energy_exporter"), EnergyExporterMenu::new, EnergyExporterPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<AmpereMeterMenu>> AMPERE_METER_MENU =
            ModList.get().isLoaded("gtceu")
                    ? reg(id("ampere_meter"), AmpereMeterMenu::new, GTAmpereMeterBE.class)
                    : reg(id("ampere_meter"), AmpereMeterMenu::new, AmpereMeterBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CrazyPatternMultiplierMenu>> CRAZY_PATTERN_MULTIPLIER_MENU =
            reg(id("crazy_pattern_multiplier"), CrazyPatternMultiplierMenu::new, CrazyPatternMultiplierHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<ImpulsedPatternProviderMenu>> IMPULSED_PATTERN_PROVIDER_MENU =
            reg(id("impulsed_pp"), ImpulsedPatternProviderMenu::new, ImpulsedPatternProviderBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<SignallingInterfaceMenu>> SIGNALLING_INTERFACE_MENU =
            reg(id("signalling_interface"), SignallingInterfaceMenu::new, SignallingInterfaceBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<SetStockAmountMenu>> SET_STOCK_AMOUNT_MENU =
            reg(id("stock_amount_menu"), SetStockAmountMenu::new, SignallingInterfaceBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<MobExportBusMenu>> MOB_EXPORT_BUS_MENU =
            reg(id("mob_export_bus"), MobExportBusMenu::new, MobExportBus.class);

    public static final DeferredHolder<MenuType<?>, MenuType<AutoEnchanterMenu>> AUTO_ENCHANTER_MENU =
            reg(id("auto_enchanter"), AutoEnchanterMenu::new, AutoEnchanterBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CrazyEmitterMultiplierMenu>> CRAZY_EMITTER_MULTIPLIER_MENU =
            reg(id("crazy_emitter_multiplier"), CrazyEmitterMultiplierMenu::new, CrazyEmitterMultiplierHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CrazyCalculatorMenu>> CRAZY_CALCULATOR_MENU =
            reg(id("crazy_calculator"), CrazyCalculatorMenu::new, CrazyCalculatorHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<EjectorMenu>> EJECTOR_MENU =
            reg(id("ejector"), EjectorMenu::new, EjectorBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<MobFormationPlaneMenu>> MOB_FORMATION_PLANE_MENU =
            reg(id("mob_formation_plane"), MobFormationPlaneMenu::new, MobFormationPlane.class);

    public static final DeferredHolder<MenuType<?>, MenuType<SpawnerExtractorControllerMenu>> SPAWNER_EXTRACTOR_CONTROLLER_MENU =
            reg(id("spawner_extractor_controller"), SpawnerExtractorControllerMenu::new, SpawnerExtractorControllerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<MobFarmControllerMenu>> MOB_FARM_CONTROLLER_MENU =
            reg(id("mob_farm_controller"), MobFarmControllerMenu::new, MobFarmControllerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingSchedulerMenu>> CRAFTING_SCHEDULER_MENU =
            reg(id("crafting_scheduler"), CraftingSchedulerMenu::new, CraftingSchedulerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneEmitterMenu>> REDSTONE_EMITTER_MENU =
            reg(id("redstone_emitter"), RedstoneEmitterMenu::new, RedstoneEmitterPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<RedstoneTerminalMenu>> REDSTONE_TERMINAL_MENU =
            reg(id("redstone_terminal"), RedstoneTerminalMenu::new, RedstoneTerminalPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<ReinforcedMatterCondenserMenu>> REINFORCED_MATTER_CONDENSER_MENU =
            reg(id("reinforced_matter_condenser"), ReinforcedMatterCondenserMenu::new, ReinforcedMatterCondenserBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<PenroseControllerMenu>> PENROSE_CONTROLLER_MENU =
            reg(id("penrose_controller"), PenroseControllerMenu::new, PenroseControllerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CrazyPatternProviderMenu>> CRAZY_PATTERN_PROVIDER_MENU =
            reg(id("crazy_pattern_provider"), CrazyPatternProviderMenu::new, PatternProviderLogicHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<EnergyStorageControllerMenu>> ENERGY_STORAGE_CONTROLLER_MENU =
            reg(id("energy_storage_controller"), EnergyStorageControllerMenu::new, EnergyStorageControllerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<VariableTerminalMenu>> VARIABLE_TERMINAL_MENU =
            reg(id("variable_terminal"), VariableTerminalMenu::new, VariableTerminalPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<AutoBuilderMenu>> AUTO_BUILDER_MENU =
            reg(id("auto_builder"), AutoBuilderMenu::new, AutoBuilderBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<BuilderPatternMenu>> BUILDER_PATTERN_MENU =
            reg(id("builder_pattern"), BuilderPatternMenu::new, BuilderPatternHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<BrokenPatternProviderMenu>> BROKEN_PATTERN_PROVIDER_MENU =
            reg(id("broken_pattern_provider"), BrokenPatternProviderMenu::new, PatternProviderLogicHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<EntropyCradleControllerMenu>> ENTROPY_CRADLE_CONTROLLER_MENU =
            reg(id("entropy_cradle_controller"), EntropyCradleControllerMenu::new, EntropyCradleControllerBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<PlayerDataExtractorMenu>> PLAYER_DATA_EXTRACTOR_MENU =
            reg(id("player_data_extractor_menu"), PlayerDataExtractorMenu::new, PlayerDataExtractorPart.class);

    public static final DeferredHolder<MenuType<?>, MenuType<DataflowPatternMenu>> DATAFLOW_PATTERN_MENU =
            reg(id("dataflow_pattern_menu"), DataflowPatternMenu::new, DataflowPatternHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<DataProcessorMenu>> DATA_PROCESSOR_MENU =
            reg(id("data_processor_menu"), DataProcessorMenu::new, DataProcessorBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CrazyPatternModifierMenuPP>> CRAZY_PATTERN_MODIFIER_MENU_PP =
            reg(id("crazy_pattern_modifier_menu_pp"), CrazyPatternModifierMenuPP::new, CrazyPatternModifierHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<NbtViewCellMenu>> NBT_VIEW_CELL_MENU =
            reg(id("nbt_view_cell_menu"), NbtViewCellMenu::new, ViewCellHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<TagViewCellMenu>> TAG_VIEW_CELL_MENU =
            reg(id("tag_view_cell_menu"), TagViewCellMenu::new, ViewCellHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<CpuPrioMenu>> CPU_PRIO_MENU =
            reg(id("cpu_prio_menu"), CpuPrioMenu::new, CpuPrioHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<ResearchStationMenu>> RESEARCH_STATION_MENU =
            reg(id("research_station_menu"), ResearchStationMenu::new, ResearchStationBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<GadgetMenu>> GADGET_MENU =
            reg(id("gadget_menu"), GadgetMenu::new, GadgetHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<DataDriveMenu>> DATA_DRIVE_MENU =
            reg(id("data_drive_menu"), DataDriveMenu::new, DataHost.class);

    public static final DeferredHolder<MenuType<?>, MenuType<RecipeFabricatorMenu>> RECIPE_FABRICATOR_MENU =
            reg(id("recipe_fabricator_menu"), RecipeFabricatorMenu::new, RecipeFabricatorBE.class);

    public static final DeferredHolder<MenuType<?>, MenuType<MobKeySelectorMenu>> MOB_KEY_SELECTOR_MENU =
            reg(id("mob_key_selector_menu"), MobKeySelectorMenu::new, MobKeySelectorHost.class);

    private CrazyMenuRegistrar() {}
}
