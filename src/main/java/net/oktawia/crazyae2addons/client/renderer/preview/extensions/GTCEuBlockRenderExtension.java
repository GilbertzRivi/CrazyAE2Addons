package net.oktawia.crazyae2addons.client.renderer.preview.extensions;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.client.renderer.preview.BlockRenderExtension;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlockAndTintGetter;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GTCEuBlockRenderExtension implements BlockRenderExtension {

    @Override
    public boolean canRender(BlockState state, @Nullable CompoundTag rawBeTag) {
        return state.getBlock() instanceof PipeBlock<?, ?, ?>;
    }

    @Override
    public @Nullable Iterable<RenderType> getPreviewRenderTypes(
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
        if (!(state.getBlock() instanceof PipeBlock<?, ?, ?> pipeBlock)) {
            return null;
        }

        CompoundTag tag = previewBlock.blockEntityTag();
        if (tag == null) {
            return null;
        }

        PipeBlockRenderer pipeRenderer = pipeBlock.getRenderer(state);
        if (pipeRenderer == null) {
            return null;
        }

        Set<RenderType> types = new LinkedHashSet<>();
        types.add(RenderType.cutoutMipped());

        BlockState frameState = getGregFrameState(tag);
        if (frameState != null) {
            BakedModel frameModel = dispatcher.getBlockModel(frameState);

            for (RenderType frameType : frameModel.getRenderTypes(
                    frameState,
                    RandomSource.create(seed),
                    ModelData.EMPTY
            )) {
                types.add(frameType);
            }
        }

        return types;
    }

    @Override
    public boolean renderForPreview(
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
        if (!(state.getBlock() instanceof PipeBlock<?, ?, ?> pipeBlock)) {
            return false;
        }

        CompoundTag tag = previewBlock.blockEntityTag();
        if (tag == null) {
            return false;
        }

        PipeBlockRenderer pipeRenderer = pipeBlock.getRenderer(state);
        if (pipeRenderer == null) {
            return false;
        }

        boolean renderedAnything = false;

        PipeModel pipeModel = pipeRenderer.getPipeModel();
        int connections = tag.getInt("connections");
        int blockedConnections = tag.getInt("blockedConnections");

        if (renderType == RenderType.cutoutMipped()) {
            BakedModel pipePreviewModel = createPipePreviewModel(
                    model,
                    pipeModel,
                    connections,
                    blockedConnections
            );

            modelRenderer.tesselateBlock(
                    localLevel,
                    pipePreviewModel,
                    state,
                    localPos,
                    poseStack,
                    bufferBuilder,
                    false,
                    RandomSource.create(seed),
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    ModelData.EMPTY,
                    RenderType.cutoutMipped()
            );

            renderedAnything = true;
        }

        BlockState frameState = getGregFrameState(tag);
        if (frameState == null) {
            return renderedAnything;
        }

        BakedModel frameModel = dispatcher.getBlockModel(frameState);
        if (!shouldRenderFrameInPass(frameModel, frameState, renderType, seed)) {
            return renderedAnything;
        }

        modelRenderer.tesselateBlock(
                localLevel,
                frameModel,
                frameState,
                localPos,
                poseStack,
                bufferBuilder,
                false,
                RandomSource.create(seed),
                seed,
                OverlayTexture.NO_OVERLAY,
                ModelData.EMPTY,
                renderType
        );

        return true;
    }

    @Override
    public boolean renderForWidget(
            PreviewBlock previewBlock,
            int[] sideMap,
            BlockRenderDispatcher dispatcher,
            PreviewBlockAndTintGetter localLevel,
            BlockState state,
            BakedModel model,
            BlockPos localPos,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            long seed
    ) {
        if (!(state.getBlock() instanceof PipeBlock<?, ?, ?> pipeBlock)) {
            return false;
        }

        CompoundTag tag = previewBlock.blockEntityTag();
        if (tag == null) {
            return false;
        }

        PipeBlockRenderer pipeRenderer = pipeBlock.getRenderer(state);
        if (pipeRenderer == null) {
            return false;
        }

        PipeModel pipeModel = pipeRenderer.getPipeModel();

        int connections = tag.getInt("connections");
        int blockedConnections = tag.getInt("blockedConnections");

        Set<RenderType> renderTypes = collectWidgetRenderTypes(dispatcher, tag, seed);

        for (RenderType renderType : renderTypes) {
            if (renderType == RenderType.cutoutMipped()) {
                BakedModel pipePreviewModel = createPipePreviewModel(
                        model,
                        pipeModel,
                        connections,
                        blockedConnections
                );

                dispatcher.getModelRenderer().tesselateBlock(
                        localLevel,
                        pipePreviewModel,
                        state,
                        localPos,
                        poseStack,
                        bufferSource.getBuffer(RenderType.cutoutMipped()),
                        false,
                        RandomSource.create(seed),
                        seed,
                        OverlayTexture.NO_OVERLAY,
                        ModelData.EMPTY,
                        RenderType.cutoutMipped()
                );
            }

            BlockState frameState = getGregFrameState(tag);
            if (frameState == null) {
                continue;
            }

            BakedModel frameModel = dispatcher.getBlockModel(frameState);
            if (!shouldRenderFrameInPass(frameModel, frameState, renderType, seed)) {
                continue;
            }

            dispatcher.getModelRenderer().tesselateBlock(
                    localLevel,
                    frameModel,
                    frameState,
                    localPos,
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

        return true;
    }

    private static Set<RenderType> collectWidgetRenderTypes(
            BlockRenderDispatcher dispatcher,
            CompoundTag tag,
            long seed
    ) {
        Set<RenderType> renderTypes = new LinkedHashSet<>();
        renderTypes.add(RenderType.cutoutMipped());

        BlockState frameState = getGregFrameState(tag);
        if (frameState == null) {
            return renderTypes;
        }

        BakedModel frameModel = dispatcher.getBlockModel(frameState);
        for (RenderType frameType : frameModel.getRenderTypes(
                frameState,
                RandomSource.create(seed),
                ModelData.EMPTY
        )) {
            renderTypes.add(frameType);
        }

        return renderTypes;
    }

    private static boolean shouldRenderFrameInPass(
            BakedModel frameModel,
            BlockState frameState,
            RenderType renderType,
            long seed
    ) {
        for (RenderType frameType : frameModel.getRenderTypes(
                frameState,
                RandomSource.create(seed),
                ModelData.EMPTY
        )) {
            if (frameType == renderType) {
                return true;
            }
        }

        return false;
    }

    private static BakedModel createPipePreviewModel(
            BakedModel baseModel,
            PipeModel pipeModel,
            int connections,
            int blockedConnections
    ) {
        return new BakedModelWrapper<>(baseModel) {
            @Override
            public List<BakedQuad> getQuads(
                    BlockState state,
                    Direction side,
                    RandomSource random,
                    ModelData data,
                    RenderType requestedRenderType
            ) {
                if (requestedRenderType == null || requestedRenderType == RenderType.cutoutMipped()) {
                    return pipeModel.bakeQuads(side, connections, blockedConnections);
                }

                return List.of();
            }

            @Override
            public ChunkRenderTypeSet getRenderTypes(
                    BlockState state,
                    RandomSource random,
                    ModelData data
            ) {
                return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
            }
        };
    }

    @Nullable
    private static BlockState getGregFrameState(CompoundTag tag) {
        if (!tag.contains("frameMaterial")) {
            return null;
        }

        return getGregFrameState(tag.getString("frameMaterial"));
    }

    @Nullable
    private static BlockState getGregFrameState(String frameMaterial) {
        if (frameMaterial == null || frameMaterial.isBlank()) {
            return null;
        }

        String materialPath = frameMaterial;
        int sep = materialPath.indexOf(':');
        if (sep >= 0 && sep + 1 < materialPath.length()) {
            materialPath = materialPath.substring(sep + 1);
        }

        ResourceLocation frameId = new ResourceLocation("gtceu", materialPath + "_frame");
        Block frameBlock = ForgeRegistries.BLOCKS.getValue(frameId);

        if (frameBlock == null || frameBlock == Blocks.AIR) {
            return null;
        }

        return frameBlock.defaultBlockState();
    }
}