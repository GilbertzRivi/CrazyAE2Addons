package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.parts.CrazyPatternProviderPart;

import java.util.List;

public class CrazyPatternProviderPartItem extends PartItem<CrazyPatternProviderPart> {

    public CrazyPatternProviderPartItem(Properties properties) {
        super(properties, CrazyPatternProviderPart.class, CrazyPatternProviderPart::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advancedTooltips) {
        CustomData data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        int addedRows = data.contains("added") ? data.copyTag().getInt("added") : 0;
        if (addedRows < 0) addedRows = 0;
        int totalSlots = (8 + addedRows) * 9;
        int filled = data.contains("filled") ? data.copyTag().getInt("filled") : 0;
        if (filled > totalSlots) filled = totalSlots;
        int percent = totalSlots > 0 ? (int) Math.round(100.0 * filled / (double) totalSlots) : 0;

        tooltip.add(Component.translatable(LangDefs.CRAZY_PROVIDER_CAPACITY_TOOLTIP.getTranslationKey()).append(String.valueOf(totalSlots))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("(" + percent + "%)")
                .withStyle(ChatFormatting.AQUA));
    }
}