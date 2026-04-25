package net.oktawia.crazyae2addons.util;

import java.util.List;

public final class StructureToolKeys {
    private StructureToolKeys() {}

    public static final List<String> AE2_CABLE_BUS_KEYS = List.of(
            "cable", "north", "south", "east", "west", "up", "down"
    );

    public static final String CLONE_METADATA_KEY        = "clone_metadata";
    public static final String CLONE_METADATA_BLOCKS_KEY = "blocks";
    public static final String CLONE_REQUIREMENTS_KEY    = "requirements";
    public static final String CLONE_KEY_POS             = "pos";
    public static final String CLONE_KEY_SETTINGS        = "settings";
    public static final String CLONE_KEY_UPGRADES        = "upgrades";
    public static final String CLONE_KEY_PARTS           = "parts";
    public static final String CLONE_KEY_STACK           = "stack";
    public static final String CLONE_KEY_COUNT           = "count";

    public static final String GTCEU_ID_PREFIX  = "gtceu:";
    public static final String GT_CABLE_ID      = "gtceu:cable";
    public static final String GT_ITEM_PIPE_ID  = "gtceu:item_pipe";
    public static final String GT_FLUID_PIPE_ID = "gtceu:fluid_pipe";

    public static final String CLONE_KEY_GREG         = "greg";
    public static final String CLONE_KEY_GREG_COVER   = "cover";
    public static final String CLONE_KEY_GREG_PIPE    = "pipe";
    public static final String CLONE_KEY_GREG_MACHINE = "machine";
}
