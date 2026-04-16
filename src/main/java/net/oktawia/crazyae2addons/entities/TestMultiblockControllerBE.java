package net.oktawia.crazyae2addons.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.blocks.TestMultiblockControllerBlock;
import net.oktawia.crazyae2addons.blocks.TestMultiblockFrameBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockControllerBE;
import net.oktawia.crazyae2addons.multiblock.MultiblockDefinition;

public class TestMultiblockControllerBE extends AbstractMultiblockControllerBE {

    public TestMultiblockControllerBE(BlockPos pos, BlockState state) {
        super(
                CrazyBlockEntityRegistrar.TEST_MULTIBLOCK_CONTROLLER_BE.get(),
                pos,
                state,
                new ItemStack(CrazyBlockRegistrar.TEST_MULTIBLOCK_CONTROLLER.get().asItem()),
                2.0F
        );
    }

    @Override
    protected MultiblockDefinition getMultiblockDefinition() {
        return null;
    }

    @Override
    protected char frameSymbol() {
        return 'F';
    }

    @Override
    protected void setOwnFormedState(boolean formed) {
        Level level = getLevel();
        if (level == null) {
            return;
        }

        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(TestMultiblockControllerBlock.FORMED)
                && state.getValue(TestMultiblockControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), state.setValue(TestMultiblockControllerBlock.FORMED, formed), 3);
        }
    }

    @Override
    protected void setMemberFormedState(BlockPos pos, boolean formed) {
        Level level = getLevel();
        if (level == null) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(TestMultiblockFrameBlock.FORMED)
                && state.getValue(TestMultiblockFrameBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(TestMultiblockFrameBlock.FORMED, formed), 3);
        }
    }
}