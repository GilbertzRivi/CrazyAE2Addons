package net.oktawia.crazyae2addons.misc;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public record CachedBlockInfo(BlockPos pos, BlockState state, BakedModel model) {}
