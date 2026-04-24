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
import net.oktawia.crazyae2addons.defs.LangDefs;

import java.util.List;

public class CrazyPatternProviderBlockItem extends AEBaseBlockItem {
    public CrazyPatternProviderBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void addCheckedInformation(ItemStack stack, Level level, List<Component> tooltip,
                                      TooltipFlag advancedTooltips) {
        super.addCheckedInformation(stack, level, tooltip, advancedTooltips);

        CompoundTag tag = stack.getTag();

        int addedRows = 0;
        int filled = 0;

        if (tag != null) {
            if (tag.contains("added", Tag.TAG_INT)) {
                addedRows = Math.max(0, tag.getInt("added"));
            } else if (tag.contains("managed", Tag.TAG_COMPOUND)) {
                CompoundTag managed = tag.getCompound("managed");
                if (managed.contains("added", Tag.TAG_INT)) {
                    addedRows = Math.max(0, managed.getInt("added"));
                }
            } else if (tag.contains("crazy_state", Tag.TAG_COMPOUND)) {
                CompoundTag state = tag.getCompound("crazy_state");
                if (state.contains("added", Tag.TAG_INT)) {
                    addedRows = Math.max(0, state.getInt("added"));
                }
            }

            if (tag.contains("filled", Tag.TAG_INT)) {
                filled = tag.getInt("filled");
            } else if (tag.contains("crazy_patterns", Tag.TAG_LIST)) {
                ListTag list = tag.getList("crazy_patterns", Tag.TAG_COMPOUND);
                filled = list.size();
            } else if (tag.contains("patterns", Tag.TAG_LIST)) {
                ListTag list = tag.getList("patterns", Tag.TAG_COMPOUND);
                filled = list.size();
            }
        }

        int totalSlots = (8 + addedRows) * 9;
        filled = Math.min(Math.max(0, filled), totalSlots);
        int percent = totalSlots > 0 ? (int) Math.round(100.0D * filled / (double) totalSlots) : 0;

        tooltip.add(Component.translatable(LangDefs.CRAZY_PROVIDER_CAPACITY_TOOLTIP.getTranslationKey(), totalSlots).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("(" + percent + "%)").withStyle(ChatFormatting.AQUA));
    }
}