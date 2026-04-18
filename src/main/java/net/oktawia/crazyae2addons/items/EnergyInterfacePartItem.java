package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.parts.EnergyInterfacePart;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class EnergyInterfacePartItem extends PartItem<EnergyInterfacePart> {
    public EnergyInterfacePartItem(Properties properties) {
        super(properties, EnergyInterfacePart.class, EnergyInterfacePart::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, level, tooltip, flag);
        if (!CrazyConfig.COMMON.EnergyInterfaceEnabled.get()) {
            tooltip.add(Component.literal("DISABLED").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("in mod's config").withStyle(ChatFormatting.GRAY));
        }
    }
}