package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import appeng.block.networking.EnergyCellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import net.oktawia.crazyae2addons.entities.EnergyStorage1kBE;
import org.jetbrains.annotations.Nullable;

public class EnergyStorage1k extends EnergyCellBlock {
    public EnergyStorage1k() {
        super((double) getMaxEnergy(), (double) (getMaxEnergy()), (int) (getMaxEnergy()/100));
    }
    static public long getMaxEnergy(){
        return 1024 * 1024 * 8;
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorage1kBE(pos, state);
    }
}
