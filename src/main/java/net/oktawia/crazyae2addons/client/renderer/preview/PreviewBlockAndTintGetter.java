package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public final class PreviewBlockAndTintGetter implements BlockAndTintGetter {

    private final ClientLevel level;
    private final Map<BlockPos, BlockState> states = new HashMap<>();
    private final Map<BlockPos, BlockEntity> blockEntities = new HashMap<>();

    public PreviewBlockAndTintGetter(ClientLevel level, PreviewStructure structure, BlockPos origin) {
        this.level = level;
        Map<BlockPos, BlockEntity> localBEs = structure.blockEntities(level);
        for (PreviewBlock block : structure.blocks()) {
            BlockPos worldPos = origin.offset(block.pos());
            states.put(worldPos, block.state());
            BlockEntity be = localBEs.get(block.pos());
            if (be != null) blockEntities.put(worldPos, be);
        }
    }

    @Override
    public float getShade(Direction direction, boolean shade) {
        return level.getShade(direction, shade);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return level.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return level.getBlockTint(pos, colorResolver);
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        BlockEntity previewBe = blockEntities.get(pos);
        return previewBe != null ? previewBe : level.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        BlockState previewState = states.get(pos);
        return previewState != null ? previewState : level.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return getBlockState(pos).getFluidState();
    }

    @Override
    public int getHeight() {
        return level.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return level.getMinBuildHeight();
    }

    @Override
    public int getBrightness(LightLayer lightLayer, BlockPos pos) {
        return 15;
    }
}
