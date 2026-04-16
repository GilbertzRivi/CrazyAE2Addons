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
        public final ModConfigSpec.IntValue CrazyProviderMaxAddRows;
        public final ModConfigSpec.IntValue AutoEnchanterCost;
        public final ModConfigSpec.IntValue AutobuilderPreviewLimit;
        public final ModConfigSpec.DoubleValue AutobuilderCostMult;
        public final ModConfigSpec.IntValue AutobuilderSpeed;
        public final ModConfigSpec.IntValue AutobuilderMineDelay;
        public final ModConfigSpec.ConfigValue<List<? extends String>> PenroseGtTiers;

        // ===== Penrose Sphere =====
        public final ModConfigSpec.BooleanValue PenroseFEOutputEnabled;
        public final ModConfigSpec.BooleanValue PenroseMeltdownExplosionsEnabled;
        public final ModConfigSpec.IntValue     PenroseMeltdownFieldRadius;
        public final ModConfigSpec.LongValue    PenroseStartupCostSingu;
        public final ModConfigSpec.LongValue    PenroseInitialMassMu;
        public final ModConfigSpec.LongValue    PenroseMassWindowMu;
        public final ModConfigSpec.DoubleValue  PenroseMassFactorMax;
        public final ModConfigSpec.DoubleValue  PenroseDutyCompensation;
        public final ModConfigSpec.DoubleValue  PenroseFeBasePerSinguFlow;
        public final ModConfigSpec.DoubleValue  PenroseHeatPerSinguFlow;
        public final ModConfigSpec.DoubleValue  PenroseHeatPeakMK;
        public final ModConfigSpec.DoubleValue  PenroseMaxHeatMK;
        public final ModConfigSpec.IntValue     PenroseMaxFeedPerTick;

        public Common(ModConfigSpec.Builder builder) {
            builder.comment("Crazy AE2 Addons - Config").push("general");
            builder.push("Features");

            CrazyProviderMaxAddRows = builder
                    .comment("How many times player can upgrade the provider; -1 to disable limit")
                    .defineInRange("crazyProviderMaxAddRows", -1, -1, Integer.MAX_VALUE);

            AutoEnchanterCost = builder
                    .comment("XP cost multiplier for the Auto Enchanter (applied to base enchant cost)")
                    .defineInRange("autoEnchanterCost", 1, 1, Integer.MAX_VALUE);

            AutobuilderPreviewLimit = builder
                    .comment("Max number of blocks shown in AutoBuilder preview")
                    .defineInRange("autobuilderPreviewLimit", 4096, 1, Integer.MAX_VALUE);

            AutobuilderCostMult = builder
                    .comment("Energy cost multiplier per block placed/mined by AutoBuilder")
                    .defineInRange("autobuilderCostMult", 1.0, 0.0, Double.MAX_VALUE);

            AutobuilderSpeed = builder
                    .comment("Ticks of delay between AutoBuilder build steps (base)")
                    .defineInRange("autobuilderSpeed", 20, 1, Integer.MAX_VALUE);

            AutobuilderMineDelay = builder
                    .comment("Extra tick delay after mining a block")
                    .defineInRange("autobuilderMineDelay", 1, 1, Integer.MAX_VALUE);

            PenroseGtTiers = builder
                    .comment("GregTech energy hatch tiers accepted in Penrose Sphere structure.\n" +
                            "Used when a block ID contains '#' as a tier wildcard, e.g. \"gtceu:#_energy_output_hatch\".\n" +
                            "Each entry replaces '#' and the result is looked up in the block registry.")
                    .defineListAllowEmpty(
                            "penroseGtTiers",
                            List.of("ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv",
                                    "uhv", "uev", "uiv", "uxv", "opv", "max"),
                            () -> "ulv",
                            obj -> obj instanceof String s && !s.isBlank()
                    );

            builder.pop();

            // ===== Penrose Sphere =====
            builder.push("PenroseSphere");

            PenroseFEOutputEnabled = builder
                    .comment("Enable FE output from Penrose Sphere.")
                    .define("penroseFeOutputEnabled", true);

            PenroseMeltdownExplosionsEnabled = builder
                    .comment("Enable meltdown explosions and black hole field effect.")
                    .define("penroseMeltdownExplosionsEnabled", true);

            PenroseMeltdownFieldRadius = builder
                    .comment("Radius for BlackHoleManager.start() after meltdown. 0 = disabled.")
                    .defineInRange("penroseMeltdownFieldRadius", 768, 0, 4096);

            PenroseStartupCostSingu = builder
                    .comment("Super Singularity items consumed to start the black hole.")
                    .defineInRange("penroseStartupCostSingu", 32_512L, 0L, Long.MAX_VALUE);

            PenroseInitialMassMu = builder
                    .comment("Initial black hole mass at startup (MU).")
                    .defineInRange("penroseInitialMassMu", 32_768L * 8_192L, 0L, Long.MAX_VALUE);

            PenroseMassWindowMu = builder
                    .comment("Mass window above initial mass before meltdown (MU).")
                    .defineInRange("penroseMassWindowMu", 1_113_600L, 0L, Long.MAX_VALUE);

            PenroseMassFactorMax = builder
                    .comment("Max mass factor multiplier at sweet-spot (1.0 .. massFactorMax).")
                    .defineInRange("penroseMassFactorMax", 2.0, 1.0, 64.0);

            PenroseDutyCompensation = builder
                    .comment("Duty-cycle compensation multiplier in energy calculations. Default: 4/3.")
                    .defineInRange("penroseDutyCompensation", 4.0 / 3.0, 0.0, 1000.0);

            PenroseFeBasePerSinguFlow = builder
                    .comment("Base FE per tick per 1.0 singu/t disk flow at heatEff=1 massFactor=1.")
                    .defineInRange("penroseFeBasePerSinguFlow", (double) (1L << 27) * 0.5, 0.0, 1.0e18);

            PenroseHeatPerSinguFlow = builder
                    .comment("Heat added per tick per 1.0 singu/t disk flow (MK/t per singu/t).")
                    .defineInRange("penroseHeatPerSinguFlow", 0.5, 0.0, 1.0e12);

            PenroseHeatPeakMK = builder
                    .comment("Heat where efficiency peaks (heatEff=1). Units: MK. Default: 50000.")
                    .defineInRange("penroseHeatPeakMK", 50_000.0, 1.0, 1.0e12);

            PenroseMaxHeatMK = builder
                    .comment("Overheat threshold -> meltdown. Units: MK. Default: 100000.")
                    .defineInRange("penroseMaxHeatMK", 100_000.0, 0.0, 1.0e12);

            PenroseMaxFeedPerTick = builder
                    .comment("Hard cap on singularities injected per tick. Default: 4096.")
                    .defineInRange("penroseMaxFeedPerTick", 4_096, 0, Integer.MAX_VALUE);

            builder.pop(); // PenroseSphere
        }
    }
}
