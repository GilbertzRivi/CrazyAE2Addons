package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.function.Supplier;

public final class PreviewStructure {

    private static final int SURFACE_RENDER_DEPTH = 3;

    private final BlockPos size;
    private final List<PreviewBlock> blocks;
    private final List<PreviewBlock> surfaceBlocks;

    private @Nullable Map<BlockPos, BlockEntity> cachedBlockEntities;

    private final Map<String, Map<BlockPos, ModelData>> modelDataCache = new HashMap<>();
    private final Map<String, CachedPreviewBuffer> previewGeometryCache = new HashMap<>();

    private PreviewStructure(
            BlockPos size,
            List<PreviewBlock> blocks,
            List<PreviewBlock> surfaceBlocks
    ) {
        this.size = size;
        this.blocks = blocks;
        this.surfaceBlocks = surfaceBlocks;
    }

    public BlockPos size() {
        return size;
    }

    public List<PreviewBlock> blocks() {
        return blocks;
    }

    public List<PreviewBlock> surfaceBlocks() {
        return surfaceBlocks;
    }

    public Map<BlockPos, BlockEntity> blockEntities(ClientLevel level) {
        if (cachedBlockEntities == null) {
            cachedBlockEntities = buildBlockEntities(level);
        }

        return cachedBlockEntities;
    }

    public ModelData getOrComputeModelData(
            String sideMapKey,
            BlockPos localPos,
            Supplier<ModelData> compute
    ) {
        return modelDataCache
                .computeIfAbsent(sideMapKey, k -> new HashMap<>())
                .computeIfAbsent(localPos, k -> compute.get());
    }

    public boolean hasPreviewGeometry(String sideMapKey) {
        return previewGeometryCache.containsKey(sideMapKey);
    }

    public @Nullable CachedPreviewBuffer getPreviewGeometry(String sideMapKey) {
        return previewGeometryCache.get(sideMapKey);
    }

    public void storePreviewGeometry(String sideMapKey, CachedPreviewBuffer buffer) {
        CachedPreviewBuffer old = previewGeometryCache.put(sideMapKey, buffer);

        if (old != null) {
            old.clear();
        }
    }

    public void close() {
        for (CachedPreviewBuffer buffer : previewGeometryCache.values()) {
            buffer.clear();
        }

        previewGeometryCache.clear();
        modelDataCache.clear();
        cachedBlockEntities = null;
    }

    private Map<BlockPos, BlockEntity> buildBlockEntities(ClientLevel level) {
        Map<BlockPos, BlockEntity> result = new HashMap<>();

        for (PreviewBlock block : blocks) {
            if (block.blockEntityTag() == null) {
                continue;
            }

            BlockEntity be = createBlockEntity(
                    level,
                    block.pos(),
                    block.state(),
                    block.blockEntityTag()
            );

            if (be != null) {
                result.put(block.pos(), be);
            }
        }

        return result;
    }

    private static @Nullable BlockEntity createBlockEntity(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            CompoundTag tag
    ) {
        CompoundTag copy = tag.copy();

        copy.putInt("x", pos.getX());
        copy.putInt("y", pos.getY());
        copy.putInt("z", pos.getZ());

        try {
            BlockEntity be = BlockEntity.loadStatic(pos, state, copy);

            if (be == null) {
                return null;
            }

            be.setLevel(level);

            try {
                be.clearRemoved();
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
            }

            try {
                be.onLoad();
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
            }

            return be;
        } catch (Throwable t) {
            CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
            return null;
        }
    }

    public static PreviewStructure fromTemplateTag(CompoundTag tag) {
        List<TemplateUtil.BlockInfo> parsed = TemplateUtil.parseBlocksFromTag(tag);
        List<PreviewBlock> blocks = new ArrayList<>(parsed.size());

        for (TemplateUtil.BlockInfo info : parsed) {
            blocks.add(new PreviewBlock(
                    info.pos(),
                    info.state(),
                    info.blockEntityTag()
            ));
        }

        BlockPos size = readSize(tag, blocks);
        List<PreviewBlock> surface = computeSurfaceDepth(blocks, SURFACE_RENDER_DEPTH);

        return new PreviewStructure(
                size,
                List.copyOf(blocks),
                List.copyOf(surface)
        );
    }

    private static BlockPos readSize(CompoundTag tag, List<PreviewBlock> blocks) {
        if (tag != null && tag.contains("size", Tag.TAG_LIST)) {
            var sizeTag = tag.getList("size", Tag.TAG_INT);

            if (sizeTag.size() >= 3) {
                return new BlockPos(
                        sizeTag.getInt(0),
                        sizeTag.getInt(1),
                        sizeTag.getInt(2)
                );
            }
        }

        if (blocks.isEmpty()) {
            return BlockPos.ZERO;
        }

        int maxX = 0;
        int maxY = 0;
        int maxZ = 0;

        for (PreviewBlock block : blocks) {
            maxX = Math.max(maxX, block.pos().getX());
            maxY = Math.max(maxY, block.pos().getY());
            maxZ = Math.max(maxZ, block.pos().getZ());
        }

        return new BlockPos(
                maxX + 1,
                maxY + 1,
                maxZ + 1
        );
    }

    private static List<PreviewBlock> computeSurfaceDepth(
            List<PreviewBlock> blocks,
            int maxDepth
    ) {
        if (blocks.isEmpty()) {
            return List.of();
        }

        Map<BlockPos, PreviewBlock> byPos = new HashMap<>();

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;

        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (PreviewBlock block : blocks) {
            BlockPos pos = block.pos();

            byPos.put(pos, block);

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());

            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }

        Bounds bounds = new Bounds(
                minX - 1,
                minY - 1,
                minZ - 1,
                maxX + 1,
                maxY + 1,
                maxZ + 1
        );

        Map<BlockPos, Integer> bestDepth = new HashMap<>();
        Queue<DepthNode> queue = new ArrayDeque<>();
        Set<BlockPos> renderPositions = new HashSet<>();

        enqueueBoundary(bounds, bestDepth, queue);

        while (!queue.isEmpty()) {
            DepthNode node = queue.poll();

            Integer knownDepth = bestDepth.get(node.pos());

            if (knownDepth == null || knownDepth != node.depth()) {
                continue;
            }

            for (Direction direction : Direction.values()) {
                BlockPos next = node.pos().relative(direction);

                if (!bounds.contains(next)) {
                    continue;
                }

                PreviewBlock nextBlock = byPos.get(next);

                if (nextBlock == null) {
                    enqueueIfBetter(next, node.depth(), bestDepth, queue);
                    continue;
                }

                int nextDepth = node.depth() + 1;

                if (nextDepth > maxDepth) {
                    continue;
                }

                renderPositions.add(next);

                if (canSeeThroughForPreview(nextBlock.state()) && nextDepth < maxDepth) {
                    enqueueIfBetter(next, nextDepth, bestDepth, queue);
                }
            }
        }

        List<PreviewBlock> result = new ArrayList<>(renderPositions.size());

        for (PreviewBlock block : blocks) {
            if (renderPositions.contains(block.pos())) {
                result.add(block);
            }
        }

        return result;
    }

    private static boolean canSeeThroughForPreview(BlockState state) {
        return !state.canOcclude();
    }

    private static void enqueueBoundary(
            Bounds bounds,
            Map<BlockPos, Integer> bestDepth,
            Queue<DepthNode> queue
    ) {
        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
                enqueueIfBetter(new BlockPos(x, y, bounds.minZ()), 0, bestDepth, queue);
                enqueueIfBetter(new BlockPos(x, y, bounds.maxZ()), 0, bestDepth, queue);
            }
        }

        for (int x = bounds.minX(); x <= bounds.maxX(); x++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                enqueueIfBetter(new BlockPos(x, bounds.minY(), z), 0, bestDepth, queue);
                enqueueIfBetter(new BlockPos(x, bounds.maxY(), z), 0, bestDepth, queue);
            }
        }

        for (int y = bounds.minY(); y <= bounds.maxY(); y++) {
            for (int z = bounds.minZ(); z <= bounds.maxZ(); z++) {
                enqueueIfBetter(new BlockPos(bounds.minX(), y, z), 0, bestDepth, queue);
                enqueueIfBetter(new BlockPos(bounds.maxX(), y, z), 0, bestDepth, queue);
            }
        }
    }

    private static void enqueueIfBetter(
            BlockPos pos,
            int depth,
            Map<BlockPos, Integer> bestDepth,
            Queue<DepthNode> queue
    ) {
        Integer oldDepth = bestDepth.get(pos);

        if (oldDepth != null && oldDepth <= depth) {
            return;
        }

        bestDepth.put(pos, depth);
        queue.add(new DepthNode(pos, depth));
    }

    private record DepthNode(BlockPos pos, int depth) {
    }

    private record Bounds(
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ
    ) {
        private boolean contains(BlockPos pos) {
            return pos.getX() >= minX
                    && pos.getX() <= maxX
                    && pos.getY() >= minY
                    && pos.getY() <= maxY
                    && pos.getZ() >= minZ
                    && pos.getZ() <= maxZ;
        }
    }
}