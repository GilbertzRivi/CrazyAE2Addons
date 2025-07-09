package net.oktawia.crazyae2addons.defs;

import appeng.core.localization.LocalizationEnum;

public enum LangDefs implements LocalizationEnum {
    ENTRY1("gui.crazyae2addons.nbt_storage_bus.confirm", "Confirm"),
    ENTRY2("gui.crazyae2addons.nbt_storage_bus.input_filter", "Input Filter"),
    ENTRY3("gui.crazyae2addons.nbt_storage_bus.load", "Load selected item's NBT"),
    ENTRY4("gui.crazyae2addons.nbt_export_bus.confirm", "Confirm"),
    ENTRY5("gui.crazyae2addons.nbt_export_bus.input_filter", "Input filter"),
    ENTRY6("gui.crazyae2addons.nbt_export_bus.load", "Load selected item's NBT"),
    ENTRY7("gui.crazyae2addons.penrose_controller.extract", "Extract singularities"),
    ENTRY8("gui.crazyae2addons.penrose_controller.insert", "Insert singularities"),
    ENTRY9("gui.crazyae2addons.penrose_controller.store_power_ae", "Store power"),
    ENTRY10("gui.crazyae2addons.penrose_controller.store_power_network", "as AE in the network power"),
    ENTRY11("gui.crazyae2addons.penrose_controller.store_power_fe", "Store power"),
    ENTRY12("gui.crazyae2addons.penrose_controller.store_power_multiblock", "as FE in the multiblock"),
    ENTRY13("gui.crazyae2addons.penrose_controller.preview_toggle", "Enable/Disable preview"),
    ENTRY14("gui.crazyae2addons.penrose_controller.change_tier", "Change preview tier"),
    ENTRY15("gui.crazyae2addons.penrose_controller.power_generation", "Power Generation"),
    ENTRY16("gui.crazyae2addons.penrose_controller.fe_per_tick", "%s FE/t"),
    ENTRY17("gui.crazyae2addons.penrose_controller.tier", "Tier: %s"),
    ENTRY18("gui.crazyae2addons.penrose_controller.preview", "Preview: %s, preview tier: %s");

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
