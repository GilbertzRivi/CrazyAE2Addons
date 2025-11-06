package net.oktawia.crazyae2addons.renderer.preview;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent.Stage;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.renderer.BuilderPreviewRenderer;

import java.util.ArrayList;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT)
public class AutoBuilderPreviewRenderer {

    @SubscribeEvent
    public static void onRender(RenderLevelStageEvent event) {
        if (!isBlockLayerStage(event.getStage())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        for (AutoBuilderBE be : AutoBuilderBE.CLIENT_INSTANCES) {
            if (be == null || be.getLevel() == null || !be.getLevel().isClientSide) continue;
            if (!be.isPreviewEnabled()) continue;

            BlockPos origin = be.getBlockPos();
            if (origin.distSqr(mc.player.blockPosition()) > 64 * 64) continue;

            Direction facing = be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                    ? be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
                    : Direction.NORTH;

            if (be.getPreviewInfo() == null || be.isPreviewDirty()) {
                rebuildCache(be, facing);
            }

            if (be.getPreviewInfo() != null) {
                BuilderPreviewRenderer.render(be.getPreviewInfo(), event);
            }
        }
    }

    private static boolean isBlockLayerStage(Stage s) {
        return s == Stage.AFTER_SOLID_BLOCKS
                || s == Stage.AFTER_CUTOUT_BLOCKS
                || s == Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS
                || s == Stage.AFTER_TRIPWIRE_BLOCKS
                || s == Stage.AFTER_TRANSLUCENT_BLOCKS;
    }


    private static void rebuildCache(AutoBuilderBE be, Direction facing) {
        Minecraft mc = Minecraft.getInstance();
        BlockRenderDispatcher dispatcher = mc.getBlockRenderer();

        var positions = be.getPreviewPositions();
        var palette   = be.getPreviewPalette();
        var indices   = be.getPreviewIndices();

        var list = new ArrayList<PreviewInfo.BlockInfo>();
        for (int i = 0; i < positions.size() && i < indices.length; i++) {
            int palIndex = indices[i];
            if (palIndex < 0 || palIndex >= palette.size()) continue;

            BlockState state = AutoBuilderPreviewStateCache.parseBlockState(palette.get(palIndex));
            if (state == null) continue;

            BakedModel model = dispatcher.getBlockModel(state);
            list.add(new PreviewInfo.BlockInfo(positions.get(i), state, model));
        }

        be.setPreviewInfo(new PreviewInfo(list));
        be.setPreviewDirty(false);
    }
}
