package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addons.defs.components.CrazyProviderDisplayData;
import net.oktawia.crazyae2addons.parts.CrazyPatternProviderPart;

import java.util.List;

public class CrazyPatternProviderPartItem extends PartItem<CrazyPatternProviderPart> {

    public CrazyPatternProviderPartItem(Properties properties) {
        super(properties, CrazyPatternProviderPart.class, CrazyPatternProviderPart::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag advancedTooltips) {
        var data = stack.getOrDefault(CrazyDataComponents.CRAZY_PROVIDER_DISPLAY.get(), CrazyProviderDisplayData.DEFAULT);
        int addedRows = Math.max(0, data.added());
        int totalSlots = (8 + addedRows) * 9;
        int filled = Math.min(data.filled(), totalSlots);
        int percent = totalSlots > 0 ? (int) Math.round(100.0 * filled / (double) totalSlots) : 0;

        tooltip.add(Component.translatable(LangDefs.CRAZY_PROVIDER_CAPACITY_TOOLTIP.getTranslationKey()).append(String.valueOf(totalSlots))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("(" + percent + "%)")
                .withStyle(ChatFormatting.AQUA));
    }
}
