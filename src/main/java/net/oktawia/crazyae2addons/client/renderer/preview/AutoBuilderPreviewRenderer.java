package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.client.renderer.BuilderPreviewRenderer;

import java.util.ArrayList;

public class AutoBuilderPreviewRenderer {

    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();

        for (AutoBuilderBE be : AutoBuilderBE.CLIENT_INSTANCES) {
            if (be == null || be.getLevel() == null || be.getLevel() != mc.level) continue;

            // Ghost cursor — render zawsze, niezależnie od odległości/frustrmu BE
            renderGhostCursor(be, poseStack, buffer, cameraPos);

            // Preview budowli — tylko gdy blisko
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

    private static void renderGhostCursor(AutoBuilderBE be, PoseStack poseStack,
                                           MultiBufferSource.BufferSource buffer, Vec3 cameraPos) {
        BlockPos ghostPos = be.getGhostRenderPos();
        if (ghostPos == null) return;

        Vec3 offset = Vec3.atLowerCornerOf(ghostPos).subtract(cameraPos);

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);

        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, lineBuffer,
                0.0, 0.0, 0.0, 1.0, 1.0, 1.0,
                1.0f, 0.75f, 0.0f, 1.0f);

        poseStack.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static void rebuildCache(AutoBuilderBE be, Direction facing) {
        var positions = be.getPreviewPositions();
        var palette = be.getPreviewPalette();
        var indices = be.getPreviewIndices();

        var list = new ArrayList<PreviewInfo.BlockInfo>();
        for (int i = 0; i < positions.size() && i < indices.length; i++) {
            int palIndex = indices[i];
            if (palIndex < 0 || palIndex >= palette.size()) continue;

            BlockState state = AutoBuilderPreviewStateCache.parseBlockState(palette.get(palIndex));
            if (state == null) continue;

            list.add(new PreviewInfo.BlockInfo(positions.get(i), state));
        }

        be.setPreviewInfo(new PreviewInfo(list));
        be.setPreviewDirty(false);
    }
}
