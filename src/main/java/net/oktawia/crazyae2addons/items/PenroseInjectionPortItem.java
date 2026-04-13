package net.oktawia.crazyae2addons.items;

import appeng.block.AEBaseBlockItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class PenroseInjectionPortItem extends AEBaseBlockItem {
    public PenroseInjectionPortItem(Block block, Properties properties) {
        super(block, properties);
    }


    @Override
    public void addCheckedInformation(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("gui.crazyae2addons.penrose_injection_tooltip"));
    }

}
