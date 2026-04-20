package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.oktawia.crazyae2addons.entities.CraftingSchedulerBE;
import net.oktawia.crazyae2addons.util.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CraftingSchedulerBlock extends AbstractMenuOpeningBlock<CraftingSchedulerBE> {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public CraftingSchedulerBlock() {
        super(AEBaseBlock.metalProps().isRedstoneConductor((state, level, pos) -> false));
        this.registerDefaultState(this.defaultBlockState().setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CraftingSchedulerBE(pos, state);
    }

    @Override
    public boolean canConnectRedstone(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @Nullable Direction side) {
        return true;
    }

    @Override
    public void neighborChanged(
            @NotNull BlockState state, Level level, @NotNull BlockPos pos,
            @NotNull Block block, @NotNull BlockPos fromPos, boolean isMoving
    ) {
        if (level.isClientSide) return;

        boolean wasPowered = state.getValue(POWERED);
        boolean isPoweredNow = level.hasNeighborSignal(pos);

        if (!wasPowered && isPoweredNow) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CraftingSchedulerBE myBE) {
                myBE.doWork();
            }
            level.setBlock(pos, state.setValue(POWERED, true), 3);
        } else if (wasPowered && !isPoweredNow) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
        }
    }
}