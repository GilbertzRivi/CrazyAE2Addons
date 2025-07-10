package net.oktawia.crazyae2addons.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.oktawia.crazyae2addons.renderer.TooltipRenderer;

import java.util.List;

public class PreviewRenderer {
    public static float alpha = 0.2f;

    public static void render(List<CachedBlockInfo> cache, RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        Vec3 playerEyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookDirection = mc.player.getLookAngle();

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        Frustum frustum = event.getFrustum();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        CachedBlockInfo pointed_in_1_4 = null;
        float closestDistance_in_1_4 = Float.MAX_VALUE;
        CachedBlockInfo pointed = null;
        float closestDistance = Float.MAX_VALUE;
        for (CachedBlockInfo info : cache) {
            BlockPos pos = info.pos();

//            AABB bounds = new AABB(pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1, pos.getX() + 0.9, pos.getY() + 0.9, pos.getZ() + 0.9);
            AABB bounds = new AABB(pos);
            if (!mc.level.isLoaded(pos) || pos.distSqr(mc.player.blockPosition()) > 48 * 48 || !frustum.isVisible(bounds)) {
                continue;
            }

            BlockState current = mc.level.getBlockState(pos);
            if (current.getBlock() == info.state().getBlock()) continue;

            if (bounds.clip(playerEyePos, playerEyePos.add(lookDirection.scale(12))).isPresent()) {
                float distance = (float) playerEyePos.distanceTo(Vec3.atCenterOf(pos));
                if (distance < closestDistance) {
                    pointed = info;
                    closestDistance = distance;
                }
                if (1.5 < distance && distance < 4.5 && distance < closestDistance_in_1_4) {
                    pointed_in_1_4 = info;
                    closestDistance_in_1_4 = distance;
                }
            }
        }
        if (pointed != null) {
            TooltipRenderer.text = pointed.state().getBlock().getName().getString();
        }
        Integer render_y = null;
        if (pointed_in_1_4 != null) {
            render_y = pointed_in_1_4.pos().getY();
        }

        for (CachedBlockInfo info : cache) {
            BlockPos pos = info.pos();
            if (render_y != null && render_y != pos.getY()) {
                continue;
            }

//            AABB bounds = new AABB(pos.getX() + 0.1, pos.getY() + 0.1, pos.getZ() + 0.1, pos.getX() + 0.9, pos.getY() + 0.9, pos.getZ() + 0.9);
            AABB bounds = new AABB(pos);
            if (!mc.level.isLoaded(pos) || pos.distSqr(mc.player.blockPosition()) > 48 * 48 || !frustum.isVisible(bounds)) {
                continue;
            }

            BlockState current = mc.level.getBlockState(pos);
            if (current.getBlock() == info.state().getBlock()) continue;

            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());
            poseStack.scale(0.8f, 0.8f, 0.8f);

            BlockState state = info.state();
            BakedModel model = blockRenderer.getBlockModel(state);

            RenderType renderType = RenderType.translucent();
            VertexConsumer translucentBuffer = buffer.getBuffer(renderType);

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            RenderSystem.depthMask(false);
            if (render_y == null) {
                RenderSystem.setShaderColor(1f, 1f, 1f, alpha);
            } else {
                RenderSystem.setShaderColor(1f, 1f, 1f, (1 - alpha) / 2);
            }

            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : model.getQuads(state, direction, mc.level.random, ModelData.EMPTY, null)) {
                    translucentBuffer.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, alpha, 0xF0F0F0, OverlayTexture.NO_OVERLAY, true);
                }
            }

            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            RenderSystem.depthMask(true);
            RenderSystem.disableBlend();

            poseStack.popPose();
        }

        poseStack.popPose();

        buffer.endBatch(RenderType.translucent());
    }
}
