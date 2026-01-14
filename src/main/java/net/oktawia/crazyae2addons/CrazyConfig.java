package net.oktawia.crazyae2addons;

import com.electronwill.nightconfig.core.Config;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.List;

public class CrazyConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {

        public final ForgeConfigSpec.BooleanValue enableCPP;
        public final ForgeConfigSpec.BooleanValue enablePeacefullSpawner;
        public final ForgeConfigSpec.BooleanValue enableEntityTicker;
        public final ForgeConfigSpec.IntValue     EntityTickerCost;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> EntityTickerBlackList;
        public final ForgeConfigSpec.BooleanValue P2PWormholeNesting;
        public final ForgeConfigSpec.BooleanValue P2PWormholeTeleportation;
        public final ForgeConfigSpec.BooleanValue ImmersiveP2PWormhole;

        public final ForgeConfigSpec.IntValue     AutoEnchanterCost;
        public final ForgeConfigSpec.BooleanValue GregEnergyExporter;
        public final ForgeConfigSpec.BooleanValue GregWormholeEUP2P;
        public final ForgeConfigSpec.BooleanValue GregWormholeGoodEuP2P;

        public final ForgeConfigSpec.IntValue     AutobuilderCostMult;
        public final ForgeConfigSpec.IntValue     AutobuilderMineDelay;
        public final ForgeConfigSpec.IntValue     AutobuilderSpeed;
        public final ForgeConfigSpec.IntValue     AutobuilderPreviewLimit;

        public final ForgeConfigSpec.IntValue     CrazyProviderMaxAddRows;

        public final ForgeConfigSpec.IntValue     CradleCapacity;
        public final ForgeConfigSpec.IntValue     CradleCost;
        public final ForgeConfigSpec.IntValue     CradleChargingSpeed;

        public final ForgeConfigSpec.BooleanValue PenroseMeltdownExplosionsEnabled;
        public final ForgeConfigSpec.IntValue     PenroseMeltdownFieldRadius;

        public final ForgeConfigSpec.LongValue    PenroseStartupCostSingu;

        public final ForgeConfigSpec.LongValue    PenroseInitialMassMu;
        public final ForgeConfigSpec.LongValue    PenroseMassWindowMu;
        public final ForgeConfigSpec.DoubleValue  PenroseMassFactorMax;

        public final ForgeConfigSpec.DoubleValue  PenroseDutyCompensation;
        public final ForgeConfigSpec.DoubleValue  PenroseFeBasePerSinguFlow;
        public final ForgeConfigSpec.DoubleValue  PenroseHeatPerSinguFlow;

        public final ForgeConfigSpec.DoubleValue  PenroseHeatPeakMK;
        public final ForgeConfigSpec.DoubleValue  PenroseMaxHeatMK;

        public final ForgeConfigSpec.IntValue     PenroseMaxFeedPerTick;

        public final ForgeConfigSpec.BooleanValue ResearchRequired;

        public final ForgeConfigSpec.IntValue     PortableSpatialStorageCostMult;

        public final ForgeConfigSpec.BooleanValue EnergyExporterEnabled;
        public final ForgeConfigSpec.BooleanValue EnergyInterfaceEnabled;

        public final ForgeConfigSpec.IntValue     FEp2pSpeed;
        public final ForgeConfigSpec.IntValue     Fluidp2pSpeed;
        public final ForgeConfigSpec.IntValue     Itemp2pSpeed;

        public final ForgeConfigSpec.ConfigValue<UnmodifiableConfig> ResearchUnitExtraQBlocks;


        public Common(ForgeConfigSpec.Builder builder) {
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

            P2PWormholeNesting = builder
                    .comment("Allow routing P2P tunnels through a Wormhole tunnel")
                    .define("nestedP2Pwormhole", false);

            P2PWormholeTeleportation = builder
                    .comment("Allow teleporting through a Wormhole P2P")
                    .define("wormholeP2PTeleportation", true);


            ImmersiveP2PWormhole = builder
                    .comment("Create immersive portals on wormhole p2ps. Expect visual glitches with shaders.")
                    .define("immersiveP2PWormhole", false);
            builder.pop();


            builder.push("Machines");
            AutoEnchanterCost = builder
                    .comment("XP cost multiplier for Auto Enchanter")
                    .defineInRange("autoEnchanterCost", 10, 0, 100);

            GregEnergyExporter = builder
                    .comment("Allow Energy Exporter part to export EU if a GregTech battery is inserted")
                    .define("energyExporterGT", false);

            GregWormholeEUP2P = builder
                    .comment("Allow Wormhole P2P to transfer EU")
                    .define("wormholeEUP2PGT", true);

            GregWormholeGoodEuP2P = builder
                    .comment("Allow Wormhole P2P to transfer EU to more than 1 output simultaneously")
                    .define("wormholeGoodEUP2PGT", false);
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


            // ===== Penrose Controller (multiblock) =====
            builder.push("PenroseSphere");

            builder.push("Meltdown");
            PenroseMeltdownExplosionsEnabled = builder
                    .comment(
                            "Enable/disable meltdown world effect (explosions + black hole field).",
                            "Default: true."
                    )
                    .define("penroseMeltdownExplosionsEnabled", true);
            PenroseMeltdownFieldRadius = builder
                    .comment(
                            "Radius used by BlackHoleManager.start(...) after meltdown.",
                            "Set to 0 to disable the field entirely."
                    )
                    .defineInRange("penroseMeltdownFieldRadius", 768, 0, 4096);
            builder.pop();

            builder.push("Startup");
            PenroseStartupCostSingu = builder
                    .comment(
                            "How many Super Singularity items are consumed to start the black hole.",
                            "Units: items. Default: 32512."
                    )
                    .defineInRange("penroseStartupCostSingu", 32_512L, 0L, Long.MAX_VALUE);
            builder.pop();

            builder.push("Balance");
            PenroseInitialMassMu = builder
                    .comment(
                            "Initial black hole mass at startup.",
                            "Units: MU (internal mass unit). Default matches old hardcoded value."
                    )
                    .defineInRange("penroseInitialMassMu", 32_768L * 8_192L, 0L, Long.MAX_VALUE);

            PenroseMassWindowMu = builder
                    .comment(
                            "Allowed mass window above initial mass before meltdown triggers.",
                            "Max mass = initialMassMu + massWindowMu.",
                            "Units: MU. Default: 1113600."
                    )
                    .defineInRange("penroseMassWindowMu", 1_113_600L, 0L, Long.MAX_VALUE);

            PenroseMassFactorMax = builder
                    .comment(
                            "Max mass factor multiplier at the sweet-spot (peak of the mass curve).",
                            "massFactorSweet(): 1.0 .. massFactorMax.",
                            "Default: 2.0 (matches current)."
                    )
                    .defineInRange("penroseMassFactorMax", 2.0, 1.0, 64.0);

            PenroseDutyCompensation = builder
                    .comment(
                            "Duty-cycle compensation multiplier used in energy calculations.",
                            "Default: 4/3 (~1.3333333)."
                    )
                    .defineInRange("penroseDutyCompensation", 4.0 / 3.0, 0.0, 1000.0);

            PenroseFeBasePerSinguFlow = builder
                    .comment(
                            "Base FE produced per 1.0 singu/t disk flow at heatEff=1 and massFactor=1.",
                            "Units: FE per tick per (singu/t).",
                            "Default equals current PenroseControllerBE FE_BASE_PER_SINGU."
                    )
                    .defineInRange("penroseFeBasePerSinguFlow", (double) (1L << 27) * 0.5, 0.0, 1.0e18);

            PenroseHeatPerSinguFlow = builder
                    .comment(
                            "Heat added per tick per 1.0 singu/t disk flow at massFactor=1.",
                            "Units: MK per tick per (singu/t)."
                    )
                    .defineInRange("penroseHeatPerSinguFlow", 0.5, 0.0, 1.0e12);

            PenroseHeatPeakMK = builder
                    .comment(
                            "Heat value where the efficiency curve peaks (heatEff reaches 1.0 at heat=peak).",
                            "Used by computeHeatEff(): x=heat/peak; eff=2x-x^2 clamped to [0..1].",
                            "Units: MK. Default: 50000."
                    )
                    .defineInRange("penroseHeatPeakMK", 50_000.0, 1.0, 1.0e12);

            PenroseMaxHeatMK = builder
                    .comment(
                            "Overheat threshold. If heat >= maxHeat -> meltdown.",
                            "Units: MK. Default: 100000."
                    )
                    .defineInRange("penroseMaxHeatMK", 100_000.0, 0.0, 1.0e12);

            PenroseMaxFeedPerTick = builder
                    .comment(
                            "Hard cap on how many singularities per tick can be injected into the disk.",
                            "Units: items/t. Default: 4096."
                    )
                    .defineInRange("penroseMaxFeedPerTick", 4_096, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.pop(); // PenroseController


            builder.push("Research");
            ResearchRequired = builder
                    .comment("Enable research mechanic (if false: Recipe Fabricator works without data drive)")
                    .define("researchEnabled", true);
            ResearchUnitExtraQBlocks = builder
                    .comment(
                            "Extra blocks allowed in the Research Unit multiblock in 'Q' slots.",
                            "Format: TOML map of \"namespace:block\" -> integer.",
                            "The integer is currently used as a multiplier for computation (count * value).",
                            "Keys with ':' must be quoted, e.g. { \"minecraft:glass\" = 1 }.",
                            "Default: empty map."
                    )
                    .define(
                            "researchUnitExtraQBlocks",
                            Config.inMemory(),
                            o -> {
                                if (!(o instanceof UnmodifiableConfig c)) return false;
                                for (var e : c.valueMap().entrySet()) {
                                    String key = e.getKey();
                                    Object val = e.getValue();

                                    if (key == null || ResourceLocation.tryParse(key) == null) return false;
                                    if (!(val instanceof Number)) return false; // TOML może dać Int/Long
                                }
                                return true;
                            }
                    );
            builder.pop();


            builder.push("Portable Spatial IO");
            PortableSpatialStorageCostMult = builder
                    .comment("FE cost multiplier for Portable Spatial IO")
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
                    .defineInRange("fep2pSpeed", 262144, 0, Integer.MAX_VALUE);

            Fluidp2pSpeed = builder
                    .comment("Extract speed for Fluid P2P (mB/t)")
                    .defineInRange("fluidp2pSpeed", 500, 0, Integer.MAX_VALUE);

            Itemp2pSpeed = builder
                    .comment("Extract speed for Item P2P (items/t)")
                    .defineInRange("itemp2pSpeed", 16, 0, Integer.MAX_VALUE);
            builder.pop();
            builder.pop();
        }
    }
}
