package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import appeng.block.networking.EnergyCellBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.oktawia.crazyae2addons.entities.EnergyStorage16mBE;
import net.oktawia.crazyae2addons.entities.EnergyStorage1kBE;
import org.jetbrains.annotations.Nullable;

public class EnergyStorage16m extends EnergyCellBlock {
    public EnergyStorage16m() {
        super((double) getMaxEnergy(), (double) (getMaxEnergy()), (int) (getMaxEnergy()/100));
    }
    static public long getMaxEnergy(){
        return 1024L * 1024 * 1024 * 8 * 16;
    }
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStorage16mBE(pos, state);
    }
}
