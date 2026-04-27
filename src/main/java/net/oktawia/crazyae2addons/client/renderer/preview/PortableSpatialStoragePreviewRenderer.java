package net.oktawia.crazyae2addons.client.renderer.preview;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
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
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

public class PortableSpatialStoragePreviewRenderer {

    private static final double MAX_DISTANCE = 50.0D;
    private static final int PREVIEW_BUFFER_SIZE = 2_097_152;

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
        if (!isPreviewRenderStage(event.getStage())) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();

        if (minecraft.player == null || minecraft.level == null) {
            return;
        }

        if (isStructureTool(minecraft.player.getOffhandItem())) {
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();

        if (!isStructureTool(stack)) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);

        try {
            renderForStage(
                    minecraft,
                    event,
                    poseStack,
                    stack
            );
        } finally {
            poseStack.popPose();

            resetPreviewRenderState(
                    minecraft,
                    isTripwireStage(event.getStage())
            );
        }
    }

    private static boolean isPreviewRenderStage(RenderLevelStageEvent.Stage stage) {
        return isSolidStage(stage)
                || isCutoutMippedStage(stage)
                || isCutoutStage(stage)
                || isBlockEntitiesStage(stage)
                || isTripwireStage(stage);
    }

    private static boolean isSolidStage(RenderLevelStageEvent.Stage stage) {
        return stage == RenderLevelStageEvent.Stage.AFTER_SOLID_BLOCKS;
    }

    private static boolean isCutoutMippedStage(RenderLevelStageEvent.Stage stage) {
        return stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_MIPPED_BLOCKS_BLOCKS;
    }

    private static boolean isCutoutStage(RenderLevelStageEvent.Stage stage) {
        return stage == RenderLevelStageEvent.Stage.AFTER_CUTOUT_BLOCKS;
    }

    private static boolean isBlockEntitiesStage(RenderLevelStageEvent.Stage stage) {
        return stage == RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES;
    }

    private static boolean isTripwireStage(RenderLevelStageEvent.Stage stage) {
        return stage == RenderLevelStageEvent.Stage.AFTER_TRIPWIRE_BLOCKS;
    }

    private void renderForStage(
            Minecraft minecraft,
            RenderLevelStageEvent event,
            PoseStack poseStack,
            ItemStack stack
    ) {
        int[] sideMap = StructureToolStackState.getPreviewSideMap(stack);
        String sideMapKey = Arrays.toString(sideMap);
        RenderLevelStageEvent.Stage stage = event.getStage();

        if (StructureToolStackState.hasStructure(stack)) {
            String structureId = StructureToolStackState.getStructureId(stack);

            if (structureId.isBlank()) {
                return;
            }

            PreviewStructure structure = PortableSpatialStoragePreviewSync.cacheGet(structureId);

            if (structure == null || structure.blocks().isEmpty()) {
                return;
            }

            BlockHitResult hit = StructureToolUtil.rayTrace(
                    minecraft.level,
                    minecraft.player,
                    MAX_DISTANCE
            );

            if (hit.getType() != HitResult.Type.BLOCK) {
                return;
            }

            BlockPos anchor = hit.getBlockPos().relative(hit.getDirection());

            CompoundTag stackTag = stack.getTag();
            BlockPos energyOrigin = TemplateUtil.getEnergyOrigin(stackTag);
            BlockPos placementOrigin = anchor.subtract(energyOrigin);

            if (isSolidStage(stage)) {
                renderGhostModelsPass(
                        minecraft,
                        poseStack,
                        structure,
                        placementOrigin,
                        sideMap,
                        sideMapKey,
                        PreviewPass.SOLID
                );
                return;
            }

            if (isCutoutMippedStage(stage)) {
                renderGhostModelsPass(
                        minecraft,
                        poseStack,
                        structure,
                        placementOrigin,
                        sideMap,
                        sideMapKey,
                        PreviewPass.CUTOUT_MIPPED
                );
                return;
            }

            if (isCutoutStage(stage)) {
                renderGhostModelsPass(
                        minecraft,
                        poseStack,
                        structure,
                        placementOrigin,
                        sideMap,
                        sideMapKey,
                        PreviewPass.CUTOUT
                );
                return;
            }

            if (isBlockEntitiesStage(stage)) {
                renderBlockEntityRenderers(
                        minecraft,
                        poseStack,
                        structure,
                        placementOrigin,
                        event.getPartialTick()
                );
                return;
            }

            if (isTripwireStage(stage)) {
                renderGhostModelsPass(
                        minecraft,
                        poseStack,
                        structure,
                        placementOrigin,
                        sideMap,
                        sideMapKey,
                        PreviewPass.TRANSLUCENT
                );

                renderLineBoxes(
                        minecraft,
                        poseStack,
                        structure,
                        placementOrigin,
                        stack.getItem() instanceof PortableSpatialCloner
                );
            }

            return;
        }

        if (!isTripwireStage(stage)) {
            return;
        }

        BlockPos selectionA = StructureToolStackState.getSelectionA(stack);
        BlockPos selectionB = StructureToolStackState.getSelectionB(stack);

        if (selectionA == null) {
            return;
        }

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
            renderSelectionPreview(
                    minecraft,
                    poseStack,
                    selectionA,
                    previewB
            );
        }
    }

    private static boolean isStructureTool(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }

        return stack.getItem() instanceof PortableSpatialStorage
                || stack.getItem() instanceof PortableSpatialCloner;
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

    private void renderGhostModelsPass(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin,
            int[] sideMap,
            String sideMapKey,
            PreviewPass pass
    ) {
        String solidKey = sideMapKey + ":natural_v3:solid";
        String cutoutKey = sideMapKey + ":natural_v3:cutout";
        String cutoutMippedKey = sideMapKey + ":natural_v3:cutout_mipped";
        String translucentKey = sideMapKey + ":natural_v3:translucent";

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

        CachedPreviewBuffer buffer;
        RenderType renderType;

        switch (pass) {
            case SOLID -> {
                buffer = structure.getPreviewGeometry(solidKey);
                renderType = RenderType.solid();

                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
            }
            case CUTOUT -> {
                buffer = structure.getPreviewGeometry(cutoutKey);
                renderType = RenderType.cutout();

                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
            }
            case CUTOUT_MIPPED -> {
                buffer = structure.getPreviewGeometry(cutoutMippedKey);
                renderType = RenderType.cutoutMipped();

                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
            }
            case TRANSLUCENT -> {
                buffer = structure.getPreviewGeometry(translucentKey);
                renderType = RenderType.translucent();

                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);
                RenderSystem.disableCull();
            }
            default -> {
                return;
            }
        }

        if (buffer == null || buffer.isEmpty()) {
            return;
        }

        MultiBufferSource.BufferSource bufferSource = previewBufferSource();

        RenderSystem.enableDepthTest();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

        minecraft.gameRenderer.lightTexture().turnOnLightLayer();

        poseStack.pushPose();
        poseStack.translate(origin.getX(), origin.getY(), origin.getZ());

        emitPreviewGeometry(
                buffer,
                poseStack,
                bufferSource.getBuffer(renderType)
        );

        bufferSource.endBatch(renderType);

        poseStack.popPose();
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

        String s = rt.toString().toLowerCase();

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
        MultiBufferSource.BufferSource bufferSource = previewBufferSource();

        Set<RenderType> usedRenderTypes = Collections.newSetFromMap(new IdentityHashMap<>());

        MultiBufferSource previewSource = renderType -> {
            usedRenderTypes.add(renderType);
            return bufferSource.getBuffer(renderType);
        };

        minecraft.gameRenderer.lightTexture().turnOnLightLayer();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
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
            String renderTypeName = renderType.toString().toLowerCase();

            if (renderType == RenderType.translucent()
                    || renderType == RenderType.translucentMovingBlock()
                    || renderType == RenderType.tripwire()
                    || renderTypeName.contains("translucent")
                    || renderTypeName.contains("tripwire")) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                RenderSystem.depthMask(false);
            } else {
                RenderSystem.disableBlend();
                RenderSystem.depthMask(true);
            }

            bufferSource.endBatch(renderType);
        }
    }

    private static void renderLineBoxes(
            Minecraft minecraft,
            PoseStack poseStack,
            PreviewStructure structure,
            BlockPos origin,
            boolean markSameBlocksAsBlue
    ) {
        MultiBufferSource.BufferSource bufferSource = previewBufferSource();
        RenderType renderType = RenderType.translucent();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        VertexConsumer quads = bufferSource.getBuffer(renderType);
        Matrix4f matrix = poseStack.last().pose();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        TextureAtlasSprite sprite = previewWhiteSprite(minecraft);

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

            addBillboardLineBox(
                    quads,
                    matrix,
                    camera,
                    sprite,
                    worldPos.getX(),
                    worldPos.getY(),
                    worldPos.getZ(),
                    worldPos.getX() + 1.0f,
                    worldPos.getY() + 1.0f,
                    worldPos.getZ() + 1.0f,
                    0.0085f,
                    red,
                    green,
                    blue,
                    1.0f
            );
        }

        bufferSource.endBatch(renderType);
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

        MultiBufferSource.BufferSource bufferSource = previewBufferSource();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        VertexConsumer fill = bufferSource.getBuffer(PreviewRenderTypes.SELECTION_FILL);

        addVisibleSelectionFillBoxColor(
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
                0.14f
        );

        bufferSource.endBatch(PreviewRenderTypes.SELECTION_FILL);

        RenderType lineRenderType = RenderType.translucent();

        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);

        VertexConsumer quads = bufferSource.getBuffer(lineRenderType);
        Matrix4f matrix = poseStack.last().pose();
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        TextureAtlasSprite sprite = previewWhiteSprite(minecraft);

        addBillboardLineBox(
                quads,
                matrix,
                camera,
                sprite,
                x1,
                y1,
                z1,
                x2,
                y2,
                z2,
                0.0085f,
                0.20f,
                0.85f,
                1.00f,
                1.00f
        );

        renderCornerMarker(
                quads,
                matrix,
                camera,
                sprite,
                a,
                1.00f,
                0.90f,
                0.20f,
                1.00f
        );

        renderCornerMarker(
                quads,
                matrix,
                camera,
                sprite,
                b,
                0.20f,
                1.00f,
                0.85f,
                1.00f
        );

        bufferSource.endBatch(lineRenderType);
    }

    private static void addVisibleSelectionFillBoxColor(
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
            quadColor(
                    consumer,
                    matrix,
                    minX, minY, minZ,
                    minX, maxY, minZ,
                    minX, maxY, maxZ,
                    minX, minY, maxZ,
                    red, green, blue, alpha
            );
        } else if (camera.x > maxX) {
            quadColor(
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
            quadColor(
                    consumer,
                    matrix,
                    minX, minY, maxZ,
                    maxX, minY, maxZ,
                    maxX, minY, minZ,
                    minX, minY, minZ,
                    red, green, blue, alpha
            );
        } else if (camera.y > maxY) {
            quadColor(
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
            quadColor(
                    consumer,
                    matrix,
                    maxX, minY, minZ,
                    maxX, maxY, minZ,
                    minX, maxY, minZ,
                    minX, minY, minZ,
                    red, green, blue, alpha
            );
        } else if (camera.z > maxZ) {
            quadColor(
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

    private static void quadColor(
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
        consumer.vertex(matrix, x1, y1, z1)
                .color(red, green, blue, alpha)
                .endVertex();

        consumer.vertex(matrix, x2, y2, z2)
                .color(red, green, blue, alpha)
                .endVertex();

        consumer.vertex(matrix, x3, y3, z3)
                .color(red, green, blue, alpha)
                .endVertex();

        consumer.vertex(matrix, x4, y4, z4)
                .color(red, green, blue, alpha)
                .endVertex();
    }

    private static void renderCornerMarker(
            VertexConsumer quads,
            Matrix4f matrix,
            Vec3 camera,
            TextureAtlasSprite sprite,
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

        addBillboardLineBox(
                quads,
                matrix,
                camera,
                sprite,
                minX,
                minY,
                minZ,
                maxX,
                maxY,
                maxZ,
                0.0115f,
                red,
                green,
                blue,
                alpha
        );
    }

    private static TextureAtlasSprite previewWhiteSprite(Minecraft minecraft) {
        return minecraft
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(new ResourceLocation("minecraft", "block/white_concrete"));
    }

    private static void addBillboardLineBox(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vec3 camera,
            TextureAtlasSprite sprite,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float halfWidth,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        billboardLine(consumer, matrix, camera, sprite, x1, y1, z1, x2, y1, z1, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x2, y1, z1, x2, y1, z2, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x2, y1, z2, x1, y1, z2, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x1, y1, z2, x1, y1, z1, halfWidth, red, green, blue, alpha);

        billboardLine(consumer, matrix, camera, sprite, x1, y2, z1, x2, y2, z1, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x2, y2, z1, x2, y2, z2, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x2, y2, z2, x1, y2, z2, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x1, y2, z2, x1, y2, z1, halfWidth, red, green, blue, alpha);

        billboardLine(consumer, matrix, camera, sprite, x1, y1, z1, x1, y2, z1, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x2, y1, z1, x2, y2, z1, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x2, y1, z2, x2, y2, z2, halfWidth, red, green, blue, alpha);
        billboardLine(consumer, matrix, camera, sprite, x1, y1, z2, x1, y2, z2, halfWidth, red, green, blue, alpha);
    }

    private static void billboardLine(
            VertexConsumer consumer,
            Matrix4f matrix,
            Vec3 camera,
            TextureAtlasSprite sprite,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float halfWidth,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        Vector3f dir = new Vector3f(
                x2 - x1,
                y2 - y1,
                z2 - z1
        );

        if (dir.lengthSquared() < 1.0e-6f) {
            return;
        }

        Vector3f mid = new Vector3f(
                (x1 + x2) * 0.5f,
                (y1 + y2) * 0.5f,
                (z1 + z2) * 0.5f
        );

        Vector3f toCamera = new Vector3f(
                (float) camera.x - mid.x,
                (float) camera.y - mid.y,
                (float) camera.z - mid.z
        );

        if (toCamera.lengthSquared() < 1.0e-6f) {
            toCamera.set(0.0f, 1.0f, 0.0f);
        }

        Vector3f side = dir.cross(toCamera, new Vector3f());

        if (side.lengthSquared() < 1.0e-6f) {
            side = dir.cross(new Vector3f(0.0f, 1.0f, 0.0f), new Vector3f());

            if (side.lengthSquared() < 1.0e-6f) {
                side = dir.cross(new Vector3f(1.0f, 0.0f, 0.0f), new Vector3f());
            }
        }

        if (side.lengthSquared() < 1.0e-6f) {
            return;
        }

        side.normalize(halfWidth);

        float u0 = sprite.getU0();
        float v0 = sprite.getV0();
        float u1 = sprite.getU1();
        float v1 = sprite.getV1();

        vertex(
                consumer,
                matrix,
                x1 + side.x,
                y1 + side.y,
                z1 + side.z,
                red,
                green,
                blue,
                alpha,
                u0,
                v0
        );

        vertex(
                consumer,
                matrix,
                x1 - side.x,
                y1 - side.y,
                z1 - side.z,
                red,
                green,
                blue,
                alpha,
                u0,
                v1
        );

        vertex(
                consumer,
                matrix,
                x2 - side.x,
                y2 - side.y,
                z2 - side.z,
                red,
                green,
                blue,
                alpha,
                u1,
                v1
        );

        vertex(
                consumer,
                matrix,
                x2 + side.x,
                y2 + side.y,
                z2 + side.z,
                red,
                green,
                blue,
                alpha,
                u1,
                v0
        );
    }

    private static void vertex(
            VertexConsumer consumer,
            Matrix4f matrix,
            float x,
            float y,
            float z,
            float red,
            float green,
            float blue,
            float alpha,
            float u,
            float v
    ) {
        consumer.vertex(matrix, x, y, z)
                .color(red, green, blue, alpha)
                .uv(u, v)
                .uv2(240, 240)
                .normal(0.0f, 1.0f, 0.0f)
                .endVertex();
    }

    private static MultiBufferSource.BufferSource previewBufferSource() {
        return MultiBufferSource.immediate(new BufferBuilder(PREVIEW_BUFFER_SIZE));
    }

    private static void resetPreviewRenderState(
            Minecraft minecraft,
            boolean turnOffLightLayer
    ) {
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);

        if (turnOffLightLayer) {
            try {
                minecraft.gameRenderer.lightTexture().turnOffLightLayer();
            } catch (Throwable ignored) {
            }
        }
    }
}