package net.oktawia.crazyae2addons.defs;

import appeng.core.localization.LocalizationEnum;

public enum LangDefs implements LocalizationEnum {
    REDSTONE_TERMINAL_KEY("key.ae2.wireless_redstone_terminal", "Open Wireless Redstone Terminal"),
    RESEARCH_FLUID_BUCKET("item.crazyae2addons.research_fluid_bucket", "Research Fluid Bucket"),
    RESEARCH_FLUID_BLOCK("block.crazyae2addons.research_fluid_block", "Research Fluid"),
    RESEARCH_FLUID_TYPE("fluid_type.crazyae2addons.research_fluid_type", "Research Fluid");

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
