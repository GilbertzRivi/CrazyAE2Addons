package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.oktawia.crazyae2addons.entities.SuperSingularityBlockBE;

public class SuperSingularityBlock extends AEBaseEntityBlock<SuperSingularityBlockBE> {
    public SuperSingularityBlock() {
        super(Properties.of().strength(4f).mapColor(MapColor.METAL).sound(SoundType.METAL));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SuperSingularityBlockBE(pos, state);
    }
}
