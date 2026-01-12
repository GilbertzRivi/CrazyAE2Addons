package net.oktawia.crazyae2addonslite.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.logic.CpuPrioHost;
import org.jetbrains.annotations.Nullable;

public class CpuPrioTunerItem extends AEBaseItem implements IMenuItem {
    public static final String NBT_CPU_POS = "cpu_pos";

    public CpuPrioTunerItem(Properties props) { super(new Properties().stacksTo(1)); }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (level.isClientSide || player == null) return InteractionResult.SUCCESS;

        BlockPos pos = ctx.getClickedPos();
        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof CraftingBlockEntity) {
            ItemStack stack = player.getItemInHand(ctx.getHand());
            CompoundTag tag = stack.getOrCreateTag();
            tag.putLong(NBT_CPU_POS, pos.asLong());
            stack.setTag(tag);

            MenuOpener.open(CrazyMenuRegistrar.CPU_PRIO_MENU.get(), player, MenuLocators.forHand(player, ctx.getHand()));
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new CpuPrioHost(player, inventorySlot, stack);
    }
}
