package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addons.defs.BlockEntities;
import net.oktawia.crazyae2addons.entities.DataTrackerBE;
import org.jetbrains.annotations.Nullable;

public class DataTrackerBlock extends AEBaseEntityBlock<DataTrackerBE> {

    public DataTrackerBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState blockstate, BlockGetter blockAccess, BlockPos pos, Direction direction) {
        BlockEntity be = blockAccess.getBlockEntity(pos);
        if (be == null){
            return 0;
        } else {
            return ((DataTrackerBE) be).active ? 15 : 0;
        }
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataTrackerBE(BlockEntities.DATA_TRACKER_BE, pos, state);
    }

    @Override
    public InteractionResult onActivated(
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            @Nullable ItemStack heldItem,
            BlockHitResult hit) {
        if (InteractionUtil.isInAlternateUseMode(player)) {
            return InteractionResult.PASS;
        }

        var be = getBlockEntity(level, pos);
        if (be != null) {
            if (!level.isClientSide()) {
                be.openMenu(player, MenuLocators.forBlockEntity(be));
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }
}