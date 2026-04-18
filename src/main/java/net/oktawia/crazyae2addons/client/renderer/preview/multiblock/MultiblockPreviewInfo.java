package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MultiblockPreviewInfo {
    public final List<BlockInfo> blockInfos;
    public final Map<Integer, Float> alpha;
    public final BlockPos origin;
    public final Direction facing;
    public float lastTick;

    public MultiblockPreviewInfo(BlockPos origin, Direction facing, List<BlockInfo> blockInfos) {
        this.origin = origin.immutable();
        this.facing = facing;
        this.blockInfos = new ArrayList<>(blockInfos);
        this.alpha = new HashMap<>();
        this.lastTick = 0.0f;
    }

    public boolean isStale(MultiblockPreviewHost host) {
        return !origin.equals(host.getPreviewOrigin()) || facing != host.getPreviewFacing();
    }

    public record BlockInfo(BlockPos pos, BlockState state, List<Block> allowedBlocks) {
        public BlockInfo {
            allowedBlocks = List.copyOf(allowedBlocks);
        }
    }
}