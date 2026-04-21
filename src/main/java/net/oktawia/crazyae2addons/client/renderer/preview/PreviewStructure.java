package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record PreviewStructure(
        BlockPos size,
        List<PreviewBlock> blocks,
        List<PreviewBlock> surfaceBlocks
) {

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
        if (tag != null && tag.contains("size", net.minecraft.nbt.Tag.TAG_LIST)) {
            var sizeTag = tag.getList("size", net.minecraft.nbt.Tag.TAG_INT);
            if (sizeTag.size() >= 3) {
                return new BlockPos(sizeTag.getInt(0), sizeTag.getInt(1), sizeTag.getInt(2));
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

        return new BlockPos(maxX + 1, maxY + 1, maxZ + 1);
    }

    private static List<PreviewBlock> computeSurface(List<PreviewBlock> blocks) {
        Map<BlockPos, PreviewBlock> byPos = new HashMap<>();
        for (PreviewBlock block : blocks) {
            byPos.put(block.pos(), block);
        }

        List<PreviewBlock> surface = new ArrayList<>();
        for (PreviewBlock block : blocks) {
            for (var side : net.minecraft.core.Direction.values()) {
                if (!byPos.containsKey(block.pos().relative(side))) {
                    surface.add(block);
                    break;
                }
            }
        }

        return surface;
    }
}