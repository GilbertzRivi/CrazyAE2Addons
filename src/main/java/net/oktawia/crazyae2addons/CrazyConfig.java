package net.oktawia.crazyae2addons;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

public final class CrazyConfig {

    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Common COMMON;

    static {
        Pair<Common, ForgeConfigSpec> pair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = pair.getLeft();
        COMMON_SPEC = pair.getRight();
    }

    public static final class Common {

        public final ForgeConfigSpec.BooleanValue DISPLAY_ENABLED;
        public final ForgeConfigSpec.BooleanValue DISPLAY_IMAGES_ENABLED;
        public final ForgeConfigSpec.BooleanValue DISPLAY_STOCK_ENABLED;
        public final ForgeConfigSpec.BooleanValue DISPLAY_ICONS_ENABLED;
        public final ForgeConfigSpec.BooleanValue DISPLAY_DELTA_ENABLED;

        public final ForgeConfigSpec.BooleanValue EMITTER_TERMINAL_ENABLED;
        public final ForgeConfigSpec.BooleanValue WIRELESS_EMITTER_TERMINAL_ENABLED;

        public final ForgeConfigSpec.BooleanValue WIRELESS_NOTIFICATION_TERMINAL_ENABLED;
        public final ForgeConfigSpec.IntValue WIRELESS_NOTIFICATION_TERMINAL_CONFIG_SLOT;

        public final ForgeConfigSpec.BooleanValue MULTI_LEVEL_EMITTER_ENABLED;
        public final ForgeConfigSpec.IntValue MULTI_LEVEL_EMITTER_CONFIG_SLOT;

        public final ForgeConfigSpec.BooleanValue TAG_LEVEL_EMITTER_ENABLED;

        public final ForgeConfigSpec.BooleanValue REDSTONE_EMITTER_TERMINAL_ENABLED;
        public final ForgeConfigSpec.BooleanValue WIRELESS_REDSTONE_TERMINAL_ENABLED;

        public final ForgeConfigSpec.BooleanValue WORMHOLE_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_TELEPORTATION_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_NESTED_P2PS_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_ITEM_PROXY_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_FLUID_PROXY_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_FE_PROXY_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_EU_PROXY_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_OTHER_CAPABILITY_PROXY_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_MERGED_CAPABILITY_PROXY_ENABLED;
        public final ForgeConfigSpec.BooleanValue WORMHOLE_REMOTE_INTERACTIONS_ENABLED;

        public final ForgeConfigSpec.BooleanValue RR_ITEM_P2P_ENABLED;
        public final ForgeConfigSpec.BooleanValue RR_FLUID_P2P_ENABLED;

        public final ForgeConfigSpec.BooleanValue CPU_PRIORITIES_ENABLED;

        public final ForgeConfigSpec.BooleanValue TAG_VIEW_CELL_ENABLED;

        public final ForgeConfigSpec.BooleanValue PATTERN_MULTIPLIER_ENABLED;

        public final ForgeConfigSpec.BooleanValue CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED;
        public final ForgeConfigSpec.BooleanValue CRAZY_PATTERN_PROVIDER_PART_ENABLED;
        public final ForgeConfigSpec.IntValue CRAZY_PROVIDER_MAX_UPGRADES;

        public final ForgeConfigSpec.BooleanValue EJECTOR_ENABLED;
        public final ForgeConfigSpec.BooleanValue EJECTOR_CRAFT_MISSING_ENABLED;

        public Common(ForgeConfigSpec.Builder builder) {
            builder.comment(
                    "Crazy AE2 Addons - Common Config",
                    "All feature toggles and feature-specific options are defined here.",
                    "For every config entry that defines a limit, -1 means no limit."
            ).push("features");

            builder.comment(
                    "Display feature.",
                    "A part that renders dynamic text, images, and AE2-related data.",
                    "Supports token-based content, colors, icons, stock/delta rendering, and merged multi-display surfaces."
            ).push("display");

            DISPLAY_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the entire display feature."
            );

            DISPLAY_IMAGES_ENABLED = bool(builder,
                    "imagesEnabled", true,
                    "Enable or disable uploading and rendering custom images on displays."
            );

            DISPLAY_STOCK_ENABLED = bool(builder,
                    "stockEnabled", true,
                    "Enable or disable dynamic rendering of resource amounts from the connected network on displays."
            );

            DISPLAY_ICONS_ENABLED = bool(builder,
                    "iconsEnabled", true,
                    "Enable or disable rendering inline resource icons on displays."
            );

            DISPLAY_DELTA_ENABLED = bool(builder,
                    "deltaEnabled", true,
                    "Enable or disable dynamic rendering of resource amount changes over time on displays."
            );

            builder.pop();

            builder.comment(
                    "Emitter terminal feature.",
                    "The emitter terminal allows the player to access, query, and configure",
                    "all level emitters in the connected grid from a single terminal."
            ).push("emitterTerminal");

            EMITTER_TERMINAL_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the emitter terminal feature."
            );

            WIRELESS_EMITTER_TERMINAL_ENABLED = bool(builder,
                    "wirelessEnabled", true,
                    "Enable or disable the wireless variant."
            );

            builder.pop();

            builder.comment(
                    "Wireless notification terminal feature.",
                    "The wireless notification terminal allows the player to configure",
                    "tracked resources in the connected grid and display their amounts",
                    "when they are above or below a given threshold."
            ).push("wirelessNotificationTerminal");

            WIRELESS_NOTIFICATION_TERMINAL_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the wireless notification terminal feature."
            );

            WIRELESS_NOTIFICATION_TERMINAL_CONFIG_SLOT = nonNegativeInt(builder,
                    "configSlot", 16,
                    "Configure how many slots the wireless notification terminal should have."
            );

            builder.pop();

            builder.comment(
                    "Multi level emitter feature.",
                    "The multi level emitter tracks and triggers multiple conditions at once."
            ).push("multiLevelEmitter");

            MULTI_LEVEL_EMITTER_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the multi level emitter feature."
            );

            MULTI_LEVEL_EMITTER_CONFIG_SLOT = nonNegativeInt(builder,
                    "configSlot", 16,
                    "Configure how many slots the multi level emitter should have."
            );

            builder.pop();

            builder.comment(
                    "Tag level emitter feature.",
                    "The tag level emitter tracks resources based on a tag expression",
                    "and can monitor multiple resources at once."
            ).push("tagLevelEmitter");

            TAG_LEVEL_EMITTER_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the tag level emitter feature."
            );

            builder.pop();

            builder.comment(
                    "Redstone emitter / terminal feature.",
                    "The redstone terminal allows the player to control the redstone output",
                    "of connected redstone emitters."
            ).push("redstoneEmitterTerminal");

            REDSTONE_EMITTER_TERMINAL_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the redstone emitter / terminal feature."
            );

            WIRELESS_REDSTONE_TERMINAL_ENABLED = bool(builder,
                    "wirelessEnabled", true,
                    "Enable or disable the wireless terminal variant."
            );

            builder.pop();

            builder.comment(
                    "Wormhole feature.",
                    "Wormhole is a large feature that acts as a universal capability proxy",
                    "and also allows remote interactions or teleportation.",
                    "The tunnel always forwards from any output to the input,",
                    "or from the input to the closest output. It never splits",
                    "resources between multiple outputs. But can merge from many",
                    "outputs to the same input."
            ).push("wormhole");

            WORMHOLE_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the entire wormhole feature."
            );

            WORMHOLE_TELEPORTATION_ENABLED = bool(builder,
                    "teleportationEnabled", true,
                    "Enable or disable teleportation through wormholes.",
                    "This controls whether clicking on a wormhole with an ender pearl",
                    "teleports the player to the other side."
            );

            WORMHOLE_NESTED_P2PS_ENABLED = bool(builder,
                    "nestedP2psEnabled", false,
                    "Allow routing P2P tunnels through a wormhole tunnel."
            );

            WORMHOLE_ITEM_PROXY_ENABLED = bool(builder,
                    "itemProxyEnabled", true,
                    "Allow item proxying through wormholes."
            );

            WORMHOLE_FLUID_PROXY_ENABLED = bool(builder,
                    "fluidProxyEnabled", true,
                    "Allow fluid proxying through wormholes."
            );

            WORMHOLE_FE_PROXY_ENABLED = bool(builder,
                    "feProxyEnabled", true,
                    "Allow FE proxying through wormholes."
            );

            WORMHOLE_EU_PROXY_ENABLED = bool(builder,
                    "euProxyEnabled", true,
                    "Allow GregTech EU proxying through wormholes."
            );

            WORMHOLE_OTHER_CAPABILITY_PROXY_ENABLED = bool(builder,
                    "otherCapabilityProxyEnabled", true,
                    "Allow proxying of other capabilities through wormholes.",
                    "Example: Mekanism heat pipes."
            );

            WORMHOLE_MERGED_CAPABILITY_PROXY_ENABLED = bool(builder,
                    "mergedCapabilityProxyEnabled", true,
                    "Allow merged capability proxying through wormholes.",
                    "Allow merging all output handlers that are the same type (e.g. items)",
                    "so the input sees them all at the same time."
            );

            WORMHOLE_REMOTE_INTERACTIONS_ENABLED = bool(builder,
                    "remoteInteractionsEnabled", true,
                    "Allow remote interactions through wormholes.",
                    "This controls whether clicking on one side of a wormhole",
                    "opens the GUI of the machine on the other side."
            );

            builder.pop();

            builder.comment(
                    "Round robin P2P features.",
                    "Round robin P2P tunnels always split inputs evenly between all outputs,",
                    "even across multiple insertions."
            ).push("roundRobinP2p");

            RR_ITEM_P2P_ENABLED = bool(builder,
                    "itemEnabled", true,
                    "Enable or disable round robin item P2P."
            );

            RR_FLUID_P2P_ENABLED = bool(builder,
                    "fluidEnabled", true,
                    "Enable or disable round robin fluid P2P."
            );

            builder.pop();

            builder.comment(
                    "CPU priorities feature.",
                    "CPU priorities make higher-priority CPUs receive crafting jobs more often,",
                    "receive crafted results before lower-priority CPUs,",
                    "and use machines before lower-priority CPUs."
            ).push("cpuPriorities");

            CPU_PRIORITIES_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable CPU priorities."
            );

            builder.pop();

            builder.comment(
                    "Tag view cell feature.",
                    "The tag view cell filters the terminal it is placed in based on a tag expression",
                    "instead of selected individual resources."
            ).push("tagViewCell");

            TAG_VIEW_CELL_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable tag view cells."
            );

            builder.pop();

            builder.comment(
                    "Pattern multiplier feature.",
                    "The pattern multiplier allows the player to multiply patterns, multiply them up to a limit,",
                    "or clear all patterns inside it. It can also multiply all patterns in any container",
                    "such as a pattern provider or chest when shift-right-clicked on it."
            ).push("patternMultiplier");

            PATTERN_MULTIPLIER_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the pattern multiplier feature."
            );

            builder.pop();

            builder.comment(
                    "Crazy pattern provider features.",
                    "The crazy pattern provider starts with 72 pattern slots",
                    "and can be upgraded using crazy upgrades.",
                    "Each upgrade adds 1 row (9 slots) to the pattern inventory."
            ).push("crazyPatternProvider");

            CRAZY_PATTERN_PROVIDER_BLOCK_ENABLED = bool(builder,
                    "blockEnabled", true,
                    "Enable or disable the crazy pattern provider block."
            );

            CRAZY_PATTERN_PROVIDER_PART_ENABLED = bool(builder,
                    "partEnabled", true,
                    "Enable or disable the crazy pattern provider part."
            );

            CRAZY_PROVIDER_MAX_UPGRADES = unlimitedInt(builder,
                    "maxUpgrades", -1,
                    "Maximum number of upgrades allowed for the crazy pattern provider.",
                    "-1 means no limit.",
                    "Note: each upgrade adds 1 row (9 slots)."
            );

            builder.pop();

            builder.comment(
                    "Ejector feature.",
                    "The ejector allows the player to configure its 36-slot config inventory",
                    "with any amount of any resource. When provided with a redstone signal,",
                    "it ejects those resources from the connected network storage to",
                    "the adjacent inventory. It can also craft missing resources",
                    "if the system has the required patterns and ingredients.",
                    "Crafting is performed atomically as a single crafting operation",
                    "using one crafting job / CPU."
            ).push("ejector");

            EJECTOR_ENABLED = bool(builder,
                    "enabled", true,
                    "Enable or disable the ejector feature."
            );

            EJECTOR_CRAFT_MISSING_ENABLED = bool(builder,
                    "craftMissingEnabled", true,
                    "Allow the ejector to craft missing items."
            );

            builder.pop();

            builder.pop();
        }

        private static ForgeConfigSpec.BooleanValue bool(
                ForgeConfigSpec.Builder builder,
                String key,
                boolean defaultValue,
                String... comment
        ) {
            return builder.comment(comment).define(key, defaultValue);
        }

        private static ForgeConfigSpec.IntValue nonNegativeInt(
                ForgeConfigSpec.Builder builder,
                String key,
                int defaultValue,
                String... comment
        ) {
            return builder.comment(comment).defineInRange(key, defaultValue, 0, Integer.MAX_VALUE);
        }

        private static ForgeConfigSpec.IntValue unlimitedInt(
                ForgeConfigSpec.Builder builder,
                String key,
                int defaultValue,
                String... comment
        ) {
            return builder.comment(comment).defineInRange(key, defaultValue, -1, Integer.MAX_VALUE);
        }

        private static ForgeConfigSpec.DoubleValue nonNegativeDouble(
                ForgeConfigSpec.Builder builder,
                String key,
                double defaultValue,
                String... comment
        ) {
            return builder.comment(comment).defineInRange(key, defaultValue, 0.0D, Double.MAX_VALUE);
        }
    }

    private CrazyConfig() {
    }
}