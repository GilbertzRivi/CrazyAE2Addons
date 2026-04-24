package net.oktawia.crazyae2addons.compat.gtceu;

import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.oktawia.crazyae2addons.client.misc.PortableSpatialStorageDummyWorld;
import net.oktawia.crazyae2addons.client.misc.PortableSpatialStorageSceneWidget;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlockAndTintGetter;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PortableSpatialStorageSceneWidgetGTCEu extends PortableSpatialStorageSceneWidget {

    public PortableSpatialStorageSceneWidgetGTCEu(int x, int y, int width, int height, PortableSpatialStorageDummyWorld world) {
        super(x, y, width, height, world);
    }

    @Override
    protected boolean classifySpecialBlock(PreviewBlock block, BlockPos pos, BlockState state, Set<BlockPos> renderedSurface) {
        if (state.getBlock() instanceof PipeBlock<?, ?, ?>) {
            if (renderedSurface.contains(pos)) {
                specialBlocks.add(pos);
            }
            return true;
        }

        return false;
    }

    @Override
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
        if (!(state.getBlock() instanceof PipeBlock<?, ?, ?> pipeBlock)) {
            return false;
        }

        CompoundTag tag = previewBlock.blockEntityTag();
        if (tag == null) {
            return true;
        }

        PipeBlockRenderer pipeRenderer = pipeBlock.getRenderer(state);
        if (pipeRenderer == null) {
            return true;
        }

        PipeModel pipeModel = pipeRenderer.getPipeModel();

        int connections = tag.getInt("connections");
        int blockedConnections = tag.getInt("blockedConnections");

        Set<RenderType> renderTypes = new LinkedHashSet<>();
        renderTypes.add(RenderType.cutoutMipped());

        BlockState frameState = GTCEuUtil.getFrameState(tag);
        BakedModel frameModel = null;

        if (frameState != null) {
            frameModel = dispatcher.getBlockModel(frameState);
            for (RenderType frameType : frameModel.getRenderTypes(frameState, RandomSource.create(seed), ModelData.EMPTY)) {
                renderTypes.add(frameType);
            }
        }

        for (RenderType renderType : renderTypes) {
            if (renderType == RenderType.cutoutMipped()) {
                BakedModel pipePreviewModel = new BakedModelWrapper<>(model) {
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
                    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
                        return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
                    }
                };

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

            if (frameState != null && frameModel != null) {
                boolean shouldRenderFrameInThisPass = false;

                for (RenderType frameType : frameModel.getRenderTypes(frameState, RandomSource.create(seed), ModelData.EMPTY)) {
                    if (frameType == renderType) {
                        shouldRenderFrameInThisPass = true;
                        break;
                    }
                }

                if (shouldRenderFrameInThisPass) {
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
            }
        }

        return true;
    }

}