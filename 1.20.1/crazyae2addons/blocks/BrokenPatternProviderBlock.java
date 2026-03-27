package net.oktawia.crazyae2addons.blocks;

import appeng.block.crafting.PatternProviderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.entities.BrokenPatternProviderBE;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import org.jetbrains.annotations.Nullable;

public class BrokenPatternProviderBlock extends PatternProviderBlock {
    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BrokenPatternProviderBE(pos, state);
    }
}
