package net.oktawia.crazyae2addons.items.block;

import appeng.block.AEBaseBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;

import java.util.List;

public class EjectorBlockItem extends AEBaseBlockItem {
    public EjectorBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void addCheckedInformation(ItemStack stack, Level level, List<Component> tooltip,
                                      TooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, level, tooltip, advancedTooltips);
        if (!CrazyConfig.COMMON.EJECTOR_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey()).withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey()).withStyle(ChatFormatting.GRAY));
        }
    }
}