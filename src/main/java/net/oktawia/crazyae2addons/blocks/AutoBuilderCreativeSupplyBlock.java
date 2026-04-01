package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.entities.AutoBuilderCreativeSupplyBE;

import org.jetbrains.annotations.Nullable;

public class AutoBuilderCreativeSupplyBlock extends AEBaseEntityBlock<AutoBuilderCreativeSupplyBE> {

    public AutoBuilderCreativeSupplyBlock() {
        super(metalProps());
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoBuilderCreativeSupplyBE(pos, state);
    }
}
