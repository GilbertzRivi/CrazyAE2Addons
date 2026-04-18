package net.oktawia.crazyae2addons.blocks.penrose;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.entities.penrose.PenroseCoilBE;
import org.jetbrains.annotations.Nullable;

public class PenroseCoilBlock extends AEBaseEntityBlock<PenroseCoilBE> {

    public PenroseCoilBlock() {
        super(AEBaseBlock.metalProps());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PenroseCoilBE(pos, state);
    }
}