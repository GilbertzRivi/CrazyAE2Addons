package net.oktawia.crazyae2addons.items;

import appeng.items.parts.PartItem;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.parts.CrazyPatternProviderPart;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;


public class CrazyPatternProviderPartItem extends PartItem<CrazyPatternProviderPart> {
    public CrazyPatternProviderPartItem(Properties properties) {
        super(properties, CrazyPatternProviderPart.class, CrazyPatternProviderPart::new);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag isAdvanced) {
        CompoundTag tag = stack.getOrCreateTag();
        int addedRows = tag.contains("added") ? tag.getInt("added") : 0;
        if (addedRows < 0) addedRows = 0;
        int totalRows = 8 + addedRows;
        int totalSlots = totalRows * 9;

        int filled = 0;
        if (tag.contains("patterns")) {
            ListTag invTag = tag.getList("patterns", Tag.TAG_COMPOUND);
            filled = invTag.size();
        }

        if (filled > totalSlots) filled = totalSlots;
        int percent = totalSlots > 0 ? (int)Math.round(100.0 * filled / (double) totalSlots) : 0;

        tooltip.add(Component.translatable("gui.crazyae2addons.crazy_provider_capacity_tooltip").append(String.valueOf(totalSlots))
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("(" + percent + "%)")
                .withStyle(ChatFormatting.AQUA));
    }
}