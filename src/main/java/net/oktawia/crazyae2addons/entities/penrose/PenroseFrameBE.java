package net.oktawia.crazyae2addons.entities.penrose;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenroseFrameBE extends AbstractMultiblockFrameBE<PenroseControllerBE> {

    public PenroseFrameBE(BlockPos pos, BlockState blockState) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_FRAME_BE.get(),
                pos,
                blockState,
                new ItemStack(CrazyBlockRegistrar.PENROSE_FRAME.get().asItem()),
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

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction dir) {
        if (!CrazyConfig.COMMON.PenroseFEOutputEnabled.get()) {
            return null;
        }

        PenroseControllerBE controller = getResolvedController();
        if (controller == null || !controller.isFormed()) {
            return null;
        }

        return controller.getEnergyStorage(dir);
    }
}