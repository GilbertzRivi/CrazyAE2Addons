package net.oktawia.crazyae2addons.items;

import appeng.block.AEBaseBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;
import net.oktawia.crazyae2addons.defs.LangDefs;

import java.util.List;


public class CrazyPatternProviderBlockItem extends AEBaseBlockItem {
    public CrazyPatternProviderBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void addCheckedInformation(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advancedTooltips) {
        CustomData customData = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int addedRows = customData.contains("added") ? customData.copyTag().getInt("added") : 0;
        if (addedRows < 0) addedRows = 0;
        int totalSlots = (8 + addedRows) * 9;

        int filled = customData.contains("filled") ? customData.copyTag().getInt("filled") : 0;

        if (filled > totalSlots) filled = totalSlots;
        int percent = totalSlots > 0 ? (int) Math.round(100.0 * filled / (double) totalSlots) : 0;

        tooltip.add(Component.translatable(LangDefs.CRAZY_PROVIDER_CAPACITY_TOOLTIP.getTranslationKey()).append(String.valueOf(totalSlots))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("(" + percent + "%)")
                .withStyle(ChatFormatting.AQUA));
    }
}