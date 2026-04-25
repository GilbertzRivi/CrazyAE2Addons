package net.oktawia.crazyae2addons.client.renderer.preview;

import com.lowdragmc.lowdraglib.gui.widget.SceneWidget;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PortableSpatialStorageSceneWidget extends SceneWidget {

    private static final BlockState FLOOR_A = Blocks.SMOOTH_STONE.defaultBlockState();
    private static final BlockState FLOOR_B = Blocks.POLISHED_ANDESITE.defaultBlockState();
    private static final int FLOOR_PADDING = 2;

    protected final PortableSpatialStorageDummyWorld previewWorld;

    protected PreviewStructure previewStructure;
    protected int[] sideMap = new int[] {0, 1, 2, 3, 4, 5};
    protected String sideMapKey = Arrays.toString(sideMap);

    protected final Set<BlockPos> extensionBlocks = new HashSet<>();
    protected final Set<BlockPos> specialBlocks = new HashSet<>();
    protected final Set<BlockPos> normalBlocks = new HashSet<>();

    protected BlockPos originMarkerPos = BlockPos.ZERO;
    protected BlockPos floorAnchorPos = BlockPos.ZERO;

    protected int floorMinX;
    protected int floorMaxX;
    protected int floorMinZ;
    protected int floorMaxZ;
    protected int floorY;
    protected boolean hasFloor = false;

    public PortableSpatialStorageSceneWidget(int x, int y, int width, int height, PortableSpatialStorageDummyWorld world) {
        super(x, y, width, height, world);
        this.previewWorld = world;
        this.useCacheBuffer(false);
    }

    public void setPreview(
            PreviewStructure structure,
            int[] sideMap,
            Set<BlockPos> renderedSurface,
            BlockPos originMarkerPos,
            BlockPos floorAnchorPos
    ) {
        this.previewStructure = structure;
        this.sideMap = Arrays.copyOf(sideMap, sideMap.length);
        this.sideMapKey = Arrays.toString(sideMap);
        this.originMarkerPos = originMarkerPos == null ? BlockPos.ZERO : originMarkerPos;
        this.floorAnchorPos = floorAnchorPos == null ? BlockPos.ZERO : floorAnchorPos;

        this.extensionBlocks.clear();
        this.normalBlocks.clear();
        this.specialBlocks.clear();
        this.hasFloor = false;

        if (structure == null || structure.blocks().isEmpty()) {
            setRenderedCore(Set.of());
            needCompileCache();
            return;
        }

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (PreviewBlock block : structure.blocks()) {
            BlockPos pos = block.pos();
            BlockState state = block.state();

            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());

            if (classifySpecialBlock(block, pos, state, renderedSurface)) {
                continue;
            }

            if (isExtensionRenderedBlock(block, state)) {
                extensionBlocks.add(pos);
            } else if (renderedSurface.contains(pos)) {
                normalBlocks.add(pos);
            }
        }

        this.floorMinX = Math.min(Math.min(minX, this.floorAnchorPos.getX()), this.originMarkerPos.getX()) - FLOOR_PADDING;
        this.floorMaxX = Math.max(Math.max(maxX, this.floorAnchorPos.getX()), this.originMarkerPos.getX()) + FLOOR_PADDING;
        this.floorMinZ = Math.min(Math.min(minZ, this.floorAnchorPos.getZ()), this.originMarkerPos.getZ()) - FLOOR_PADDING;
        this.floorMaxZ = Math.max(Math.max(maxZ, this.floorAnchorPos.getZ()), this.originMarkerPos.getZ()) + FLOOR_PADDING;

        int floorBaseY = Math.min(minY, this.originMarkerPos.getY());
        this.floorY = floorBaseY - 1;
        this.hasFloor = true;

        setRenderedCore(normalBlocks);
        needCompileCache();
    }

    protected boolean classifySpecialBlock(PreviewBlock block, BlockPos pos, BlockState state, Set<BlockPos> renderedSurface) {
        return false;
    }

    protected boolean renderSpecialBlock(
            PreviewBlock previewBlock,
            BlockRenderDispatcher dispatcher,
            PreviewBlockAndTintGetter localLevel,
            BlockState state,
            BakedModel model,
            BlockPos localPos,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            long seed
    ) {
        return false;
    }

    private boolean isExtensionRenderedBlock(PreviewBlock previewBlock, BlockState state) {
        CompoundTag rawBeTag = previewBlock.blockEntityTag();

        for (BlockRenderExtension extension : BlockRenderExtensions.all()) {
            if (extension.canRender(state, rawBeTag)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void renderBeforeBatchEnd(MultiBufferSource bufferSource, float partialTicks) {
        if (previewStructure == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel clientLevel = minecraft.level;
        if (clientLevel == null) {
            return;
        }

        renderFloor(bufferSource, minecraft);
        renderOriginMarker(bufferSource);

        if (extensionBlocks.isEmpty() && specialBlocks.isEmpty()) {
            return;
        }

        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        PreviewBlockAndTintGetter localLevel = new PreviewBlockAndTintGetter(clientLevel, previewStructure, BlockPos.ZERO);

        PoseStack poseStack = new PoseStack();

        for (PreviewBlock previewBlock : previewStructure.blocks()) {
            BlockPos localPos = previewBlock.pos();

            if (!extensionBlocks.contains(localPos) && !specialBlocks.contains(localPos)) {
                continue;
            }

            BlockState state = previewBlock.state();
            BakedModel model = dispatcher.getBlockModel(state);
            long seed = state.getSeed(localPos);

            ModelData modelData = PreviewRenderModelDataHelper.getPreviewModelData(
                    previewStructure,
                    previewBlock,
                    sideMap,
                    sideMapKey,
                    clientLevel,
                    model,
                    localLevel
            );

            poseStack.pushPose();
            poseStack.translate(localPos.getX(), localPos.getY(), localPos.getZ());

            try {
                if (!renderExtensionBlock(
                        previewBlock,
                        dispatcher,
                        localLevel,
                        state,
                        model,
                        localPos,
                        poseStack,
                        bufferSource,
                        seed
                ) && !renderSpecialBlock(
                        previewBlock,
                        dispatcher,
                        localLevel,
                        state,
                        model,
                        localPos,
                        poseStack,
                        bufferSource,
                        seed
                )) {
                    renderStandardBlock(
                            dispatcher,
                            localLevel,
                            state,
                            model,
                            localPos,
                            poseStack,
                            bufferSource,
                            seed,
                            modelData
                    );
                }
            } catch (Throwable ignored) {
            }

            poseStack.popPose();
        }
    }

    private boolean renderExtensionBlock(
            PreviewBlock previewBlock,
            BlockRenderDispatcher dispatcher,
            PreviewBlockAndTintGetter localLevel,
            BlockState state,
            BakedModel model,
            BlockPos localPos,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            long seed
    ) {
        CompoundTag rawBeTag = previewBlock.blockEntityTag();

        for (BlockRenderExtension extension : BlockRenderExtensions.all()) {
            if (!extension.canRender(state, rawBeTag)) {
                continue;
            }

            if (extension.renderForWidget(
                    previewBlock,
                    sideMap,
                    dispatcher,
                    localLevel,
                    state,
                    model,
                    localPos,
                    poseStack,
                    bufferSource,
                    seed
            )) {
                return true;
            }
        }

        return false;
    }

    private void renderStandardBlock(
            BlockRenderDispatcher dispatcher,
            PreviewBlockAndTintGetter localLevel,
            BlockState state,
            BakedModel model,
            BlockPos localPos,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            long seed,
            ModelData modelData
    ) {
        for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(seed), modelData)) {
            dispatcher.getModelRenderer().tesselateBlock(
                    localLevel,
                    model,
                    state,
                    localPos,
                    poseStack,
                    bufferSource.getBuffer(renderType),
                    false,
                    RandomSource.create(seed),
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    modelData,
                    renderType
            );
        }
    }

    private void renderFloor(MultiBufferSource bufferSource, Minecraft minecraft) {
        if (!hasFloor) {
            return;
        }

        BlockRenderDispatcher dispatcher = minecraft.getBlockRenderer();
        PoseStack poseStack = new PoseStack();

        for (int x = floorMinX; x <= floorMaxX; x++) {
            for (int z = floorMinZ; z <= floorMaxZ; z++) {
                BlockPos pos = new BlockPos(x, floorY, z);
                BlockState state = ((x + z) & 1) == 0 ? FLOOR_A : FLOOR_B;
                BakedModel model = dispatcher.getBlockModel(state);
                long seed = state.getSeed(pos);

                poseStack.pushPose();
                poseStack.translate(pos.getX(), pos.getY(), pos.getZ());

                try {
                    for (RenderType renderType : model.getRenderTypes(state, RandomSource.create(seed), ModelData.EMPTY)) {
                        dispatcher.getModelRenderer().tesselateBlock(
                                previewWorld,
                                model,
                                state,
                                pos,
                                poseStack,
                                bufferSource.getBuffer(renderType),
                                false,
                                RandomSource.create(seed),
                                seed,
                                OverlayTexture.NO_OVERLAY,
                                ModelData.EMPTY,
                                renderType
                        );
                    }
                } catch (Throwable ignored) {
                }

                poseStack.popPose();
            }
        }
    }

    private void renderOriginMarker(MultiBufferSource bufferSource) {
        PoseStack poseStack = new PoseStack();
        VertexConsumer lines = bufferSource.getBuffer(RenderType.lines());

        double minX = originMarkerPos.getX() + 0.001;
        double minY = originMarkerPos.getY() + 0.001;
        double minZ = originMarkerPos.getZ() + 0.001;
        double maxX = originMarkerPos.getX() + 0.999;
        double maxY = originMarkerPos.getY() + 0.999;
        double maxZ = originMarkerPos.getZ() + 0.999;

        LevelRenderer.renderLineBox(
                poseStack,
                lines,
                minX, minY, minZ,
                maxX, maxY, maxZ,
                1.0F, 0.1F, 0.1F, 1.0F
        );
    }
}