package net.oktawia.crazyae2addons.util;

import appeng.block.AEBaseEntityBlock;
import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractMenuOpeningBlock<T extends AEBaseBlockEntity & IMenuOpeningBlockEntity>
        extends AEBaseEntityBlock<T> {

    protected AbstractMenuOpeningBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player,
                                         InteractionHand hand, @Nullable ItemStack heldItem,
                                         BlockHitResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof IMenuOpeningBlockEntity menuBe)) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            menuBe.openMenu(player, MenuLocators.forBlockEntity(be));
        }

        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}