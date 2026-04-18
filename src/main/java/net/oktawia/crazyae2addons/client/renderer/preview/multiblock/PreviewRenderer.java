package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

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
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PreviewRenderer {
    public static float previewAlpha = 0.38f;
    public static float alphaStep = 0.08f;

    private PreviewRenderer() {
    }

    public static void render(MultiblockPreviewInfo previewInfo, RenderLevelStageEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || previewInfo == null) return;

        float tick = mc.level.getGameTime() + event.getPartialTick();

        Vec3 playerEyePos = mc.player.getEyePosition();
        Vec3 lookDirection = mc.player.getLookAngle();

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buffer = mc.renderBuffers().bufferSource();
        BlockRenderDispatcher blockRenderer = mc.getBlockRenderer();
        Frustum frustum = event.getFrustum();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        List<MultiblockPreviewInfo.BlockInfo> visibleBlocks = new ArrayList<>();
        List<BlockPos> invalidBlocks = new ArrayList<>();

        for (MultiblockPreviewInfo.BlockInfo info : previewInfo.blockInfos) {
            BlockPos pos = info.pos();
            AABB bounds = new AABB(pos);

            if (!mc.level.isLoaded(pos)
                    || pos.distSqr(mc.player.blockPosition()) > 48 * 48
                    || !frustum.isVisible(bounds)) {
                continue;
            }

            BlockState current = mc.level.getBlockState(pos);
            boolean validHere = info.allowedBlocks().contains(current.getBlock());

            if (validHere) {
                continue;
            }

            if (!current.isAir()) {
                invalidBlocks.add(pos);
            }

            visibleBlocks.add(info);
        }

        List<MultiblockPreviewInfo.BlockInfo> glassBlocks = new ArrayList<>();
        List<MultiblockPreviewInfo.BlockInfo> solidBlocks = new ArrayList<>();
        for (MultiblockPreviewInfo.BlockInfo info : visibleBlocks) {
            if (isGlass(info.state())) {
                glassBlocks.add(info);
            } else {
                solidBlocks.add(info);
            }
        }

        MultiblockPreviewInfo.BlockInfo pointed = null;
        float closestDistance = Float.MAX_VALUE;

        for (MultiblockPreviewInfo.BlockInfo info : visibleBlocks) {
            BlockPos pos = info.pos();
            AABB bounds = new AABB(pos);

            if (bounds.clip(playerEyePos, playerEyePos.add(lookDirection.scale(12))).isPresent()) {
                float distance = (float) playerEyePos.distanceTo(Vec3.atCenterOf(pos));
                if (distance < closestDistance) {
                    pointed = info;
                    closestDistance = distance;
                }
            }
        }

        float reach = mc.gameMode != null ? mc.gameMode.getPickRange() : 5.0f;
        if (pointed != null && closestDistance <= reach) {
            PreviewTooltipLayer.set(
                    pointed.state().getBlock().getName().getString(),
                    null,
                    PreviewTooltipLayer.DEFAULT_TTL_MS
            );
        } else {
            PreviewTooltipLayer.set(null, null, 0L);
        }

        float deltaTick = tick - previewInfo.lastTick;
        for (Map.Entry<Integer, Float> entry : previewInfo.alpha.entrySet()) {
            int y = entry.getKey();
            float current = entry.getValue();
            float target = previewAlpha;

            if (current < target) {
                previewInfo.alpha.put(y, Math.min(target, current + deltaTick * alphaStep));
            } else if (current > target) {
                previewInfo.alpha.put(y, Math.max(target, current - deltaTick * alphaStep));
            }
        }

        previewInfo.lastTick = tick;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.depthMask(false);

        for (MultiblockPreviewInfo.BlockInfo info : solidBlocks) {
            BlockPos pos = info.pos();
            BlockState state = info.state();

            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.06f, pos.getY() + 0.06f, pos.getZ() + 0.06f);
            poseStack.scale(0.88f, 0.88f, 0.88f);

            BakedModel model = blockRenderer.getBlockModel(state);
            VertexConsumer translucentBuffer = buffer.getBuffer(RenderType.translucent());

            float alpha = previewInfo.alpha.computeIfAbsent(pos.getY(), y -> previewAlpha);

            if (alpha > 0) {
                for (Direction direction : Direction.values()) {
                    RandomSource random = RandomSource.create(42L);
                    for (BakedQuad quad : model.getQuads(state, direction, random, ModelData.EMPTY, null)) {
                        translucentBuffer.putBulkData(
                                poseStack.last(),
                                quad,
                                1f, 1f, 1f, alpha,
                                0x00F000F0,
                                OverlayTexture.NO_OVERLAY,
                                true
                        );
                    }
                }

                RandomSource random = RandomSource.create(42L);
                for (BakedQuad quad : model.getQuads(state, null, random, ModelData.EMPTY, null)) {
                    translucentBuffer.putBulkData(
                            poseStack.last(),
                            quad,
                            1f, 1f, 1f, alpha,
                            0x00F000F0,
                            OverlayTexture.NO_OVERLAY,
                            true
                    );
                }
            }

            poseStack.popPose();
        }

        VertexConsumer lineBuffer = buffer.getBuffer(RenderType.lines());
        for (MultiblockPreviewInfo.BlockInfo info : glassBlocks) {
            BlockPos pos = info.pos();

            poseStack.pushPose();
            poseStack.translate(pos.getX() + 0.06f, pos.getY() + 0.06f, pos.getZ() + 0.06f);
            poseStack.scale(0.88f, 0.88f, 0.88f);

            float alpha = previewInfo.alpha.computeIfAbsent(pos.getY(), y -> previewAlpha);

            if (alpha > 0) {
                LevelRenderer.renderLineBox(
                        poseStack,
                        lineBuffer,
                        0.0, 0.0, 0.0,
                        1.0, 1.0, 1.0,
                        0.30f, 0.65f, 1.0f, Math.min(1.0f, alpha + 0.25f)
                );
            }

            poseStack.popPose();
        }

        VertexConsumer invalidLineBuffer = buffer.getBuffer(RenderType.lines());
        for (BlockPos pos : invalidBlocks) {
            poseStack.pushPose();
            poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

            LevelRenderer.renderLineBox(
                    poseStack,
                    invalidLineBuffer,
                    0.0, 0.0, 0.0,
                    1.0, 1.0, 1.0,
                    1.0f, 0.15f, 0.15f, 0.95f
            );

            poseStack.popPose();
        }

        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();

        poseStack.popPose();

        buffer.endBatch(RenderType.translucent());
        buffer.endBatch(RenderType.lines());
    }

    private static boolean isGlass(BlockState state) {
        ResourceLocation key = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return key != null && key.getPath().toLowerCase().contains("glass");
    }
}