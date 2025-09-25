package net.oktawia.crazyae2addons;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class CrazyConfig {
    public static final ModConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ModConfigSpec> pair = new ModConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {

        public final ModConfigSpec.BooleanValue enableCPP;
        public final ModConfigSpec.BooleanValue enablePeacefullSpawner;
        public final ModConfigSpec.BooleanValue enableEntityTicker;
        public final ModConfigSpec.IntValue     EntityTickerCost;
        public final ModConfigSpec.ConfigValue<List<? extends String>> EntityTickerBlackList;
        public final ModConfigSpec.BooleanValue NestedP2PWormhole;

        public final ModConfigSpec.IntValue     AutoEnchanterCost;
        public final ModConfigSpec.BooleanValue GregEnergyExporter;

        public final ModConfigSpec.IntValue     AutobuilderCostMult;
        public final ModConfigSpec.IntValue     AutobuilderMineDelay;
        public final ModConfigSpec.IntValue     AutobuilderSpeed;
        public final ModConfigSpec.IntValue     AutobuilderPreviewLimit;

        public final ModConfigSpec.IntValue     CrazyProviderMaxAddRows;

        public final ModConfigSpec.IntValue     CradleCapacity;
        public final ModConfigSpec.IntValue     CradleCost;
        public final ModConfigSpec.IntValue     CradleChargingSpeed;

        public final ModConfigSpec.IntValue     PenroseGenT0;
        public final ModConfigSpec.IntValue     PenroseGenT1;
        public final ModConfigSpec.IntValue     PenroseGenT2;
        public final ModConfigSpec.IntValue     PenroseGenT3;
        public final ModConfigSpec.ConfigValue<List<? extends String>> PenroseGoodFuel;
        public final ModConfigSpec.ConfigValue<List<? extends String>> PenroseBestFuel;

        public final ModConfigSpec.BooleanValue ResearchRequired;

        public final ModConfigSpec.IntValue     NokiaCost;

        public final ModConfigSpec.BooleanValue EnergyExporterEnabled;
        public final ModConfigSpec.BooleanValue EnergyInterfaceEnabled;

        public final ModConfigSpec.IntValue     FEp2pSpeed;
        public final ModConfigSpec.IntValue     Fluidp2pSpeed;
        public final ModConfigSpec.IntValue     Itemp2pSpeed;

        public Common(ModConfigSpec.Builder builder) {
            builder.comment("Crazy AE2 Addons - Config").push("general");

            builder.push("Features");
            enableCPP = builder
                    .comment("Enable Pattern Providers to set GregTech machine circuit when pushing")
                    .define("enableCPP", true);

            enablePeacefullSpawner = builder
                    .comment("Allow Spawner Controller to work in Peaceful mode")
                    .define("enablePeacefullSpawner", true);

            enableEntityTicker = builder
                    .comment("Enable/disable Entity Ticker")
                    .define("enableEntityTicker", true);

            EntityTickerCost = builder
                    .comment("Power cost multiplier for Entity Ticker")
                    .defineInRange("EntityTickerCost", 512, 0, Integer.MAX_VALUE);

            EntityTickerBlackList = builder
                    .comment("Blocks on which Entity Ticker should not work")
                    .defineList("EntityTickerBlackList", List.of(), o -> o instanceof String);

            NestedP2PWormhole = builder
                    .comment("Allow routing P2P tunnels through a Wormhole tunnel")
                    .define("nestedP2Pwormhole", false);
            builder.pop();


            builder.push("Machines");
            AutoEnchanterCost = builder
                    .comment("XP cost multiplier for Auto Enchanter")
                    .defineInRange("autoEnchanterCost", 10, 0, 100);

            GregEnergyExporter = builder
                    .comment("Allow Energy Exporter part to export EU if a GregTech battery is inserted")
                    .define("energyExporterGT", false);
            builder.pop();


            builder.push("Autobuilder");
            AutobuilderCostMult = builder
                    .comment("FE cost multiplier for Autobuilder")
                    .defineInRange("autobuilderCost", 5, 0, 100);

            AutobuilderMineDelay = builder
                    .comment("Ticks to wait after each broken block")
                    .defineInRange("autobuilderMineDelay", 2, 0, 10);

            AutobuilderSpeed = builder
                    .comment("Operations per tick Autobuilder can perform")
                    .defineInRange("autobuilderSpeed", 128, 0, Integer.MAX_VALUE);

            AutobuilderPreviewLimit = builder
                    .comment("How many preview blocks Autobuilder can show at once")
                    .defineInRange("autobuilderPreviewLimit", 8192, 0, Integer.MAX_VALUE);
            builder.pop();


            builder.push("CrazyPatternProvider");
            CrazyProviderMaxAddRows = builder
                    .comment("How many times player can upgrade the provider; -1 to disable limit")
                    .defineInRange("crazyProviderMaxAddRows", -1, -1, Integer.MAX_VALUE);
            builder.pop();


            builder.push("EntropyCradle");
            CradleCapacity = builder
                    .comment("How much FE Entropy Cradle can store")
                    .defineInRange("cradleCapacity", 600_000_000, 0, Integer.MAX_VALUE);

            CradleCost = builder
                    .comment("How much FE the cradle uses per operation")
                    .defineInRange("cradleCost", 600_000_000, 0, Integer.MAX_VALUE);

            CradleChargingSpeed = builder
                    .comment("How much FE per second the cradle can receive")
                    .defineInRange("cradleChargingSpeed", 50_000_000, 0, Integer.MAX_VALUE);
            builder.pop();


            builder.push("PenroseSphere");
            PenroseGenT0 = builder
                    .comment("Max FE production when capped (tier 0)")
                    .defineInRange("penroseGenT0", 262_144, 0, Integer.MAX_VALUE);

            PenroseGenT1 = builder
                    .comment("Max FE production when capped (tier 1)")
                    .defineInRange("penroseGenT1", 1_048_576, 0, Integer.MAX_VALUE);

            PenroseGenT2 = builder
                    .comment("Max FE production when capped (tier 2)")
                    .defineInRange("penroseGenT2", 4_194_304, 0, Integer.MAX_VALUE);

            PenroseGenT3 = builder
                    .comment("Max FE production when capped (tier 3)")
                    .defineInRange("penroseGenT3", 16_777_216, 0, Integer.MAX_VALUE);

            PenroseGoodFuel = builder
                    .comment("Fuel boosting production x4")
                    .defineList("penroseGoodFuel", List.of("ae2:matter_ball"), o -> o instanceof String);

            PenroseBestFuel = builder
                    .comment("Fuel boosting production x64")
                    .defineList("penroseBestFuel", List.of("ae2:singularity"), o -> o instanceof String);
            builder.pop();


            builder.push("Research");
            ResearchRequired = builder
                    .comment("Enable research mechanic (if false: Recipe Fabricator works without data drive)")
                    .define("researchEnabled", true);
            builder.pop();


            builder.push("Nokia");
            NokiaCost = builder
                    .comment("FE cost multiplier for Nokia 3310")
                    .defineInRange("nokiaCost", 5, 0, 100);
            builder.pop();


            builder.push("EnergyParts");
            EnergyExporterEnabled = builder
                    .comment("Enable Energy Exporter")
                    .define("energyExporterEnabled", true);

            EnergyInterfaceEnabled = builder
                    .comment("Enable Energy Interface")
                    .define("energyInterfaceEnabled", true);
            builder.pop();


            builder.push("P2PSpeeds");
            FEp2pSpeed = builder
                    .comment("Extract speed for FE P2P (FE/t)")
                    .defineInRange("fep2pSpeed", 1_048_576, 0, Integer.MAX_VALUE);

            Fluidp2pSpeed = builder
                    .comment("Extract speed for Fluid P2P (mB/t)")
                    .defineInRange("fluidp2pSpeed", 1024, 0, Integer.MAX_VALUE);

            Itemp2pSpeed = builder
                    .comment("Extract speed for Item P2P (items/t)")
                    .defineInRange("itemp2pSpeed", 16, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.pop();
        }
    }
}
