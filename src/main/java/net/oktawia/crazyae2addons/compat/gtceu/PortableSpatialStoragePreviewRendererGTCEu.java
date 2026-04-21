package net.oktawia.crazyae2addons.compat.gtceu;

import com.gregtechceu.gtceu.api.block.MaterialPipeBlock;
import com.gregtechceu.gtceu.api.block.PipeBlock;
import com.gregtechceu.gtceu.client.model.PipeModel;
import com.gregtechceu.gtceu.client.renderer.block.PipeBlockRenderer;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
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
import net.oktawia.crazyae2addons.client.renderer.preview.PortableSpatialStoragePreviewRenderer;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlockAndTintGetter;

import java.util.List;

public class PortableSpatialStoragePreviewRendererGTCEu extends PortableSpatialStoragePreviewRenderer {
    @Override
    protected void tesselatePreviewBlock(
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
            long seed,
            ModelData modelData
    ) {
        if (!(state.getBlock() instanceof PipeBlock<?, ?, ?> pipeBlock)) {
            super.tesselatePreviewBlock(
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
                    seed,
                    modelData
            );
            return;
        }

        CompoundTag tag = previewBlock.blockEntityTag();
        if (tag == null) {
            super.tesselatePreviewBlock(
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
                    seed,
                    modelData
            );
            return;
        }

        PipeBlockRenderer pipeRenderer = pipeBlock.getRenderer(state);
        if (pipeRenderer == null) {
            super.tesselatePreviewBlock(
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
                    seed,
                    modelData
            );
            return;
        }

        PipeModel pipeModel = pipeRenderer.getPipeModel();

        int connections = tag.getInt("connections");
        int blockedConnections = tag.getInt("blockedConnections");

        BakedModel pipePreviewModel = new BakedModelWrapper<>(model) {
            @Override
            public List<BakedQuad> getQuads(
                    BlockState state,
                    Direction side,
                    RandomSource random,
                    ModelData data,
                    RenderType renderType
            ) {
                if (renderType == null || renderType == RenderType.cutoutMipped()) {
                    return pipeModel.bakeQuads(side, connections, blockedConnections);
                }
                return List.of();
            }

            @Override
            public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource random, ModelData data) {
                return ChunkRenderTypeSet.of(RenderType.cutoutMipped());
            }
        };

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
    }
}