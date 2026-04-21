package net.oktawia.crazyae2addons.defs;

import appeng.core.localization.LocalizationEnum;

public enum LangDefs implements LocalizationEnum {
    LANG("key", "val"),
    PROVIDER_MAX("gui.crazyae2addons.provider_max", "Maximum capacity reached!"),
    CRAZY_PROVIDER_CAPACITY_TOOLTIP("gui.crazyae2addons.crazy_provider_capacity_tooltip", "Capacity: "),
    MOD_NAME("gui.crazyae2addons.mod_name", "Crazy AE2 Addons"),
    PATTERNS("gui.crazyae2addons.patterns", "Patterns"),
    NOTHING("gui.crazyae2addons.nothing", "Nothing"),
    EJECTOR_LOAD_PATTERN("gui.crazyae2addons.ejector_load_pattern", "Load settings from pattern"),
    EJECTOR_MIDDLE_CLICK("gui.crazyae2addons.ejector_middle_click", "Middle mouse button to set amount"),
    BACK("gui.crazyae2addons.back", "Back"),
    ICON("gui.crazyae2addons.icon", "Icon"),
    STOCK("gui.crazyae2addons.stock", "Stock"),
    DELTA("gui.crazyae2addons.delta", "Delta"),
    INSERT("gui.crazyae2addons.insert", "Insert"),
    ANY("gui.crazyae2addons.any", "Any"),
    ITEM_ID_LABEL("gui.crazyae2addons.item_id_label", "Item ID:"),
    TYPE_LABEL("gui.crazyae2addons.type_label", "Type:"),
    DIVISOR_LABEL("gui.crazyae2addons.divisor_label", "Divisor:"),
    PER_LABEL("gui.crazyae2addons.per_label", "Per:"),
    WINDOW_LABEL("gui.crazyae2addons.window_label", "Window:"),
    KEY_TYPES_COLLAPSED("gui.crazyae2addons.key_types_collapsed", "Key types ▼"),
    KEY_TYPES_EXPANDED("gui.crazyae2addons.key_types_expanded", "Key types ▲"),
    TYPES_HEADER("gui.crazyae2addons.types_header", "Types"),
    DIV_RAW("gui.crazyae2addons.div_raw", "raw"),
    DIV_10("gui.crazyae2addons.div_10", "/10"),
    DIV_100("gui.crazyae2addons.div_100", "/100"),
    DIV_1K("gui.crazyae2addons.div_1k", "/1k"),
    DIV_10K("gui.crazyae2addons.div_10k", "/10k"),
    DIV_100K("gui.crazyae2addons.div_100k", "/100k"),
    DIV_1M("gui.crazyae2addons.div_1m", "/1M"),
    UNIT_TICKS("gui.crazyae2addons.unit_ticks", "t"),
    UNIT_SECONDS("gui.crazyae2addons.unit_seconds", "s"),
    UNIT_MINUTES("gui.crazyae2addons.unit_minutes", "m"),
    SAVE("gui.crazyae2addons.save", "Save"),
    INSERT_TOKEN("gui.crazyae2addons.insert_token", "Insert Token"),
    JOIN_DISPLAYS("gui.crazyae2addons.join_displays", "Join Displays"),
    ADD_MARGIN("gui.crazyae2addons.add_margin", "Add 3% Margin"),
    CENTER_TEXT("gui.crazyae2addons.center_text", "Center Text"),
    PICK_FILE("gui.crazyae2addons.pick_file", "Pick File"),
    REMOVE("gui.crazyae2addons.remove", "Remove"),
    IMAGES("gui.crazyae2addons.images", "Images"),
    BACKGROUND_COLOR("gui.crazyae2addons.background_color", "Background Color"),
    CHANGE_SELECTED_TEXT_COLOR("gui.crazyae2addons.change_selected_text_color", "Change Selected Text Color"),
    UNSAVED_CHANGES_TITLE("gui.crazyae2addons.unsaved_changes_title", "Are you sure?"),
    UNSAVED_CHANGES_TEXT("gui.crazyae2addons.unsaved_changes_text", "You have not saved the changes."),
    PICK_FILE_TOOLTIP("gui.crazyae2addons.pick_file_tooltip", "Pick and upload image"),
    REMOVE_IMAGE_TOOLTIP("gui.crazyae2addons.remove_image_tooltip", "Remove selected image"),
    IMAGE_LIST_TOOLTIP("gui.crazyae2addons.image_list_tooltip", "Uploaded images"),
    X_PERCENT("gui.crazyae2addons.x_percent", "X %"),
    Y_PERCENT("gui.crazyae2addons.y_percent", "Y %"),
    SCALE_PERCENT("gui.crazyae2addons.scale_percent", "Scale %"),
    X_PERCENT_TOOLTIP("gui.crazyae2addons.x_percent_tooltip", "Horizontal position in percent. 0 = left edge, 100 = right edge"),
    Y_PERCENT_TOOLTIP("gui.crazyae2addons.y_percent_tooltip", "Vertical position in percent. 0 = top edge, 100 = bottom edge"),
    SCALE_PERCENT_TOOLTIP("gui.crazyae2addons.scale_percent_tooltip", "Image scale in percent"),
    IMAGE_UPLOAD_OK("gui.crazyae2addons.image_upload_ok", "Image uploaded"),
    IMAGE_UPLOAD_CANCELLED("gui.crazyae2addons.image_upload_cancelled", "Image selection cancelled"),
    IMAGE_UPLOAD_CLIPBOARD_EMPTY("gui.crazyae2addons.image_upload_clipboard_empty", "Clipboard is empty"),
    IMAGE_UPLOAD_INVALID_PATH("gui.crazyae2addons.image_upload_invalid_path", "Clipboard does not contain a valid file path"),
    IMAGE_UPLOAD_INVALID_IMAGE("gui.crazyae2addons.image_upload_invalid_image", "Unsupported or corrupted image file"),
    IMAGE_UPLOAD_TOO_LARGE("gui.crazyae2addons.image_upload_too_large", "Image file is too large (%s bytes)"),
    IMAGE_UPLOAD_FAILED("gui.crazyae2addons.image_upload_failed", "Failed to upload image"),
    PREVIEW_DIMENSIONS("gui.crazyae2addons.preview_dimensions", "Preview %sx%s"),
    IMAGE_PREVIEW_BOUNDS("gui.crazyae2addons.image_preview_bounds", "%s%% @ %s,%s"),
    MISSING("gui.crazyae2addons.missing", "Missing"),
    CONFIG("gui.crazyae2addons.config", "Config"),
    INSERT_IMAGES("gui.crazyae2addons.display_images", "Insert Images"),
    NOTIFICATION_TERMINAL_UNIT_LINE("gui.crazyae2addons.notification_terminal.unit_line", "Unit: %s"),
    NOTIFICATION_TERMINAL_DISABLED("gui.crazyae2addons.notification_terminal.disabled", "Disabled"),
    NOTIFICATION_TERMINAL_INVALID_NUMBER("gui.crazyae2addons.notification_terminal.invalid_number", "Invalid number"),
    NOTIFICATION_TERMINAL_HUD_X_TOOLTIP("gui.crazyae2addons.notification_terminal.hud_x_tooltip", "Hud X in % (0-100)"),
    NOTIFICATION_TERMINAL_HUD_Y_TOOLTIP("gui.crazyae2addons.notification_terminal.hud_y_tooltip", "Hud Y in % (0-100)"),
    NOTIFICATION_TERMINAL_HIDE_ABOVE_TOOLTIP("gui.crazyae2addons.notification_terminal.hide_above_tooltip", "Do not render entries that are >= threshold"),
    NOTIFICATION_TERMINAL_HIDE_BELOW_TOOLTIP("gui.crazyae2addons.notification_terminal.hide_below_tooltip", "Do not render entries that are < threshold"),
    AND("gui.crazyae2addons.and", "AND"),
    OR("gui.crazyae2addons.or", "OR"),
    ITEMS("gui.crazyae2addons.items", "Items"),
    INVALID_NUMBER("gui.crazyae2addons.invalid_number", "Invalid number"),
    MULTI_EMITTER_LOGIC("gui.crazyae2addons.multi_emitter.logic", "Logic mode"),
    MULTI_EMITTER_CMP_ABOVE("gui.crazyae2addons.multi_emitter.cmp_above", "Emit when above or equal"),
    MULTI_EMITTER_CMP_BELOW("gui.crazyae2addons.multi_emitter.cmp_below", "Emit when below"),
    MULTI_EMITTER_EMIT_WHEN_CRAFTING("gui.crazyae2addons.multi_emitter.emit_when_crafting", "Emit when crafting"),
    MULTI_EMITTER_EMIT_WHEN_NOT_CRAFTING("gui.crazyae2addons.multi_emitter.emit_when_not_crafting", "Emit when not crafting"),
    MULTI_EMITTER_UNIT_LINE("gui.crazyae2addons.multi_emitter.unit_line", "Unit: %s"),
    EMITTER_TERMINAL_SEARCH("gui.crazyae2addons.emitter_terminal.search", "Search"),
    EXPRESSION_HINT("gui.crazyae2addons.expression_hint", "Expression"),
    THRESHOLD_TOOLTIP("gui.crazyae2addons.threshold_tooltip", "Threshold value"),
    APPLY("gui.crazyae2addons.apply", "Apply"),
    SEARCH("gui.crazyae2addons.search", "Search"),
    NAME("gui.crazyae2addons.name", "Name"),
    FLIP_HORIZONTAL("gui.crazyae2addons.flip_horizontal", "Flip horizontally"),
    FLIP_VERTICAL("gui.crazyae2addons.flip_vertical", "Flip vertically"),
    ROTATE_CLOCKWISE("gui.crazyae2addons.rotate_clockwise", "Rotate clockwise"),
    NO_BLOCK_IN_RANGE("gui.crazyae2addons.no_block_in_range", "No block in range."),
    PASTE_OR_CLEAR_FIRST("gui.crazyae2addons.paste_or_clear_first", "Paste or clear stored structure first."),
    CORNER_A_SELECTED("gui.crazyae2addons.corner_a_selected", "Corner A selected."),
    CORNER_B_SELECTED("gui.crazyae2addons.corner_b_selected", "Corner B selected."),
    SELECTION_RESTARTED("gui.crazyae2addons.selection_restarted", "Selection restarted."),
    FAILED_TO_SAVE_STRUCTURE("gui.crazyae2addons.failed_to_save_structure", "Failed to save structure."),
    STRUCTURE_CUT_AND_SAVED("gui.crazyae2addons.structure_cut_and_saved", "Structure cut and saved."),
    FAILED_TO_LOAD_STRUCTURE("gui.crazyae2addons.failed_to_load_structure", "Failed to load structure."),
    STORED_STRUCTURE_NOT_FOUND("gui.crazyae2addons.stored_structure_not_found", "Stored structure not found."),
    FAILED_TO_PASTE_STRUCTURE("gui.crazyae2addons.failed_to_paste_structure", "Failed to paste structure."),
    STRUCTURE_PASTED("gui.crazyae2addons.structure_pasted", "Structure pasted."),
    PORTABLE_SPATIAL_IO_SHORT("gui.crazyae2addons.portable_spatial_storage_short", "Portable Spatial IO"),
    PASTE_COLLISION("gui.crazyae2addons.paste_collision", "Cannot paste structure: collision detected.");


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