package net.oktawia.crazyae2addons.recipes;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Collections;
import java.util.Map;

public final class StructureSnapshot {
    private final int sizeX, sizeY, sizeZ;
    private final Map<BlockPos, BlockState> blocks; // tylko nie-air (opcjonalnie)

    public StructureSnapshot(int sizeX, int sizeY, int sizeZ, Map<BlockPos, BlockState> blocks) {
        this.sizeX = sizeX;
        this.sizeY = sizeY;
        this.sizeZ = sizeZ;
        this.blocks = Map.copyOf(blocks);
    }

    public int sizeX() { return sizeX; }
    public int sizeY() { return sizeY; }
    public int sizeZ() { return sizeZ; }

    public BlockState get(int x, int y, int z) {
        return blocks.getOrDefault(new BlockPos(x, y, z), null);
    }

    public Map<BlockPos, BlockState> blocks() {
        return Collections.unmodifiableMap(blocks);
    }
}
