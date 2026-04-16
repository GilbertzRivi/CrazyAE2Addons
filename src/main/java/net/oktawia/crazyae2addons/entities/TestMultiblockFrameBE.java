package net.oktawia.crazyae2addons.entities;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;

public class TestMultiblockFrameBE extends AbstractMultiblockFrameBE<TestMultiblockControllerBE> {

    public TestMultiblockFrameBE(BlockPos pos, BlockState state) {
        super(
                CrazyBlockEntityRegistrar.TEST_MULTIBLOCK_FRAME_BE.get(),
                pos,
                state,
                new ItemStack(CrazyBlockRegistrar.TEST_MULTIBLOCK_FRAME.get().asItem()),
                1.0F
        );
    }

    @Override
    protected Class<TestMultiblockControllerBE> controllerClass() {
        return TestMultiblockControllerBE.class;
    }
}