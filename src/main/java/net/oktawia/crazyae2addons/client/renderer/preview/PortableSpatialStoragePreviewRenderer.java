package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.interfaces.RenderTypeTextureAccess;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PortableSpatialStoragePreviewRenderer {

    private static final double MAX_DISTANCE = 50.0D;
    private static final float PREVIEW_ALPHA = 0.75f;

    private enum PreviewPass {
        SOLID,
        CUTOUT,
        CUTOUT_MIPPED,
        TRANSLUCENT
    }

    private static final Map<RenderType, RenderType> translucentTypeCache = new IdentityHashMap<>();

    public PortableSpatialStoragePreviewRenderer() {
    }

    @SubscribeEvent
    public void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        ItemStack stack = StructureToolUtil.findHeld(
                minecraft.player,
                PortableSpatialStorage.class,
                PortableSpatialCloner.class
        );
        if (stack.isEmpty()) {
            return;
        }

        int[] sideMap = StructureToolStackState.getPreviewSideMap(stack);
        String sideMapKey = Arrays.toString(sideMap);

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        if (StructureToolStackState.hasStructure(stack)) {
            String structureId = StructureToolStackState.getStructureId(stack);
            if (!structureId.isBlank()) {
                PreviewStructure structure = PortableSpatialStoragePreviewSync.cacheGet(structureId);
                if (structure != null && !structure.blocks().isEmpty()) {
                    BlockHitResult hit = StructureToolUtil.rayTrace(minecraft.level, minecraft.player, MAX_DISTANCE);
                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockPos anchor = hit.getBlockPos().relative(hit.getDirection());

                        CompoundTag stackTag = stack.getTag();
                        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(stackTag);

                        BlockPos placementOrigin = anchor.subtract(energyOrigin);

                        renderGhostModels(minecraft, poseStack, structure, placementOrigin, sideMap, sideMapKey);
                        renderBlockEntityRenderers(minecraft, poseStack, structure, placementOrigin, event.getPartialTick());
                        renderLineBoxes(minecraft, poseStack, structure, placementOrigin);
                    }
                }
            }

            poseStack.popPose();
            return;
        }

        BlockPos selectionA = StructureToolStackState.getSelectionA(stack);
        BlockPos selectionB = StructureToolStackState.getSelectionB(stack);

        if (selectionA != null) {
            BlockPos previewB = selectionB;
            if (previewB == null) {
                BlockHitResult hit = StructureToolUtil.rayTrace(minecraft.level, minecraft.player, MAX_DISTANCE);
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

    protected Iterable<RenderType> getPreviewRenderTypes(
            PreviewBlock previewBlock,
            int[] sideMap,
            BlockRenderDispatcher dispatcher,
            PreviewBlockAndTintGetter localLevel,
            BakedModel model,
            BlockState state,
            BlockPos localPos,
            long seed,
            ModelData modelData
    ) {
        CompoundTag rawBeTag = previewBlock.blockEntityTag();

        for (BlockRenderExtension extension : BlockRenderExtensions.all()) {
            if (!extension.canRender(state, rawBeTag)) {
                continue;
            }

            Iterable<RenderType> renderTypes = extension.getPreviewRenderTypes(
                    previewBlock,
                    sideMap,
                    dispatcher,
                    localLevel,
                    model,
                    state,
                    localPos,
                    seed,
                    modelData
            );

            if (renderTypes != null) {
                return renderTypes;
            }
        }

        return model.getRenderTypes(state, RandomSource.create(seed), modelData);
    }

    protected void tesselatePreviewBlockForRenderType(
            PreviewBlock previewBlock,
            int[] sideMap,
            BlockRenderDispatcher dispatcher,
            ModelBlockRenderer modelRenderer,
            PreviewBlockAndTintGetter localLevel,
            BakedModel model,
            BlockState state,
            BlockPos localPos,
            PoseStack poseStack,
            BufferBuilder bufferBuilder,
            RenderType renderType,
            long seed,
            ModelData modelData
    ) {
        CompoundTag rawBeTag = previewBlock.blockEntityTag();

        for (BlockRenderExtension extension : BlockRenderExtensions.all()) {
            if (!extension.canRender(state, rawBeTag)) {
                continue;
            }

            if (extension.renderForPreview(
                    previewBlock,
                    sideMap,
                    dispatcher,
                    modelRenderer,
                    localLevel,
                    model,
                    state,
                    localPos,
                    poseStack,
                    bufferBuilder,
                    renderType,
                    seed,
                    modelData
            )) {
                return;
            }
        }

        modelRenderer.tesselateBlock(
                localLevel,
                model,
                state,
                localPos,
                poseStack,
                bufferBuilder,
                false,
                RandomSource.create(seed),
                seed,
                OverlayTexture.NO_OVERLAY,
                modelData,
                renderType
        );
    }

    private void renderGhostModels(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin,
            int[] sideMap,
            String sideMapKey
    ) {
        String solidKey = sideMapKey + ":solid";
        String cutoutKey = sideMapKey + ":cutout";
        String cutoutMippedKey = sideMapKey + ":cutout_mipped";
        String translucentKey = sideMapKey + ":translucent";

        if (!structure.hasVertexBuffer(solidKey)
                || !structure.hasVertexBuffer(cutoutKey)
                || !structure.hasVertexBuffer(cutoutMippedKey)
                || !structure.hasVertexBuffer(translucentKey)) {
            buildAndStoreVertexBuffers(
                    minecraft,
                    structure,
                    sideMap,
                    sideMapKey,
                    solidKey,
                    cutoutKey,
                    cutoutMippedKey,
                    translucentKey
            );
        }

        Matrix4f modelView = new Matrix4f(poseStack.last().pose());
        modelView.translate(origin.getX(), origin.getY(), origin.getZ());

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, PREVIEW_ALPHA);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        minecraft.gameRenderer.lightTexture().turnOnLightLayer();

        VertexBuffer solidVb = structure.getVertexBuffer(solidKey);
        if (solidVb != null) {
            RenderSystem.setShader(GameRenderer::getRendertypeSolidShader);
            RenderSystem.depthMask(true);
            solidVb.bind();
            solidVb.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        }

        VertexBuffer cutoutVb = structure.getVertexBuffer(cutoutKey);
        if (cutoutVb != null) {
            RenderSystem.setShader(GameRenderer::getRendertypeCutoutShader);
            RenderSystem.depthMask(true);
            cutoutVb.bind();
            cutoutVb.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        }

        VertexBuffer cutoutMippedVb = structure.getVertexBuffer(cutoutMippedKey);
        if (cutoutMippedVb != null) {
            RenderSystem.setShader(GameRenderer::getRendertypeCutoutMippedShader);
            RenderSystem.depthMask(true);
            cutoutMippedVb.bind();
            cutoutMippedVb.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        }

        VertexBuffer translucentVb = structure.getVertexBuffer(translucentKey);
        if (translucentVb != null) {
            RenderSystem.setShader(GameRenderer::getRendertypeTranslucentShader);
            RenderSystem.depthMask(false);
            translucentVb.bind();
            translucentVb.drawWithShader(modelView, RenderSystem.getProjectionMatrix(), RenderSystem.getShader());
            VertexBuffer.unbind();
        }

        minecraft.gameRenderer.lightTexture().turnOffLightLayer();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
    }

    private void buildAndStoreVertexBuffers(
            Minecraft minecraft,
            PreviewStructure structure,
            int[] sideMap,
            String sideMapKey,
            String solidKey,
            String cutoutKey,
            String cutoutMippedKey,
            String translucentKey
    ) {
        ClientLevel level = minecraft.level;
        var dispatcher = minecraft.getBlockRenderer();
        var modelRenderer = dispatcher.getModelRenderer();
        PreviewBlockAndTintGetter localLevel = new PreviewBlockAndTintGetter(level, structure, BlockPos.ZERO);

        BufferBuilder solidBB = new BufferBuilder(1048576);
        solidBB.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        BufferBuilder cutoutBB = new BufferBuilder(1048576);
        cutoutBB.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        BufferBuilder cutoutMippedBB = new BufferBuilder(1048576);
        cutoutMippedBB.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        BufferBuilder translucentBB = new BufferBuilder(1048576);
        translucentBB.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.BLOCK);

        PoseStack ps = new PoseStack();

        for (PreviewBlock previewBlock : structure.surfaceBlocks()) {
            BlockPos localPos = previewBlock.pos();
            BlockState state = previewBlock.state();
            BakedModel model = dispatcher.getBlockModel(state);
            long seed = state.getSeed(localPos);

            ModelData modelData = PreviewRenderModelDataHelper.getPreviewModelData(
                    structure,
                    previewBlock,
                    sideMap,
                    sideMapKey,
                    level,
                    model,
                    localLevel
            );

            ps.pushPose();
            ps.translate(localPos.getX(), localPos.getY(), localPos.getZ());

            try {
                for (RenderType renderType : getPreviewRenderTypes(
                        previewBlock,
                        sideMap,
                        dispatcher,
                        localLevel,
                        model,
                        state,
                        localPos,
                        seed,
                        modelData
                )) {
                    BufferBuilder target = switch (classifyRenderType(renderType)) {
                        case SOLID -> solidBB;
                        case CUTOUT -> cutoutBB;
                        case CUTOUT_MIPPED -> cutoutMippedBB;
                        case TRANSLUCENT -> translucentBB;
                    };

                    tesselatePreviewBlockForRenderType(
                            previewBlock,
                            sideMap,
                            dispatcher,
                            modelRenderer,
                            localLevel,
                            model,
                            state,
                            localPos,
                            ps,
                            target,
                            renderType,
                            seed,
                            modelData
                    );
                }
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug("Preview tesselation failed for {} at {}: {}", state, localPos, t.getMessage());
            }

            ps.popPose();
        }

        structure.storeVertexBuffer(solidKey, uploadBuffer(solidBB));
        structure.storeVertexBuffer(cutoutKey, uploadBuffer(cutoutBB));
        structure.storeVertexBuffer(cutoutMippedKey, uploadBuffer(cutoutMippedBB));
        structure.storeVertexBuffer(translucentKey, uploadBuffer(translucentBB));
    }

    private static VertexBuffer uploadBuffer(BufferBuilder bb) {
        VertexBuffer vb = new VertexBuffer(VertexBuffer.Usage.STATIC);
        vb.bind();
        vb.upload(bb.end());
        VertexBuffer.unbind();
        return vb;
    }

    private static PreviewPass classifyRenderType(RenderType rt) {
        if (rt == RenderType.translucent()
                || rt == RenderType.translucentMovingBlock()
                || rt == RenderType.tripwire()) {
            return PreviewPass.TRANSLUCENT;
        }

        if (rt == RenderType.cutoutMipped()) {
            return PreviewPass.CUTOUT_MIPPED;
        }

        if (rt == RenderType.cutout()) {
            return PreviewPass.CUTOUT;
        }

        if (rt == RenderType.solid()) {
            return PreviewPass.SOLID;
        }

        String s = rt.toString();

        if (s.contains("translucent") || s.contains("tripwire")) {
            return PreviewPass.TRANSLUCENT;
        }

        if (s.contains("cutout_mipped") || s.contains("cutoutmipped")) {
            return PreviewPass.CUTOUT_MIPPED;
        }

        if (s.contains("cutout")) {
            return PreviewPass.CUTOUT;
        }

        return PreviewPass.SOLID;
    }

    @SuppressWarnings("unchecked")
    private void renderBlockEntityRenderers(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin,
            float partialTick
    ) {
        Map<BlockPos, BlockEntity> blockEntities = structure.blockEntities(minecraft.level);
        if (blockEntities.isEmpty()) return;

        BlockEntityRenderDispatcher beDispatcher = minecraft.getBlockEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        MultiBufferSource ghostSource = renderType ->
                new AlphaScaledVertexConsumer(bufferSource.getBuffer(toTranslucentRenderType(renderType)), PREVIEW_ALPHA);

        minecraft.gameRenderer.lightTexture().turnOnLightLayer();

        for (PreviewBlock previewBlock : structure.surfaceBlocks()) {
            BlockEntity be = blockEntities.get(previewBlock.pos());
            if (be == null) continue;

            BlockEntityRenderer<BlockEntity> renderer = beDispatcher.getRenderer(be);
            if (renderer == null) continue;

            BlockPos worldPos = origin.offset(previewBlock.pos());
            poseStack.pushPose();
            poseStack.translate(worldPos.getX(), worldPos.getY(), worldPos.getZ());
            try {
                renderer.render(be, partialTick, poseStack, ghostSource, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY);
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
            }
            poseStack.popPose();
        }

        bufferSource.endBatch();
        minecraft.gameRenderer.lightTexture().turnOffLightLayer();
    }

    private static RenderType toTranslucentRenderType(RenderType renderType) {
        return translucentTypeCache.computeIfAbsent(renderType, rt -> {
            ResourceLocation texture = extractRenderTypeTexture(rt);
            if (texture == null) return rt;
            return rt.toString().contains("no_cull")
                    ? RenderType.entityTranslucent(texture)
                    : RenderType.entityTranslucentCull(texture);
        });
    }

    private static ResourceLocation extractRenderTypeTexture(RenderType renderType) {
        if (renderType instanceof RenderTypeTextureAccess access) {
            return access.crazyae2addons$getTextureLocation();
        }
        return null;
    }

    private record AlphaScaledVertexConsumer(VertexConsumer delegate, float alpha) implements VertexConsumer {

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(r, g, b, Math.round(a * alpha));
            return this;
        }

        @Override
        public VertexConsumer uv(float u, float v) {
            delegate.uv(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int u, int v) {
            delegate.overlayCoords(u, v);
            return this;
        }

        @Override
        public VertexConsumer uv2(int u, int v) {
            delegate.uv2(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            delegate.normal(x, y, z);
            return this;
        }

        @Override
        public void endVertex() {
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, Math.round(a * alpha));
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
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
        RenderSystem.lineWidth(5.0F);
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

            if (currentState.canBeReplaced() || currentState.isAir()) {
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
}