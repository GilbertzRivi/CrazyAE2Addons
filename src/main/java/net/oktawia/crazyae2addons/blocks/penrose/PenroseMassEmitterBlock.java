package net.oktawia.crazyae2addons.blocks.penrose;

import appeng.block.AEBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.entities.penrose.PenroseMassEmitterBE;
import net.oktawia.crazyae2addons.logic.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PenroseMassEmitterBlock extends AbstractMenuOpeningBlock<PenroseMassEmitterBE> {

    public PenroseMassEmitterBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PenroseMassEmitterBE(pos, state);
    }

    @Override
    protected boolean isSignalSource(@NotNull BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(@NotNull BlockState state, BlockGetter level, @NotNull BlockPos pos, @NotNull Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PenroseMassEmitterBE emitter) {
            return emitter.shouldEmit() ? 15 : 0;
        }
        return 0;
    }

    @Override
    protected int getDirectSignal(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @NotNull Direction side) {
        return getSignal(state, level, pos, side);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PenroseMassEmitterBE emitter) {
            return emitter.shouldEmit() ? 15 : 0;
        }
        return 0;
    }

    @Override
    public boolean canConnectRedstone(@NotNull BlockState state, @NotNull BlockGetter level, @NotNull BlockPos pos, @Nullable Direction side) {
        return true;
    }
}