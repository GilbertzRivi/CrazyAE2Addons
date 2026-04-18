package net.oktawia.crazyae2addons.blocks.penrose;

import appeng.block.AEBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHeatVentBE;
import net.oktawia.crazyae2addons.logic.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PenroseHeatVentBlock extends AbstractMenuOpeningBlock<PenroseHeatVentBE> {

    public PenroseHeatVentBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PenroseHeatVentBE(pos, state);
    }

    @Override
    public boolean canConnectRedstone(@NotNull BlockState state, @NotNull BlockGetter level,
                                      @NotNull BlockPos pos, @Nullable Direction side) {
        return true;
    }
}