package net.oktawia.crazyae2addons.renderer.preview;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.HashMap;

@OnlyIn(Dist.CLIENT)
public class PreviewInfo {
    public final ArrayList<BlockInfo> blockInfos;
    public final HashMap<Integer, Float> alpha;
    public Integer focusY;
    public float lastTick;

    public PreviewInfo(ArrayList<BlockInfo> blockInfos) {
        this.blockInfos = blockInfos;
        this.alpha = new HashMap<>();
        this.focusY = null;
        this.lastTick = 0.0f;
    }

    public record BlockInfo(BlockPos pos, BlockState state, BakedModel model) {
    }
}
