package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.networking.EnergyCellBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.blocks.EnergyStorageBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;

public class EnergyStorageBE extends EnergyCellBlockEntity {

    public EnergyStorageBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.ENERGY_STORAGE_BE.get(), pos, state);
    }

    @Override
    public double getAEMaxPower() {
        if (getBlockState().getBlock() instanceof EnergyStorageBlock esBlock) {
            return esBlock.getMaxEnergy();
        }
        return super.getAEMaxPower();
    }
}
