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
        public final ModConfigSpec.BooleanValue P2PWormholeNesting;
        public final ModConfigSpec.BooleanValue P2PWormholeTeleportation;
        public final ModConfigSpec.BooleanValue EnergyInterfaceEnabled;
        public final ModConfigSpec.BooleanValue EnergyExporterEnabled;

        public final ModConfigSpec.IntValue CrazyProviderMaxAddRows;

        public final ModConfigSpec.IntValue AutobuilderPreviewLimit;
        public final ModConfigSpec.DoubleValue AutobuilderCostMult;
        public final ModConfigSpec.IntValue AutobuilderSpeed;
        public final ModConfigSpec.IntValue AutobuilderMineDelay;

        public final ModConfigSpec.ConfigValue<List<? extends String>> PenroseGtTiers;

        public Common(ModConfigSpec.Builder builder) {
            builder.comment("Crazy AE2 Addons - Config").push("general");
            builder.push("Features");

            P2PWormholeNesting = builder
                    .comment("Allow routing P2P tunnels through a Wormhole tunnel")
                    .define("nestedP2Pwormhole", false);

            P2PWormholeTeleportation = builder
                    .comment("Allow teleporting through a Wormhole P2P")
                    .define("wormholeP2PTeleportation", true);

            EnergyInterfaceEnabled = builder
                    .comment("Enable Energy Interface Part - exposes Forge Energy capability to AE2 grid")
                    .define("energyInterfaceEnabled", true);

            EnergyExporterEnabled = builder
                    .comment("Enable Energy Exporter Part - exports energy from ME to adjacent block")
                    .define("energyExporterEnabled", true);

            CrazyProviderMaxAddRows = builder
                    .comment("How many times player can upgrade the provider; -1 to disable limit")
                    .defineInRange("crazyProviderMaxAddRows", -1, -1, Integer.MAX_VALUE);

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
                    .defineListAllowEmpty("penroseGtTiers",
                            List.of("ulv", "lv", "mv", "hv", "ev", "iv", "luv", "zpm", "uv",
                                    "uhv", "uev", "uiv", "uxv", "opv", "max"),
                            obj -> obj instanceof String s && !s.isBlank());

            builder.pop();
        }
    }
}
