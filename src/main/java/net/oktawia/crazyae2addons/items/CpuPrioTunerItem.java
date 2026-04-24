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
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPrioHost;
import org.jetbrains.annotations.Nullable;

public class CpuPrioTunerItem extends AEBaseItem implements IMenuItem {

    public static final String NBT_CPU_POS = "cpu_pos";

    public CpuPrioTunerItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
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