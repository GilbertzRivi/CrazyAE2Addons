package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public record PreviewBlock(BlockPos pos, BlockState state, @Nullable CompoundTag blockEntityTag) {}