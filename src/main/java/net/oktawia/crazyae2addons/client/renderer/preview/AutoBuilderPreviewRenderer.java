package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.oktawia.crazyae2addons.client.renderer.BuilderPreviewRenderer;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.logic.builder.BuilderCoordMath;

import java.util.ArrayList;
import java.util.Arrays;

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

            renderGhostCursor(be, poseStack, buffer, cameraPos);

            if (!be.isPreviewEnabled()) continue;
            if (be.getBlockPos().distSqr(mc.player.blockPosition()) > 64 * 64) continue;

            if (be.getPreviewInfo() == null || be.isPreviewDirty()
                    || (be.getPreviewInfo().blockInfos.isEmpty() && be.getPreviewPositions().length > 0)
            ) {
                rebuildCache(be);
            }

            if (be.getPreviewInfo() != null) {
                BuilderPreviewRenderer.render(be.getPreviewInfo(), event);
            }
        }
    }

    private static void renderGhostCursor(
            AutoBuilderBE be,
            PoseStack poseStack,
            MultiBufferSource.BufferSource buffer,
            Vec3 cameraPos
    ) {
        BlockPos ghostPos = be.getGhostRenderPos();
        if (ghostPos == null) return;

        Vec3 offset = Vec3.atLowerCornerOf(ghostPos).subtract(cameraPos);

        poseStack.pushPose();
        poseStack.translate(offset.x, offset.y, offset.z);

        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(
                poseStack,
                lineBuffer,
                0.0, 0.0, 0.0,
                1.0, 1.0, 1.0,
                1.0f, 0.75f, 0.0f, 1.0f
        );

        poseStack.popPose();
        buffer.endBatch(RenderType.lines());
    }

    private static void rebuildCache(AutoBuilderBE be) {
        var positions = be.getPreviewPositions();
        var palette = be.getPreviewPalette();
        var indices = be.getPreviewIndices();

        Direction machineFacing = be.getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING)
                ? be.getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite()
                : Direction.NORTH;

        int delta = Math.floorMod(
                BuilderCoordMath.yawStepsFromNorth(machineFacing) - BuilderCoordMath.yawStepsFromNorth(be.getSourceFacing()), 4
        );

        var list = new ArrayList<PreviewInfo.BlockInfo>();
        for (int i = 0; i < positions.length && i < indices.length; i++) {
            int palIndex = indices[i];
            if (palIndex < 0 || palIndex >= palette.length) continue;

            BlockPos pos = positions[i];
            if (pos == null) continue;

            BlockState state = AutoBuilderPreviewStateCache.parseBlockState(palette[palIndex]);
            if (state == null) continue;

            state = BuilderCoordMath.rotateStateByDelta(state, delta);
            list.add(new PreviewInfo.BlockInfo(pos, state));
        }

        be.setPreviewInfo(new PreviewInfo(list));
        be.setPreviewDirty(false);
    }
}