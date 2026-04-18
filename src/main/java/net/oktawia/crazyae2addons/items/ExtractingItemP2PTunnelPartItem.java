package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.parts.ExtractingItemP2PTunnelPart;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ExtractingItemP2PTunnelPartItem extends PartItem<ExtractingItemP2PTunnelPart> {
    public ExtractingItemP2PTunnelPartItem(Properties properties) {
        super(properties, ExtractingItemP2PTunnelPart.class, ExtractingItemP2PTunnelPart::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!CrazyConfig.COMMON.Itemp2pEnabled.get()) {
            tooltip.add(Component.literal("DISABLED").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("in mod's config").withStyle(ChatFormatting.GRAY));
        }
    }
}