package net.oktawia.crazyae2addons.compat.GregTech;

import appeng.items.parts.PartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GTEnergyExporterPartItem extends PartItem<GTEnergyExporterPart> {
    public GTEnergyExporterPartItem(Properties properties) {
        super(properties, GTEnergyExporterPart.class, GTEnergyExporterPart::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!CrazyConfig.COMMON.EnergyExporterEnabled.get()) {
            tooltip.add(Component.literal("DISABLED").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("in mod's config").withStyle(ChatFormatting.GRAY));
        }
    }
}