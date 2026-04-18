package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import net.minecraft.core.BlockPos;
import net.oktawia.crazyae2addons.multiblock.MultiblockDefinition;

import java.util.ArrayList;

public final class PreviewCacheBuilder {
    private PreviewCacheBuilder() {
    }

    public static MultiblockPreviewInfo rebuild(MultiblockPreviewHost host) {
        var definition = host.getPreviewDefinition();
        var origin = host.getPreviewOrigin();
        var facing = host.getPreviewFacing();

        var blockInfos = new ArrayList<MultiblockPreviewInfo.BlockInfo>();

        for (MultiblockDefinition.PatternEntry entry : definition.getEntries(facing)) {
            var symbol = definition.getSymbol(entry.symbol());
            if (symbol == null) continue;

            BlockPos worldPos = origin.offset(entry.relX(), entry.relY(), entry.relZ());

            blockInfos.add(new MultiblockPreviewInfo.BlockInfo(
                    worldPos,
                    host.getPreviewState(entry, symbol),
                    symbol.blocks()
            ));
        }

        return new MultiblockPreviewInfo(origin, facing, blockInfos);
    }
}