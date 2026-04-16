package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.oktawia.crazyae2addons.entities.TestMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class TestMultiblockFrameBlock extends AEBaseEntityBlock<TestMultiblockFrameBE> {
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public TestMultiblockFrameBlock() {
        super(AEBaseBlock.metalProps());

        this.registerDefaultState(
            this.defaultBlockState()
                .setValue(FORMED, false)
        );
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FORMED);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TestMultiblockFrameBE(pos, state);
    }
}