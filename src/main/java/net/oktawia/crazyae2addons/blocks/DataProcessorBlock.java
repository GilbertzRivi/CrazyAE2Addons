package net.oktawia.crazyae2addons.blocks;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addons.entities.DataProcessorBE;
import org.jetbrains.annotations.Nullable;

public class DataProcessorBlock extends AEBaseEntityBlock<DataProcessorBE> implements IUpgradeableObject {
    public DataProcessorBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DataProcessorBE(pos, state);
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