package net.oktawia.crazyae2addons.entities.penrose;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockFrameBE;
import org.jetbrains.annotations.Nullable;

public class PenrosePortBE extends AbstractMultiblockFrameBE<PenroseControllerBE> {

    public PenrosePortBE(BlockPos pos, BlockState state) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_PORT_BE.get(),
                pos,
                state,
                new ItemStack(CrazyBlockRegistrar.PENROSE_PORT.get().asItem()),
                2.0F
        );
    }

    @Override
    protected Class<PenroseControllerBE> controllerClass() {
        return PenroseControllerBE.class;
    }

    @Override
    public void setController(@Nullable BlockEntity controller) {
        unregisterFromController();
        super.setController(controller);
    }

    @Override
    protected void onControllerChanged(@Nullable PenroseControllerBE newController) {
        if (newController != null) {
            newController.registerPort(this);
            connectToControllerGrid();
        } else {
            disconnectFromControllerGrid();
        }

        invalidateCapabilities();
    }

    @Override
    public void setRemoved() {
        unregisterFromController();
        super.setRemoved();
        invalidateCapabilities();
    }

    private void unregisterFromController() {
        if (activeController != null) {
            activeController.unregisterPort(this);
        }
    }

    public @Nullable IEnergyStorage getExposedEnergy(@Nullable Direction dir) {
        if (!CrazyConfig.COMMON.PenroseFEOutputEnabled.get()) {
            return null;
        }

        PenroseControllerBE controller = getResolvedController();
        if (controller == null || !controller.isFormed()) {
            return null;
        }

        return controller.getEnergyStorage(dir);
    }

    public void tickPort(Level level, IEnergyStorage source) {
        if (source == null || level == null) {
            return;
        }

        PenroseControllerBE controller = getResolvedController();
        if (controller == null || !controller.isFormed()) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            if (neighbor == null
                    || neighbor instanceof PenroseFrameBE
                    || neighbor instanceof PenroseCoilBE
                    || neighbor instanceof PenroseControllerBE
                    || neighbor instanceof PenrosePortBE) {
                continue;
            }

            IEnergyStorage target = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    neighborPos,
                    dir.getOpposite()
            );

            if (target == null || !target.canReceive()) {
                continue;
            }

            int available = source.getEnergyStored();
            if (available <= 0) {
                return;
            }

            int accepted = target.receiveEnergy(available, true);
            if (accepted <= 0) {
                continue;
            }

            int toMove = Math.min(available, accepted);
            int extracted = source.extractEnergy(toMove, false);
            if (extracted > 0) {
                target.receiveEnergy(extracted, false);
            }
        }
    }
}