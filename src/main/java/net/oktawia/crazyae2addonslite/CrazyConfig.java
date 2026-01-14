package net.oktawia.crazyae2addonslite;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public class CrazyConfig {
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static class Common {

        public final ForgeConfigSpec.BooleanValue P2PWormholeNesting;
        public final ForgeConfigSpec.BooleanValue P2PWormholeTeleportation;
        public final ForgeConfigSpec.BooleanValue ImmersiveP2PWormhole;

        public final ForgeConfigSpec.BooleanValue GregWormholeEUP2P;
        public final ForgeConfigSpec.BooleanValue GregWormholeGoodEuP2P;

        public final ForgeConfigSpec.IntValue     CrazyProviderMaxAddRows;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Crazy AE2 Addons - Config").push("general");

            builder.push("Features");
            P2PWormholeNesting = builder
                    .comment("Allow routing P2P tunnels through a Wormhole tunnel")
                    .define("nestedP2Pwormhole", false);

            P2PWormholeTeleportation = builder
                    .comment("Allow teleporting through a Wormhole P2P")
                    .define("wormholeP2PTeleportation", true);

            ImmersiveP2PWormhole = builder
                    .comment("Create immersive portals on wormhole p2ps. Expect visual glitches with shaders.")
                    .define("immersiveP2PWormhole", false);

            GregWormholeEUP2P = builder
                    .comment("Allow Wormhole P2P to transfer EU")
                    .define("wormholeEUP2PGT", false);

            GregWormholeGoodEuP2P = builder
                    .comment("Allow Wormhole P2P to transfer EU to more than 1 output simultaneously")
                    .define("wormholeGoodEUP2PGT", false);

            CrazyProviderMaxAddRows = builder
                    .comment("How many times player can upgrade the provider; -1 to disable limit")
                    .defineInRange("crazyProviderMaxAddRows", -1, -1, Integer.MAX_VALUE);
            builder.pop();
        }
    }
}
