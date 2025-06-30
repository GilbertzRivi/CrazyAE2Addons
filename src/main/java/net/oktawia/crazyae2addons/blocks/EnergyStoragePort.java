package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.oktawia.crazyae2addons.entities.EnergyStoragePortBE;
import org.jetbrains.annotations.Nullable;

public class EnergyStoragePort extends AEBaseEntityBlock<EnergyStoragePortBE> {

    public EnergyStoragePort() {
        super(Properties.of().strength(2f).mapColor(MapColor.METAL).sound(SoundType.METAL));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EnergyStoragePortBE(pos, state);
    }
}
