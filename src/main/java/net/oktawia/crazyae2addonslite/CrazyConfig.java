package net.oktawia.crazyae2addonslite;

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

        public final ForgeConfigSpec.BooleanValue NestedP2PWormhole;
        public final ForgeConfigSpec.BooleanValue P2PWormholeTeleportation;

        public final ForgeConfigSpec.BooleanValue GregWormholeEUP2P;
        public final ForgeConfigSpec.BooleanValue GregWormholeGoodEuP2P;

        public final ForgeConfigSpec.IntValue     CrazyProviderMaxAddRows;
        public final ForgeConfigSpec.IntValue     PortableSpatialStorageCostMult;

        public final ForgeConfigSpec.IntValue     FEp2pSpeed;
        public final ForgeConfigSpec.IntValue     Fluidp2pSpeed;
        public final ForgeConfigSpec.IntValue     Itemp2pSpeed;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment("Crazy AE2 Addons - Config").push("general");

            builder.push("Features");
            NestedP2PWormhole = builder
                    .comment("Allow routing P2P tunnels through a Wormhole tunnel")
                    .define("nestedP2Pwormhole", false);

            P2PWormholeTeleportation = builder
                    .comment("Allow teleporting through a Wormhole P2P")
                    .define("wormholeP2PTeleportation", false);

            GregWormholeEUP2P = builder
                    .comment("Allow Wormhole P2P to transfer EU")
                    .define("wormholeEUP2PGT", true);

            GregWormholeGoodEuP2P = builder
                    .comment("Allow Wormhole P2P to transfer EU to more than 1 output simultaneously")
                    .define("wormholeGoodEUP2PGT", false);

            CrazyProviderMaxAddRows = builder
                    .comment("How many times player can upgrade the provider; -1 to disable limit")
                    .defineInRange("crazyProviderMaxAddRows", -1, -1, Integer.MAX_VALUE);

            PortableSpatialStorageCostMult = builder
                    .comment("FE cost multiplier for Portable Spatial IO")
                    .defineInRange("portableSpatialStorageCostMult", 5, 0, 100);

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
        }
    }
}
