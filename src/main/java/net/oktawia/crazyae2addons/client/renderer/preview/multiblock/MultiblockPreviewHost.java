package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.multiblock.MultiblockDefinition;
import org.jetbrains.annotations.Nullable;

public interface MultiblockPreviewHost {
    boolean isPreviewEnabled();

    @Nullable
    MultiblockPreviewInfo getPreviewInfo();

    void setPreviewInfo(@Nullable MultiblockPreviewInfo previewInfo);

    MultiblockDefinition getPreviewDefinition();

    BlockPos getPreviewOrigin();

    Direction getPreviewFacing();

    BlockState getPreviewState(MultiblockDefinition.PatternEntry entry, MultiblockDefinition.SymbolDef symbol);
}