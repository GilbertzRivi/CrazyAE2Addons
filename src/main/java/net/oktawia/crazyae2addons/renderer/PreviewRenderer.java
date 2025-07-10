package net.oktawia.crazyae2addons.renderer;

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
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;

import java.util.ArrayList;
import java.util.Map;

import static java.lang.Float.max;
import static java.lang.Float.min;

public class PreviewRenderer {
    public static float previewAlpha = 0.2f;
    public static float layeredAlpha = 0.5f;
    public static float alphaStep = 0.05f;

    public static void render(PreviewInfo previewInfo, RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        float tick = event.getRenderTick() + event.getPartialTick();
        Vec3 playerEyePos = mc.player.getEyePosition(1.0f);
        Vec3 lookDirection = mc.player.getLookAngle();

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        Frustum frustum = event.getFrustum();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        ArrayList<PreviewInfo.BlockInfo> visibleBlocks = new ArrayList<>();
        for (PreviewInfo.BlockInfo info : previewInfo.blockInfos) {
            BlockPos pos = info.pos();

            AABB bounds = new AABB(pos);
            if (!mc.level.isLoaded(pos) || pos.distSqr(mc.player.blockPosition()) > 48 * 48 || !frustum.isVisible(bounds)) {
                continue;
            }

            BlockState current = mc.level.getBlockState(pos);
            if (current.getBlock() == info.state().getBlock()) continue;

            visibleBlocks.add(info);
        }

        PreviewInfo.BlockInfo pointed_in_1_4 = null;
        float closestDistance_in_1_4 = Float.MAX_VALUE;
        boolean stillFocused = false;
        if (previewInfo.layerInfo.focusY != null) {
            for (PreviewInfo.BlockInfo info : visibleBlocks) {
                BlockPos pos = info.pos();
                if (pos.getY() != previewInfo.layerInfo.focusY) continue;
                AABB bounds = new AABB(pos);
                if (bounds.clip(playerEyePos, playerEyePos.add(lookDirection.scale(12))).isPresent()) {
                    float distance = (float) playerEyePos.distanceTo(Vec3.atCenterOf(pos));
                    if (1 < distance && distance < 4) {
                        stillFocused = true;
                        break;
                    }
                }
            }
        }
        if (!stillFocused) {
            previewInfo.layerInfo.focusY = null;
        }
        PreviewInfo.BlockInfo pointed = null;
        float closestDistance = Float.MAX_VALUE;
        for (PreviewInfo.BlockInfo info : visibleBlocks) {
            BlockPos pos = info.pos();
            if (stillFocused && pos.getY() != previewInfo.layerInfo.focusY) continue;
            AABB bounds = new AABB(pos);
            if (bounds.clip(playerEyePos, playerEyePos.add(lookDirection.scale(12))).isPresent()) {
                float distance = (float) playerEyePos.distanceTo(Vec3.atCenterOf(pos));
                if (distance < closestDistance) {
                    pointed = info;
                    closestDistance = distance;
                }
                if (!stillFocused) {
                    if (1 < distance && distance < 4 && distance < closestDistance_in_1_4) {
                        pointed_in_1_4 = info;
                        closestDistance_in_1_4 = distance;
                    }
                }
            }
        }
        if (pointed != null) {
            TooltipRenderer.text = pointed.state().getBlock().getName().getString();
        }
        if (!stillFocused) {
            previewInfo.layerInfo.focusY = pointed_in_1_4 == null ? null : pointed_in_1_4.pos().getY();
        }

        float deltaTick = tick - previewInfo.layerInfo.lastTick;
        for (Map.Entry<Integer, Float> entry : previewInfo.layerInfo.alpha.entrySet()) {
            int key = entry.getKey();
            float value = entry.getValue();
            if (previewInfo.layerInfo.focusY == null) {
                if (value > previewAlpha) {
                    previewInfo.layerInfo.alpha.put(key, max(previewAlpha, previewInfo.layerInfo.alpha.get(key) - deltaTick * alphaStep));
                }
                if (value < previewAlpha) {
                    previewInfo.layerInfo.alpha.put(key, min(previewAlpha, previewInfo.layerInfo.alpha.get(key) + deltaTick * alphaStep));
                }
            } else {
                if (key == previewInfo.layerInfo.focusY) {
                    if (value < layeredAlpha) {
                        previewInfo.layerInfo.alpha.put(key, min(layeredAlpha, previewInfo.layerInfo.alpha.get(key) + deltaTick * alphaStep));
                    }
                } else {
                    if (value > 0) {
                        previewInfo.layerInfo.alpha.put(key, max(0, previewInfo.layerInfo.alpha.get(key) - deltaTick * alphaStep));
                    }
                }
            }
        }
        previewInfo.layerInfo.lastTick = tick;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        for (PreviewInfo.BlockInfo info : visibleBlocks) {
            BlockPos pos = info.pos();

            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.1f, pos.getY() + 0.1f, pos.getZ() + 0.1f);
            poseStack.scale(0.8f, 0.8f, 0.8f);

            BlockState state = info.state();
            BakedModel model = blockRenderer.getBlockModel(state);

            RenderType renderType = RenderType.translucent();
            VertexConsumer translucentBuffer = buffer.getBuffer(renderType);

            float alpha = previewInfo.layerInfo.alpha.computeIfAbsent(pos.getY(), k -> previewInfo.layerInfo.focusY == null ? previewAlpha : layeredAlpha);
            for (Direction direction : Direction.values()) {
                for (BakedQuad quad : model.getQuads(state, direction, mc.level.random, ModelData.EMPTY, null)) {
                    translucentBuffer.putBulkData(poseStack.last(), quad, 1f, 1f, 1f, alpha, 0xF0F0F0, OverlayTexture.NO_OVERLAY, true);
                }
            }

            poseStack.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();

        buffer.endBatch(RenderType.translucent());
    }
}
