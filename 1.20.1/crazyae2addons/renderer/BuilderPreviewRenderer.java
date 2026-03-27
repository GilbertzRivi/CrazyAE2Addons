package net.oktawia.crazyae2addons.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;

import java.util.ArrayList;
import java.util.List;

public class BuilderPreviewRenderer {

    public static float BUILDER_ALPHA = 1.0f;
    public static int   MAX_DIST_SQ   = 64 * 64;

    public static void render(PreviewInfo previewInfo, RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || previewInfo == null) return;

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos      = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer   = mc.getBlockRenderer();
        Frustum frustum                       = event.getFrustum();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        List<PreviewInfo.BlockInfo> visibleBlocks = new ArrayList<>();
        for (PreviewInfo.BlockInfo info : previewInfo.blockInfos) {
            BlockPos pos = info.pos();
            if (!mc.level.isLoaded(pos)) continue;
            if (pos.distSqr(mc.player.blockPosition()) > MAX_DIST_SQ) continue;
            if (!frustum.isVisible(new AABB(pos))) continue;

            BlockState current = mc.level.getBlockState(pos);
            if (current.getBlock() == info.state().getBlock()) continue;

            visibleBlocks.add(info);
        }

        List<PreviewInfo.BlockInfo> glassBlocks = new ArrayList<>();
        List<PreviewInfo.BlockInfo> solidBlocks = new ArrayList<>();
        for (PreviewInfo.BlockInfo info : visibleBlocks) {
            if (isGlass(info.state())) {
                glassBlocks.add(info);
            } else {
                solidBlocks.add(info);
            }
        }

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        RenderSystem.setShaderColor(1f, 1f, 1f, BUILDER_ALPHA);

        RenderType translucent = RenderType.translucent();
        VertexConsumer translucentBuffer = buffer.getBuffer(translucent);

        for (PreviewInfo.BlockInfo info : solidBlocks) {
            BlockPos pos   = info.pos();
            BlockState state = info.state();

            BakedModel model = blockRenderer.getBlockModel(state);

            poseStack.pushPose();
            final float SCALE = 0.99f;
            final float DELTA = (1.0f - SCALE) * 0.5f;
            poseStack.translate(pos.getX() + DELTA, pos.getY() + DELTA, pos.getZ() + DELTA);
            poseStack.scale(SCALE, SCALE, SCALE);

            RandomSource rand = RandomSource.create();

            ChunkRenderTypeSet layers = ItemBlockRenderTypes.getRenderLayers(state);
            if (!layers.iterator().hasNext()) {
                layers = ChunkRenderTypeSet.of(RenderType.solid());
            }

            for (RenderType queryLayer : layers) {
                rand.setSeed(Mth.getSeed(pos));

                for (Direction dir : Direction.values()) {
                    for (BakedQuad quad : model.getQuads(state, dir, rand, ModelData.EMPTY, queryLayer)) {
                        translucentBuffer.putBulkData(
                                poseStack.last(), quad,
                                1f, 1f, 1f, BUILDER_ALPHA,
                                0xF0F0F0, OverlayTexture.NO_OVERLAY, true
                        );
                    }
                }

                rand.setSeed(Mth.getSeed(pos));
                for (BakedQuad quad : model.getQuads(state, null, rand, ModelData.EMPTY, queryLayer)) {
                    translucentBuffer.putBulkData(
                            poseStack.last(), quad,
                            1f, 1f, 1f, BUILDER_ALPHA,
                            0xF0F0F0, OverlayTexture.NO_OVERLAY, true
                    );
                }
            }

            poseStack.popPose();
        }

        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lines());
        for (PreviewInfo.BlockInfo info : glassBlocks) {
            BlockPos pos   = info.pos();
            BlockState state = info.state();

            poseStack.pushPose();
            final float SCALE = 0.99f;
            final float DELTA = (1.0f - SCALE) * 0.5f;
            poseStack.translate(pos.getX() + DELTA, pos.getY() + DELTA, pos.getZ() + DELTA);
            poseStack.scale(SCALE, SCALE, SCALE);

            float r = 0.8f;
            float g = 0.9f;
            float b = 1.0f;

            LevelRenderer.renderLineBox(
                    poseStack,
                    lineBuffer,
                    0.0, 0.0, 0.0,
                    1.0, 1.0, 1.0,
                    r, g, b, BUILDER_ALPHA
            );

            poseStack.popPose();
        }

        buffer.endBatch(translucent);
        buffer.endBatch(RenderType.lines());
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        RenderSystem.enableCull();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();
    }

    private static boolean isGlass(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && key.getPath().toLowerCase().contains("glass");
    }
}
