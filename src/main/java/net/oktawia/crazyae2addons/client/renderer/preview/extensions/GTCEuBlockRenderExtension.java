package net.oktawia.crazyae2addons.client.renderer.preview.extensions;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.client.renderer.preview.BlockRenderExtension;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlockAndTintGetter;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class GTCEuBlockRenderExtension implements BlockRenderExtension {

    private static final String GT_MODEL_PROPERTIES_CLASS =
            "com.gregtechceu.gtceu.client.model.GTModelProperties";

    private static final RenderType PIPE_RENDER_TYPE = RenderType.cutoutMipped();

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
        if (!(state.getBlock() instanceof PipeBlock<?, ?, ?>)) {
            return null;
        }

        CompoundTag tag = previewBlock.blockEntityTag();

        if (tag == null) {
            return null;
        }

        Set<RenderType> types = new LinkedHashSet<>();
        types.add(PIPE_RENDER_TYPE);

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
            VertexConsumer vertexConsumer,
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

        int connections = tag.getInt("connections");
        int blockedConnections = tag.getInt("blockedConnections");

        boolean renderedAnything = false;

        if (renderType == PIPE_RENDER_TYPE) {
            if (tryRenderRealGregPipeModel(
                    modelRenderer,
                    localLevel,
                    model,
                    state,
                    localPos,
                    poseStack,
                    vertexConsumer,
                    seed,
                    connections,
                    blockedConnections
            )) {
                renderedAnything = true;
            } else {
                PipeBlockRenderer pipeRenderer = pipeBlock.getRenderer(state);

                if (pipeRenderer != null) {
                    PipeModel pipeModel = pipeRenderer.getPipeModel();

                    if (pipeModel != null) {
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
                                vertexConsumer,
                                false,
                                RandomSource.create(seed),
                                seed,
                                OverlayTexture.NO_OVERLAY,
                                ModelData.EMPTY,
                                PIPE_RENDER_TYPE
                        );

                        renderedAnything = true;
                    }
                }
            }
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
                vertexConsumer,
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

        int connections = tag.getInt("connections");
        int blockedConnections = tag.getInt("blockedConnections");

        Set<RenderType> renderTypes = collectWidgetRenderTypes(
                dispatcher,
                tag,
                seed
        );

        for (RenderType renderType : renderTypes) {
            if (renderType == PIPE_RENDER_TYPE) {
                VertexConsumer guiConsumer = bufferSource.getBuffer(
                        RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS)
                );

                if (!tryRenderRealGregPipeModel(
                        dispatcher.getModelRenderer(),
                        localLevel,
                        model,
                        state,
                        localPos,
                        poseStack,
                        guiConsumer,
                        seed,
                        connections,
                        blockedConnections
                )) {
                    PipeBlockRenderer pipeRenderer = pipeBlock.getRenderer(state);

                    if (pipeRenderer != null) {
                        PipeModel pipeModel = pipeRenderer.getPipeModel();

                        if (pipeModel != null) {
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
                                    guiConsumer,
                                    false,
                                    RandomSource.create(seed),
                                    seed,
                                    OverlayTexture.NO_OVERLAY,
                                    ModelData.EMPTY,
                                    PIPE_RENDER_TYPE
                            );
                        }
                    }
                }
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
                    bufferSource.getBuffer(toGuiSafeRenderType(renderType)),
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

    private static boolean tryRenderRealGregPipeModel(
            ModelBlockRenderer modelRenderer,
            PreviewBlockAndTintGetter localLevel,
            BakedModel model,
            BlockState state,
            BlockPos localPos,
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            long seed,
            int connections,
            int blockedConnections
    ) {
        GregModelDataResult gregModelData = createGregPipeModelData(
                localLevel,
                localPos,
                connections,
                blockedConnections
        );

        if (!gregModelData.usable()) {
            return false;
        }

        CountingVertexConsumer countingConsumer = new CountingVertexConsumer(vertexConsumer);

        try {
            modelRenderer.tesselateBlock(
                    localLevel,
                    model,
                    state,
                    localPos,
                    poseStack,
                    countingConsumer,
                    false,
                    RandomSource.create(seed),
                    seed,
                    OverlayTexture.NO_OVERLAY,
                    gregModelData.modelData(),
                    PIPE_RENDER_TYPE
            );
        } catch (Throwable ignored) {
            return false;
        }

        return countingConsumer.vertexCount() > 0;
    }

    private static GregModelDataResult createGregPipeModelData(
            BlockAndTintGetter level,
            BlockPos localPos,
            int connections,
            int blockedConnections
    ) {
        ModelData.Builder builder = ModelData.builder();
        boolean usable = false;

        usable |= putModelPropertyIfPresent(builder, "LEVEL", level);
        usable |= putModelPropertyIfPresent(builder, "POS", localPos);
        usable |= putModelPropertyIfPresent(builder, "PIPE_CONNECTION_MASK", connections);
        usable |= putModelPropertyIfPresent(builder, "PIPE_BLOCKED_MASK", blockedConnections);

        return new GregModelDataResult(
                usable ? builder.build() : ModelData.EMPTY,
                usable
        );
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean putModelPropertyIfPresent(
            ModelData.Builder builder,
            String fieldName,
            Object value
    ) {
        try {
            Class<?> propertiesClass = Class.forName(GT_MODEL_PROPERTIES_CLASS);
            Field field = propertiesClass.getField(fieldName);
            Object property = field.get(null);

            if (!(property instanceof ModelProperty<?> modelProperty)) {
                return false;
            }

            builder.with((ModelProperty) modelProperty, value);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private record GregModelDataResult(ModelData modelData, boolean usable) {
    }

    private static final class CountingVertexConsumer implements VertexConsumer {

        private final VertexConsumer delegate;
        private int vertexCount;

        private CountingVertexConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        public int vertexCount() {
            return vertexCount;
        }

        @Override
        public VertexConsumer vertex(double x, double y, double z) {
            delegate.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int r, int g, int b, int a) {
            delegate.color(r, g, b, a);
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
            vertexCount++;
            delegate.endVertex();
        }

        @Override
        public void defaultColor(int r, int g, int b, int a) {
            delegate.defaultColor(r, g, b, a);
        }

        @Override
        public void unsetDefaultColor() {
            delegate.unsetDefaultColor();
        }
    }

    private static Set<RenderType> collectWidgetRenderTypes(
            BlockRenderDispatcher dispatcher,
            CompoundTag tag,
            long seed
    ) {
        Set<RenderType> renderTypes = new LinkedHashSet<>();

        renderTypes.add(PIPE_RENDER_TYPE);

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

    private static RenderType toGuiSafeRenderType(RenderType renderType) {
        String name = renderType.toString();

        if (name.contains("translucent")) {
            return RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        }

        return RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);
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
                    @Nullable BlockState state,
                    @Nullable Direction side,
                    RandomSource random
            ) {
                return pipeModel.bakeQuads(
                        side,
                        connections,
                        blockedConnections
                );
            }

            @Override
            public List<BakedQuad> getQuads(
                    @Nullable BlockState state,
                    @Nullable Direction side,
                    RandomSource random,
                    ModelData data,
                    @Nullable RenderType requestedRenderType
            ) {
                if (requestedRenderType == null || requestedRenderType == PIPE_RENDER_TYPE) {
                    return pipeModel.bakeQuads(
                            side,
                            connections,
                            blockedConnections
                    );
                }

                return List.of();
            }

            @Override
            public ChunkRenderTypeSet getRenderTypes(
                    BlockState state,
                    RandomSource random,
                    ModelData data
            ) {
                return ChunkRenderTypeSet.of(PIPE_RENDER_TYPE);
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

        ResourceLocation frameId = new ResourceLocation(
                "gtceu",
                materialPath + "_frame"
        );

        Block frameBlock = ForgeRegistries.BLOCKS.getValue(frameId);

        if (frameBlock == null || frameBlock == Blocks.AIR) {
            return null;
        }

        return frameBlock.defaultBlockState();
    }
}