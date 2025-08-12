package net.oktawia.crazyae2addons;

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
        public final ForgeConfigSpec.IntValue EntityTickerCost;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> EntityTickerBlackList;
        public final ForgeConfigSpec.BooleanValue NestedP2PWormhole;
        public final ForgeConfigSpec.IntValue PatternUnitCapacity;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Crazy AE2 Addons - Config").push("general");

            enableCPP = builder
                .comment("Enables The functionality of pattern providers to set the circuit of the GregTech's machine its pushing to")
                .define("enableCPP", true);

            enablePeacefullSpawner = builder
                .comment("Enables Spawner Controller to work also on peacefull mode")
                .define("enablePeacefullSpawner", true);

            enableEntityTicker = builder
                .comment("Enables/disables the entity ticker")
                .define("enableEntityTicker", true);

            EntityTickerCost = builder
                .comment("You can set the power cost multiplayer for the entity ticker here")
                .defineInRange("EntityTickerCost", 512, 0, 4096);

            EntityTickerBlackList = builder
                .comment("Blocks on which entity ticker should not work")
                    .defineList(
                        "EntityTickerBlackList",
                        List.of(),
                        o -> o instanceof String
                    );

            NestedP2PWormhole = builder
                    .comment("If it should be possible to route p2p tunnels through a wormhole tunnel")
                            .define("nestedP2Pwormhole", false);

            PatternUnitCapacity = builder
                    .comment("Amount of rows for patterns stored by the Pattern management unit")
                        .defineInRange("PUnitCapacity", 216, 8, 1024);

            builder.pop();
        }
    }
}
