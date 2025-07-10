package net.oktawia.crazyae2addons.renderer.preview;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;

public class PreviewInfo {
    public final ArrayList<BlockInfo> blockInfos;
    public final LayerInfo layerInfo;

    public PreviewInfo(ArrayList<BlockInfo> blockInfos) {
        this.blockInfos = blockInfos;
        this.layerInfo = new LayerInfo(new HashMap<>(), null);
    }

    public static class LayerInfo {
        public final HashMap<Integer, Float> alpha;
        public Integer focusY;
        public float lastTick;

        public LayerInfo(HashMap<Integer, Float> alpha, Integer focusY) {
            this.alpha = alpha;
            this.focusY = focusY;
            this.lastTick = 0.0f;
        }
    }

    public record BlockInfo(BlockPos pos, BlockState state, BakedModel model) {
    }
}

