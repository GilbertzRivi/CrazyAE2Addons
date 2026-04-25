package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPrioHost;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CpuPrioTunerItem extends AEBaseItem implements IMenuItem {

    public static final String NBT_CPU_POS = "cpu_pos";

    public CpuPrioTunerItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, level, tooltip, advancedTooltips);

        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return InteractionResult.PASS;
        }

        Level level = context.getLevel();
        Player player = context.getPlayer();

        if (player == null) {
            return InteractionResult.PASS;
        }

        BlockPos pos = context.getClickedPos();
        BlockEntity blockEntity = level.getBlockEntity(pos);

        if (!(blockEntity instanceof CraftingBlockEntity)) {
            return InteractionResult.PASS;
        }

        ItemStack stack = player.getItemInHand(context.getHand());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putLong(NBT_CPU_POS, pos.asLong());
        stack.setTag(tag);

        if (!level.isClientSide()) {
            MenuOpener.open(
                    CrazyMenuRegistrar.CPU_PRIO_MENU.get(),
                    player,
                    MenuLocators.forHand(player, context.getHand())
            );
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new CpuPrioHost(player, inventorySlot, stack);
    }
}