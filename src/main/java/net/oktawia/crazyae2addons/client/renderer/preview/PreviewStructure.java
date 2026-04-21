package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.vertex.VertexBuffer;
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

import java.util.*;
import java.util.function.Supplier;

public final class PreviewStructure {

    private final BlockPos size;
    private final List<PreviewBlock> blocks;
    private final List<PreviewBlock> surfaceBlocks;

    private @Nullable Map<BlockPos, BlockEntity> cachedBlockEntities;
    private final Map<String, Map<BlockPos, ModelData>> modelDataCache = new HashMap<>();
    private final Map<String, VertexBuffer> vertexBufferCache = new HashMap<>();

    private PreviewStructure(BlockPos size, List<PreviewBlock> blocks, List<PreviewBlock> surfaceBlocks) {
        this.size = size;
        this.blocks = blocks;
        this.surfaceBlocks = surfaceBlocks;
    }

    public BlockPos size() { return size; }
    public List<PreviewBlock> blocks() { return blocks; }
    public List<PreviewBlock> surfaceBlocks() { return surfaceBlocks; }

    public Map<BlockPos, BlockEntity> blockEntities(ClientLevel level) {
        if (cachedBlockEntities == null) {
            cachedBlockEntities = buildBlockEntities(level);
        }
        return cachedBlockEntities;
    }

    public ModelData getOrComputeModelData(String sideMapKey, BlockPos localPos, Supplier<ModelData> compute) {
        return modelDataCache
                .computeIfAbsent(sideMapKey, k -> new HashMap<>())
                .computeIfAbsent(localPos, k -> compute.get());
    }

    public boolean hasVertexBuffer(String sideMapKey) {
        return vertexBufferCache.containsKey(sideMapKey);
    }

    public @Nullable VertexBuffer getVertexBuffer(String sideMapKey) {
        return vertexBufferCache.get(sideMapKey);
    }

    public void storeVertexBuffer(String sideMapKey, VertexBuffer vb) {
        VertexBuffer old = vertexBufferCache.put(sideMapKey, vb);
        if (old != null) old.close();
    }

    public void close() {
        vertexBufferCache.values().forEach(VertexBuffer::close);
        vertexBufferCache.clear();
        modelDataCache.clear();
    }

    private Map<BlockPos, BlockEntity> buildBlockEntities(ClientLevel level) {
        Map<BlockPos, BlockEntity> result = new HashMap<>();
        for (PreviewBlock block : blocks) {
            if (block.blockEntityTag() == null) continue;
            BlockEntity be = createBlockEntity(level, block.pos(), block.state(), block.blockEntityTag());
            if (be != null) result.put(block.pos(), be);
        }
        return result;
    }

    private static @Nullable BlockEntity createBlockEntity(ClientLevel level, BlockPos pos, BlockState state, CompoundTag tag) {
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
            blocks.add(new PreviewBlock(info.pos(), info.state(), info.blockEntityTag()));
        }
        BlockPos size = readSize(tag, blocks);
        List<PreviewBlock> surface = computeSurface(blocks);
        return new PreviewStructure(size, List.copyOf(blocks), List.copyOf(surface));
    }

    private static BlockPos readSize(CompoundTag tag, List<PreviewBlock> blocks) {
        if (tag != null && tag.contains("size", Tag.TAG_LIST)) {
            var sizeTag = tag.getList("size", Tag.TAG_INT);
            if (sizeTag.size() >= 3) {
                return new BlockPos(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2));
            }
        }
        if (blocks.isEmpty()) return BlockPos.ZERO;
        int maxX = 0, maxY = 0, maxZ = 0;
        for (PreviewBlock block : blocks) {
            maxX = Math.max(maxX, block.pos().getX());
            maxY = Math.max(maxY, block.pos().getY());
            maxZ = Math.max(maxZ, block.pos().getZ());
        }
        return new BlockPos(maxX + 1, maxY + 1, maxZ + 1);
    }

    private static List<PreviewBlock> computeSurface(List<PreviewBlock> blocks) {
        Map<BlockPos, BlockState> byPos = new HashMap<>();
        for (PreviewBlock block : blocks) byPos.put(block.pos(), block.state());
        List<PreviewBlock> surface = new ArrayList<>();
        for (PreviewBlock block : blocks) {
            for (Direction side : Direction.values()) {
                BlockState neighbor = byPos.get(block.pos().relative(side));
                if (neighbor == null || !neighbor.canOcclude()) {
                    surface.add(block);
                    break;
                }
            }
        }
        return surface;
    }
}