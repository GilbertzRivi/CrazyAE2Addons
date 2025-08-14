package net.oktawia.crazyae2addons.items;

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


public class CrazyPatternProviderBlockItem extends AEBaseBlockItem {
    public CrazyPatternProviderBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void addCheckedInformation(ItemStack stack,
                                @org.jetbrains.annotations.Nullable Level level,
                                java.util.List<Component> tooltip,
                                TooltipFlag flag) {
        CompoundTag tag = stack.getOrCreateTag();
        int addedRows = tag.contains("added") ? tag.getInt("added") : 0;
        if (addedRows < 0) addedRows = 0;
        int totalRows = 8 + addedRows;
        int totalSlots = totalRows * 9;

        int filled = 0;
        if (tag.contains("dainv")) {
            ListTag invTag = tag.getList("dainv", Tag.TAG_COMPOUND);
            filled = invTag.size();
        }

        if (filled > totalSlots) filled = totalSlots;
        int percent = totalSlots > 0 ? (int)Math.round(100.0 * filled / (double) totalSlots) : 0;

        tooltip.add(Component.literal("Capacity: " + totalSlots)
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("(" + percent + "%)")
                .withStyle(ChatFormatting.AQUA));
    }
}
