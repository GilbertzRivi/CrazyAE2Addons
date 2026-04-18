package net.oktawia.crazyae2addons.client.textures;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

@FunctionalInterface
public interface ConnectedTextureRule {
    boolean connects(BlockAndTintGetter level,
                     BlockPos selfPos, BlockState selfState,
                     BlockPos otherPos, BlockState otherState,
                     Direction face);
}