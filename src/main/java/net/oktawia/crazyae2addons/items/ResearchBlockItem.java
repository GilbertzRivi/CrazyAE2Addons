package net.oktawia.crazyae2addons.items;

import appeng.block.AEBaseBlockItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.oktawia.crazyae2addons.CrazyConfig;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ResearchBlockItem extends AEBaseBlockItem {
    public ResearchBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void addCheckedInformation(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        super.addCheckedInformation(stack, level, tooltip, flag);
        if (!CrazyConfig.COMMON.ResearchRequired.get()) {
            tooltip.add(Component.literal("DISABLED").withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal("in mod's config").withStyle(ChatFormatting.GRAY));
        }
    }
}
