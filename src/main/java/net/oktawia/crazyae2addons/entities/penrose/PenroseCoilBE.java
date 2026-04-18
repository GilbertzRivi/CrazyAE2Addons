package net.oktawia.crazyae2addons.entities.penrose;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseCoilBE extends AbstractMultiblockFrameBE<PenroseControllerBE> {

    public PenroseCoilBE(BlockPos pos, BlockState blockState) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_COIL_BE.get(),
                pos,
                blockState,
                new ItemStack(CrazyBlockRegistrar.PENROSE_COIL.get().asItem()),
                2.0F
        );
    }

    @Override
    protected Class<PenroseControllerBE> controllerClass() {
        return PenroseControllerBE.class;
    }

    @Override
    protected void onControllerChanged(@Nullable PenroseControllerBE newController) {
        if (newController != null) {
            connectToControllerGrid();
        } else {
            disconnectFromControllerGrid();
        }
    }
}