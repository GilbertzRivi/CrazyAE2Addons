package net.oktawia.crazyae2addons.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static java.lang.Float.max;
import static java.lang.Float.min;

public class PreviewRenderer {
    public static float previewAlpha  = 0.2f;
    public static float layeredAlpha  = 0.5f;
    public static float alphaStep     = 0.05f;

    public static void render(PreviewInfo previewInfo, RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || previewInfo == null) return;

        float tick = event.getRenderTick() + event.getPartialTick();
        Vec3 playerEyePos  = mc.player.getEyePosition(1.0f);
        Vec3 lookDirection = mc.player.getLookAngle();

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

            AABB bounds = new AABB(pos);
            if (!mc.level.isLoaded(pos)
                    || pos.distSqr(mc.player.blockPosition()) > 48 * 48
                    || !frustum.isVisible(bounds)) {
                continue;
            }

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

        PreviewInfo.BlockInfo pointed_in_1_4 = null;
        float closestDistance_in_1_4 = Float.MAX_VALUE;
        boolean stillFocused = false;

        if (previewInfo.focusY != null) {
            for (PreviewInfo.BlockInfo info : visibleBlocks) {
                BlockPos pos = info.pos();
                if (pos.getY() != previewInfo.focusY) continue;
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
            previewInfo.focusY = null;
        }

        PreviewInfo.BlockInfo pointed = null;
        float closestDistance = Float.MAX_VALUE;
        for (PreviewInfo.BlockInfo info : visibleBlocks) {
            BlockPos pos = info.pos();
            if (stillFocused && pos.getY() != previewInfo.focusY) continue;
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
            String name = pointed.state().getBlock().getName().getString();
            PreviewTooltipRenderer.set(name, null, PreviewTooltipRenderer.DEFAULT_TTL_MS);
        }
        if (!stillFocused) {
            previewInfo.focusY = pointed_in_1_4 == null ? null : pointed_in_1_4.pos().getY();
        }

        float deltaTick = tick - previewInfo.lastTick;

        for (Map.Entry<Integer, Float> entry : previewInfo.alpha.entrySet()) {
            int y = entry.getKey();
            float current = entry.getValue();

            float target = previewAlpha;
            if (previewInfo.focusY != null && y == previewInfo.focusY) {
                target = layeredAlpha;
            }

            if (current < target) {
                previewInfo.alpha.put(y, min(target, current + deltaTick * alphaStep));
            } else if (current > target) {
                previewInfo.alpha.put(y, max(target, current - deltaTick * alphaStep));
            }
        }

        previewInfo.lastTick = tick;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        for (PreviewInfo.BlockInfo info : solidBlocks) {
            BlockPos pos = info.pos();
            BlockState state = info.state();

            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.1f, pos.getY() + 0.1f, pos.getZ() + 0.1f);
            poseStack.scale(0.8f, 0.8f, 0.8f);

            BakedModel model = blockRenderer.getBlockModel(state);

            RenderType renderType = RenderType.translucent();
            VertexConsumer translucentBuffer = buffer.getBuffer(renderType);

            float alpha = previewInfo.alpha.computeIfAbsent(pos.getY(), y ->
                    (previewInfo.focusY != null && y == previewInfo.focusY)
                            ? layeredAlpha
                            : previewAlpha
            );

            if (alpha > 0) {
                for (Direction direction : Direction.values()) {
                    for (BakedQuad quad : model.getQuads(state, direction, mc.level.random, ModelData.EMPTY, null)) {
                        translucentBuffer.putBulkData(
                                poseStack.last(),
                                quad,
                                1f, 1f, 1f, alpha,
                                0xF0F0F0,
                                OverlayTexture.NO_OVERLAY,
                                true
                        );
                    }
                }
                for (BakedQuad quad : model.getQuads(state, null, mc.level.random, ModelData.EMPTY, null)) {
                    translucentBuffer.putBulkData(
                            poseStack.last(),
                            quad,
                            1f, 1f, 1f, alpha,
                            0xF0F0F0,
                            OverlayTexture.NO_OVERLAY,
                            true
                    );
                }
            }

            poseStack.popPose();
        }

        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lines());
        for (PreviewInfo.BlockInfo info : glassBlocks) {
            BlockPos pos = info.pos();
            BlockState state = info.state();

            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.1f, pos.getY() + 0.1f, pos.getZ() + 0.1f);
            poseStack.scale(0.8f, 0.8f, 0.8f);

            float alpha = previewInfo.alpha.computeIfAbsent(pos.getY(), y ->
                    (previewInfo.focusY != null && y == previewInfo.focusY)
                            ? layeredAlpha
                            : previewAlpha
            );
            if (alpha > 0) {
                float r = 0.8f;
                float g = 0.9f;
                float b = 1.0f;

                LevelRenderer.renderLineBox(
                        poseStack,
                        lineBuffer,
                        0.0, 0.0, 0.0,
                        1.0, 1.0, 1.0,
                        r, g, b, alpha
                );
            }

            poseStack.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();

        buffer.endBatch(RenderType.translucent());
        buffer.endBatch(RenderType.lines());
    }

    private static boolean isGlass(BlockState state) {
        var key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return key != null && key.getPath().toLowerCase().contains("glass");
    }
}
