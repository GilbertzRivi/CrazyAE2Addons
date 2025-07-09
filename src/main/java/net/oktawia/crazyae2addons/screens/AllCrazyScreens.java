package net.oktawia.crazyae2addons.screens;

import java.util.HashMap;

public class AllCrazyScreens {
    public static final HashMap<String, String> I18N = new HashMap<>();

    public static void loadAllClass() {
        try {
            Class.forName("net.oktawia.crazyae2addons.screens.AmpereMeterScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.AutoBuilderScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.AutoEnchanterScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.BrokenPatternProviderScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.ChunkyFluidP2PTunnelScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.CraftingSchedulerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.CrazyCalculatorScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.CrazyEmitterMultiplierScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.CrazyPatternModifierScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.CrazyPatternProviderScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.EjectorScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.EnergyExporterScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.EnergyStorageControllerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.EntityTickerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.EntropyCradleControllerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.ImpulsedPatternProviderScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.MEDataControllerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.MobFarmControllerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.PatternManagementUnitControllerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.RedstoneEmitterScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.RedstoneTerminalScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.ReinforcedMatterCondenserScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.RightClickProviderScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.SetStockAmountScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.SignallingInterfaceScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.SpawnerExtractorControllerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.VariableTerminalScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.WirelessRedstoneTerminalScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.BuilderPatternScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.CrazyPatternMultiplierScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.DataExtractorScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.DataflowPatternScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.DisplayScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.PlayerDataExtractorScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.NBTExportBusScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.NBTStorageBusScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.PenroseControllerScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.CrazyScreen");
            Class.forName("net.oktawia.crazyae2addons.screens.AllCrazyScreens");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Failed to load Crazy AE2 Addons screens", e);
        }
    }
}
