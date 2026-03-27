package net.oktawia.crazyae2addons.defs;

import appeng.core.localization.LocalizationEnum;

public enum LangDefs implements LocalizationEnum {
    LANG("key", "val"),
    PROVIDER_MAX("gui.crazyae2addons.provider_max", "Maximum capacity reached!"),
    CRAZY_PROVIDER_CAPACITY_TOOLTIP("gui.crazyae2addons.crazy_provider_capacity_tooltip", "Capacity: ");

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