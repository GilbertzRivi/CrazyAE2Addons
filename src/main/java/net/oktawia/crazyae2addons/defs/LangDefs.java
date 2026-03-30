package net.oktawia.crazyae2addons.defs;

import appeng.core.localization.LocalizationEnum;

public enum LangDefs implements LocalizationEnum {
    LANG("key", "val"),
    PROVIDER_MAX("gui.crazyae2addons.provider_max", "Maximum capacity reached!"),
    CRAZY_PROVIDER_CAPACITY_TOOLTIP("gui.crazyae2addons.crazy_provider_capacity_tooltip", "Capacity: "),
    AMOUNT("gui.crazyae2addons.amount", "Amount"),
    TARGET("gui.crazyae2addons.target", "Target"),
    CRAFTING_SCHEDULER_TARGET("gui.crazyae2addons.crafting_scheduler_target", "Target"),
    MOD_NAME("gui.crazyae2addons.mod_name", "Crazy AE2 Addons"),
    SKIP_MISSING("gui.crazyae2addons.skip_missing", "Skip Missing"),
    SKIP_MISSING_TOOLTIP("gui.crazyae2addons.skip_missing_tooltip", "Skip blocks that are missing from the network"),
    MOVE_FORWARD("gui.crazyae2addons.move_forward", "Move forward"),
    MOVE_BACKWARD("gui.crazyae2addons.move_backward", "Move backward"),
    MOVE_RIGHT("gui.crazyae2addons.move_right", "Move right"),
    MOVE_LEFT("gui.crazyae2addons.move_left", "Move left"),
    MOVE_UP("gui.crazyae2addons.move_up", "Move up"),
    MOVE_DOWN("gui.crazyae2addons.move_down", "Move down"),
    SHOW_PREVIEW("gui.crazyae2addons.show_preview", "Show Preview"),
    HIDE_PREVIEW("gui.crazyae2addons.hide_preview", "Hide Preview"),
    AXIS_X("gui.crazyae2addons.axis_x", "X"),
    AXIS_Y("gui.crazyae2addons.axis_y", "Y"),
    AXIS_Z("gui.crazyae2addons.axis_z", "Z"),
    PATTERN("gui.crazyae2addons.pattern", "Pattern"),
    OFFSET("gui.crazyae2addons.offset", "Offset"),
    MISSING("gui.crazyae2addons.missing", "Missing:"),
    CONFIRM("gui.crazyae2addons.confirm", "Save"),
    FLIP_HORIZONTAL("gui.crazyae2addons.flip_horizontal", "Flip Horizontal"),
    FLIP_VERTICAL("gui.crazyae2addons.flip_vertical", "Flip Vertical"),
    ROTATE_CW("gui.crazyae2addons.rotate_cw", "Rotate CW"),
    RENAME("gui.crazyae2addons.rename", "Rename"),
    DELAY("gui.crazyae2addons.delay", "Tick Delay"),
    PROGRAM_SAVED("gui.crazyae2addons.program_saved", "Pattern saved! "),
    PROGRAM_INVALID("gui.crazyae2addons.program_invalid", "Pattern is invalid"),
    PROGRAM_NO_CODE("gui.crazyae2addons.program_no_code", "No program loaded"),
    PROGRAM_CHANGES_FAILED("gui.crazyae2addons.program_changes_failed", "Failed to apply changes"),
    CORNER_SET_A("gui.crazyae2addons.corner_set_a", "Corner A set"),
    CORNER_SET_B("gui.crazyae2addons.corner_set_b", "Corner B set (origin)"),
    CORNER_RESET("gui.crazyae2addons.corner_reset", "Corners reset"),
    ENERGY_NEEDED("gui.crazyae2addons.energy_needed", "Energy needed: "),
    NOTHING("gui.crazyae2addons.nothing", "nothing");

    private final String key;
    private final String value;

    LangDefs(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public String getTranslationKey() {
        return key;
    }

    @Override
    public String getEnglishText() {
        return value;
    }
}