package net.oktawia.crazyae2addons.defs;

import appeng.init.client.InitScreens;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.*;
import net.oktawia.crazyae2addons.mobstorage.MobExportBusMenu;
import net.oktawia.crazyae2addons.mobstorage.MobExportBusScreen;
import net.oktawia.crazyae2addons.mobstorage.MobFormationPlaneMenu;
import net.oktawia.crazyae2addons.mobstorage.MobFormationPlaneScreen;
import net.oktawia.crazyae2addons.screens.*;

public final class Screens {

    public static void register() {

        InitScreens.register(CrazyMenuRegistrar.ENTITY_TICKER_MENU.get(),
                EntityTickerScreen<EntityTickerMenu>::new,
                "/screens/entity_ticker.json");

        InitScreens.register(CrazyMenuRegistrar.NBT_EXPORT_BUS_MENU.get(),
                NBTExportBusScreen<NBTExportBusMenu>::new,
                "/screens/nbt_export_bus.json");

        InitScreens.register(CrazyMenuRegistrar.NBT_STORAGE_BUS_MENU.get(),
                NBTStorageBusScreen<NBTStorageBusMenu>::new,
                "/screens/nbt_storage_bus.json");

        InitScreens.register(CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU.get(),
                CrazyPatternModifierScreen<CrazyPatternModifierMenu>::new,
                "/screens/crazy_pattern_modifier.json");

        InitScreens.register(CrazyMenuRegistrar.DISPLAY_MENU.get(),
                DisplayScreen<DisplayMenu>::new,
                "/screens/display.json");

        InitScreens.register(CrazyMenuRegistrar.CHUNKY_FLUID_P2P_TUNNEL_MENU.get(),
                ChunkyFluidP2PTunnelScreen<ChunkyFluidP2PTunnelMenu>::new,
                "/screens/chunky_fluid_p2p_tunnel.json");

        InitScreens.register(CrazyMenuRegistrar.ENERGY_EXPORTER_MENU.get(),
                EnergyExporterScreen<EnergyExporterMenu>::new,
                "/screens/energy_exporter.json");

        InitScreens.register(CrazyMenuRegistrar.AMPERE_METER_MENU.get(),
                AmpereMeterScreen<AmpereMeterMenu>::new,
                "/screens/ampere_meter.json");

        InitScreens.register(CrazyMenuRegistrar.CRAZY_PATTERN_MULTIPLIER_MENU.get(),
                CrazyPatternMultiplierScreen<CrazyPatternMultiplierMenu>::new,
                "/screens/crazy_pattern_multiplier.json");

        InitScreens.register(CrazyMenuRegistrar.MOB_EXPORT_BUS_MENU.get(),
                MobExportBusScreen<MobExportBusMenu>::new,
                "/screens/mob_export_bus.json");

        InitScreens.register(
                CrazyMenuRegistrar.AUTO_ENCHANTER_MENU.get(),
                AutoEnchanterScreen<AutoEnchanterMenu>::new,
                "/screens/auto_enchanter.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.CRAZY_EMITTER_MULTIPLIER_MENU.get(),
                CrazyEmitterMultiplierScreen<CrazyEmitterMultiplierMenu>::new,
                "/screens/crazy_emitter_multiplier.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.CRAZY_CALCULATOR_MENU.get(),
                CrazyCalculatorScreen<CrazyCalculatorMenu>::new,
                "/screens/crazy_calculator.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.EJECTOR_MENU.get(),
                EjectorScreen<EjectorMenu>::new,
                "/screens/ejector.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.MOB_FORMATION_PLANE_MENU.get(),
                MobFormationPlaneScreen<MobFormationPlaneMenu>::new,
                "/screens/mob_formation_plane.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.SPAWNER_EXTRACTOR_CONTROLLER_MENU.get(),
                SpawnerExtractorControllerScreen<SpawnerExtractorControllerMenu>::new,
                "/screens/spawner_controller.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.MOB_FARM_CONTROLLER_MENU.get(),
                MobFarmControllerScreen<MobFarmControllerMenu>::new,
                "/screens/mob_farm_controller.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.CRAFTING_SCHEDULER_MENU.get(),
                CraftingSchedulerScreen<CraftingSchedulerMenu>::new,
                "/screens/crafting_scheduler.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.REDSTONE_EMITTER_MENU.get(),
                RedstoneEmitterScreen<RedstoneEmitterMenu>::new,
                "/screens/redstone_emitter.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.REDSTONE_TERMINAL_MENU.get(),
                RedstoneTerminalScreen<RedstoneTerminalMenu>::new,
                "/screens/redstone_terminal.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.WIRELESS_REDSTONE_TERMINAL_MENU.get(),
                WirelessRedstoneTerminalScreen<WirelessRedstoneTerminalMenu>::new,
                "/screens/wireless_redstone_terminal.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.REINFORCED_MATTER_CONDENSER_MENU.get(),
                ReinforcedMatterCondenserScreen<ReinforcedMatterCondenserMenu>::new,
                "/screens/reinforced_matter_condenser.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(),
                PenroseControllerScreen<PenroseControllerMenu>::new,
                "/screens/penrose_controller.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(),
                CrazyPatternProviderScreen<CrazyPatternProviderMenu>::new,
                "/screens/crazy_pattern_provider.json"
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
                CrazyMenuRegistrar.BROKEN_PATTERN_PROVIDER_MENU.get(),
                BrokenPatternProviderScreen<BrokenPatternProviderMenu>::new,
                "/screens/broken_pattern_provider.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.ENTROPY_CRADLE_CONTROLLER_MENU.get(),
                EntropyCradleControllerScreen<EntropyCradleControllerMenu>::new,
                "/screens/entropy_cradle_controller.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU_PP.get(),
                CrazyPatternModifierScreenPP<CrazyPatternModifierMenuPP>::new,
                "/screens/crazy_pattern_modifier_pp.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.NBT_VIEW_CELL_MENU.get(),
                NbtViewCellScreen<NbtViewCellMenu>::new,
                "/screens/view_cell.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.TAG_VIEW_CELL_MENU.get(),
                TagViewCellScreen<TagViewCellMenu>::new,
                "/screens/view_cell.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.CPU_PRIO_MENU.get(),
                CpuPrioScreen<CpuPrioMenu>::new,
                "/screens/cpu_prio.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.RESEARCH_STATION_MENU.get(),
                ResearchStationScreen<ResearchStationMenu>::new,
                "/screens/research_station.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.GADGET_MENU.get(),
                PortableSpatialStorageScreen<PortableSpatialStorageMenu>::new,
                "/screens/portable_spatial_storage.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.COPY_GADGET_MENU.get(),
                PortableAutobuilderScreen<PortableAutobuilderMenu>::new,
                "/screens/portable_builder.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.DATA_DRIVE_MENU.get(),
                DataDriveScreen<DataDriveMenu>::new,
                "/screens/data_drive.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.RECIPE_FABRICATOR_MENU.get(),
                RecipeFabricatorScreen<RecipeFabricatorMenu>::new,
                "/screens/fabricator.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.MOB_KEY_SELECTOR_MENU.get(),
                MobKeySelectorScreen<MobKeySelectorMenu>::new,
                "/screens/mob_key_selector.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.RESEARCH_UNIT_MENU.get(),
                ResearchUnitScreen<ResearchUnitMenu>::new,
                "/screens/research_unit.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.RESEARCH_PEDESTAL_MENU.get(),
                ResearchPedestalScreen<ResearchPedestalMenu>::new,
                "/screens/research_pedestal.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.PENROSE_MASS_EMITTER_MENU.get(),
                PenroseMassEmitterScreen<PenroseMassEmitterMenu>::new,
                "/screens/penrose_mass_emitter.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.PENROSE_HEAT_EMITTER_MENU.get(),
                PenroseHeatEmitterScreen<PenroseHeatEmitterMenu>::new,
                "/screens/penrose_heat_emitter.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.PENROSE_INJECTION_PORT_MENU.get(),
                PenroseInjectionPortScreen<PenroseInjectionPortMenu>::new,
                "/screens/penrose_injection_port.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.PENROSE_HEAT_VENT_MENU.get(),
                PenroseHeatVentScreen<PenroseHeatVentMenu>::new,
                "/screens/penrose_heat_vent.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.PENROSE_HAWKING_VENT_MENU.get(),
                PenroseHawkingVentScreen<PenroseHawkingVentMenu>::new,
                "/screens/penrose_hawking_vent.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.WIRELESS_NOTIFICATION_TERMINAL_MENU.get(),
                WirelessNotificationTerminalScreen<WirelessNotificationTerminalMenu>::new,
                "/screens/wireless_notification_terminal.json"
        );

        InitScreens.register(
                CrazyMenuRegistrar.MULTI_LEVEL_EMITTER_MENU.get(),
                MultiLevelEmitterScreen<MultiLevelEmitterMenu>::new,
                "/screens/multi_level_emitter.json"
        );
    }

    private Screens() {}
}