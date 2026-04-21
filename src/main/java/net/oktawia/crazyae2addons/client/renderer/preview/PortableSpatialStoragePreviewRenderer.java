package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.cutpaste.CutPasteStackState;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class PortableSpatialStoragePreviewRenderer {

    private static final double MAX_DISTANCE = 50.0D;

    private PortableSpatialStoragePreviewRenderer() {
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ItemStack stack = findHeldPortableSpatialStorage(minecraft);
        if (stack.isEmpty()) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        if (CutPasteStackState.hasStructure(stack)) {
            String structureId = CutPasteStackState.getStructureId(stack);
            if (!structureId.isBlank()) {
                PreviewStructure structure = PortableSpatialStoragePreviewCache.get(structureId);
                if (structure != null && !structure.blocks().isEmpty()) {
                    BlockHitResult hit = rayTrace(minecraft, MAX_DISTANCE);
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockPos origin = hit.getBlockPos().relative(hit.getDirection());
                        renderGhostModels(minecraft, poseStack, structure, origin);
                        renderLineBoxes(minecraft, poseStack, structure, origin);
                    }
                }
            }

            poseStack.popPose();
            return;
        }

        BlockPos selectionA = CutPasteStackState.getSelectionA(stack);
        BlockPos selectionB = CutPasteStackState.getSelectionB(stack);

        if (selectionA != null) {
            BlockPos previewB = selectionB;
            if (previewB == null) {
                BlockHitResult hit = rayTrace(minecraft, MAX_DISTANCE);
                if (hit.getType() == HitResult.Type.BLOCK) {
                    previewB = hit.getBlockPos();
                }
            }

            if (previewB != null) {
                renderSelectionPreview(poseStack, selectionA, previewB);
            }
        }

        poseStack.popPose();
    }

    private static ItemStack findHeldPortableSpatialStorage(Minecraft minecraft) {
        ItemStack mainHand = minecraft.player.getMainHandItem();
        if (mainHand.getItem() instanceof PortableSpatialStorage) {
            return mainHand;
        }

        ItemStack offHand = minecraft.player.getOffhandItem();
        if (offHand.getItem() instanceof PortableSpatialStorage) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    private static void renderGhostModels(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin
    ) {
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        PreviewBlockAndTintGetter previewLevel = new PreviewBlockAndTintGetter(minecraft.level, structure.blocks(), origin);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 0.45f);

        for (PreviewBlock previewBlock : structure.surfaceBlocks()) {
            BlockPos worldPos = origin.offset(previewBlock.pos());

            BlockState previewState = previewBlock.state();
            BlockState currentState = minecraft.level.getBlockState(worldPos);

            boolean replaceable = currentState.canBeReplaced() || currentState.isAir();

            if (!replaceable) {
                continue;
            }

            ModelData modelData = getPreviewModelData(previewLevel, worldPos);

            poseStack.pushPose();
            poseStack.translate(worldPos.getX(), worldPos.getY(), worldPos.getZ());

            try {
                renderBlockModelWithAllLayers(
                        minecraft,
                        previewLevel,
                        previewState,
                        worldPos,
                        poseStack,
                        bufferSource,
                        modelData
                );
            } catch (Exception ignored) {
                try {
                    minecraft.getBlockRenderer().renderSingleBlock(
                            previewState,
                            poseStack,
                            bufferSource,
                            LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY
                    );
                } catch (Exception ignoredToo) {
                }
            }

            poseStack.popPose();
        }

        bufferSource.endBatch();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private static ModelData getPreviewModelData(
            PreviewBlockAndTintGetter previewLevel,
            BlockPos worldPos
    ) {
        BlockEntity blockEntity = previewLevel.getBlockEntity(worldPos);
        if (blockEntity == null) {
            return ModelData.EMPTY;
        }

        return blockEntity.getModelData();
    }

    private static void renderBlockModelWithAllLayers(
            Minecraft minecraft,
            PreviewBlockAndTintGetter previewLevel,
            BlockState state,
            BlockPos worldPos,
            PoseStack poseStack,
            MultiBufferSource.BufferSource bufferSource,
            ModelData modelData
    ) {
        var dispatcher = minecraft.getBlockRenderer();
        var modelRenderer = dispatcher.getModelRenderer();
        BakedModel model = dispatcher.getBlockModel(state);

        long seed = state.getSeed(worldPos);
        RandomSource random = RandomSource.create(seed);

        for (RenderType renderType : model.getRenderTypes(state, random, modelData)) {
            var consumer = bufferSource.getBuffer(RenderTypeHelper.getMovingBlockRenderType(renderType));

            modelRenderer.tesselateBlock(
                    previewLevel,
                    model,
                    state,
                    worldPos,
                    poseStack,
                    consumer,
                    false,
                    RandomSource.create(seed),
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    renderType
            );
        }
    }

    private static void renderLineBoxes(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin
    ) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.lineWidth(2.0F);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        for (PreviewBlock previewBlock : structure.blocks()) {
            BlockPos worldPos = origin.offset(previewBlock.pos());

            BlockState currentState = minecraft.level.getBlockState(worldPos);

            boolean replaceable = currentState.canBeReplaced() || currentState.isAir();

            if (replaceable) {
                continue;
            }

            addLineBox(
                    buffer,
                    poseMatrix,
                    normalMatrix,
                    worldPos.getX(),
                    worldPos.getY(),
                    worldPos.getZ(),
                    worldPos.getX() + 1.0f,
                    worldPos.getY() + 1.0f,
                    worldPos.getZ() + 1.0f,
                    1.0f, 0.0f, 0.0f, 1.0f
            );
        }

        BufferUploader.drawWithShader(buffer.end());

        RenderSystem.lineWidth(1.0F);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderSelectionPreview(PoseStack poseStack, BlockPos a, BlockPos b) {
        int minX = Math.min(a.getX(), b.getX());
        int minY = Math.min(a.getY(), b.getY());
        int minZ = Math.min(a.getZ(), b.getZ());

        int maxX = Math.max(a.getX(), b.getX());
        int maxY = Math.max(a.getY(), b.getY());
        int maxZ = Math.max(a.getZ(), b.getZ());

        float x1 = minX;
        float y1 = minY;
        float z1 = minZ;
        float x2 = maxX + 1.0f;
        float y2 = maxY + 1.0f;
        float z2 = maxZ + 1.0f;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        renderFilledSelectionCuboid(poseStack, x1, y1, z1, x2, y2, z2, 0.20f, 0.85f, 1.00f, 0.12f);
        renderSelectionOutline(poseStack, x1, y1, z1, x2, y2, z2, 0.20f, 0.85f, 1.00f, 0.95f);

        renderCornerMarker(poseStack, a, 1.00f, 0.90f, 0.20f, 1.00f);
        renderCornerMarker(poseStack, b, 0.20f, 1.00f, 0.85f, 1.00f);

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void renderFilledSelectionCuboid(
            PoseStack poseStack,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float red, float green, float blue, float alpha
    ) {
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();

        buffer.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        addFilledBox(buffer, poseMatrix, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);

        BufferUploader.drawWithShader(buffer.end());
    }

    private static void renderSelectionOutline(
            PoseStack poseStack,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float red, float green, float blue, float alpha
    ) {
        RenderSystem.lineWidth(2.0F);
        RenderSystem.setShader(GameRenderer::getRendertypeLinesShader);

        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder buffer = tesselator.getBuilder();

        PoseStack.Pose pose = poseStack.last();
        Matrix4f poseMatrix = pose.pose();
        Matrix3f normalMatrix = pose.normal();

        buffer.begin(VertexFormat.Mode.LINES, DefaultVertexFormat.POSITION_COLOR_NORMAL);

        addLineBox(buffer, poseMatrix, normalMatrix, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);

        BufferUploader.drawWithShader(buffer.end());
        RenderSystem.lineWidth(1.0F);
    }

    private static void renderCornerMarker(
            PoseStack poseStack,
            BlockPos pos,
            float red, float green, float blue, float alpha
    ) {
        float expand = 0.05f;

        float minX = pos.getX() - expand;
        float minY = pos.getY() - expand;
        float minZ = pos.getZ() - expand;
        float maxX = pos.getX() + 1.0f + expand;
        float maxY = pos.getY() + 1.0f + expand;
        float maxZ = pos.getZ() + 1.0f + expand;

        renderSelectionOutline(poseStack, minX, minY, minZ, maxX, maxY, maxZ, red, green, blue, alpha);
    }

    private static void addFilledBox(
            BufferBuilder buffer,
            Matrix4f poseMatrix,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float red, float green, float blue, float alpha
    ) {
        quad(buffer, poseMatrix, minX, minY, minZ, maxX, minY, minZ, maxX, minY, maxZ, minX, minY, maxZ, red, green, blue, alpha);
        quad(buffer, poseMatrix, minX, maxY, minZ, minX, maxY, maxZ, maxX, maxY, maxZ, maxX, maxY, minZ, red, green, blue, alpha);

        quad(buffer, poseMatrix, minX, minY, minZ, minX, maxY, minZ, maxX, maxY, minZ, maxX, minY, minZ, red, green, blue, alpha);
        quad(buffer, poseMatrix, minX, minY, maxZ, maxX, minY, maxZ, maxX, maxY, maxZ, minX, maxY, maxZ, red, green, blue, alpha);

        quad(buffer, poseMatrix, minX, minY, minZ, minX, minY, maxZ, minX, maxY, maxZ, minX, maxY, minZ, red, green, blue, alpha);
        quad(buffer, poseMatrix, maxX, minY, minZ, maxX, maxY, minZ, maxX, maxY, maxZ, maxX, minY, maxZ, red, green, blue, alpha);
    }

    private static void quad(
            BufferBuilder buffer,
            Matrix4f poseMatrix,
            float x1, float y1, float z1,
            float x2, float y2, float z2,
            float x3, float y3, float z3,
            float x4, float y4, float z4,
            float red, float green, float blue, float alpha
    ) {
        buffer.vertex(poseMatrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        buffer.vertex(poseMatrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        buffer.vertex(poseMatrix, x3, y3, z3).color(red, green, blue, alpha).endVertex();
        buffer.vertex(poseMatrix, x4, y4, z4).color(red, green, blue, alpha).endVertex();
    }

    private static void addLineBox(
            BufferBuilder buffer,
            Matrix4f poseMatrix,
            Matrix3f normalMatrix,
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float red, float green, float blue, float alpha
    ) {
        LevelRenderer.renderLineBox(
                new VertexConsumer() {
                    @Override
                    public VertexConsumer vertex(double x, double y, double z) {
                        return buffer.vertex(poseMatrix, (float) x, (float) y, (float) z);
                    }

                    @Override
                    public VertexConsumer color(int r, int g, int b, int a) {
                        return buffer.color(r, g, b, a);
                    }

                    @Override
                    public VertexConsumer uv(float u, float v) {
                        return this;
                    }

                    @Override
                    public VertexConsumer overlayCoords(int u, int v) {
                        return this;
                    }

                    @Override
                    public VertexConsumer uv2(int u, int v) {
                        return this;
                    }

                    @Override
                    public VertexConsumer normal(float x, float y, float z) {
                        return buffer.normal(normalMatrix, x, y, z);
                    }

                    @Override
                    public void endVertex() {
                        buffer.endVertex();
                    }

                    @Override
                    public void defaultColor(int r, int g, int b, int a) {
                    }

                    @Override
                    public void unsetDefaultColor() {
                    }
                },
                minX, minY, minZ,
                maxX, maxY, maxZ,
                red, green, blue, alpha
        );
    }

    private static BlockHitResult rayTrace(Minecraft minecraft, double maxDistance) {
        Vec3 eye = minecraft.player.getEyePosition(1.0F);
        Vec3 look = minecraft.player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        ClipContext context = new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                minecraft.player
        );

        HitResult result = minecraft.level.clip(context);
        if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK) {
            return blockHit;
        }

        return BlockHitResult.miss(end, Direction.getNearest(look.x, look.y, look.z), BlockPos.containing(end));
    }
}