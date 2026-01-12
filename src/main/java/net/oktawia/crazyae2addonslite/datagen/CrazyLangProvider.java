package net.oktawia.crazyae2addonslite.datagen;

import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.Utils;
import net.oktawia.crazyae2addonslite.defs.LangDefs;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

public class CrazyLangProvider extends LanguageProvider {
    public CrazyLangProvider(PackOutput output, String locale) {
        super(output, CrazyAddons.MODID, locale);
    }

    @Override
    protected void addTranslations() {
        for (var item : CrazyItemRegistrar.getItems()){
            this.add(item.getDescriptionId(), Utils.toTitle(ForgeRegistries.ITEMS.getKey(item).getPath()));
        }
        for (var block : CrazyBlockRegistrar.getBlocks()){
            this.add(block.getDescriptionId(), Utils.toTitle(ForgeRegistries.BLOCKS.getKey(block).getPath()));
        }
        for (var entry : LangDefs.values()) {
            this.add(entry.getTranslationKey(), entry.getEnglishText());
        }
    }
}
