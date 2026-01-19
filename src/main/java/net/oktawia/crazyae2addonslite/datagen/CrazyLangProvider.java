package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.Utils;
import net.oktawia.crazyae2addonslite.defs.LangDefs;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

public class CrazyLangProvider extends LanguageProvider {

    public CrazyLangProvider(PackOutput output, String locale) {
        super(output, CrazyAddonslite.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        for (var item : CrazyItemRegistrar.getItems()) {
            var key = BuiltInRegistries.ITEM.getKey(item);
            this.add(item.getDescriptionId(), Utils.toTitle(key.getPath()));
        }
        for (var block : CrazyBlockRegistrar.getBlocks()) {
            var key = BuiltInRegistries.BLOCK.getKey(block);
            this.add(block.getDescriptionId(), Utils.toTitle(key.getPath()));
        }
        for (var entry : LangDefs.values()) {
            this.add(entry.getTranslationKey(), entry.getEnglishText());
        }
    }
}
