package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
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
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.joml.Matrix4f;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class PortableSpatialStoragePreviewRenderer {

    private static final double MAX_DISTANCE = 50.0D;

    private enum PreviewPass {
        SOLID,
        CUTOUT,
        CUTOUT_MIPPED,
        TRANSLUCENT
    }

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

        ItemStack stack = minecraft.player.getMainHandItem();

        if (!(stack.getItem() instanceof PortableSpatialStorage)
                && !(stack.getItem() instanceof PortableSpatialCloner)) {
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
                    BlockHitResult hit = StructureToolUtil.rayTrace(
                            minecraft.level,
                            minecraft.player,
                            MAX_DISTANCE
                    );

                    if (hit.getType() == HitResult.Type.BLOCK) {
                        BlockPos anchor = hit.getBlockPos().relative(hit.getDirection());

                        CompoundTag stackTag = stack.getTag();
                        BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(stackTag);

                        BlockPos placementOrigin = anchor.subtract(energyOrigin);

                        renderGhostModels(
                                minecraft,
                                poseStack,
                                structure,
                                placementOrigin,
                                sideMap,
                                sideMapKey
                        );

                        renderBlockEntityRenderers(
                                minecraft,
                                poseStack,
                                structure,
                                placementOrigin,
                                event.getPartialTick()
                        );

                        renderLineBoxes(
                                minecraft,
                                poseStack,
                                structure,
                                placementOrigin,
                                stack.getItem() instanceof PortableSpatialCloner
                        );
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
                BlockHitResult hit = StructureToolUtil.rayTrace(
                        minecraft.level,
                        minecraft.player,
                        MAX_DISTANCE
                );

                if (hit.getType() == HitResult.Type.BLOCK) {
                    previewB = hit.getBlockPos();
                }
            }

            if (previewB != null) {
                renderSelectionPreview(minecraft, poseStack, selectionA, previewB);
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
            VertexConsumer vertexConsumer,
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
                    vertexConsumer,
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
                vertexConsumer,
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

        if (!structure.hasPreviewGeometry(solidKey)
                || !structure.hasPreviewGeometry(cutoutKey)
                || !structure.hasPreviewGeometry(cutoutMippedKey)
                || !structure.hasPreviewGeometry(translucentKey)) {
            buildAndStorePreviewGeometry(
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

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

        minecraft.gameRenderer.lightTexture().turnOnLightLayer();

        poseStack.pushPose();
        poseStack.translate(origin.getX(), origin.getY(), origin.getZ());

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        emitPreviewGeometry(
                structure.getPreviewGeometry(solidKey),
                poseStack,
                bufferSource.getBuffer(RenderType.solid())
        );

        emitPreviewGeometry(
                structure.getPreviewGeometry(cutoutMippedKey),
                poseStack,
                bufferSource.getBuffer(RenderType.cutoutMipped())
        );

        emitPreviewGeometry(
                structure.getPreviewGeometry(cutoutKey),
                poseStack,
                bufferSource.getBuffer(RenderType.cutout())
        );

        emitPreviewGeometry(
                structure.getPreviewGeometry(translucentKey),
                poseStack,
                bufferSource.getBuffer(RenderType.translucent())
        );

        bufferSource.endBatch(RenderType.solid());
        bufferSource.endBatch(RenderType.cutoutMipped());
        bufferSource.endBatch(RenderType.cutout());
        bufferSource.endBatch(RenderType.translucent());

        poseStack.popPose();

        minecraft.gameRenderer.lightTexture().turnOffLightLayer();

        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    private static void emitPreviewGeometry(
            CachedPreviewBuffer buffer,
            PoseStack poseStack,
            VertexConsumer consumer
    ) {
        if (buffer == null || buffer.isEmpty()) {
            return;
        }

        buffer.emitBlock(poseStack.last(), consumer);
    }

    private void buildAndStorePreviewGeometry(
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

        PreviewBlockAndTintGetter localLevel = new PreviewBlockAndTintGetter(
                level,
                structure,
                BlockPos.ZERO
        );

        CachedPreviewBuffer solidBuffer = new CachedPreviewBuffer();
        CachedPreviewBuffer cutoutBuffer = new CachedPreviewBuffer();
        CachedPreviewBuffer cutoutMippedBuffer = new CachedPreviewBuffer();
        CachedPreviewBuffer translucentBuffer = new CachedPreviewBuffer();

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
                    CachedPreviewBuffer target = switch (classifyRenderType(renderType)) {
                        case SOLID -> solidBuffer;
                        case CUTOUT -> cutoutBuffer;
                        case CUTOUT_MIPPED -> cutoutMippedBuffer;
                        case TRANSLUCENT -> translucentBuffer;
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
                CrazyAddons.LOGGER.debug(
                        "Preview tesselation failed for {} at {}: {}",
                        state,
                        localPos,
                        t.getMessage()
                );
            }

            ps.popPose();
        }

        structure.storePreviewGeometry(solidKey, solidBuffer);
        structure.storePreviewGeometry(cutoutKey, cutoutBuffer);
        structure.storePreviewGeometry(cutoutMippedKey, cutoutMippedBuffer);
        structure.storePreviewGeometry(translucentKey, translucentBuffer);
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

    private void renderBlockEntityRenderers(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin,
            float partialTick
    ) {
        Map<BlockPos, BlockEntity> blockEntities = structure.blockEntities(minecraft.level);

        if (blockEntities.isEmpty()) {
            return;
        }

        BlockEntityRenderDispatcher beDispatcher = minecraft.getBlockEntityRenderDispatcher();
        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        Set<RenderType> usedRenderTypes = Collections.newSetFromMap(new IdentityHashMap<>());

        MultiBufferSource previewSource = renderType -> {
            usedRenderTypes.add(renderType);
            return bufferSource.getBuffer(renderType);
        };

        minecraft.gameRenderer.lightTexture().turnOnLightLayer();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

        for (PreviewBlock previewBlock : structure.surfaceBlocks()) {
            BlockEntity be = blockEntities.get(previewBlock.pos());

            if (be == null) {
                continue;
            }

            BlockEntityRenderer<BlockEntity> renderer = beDispatcher.getRenderer(be);

            if (renderer == null) {
                continue;
            }

            BlockPos worldPos = origin.offset(previewBlock.pos());

            poseStack.pushPose();
            poseStack.translate(worldPos.getX(), worldPos.getY(), worldPos.getZ());

            try {
                renderer.render(
                        be,
                        partialTick,
                        poseStack,
                        previewSource,
                        LightTexture.FULL_BRIGHT,
                        OverlayTexture.NO_OVERLAY
                );
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
            }

            poseStack.popPose();
        }

        for (RenderType renderType : usedRenderTypes) {
            bufferSource.endBatch(renderType);
        }

        RenderSystem.depthMask(true);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        minecraft.gameRenderer.lightTexture().turnOffLightLayer();
    }

    private static void renderLineBoxes(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin,
            boolean markSameBlocksAsBlue
    ) {
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        for (PreviewBlock previewBlock : structure.blocks()) {
            BlockPos worldPos = origin.offset(previewBlock.pos());
            BlockState currentState = minecraft.level.getBlockState(worldPos);

            if (currentState.canBeReplaced() || currentState.isAir()) {
                continue;
            }

            BlockState previewState = previewBlock.state();

            boolean sameBlock = currentState.equals(previewState);
            boolean blueInfoBox = markSameBlocksAsBlue && sameBlock;

            float red = blueInfoBox ? 0.20f : 1.00f;
            float green = blueInfoBox ? 0.85f : 0.00f;
            float blue = blueInfoBox ? 1.00f : 0.00f;

            LevelRenderer.renderLineBox(
                    poseStack,
                    lines,
                    worldPos.getX(),
                    worldPos.getY(),
                    worldPos.getZ(),
                    worldPos.getX() + 1.0f,
                    worldPos.getY() + 1.0f,
                    worldPos.getZ() + 1.0f,
                    red,
                    green,
                    blue,
                    1.0f
            );
        }

        bufferSource.endBatch(RenderType.lines());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderSelectionPreview(
            Minecraft minecraft,
            PoseStack poseStack,
            BlockPos a,
            BlockPos b
    ) {
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

        MultiBufferSource.BufferSource bufferSource = minecraft.renderBuffers().bufferSource();

        VertexConsumer fill = bufferSource.getBuffer(PreviewRenderTypes.SELECTION_FILL);

        addVisibleSelectionFillBox(
                minecraft,
                poseStack,
                fill,
                x1,
                y1,
                z1,
                x2,
                y2,
                z2,
                0.20f,
                0.85f,
                1.00f,
                0.075f
        );

        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        LevelRenderer.renderLineBox(
                poseStack,
                lines,
                x1,
                y1,
                z1,
                x2,
                y2,
                z2,
                0.20f,
                0.85f,
                1.00f,
                0.95f
        );

        renderCornerMarker(
                lines,
                poseStack,
                a,
                1.00f,
                0.90f,
                0.20f,
                1.00f
        );

        renderCornerMarker(
                lines,
                poseStack,
                b,
                0.20f,
                1.00f,
                0.85f,
                1.00f
        );

        bufferSource.endBatch(PreviewRenderTypes.SELECTION_FILL);
        bufferSource.endBatch(RenderType.lines());

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    private static void addVisibleSelectionFillBox(
            Minecraft minecraft,
            PoseStack poseStack,
            VertexConsumer consumer,
            float minX,
            float minY,
            float minZ,
            float maxX,
            float maxY,
            float maxZ,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        Matrix4f matrix = poseStack.last().pose();

        if (camera.x < minX) {
            quad(
                    consumer,
                    matrix,
                    minX, minY, minZ,
                    minX, maxY, minZ,
                    minX, maxY, maxZ,
                    minX, minY, maxZ,
                    red, green, blue, alpha
            );
        } else if (camera.x > maxX) {
            quad(
                    consumer,
                    matrix,
                    maxX, minY, maxZ,
                    maxX, maxY, maxZ,
                    maxX, maxY, minZ,
                    maxX, minY, minZ,
                    red, green, blue, alpha
            );
        }

        if (camera.y < minY) {
            quad(
                    consumer,
                    matrix,
                    minX, minY, maxZ,
                    maxX, minY, maxZ,
                    maxX, minY, minZ,
                    minX, minY, minZ,
                    red, green, blue, alpha
            );
        } else if (camera.y > maxY) {
            quad(
                    consumer,
                    matrix,
                    minX, maxY, minZ,
                    maxX, maxY, minZ,
                    maxX, maxY, maxZ,
                    minX, maxY, maxZ,
                    red, green, blue, alpha
            );
        }

        if (camera.z < minZ) {
            quad(
                    consumer,
                    matrix,
                    maxX, minY, minZ,
                    maxX, maxY, minZ,
                    minX, maxY, minZ,
                    minX, minY, minZ,
                    red, green, blue, alpha
            );
        } else if (camera.z > maxZ) {
            quad(
                    consumer,
                    matrix,
                    minX, minY, maxZ,
                    minX, maxY, maxZ,
                    maxX, maxY, maxZ,
                    maxX, minY, maxZ,
                    red, green, blue, alpha
            );
        }
    }

    private static void quad(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float x4,
            float y4,
            float z4,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        consumer.vertex(matrix, x1, y1, z1).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, x2, y2, z2).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, x3, y3, z3).color(red, green, blue, alpha).endVertex();
        consumer.vertex(matrix, x4, y4, z4).color(red, green, blue, alpha).endVertex();
    }

    private static void renderCornerMarker(
            VertexConsumer lines,
            PoseStack poseStack,
            BlockPos pos,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        float expand = 0.05f;

        float minX = pos.getX() - expand;
        float minY = pos.getY() - expand;
        float minZ = pos.getZ() - expand;

        float maxX = pos.getX() + 1.0f + expand;
        float maxY = pos.getY() + 1.0f + expand;
        float maxZ = pos.getZ() + 1.0f + expand;

        LevelRenderer.renderLineBox(
                poseStack,
                lines,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                red,
                green,
                blue,
                alpha
        );
    }
}