package net.oktawia.crazyae2addonslite;

import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

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

        public final ModConfigSpec.IntValue CrazyProviderMaxAddRows;

        public Common(ModConfigSpec.Builder builder) {
            builder.comment("Crazy AE2 Addons lite - Config").push("general");
            builder.push("Features");

            P2PWormholeNesting = builder
                    .comment("Allow routing P2P tunnels through a Wormhole tunnel")
                    .define("nestedP2Pwormhole", false);

            P2PWormholeTeleportation = builder
                    .comment("Allow teleporting through a Wormhole P2P")
                    .define("wormholeP2PTeleportation", true);

            CrazyProviderMaxAddRows = builder
                    .comment("How many times player can upgrade the provider; -1 to disable limit")
                    .defineInRange("crazyProviderMaxAddRows", -1, -1, Integer.MAX_VALUE);

            builder.pop();
        }
    }
}
