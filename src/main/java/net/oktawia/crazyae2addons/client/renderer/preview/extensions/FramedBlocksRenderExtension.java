package net.oktawia.crazyae2addons.client.renderer.preview.extensions;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.client.renderer.preview.BlockRenderExtension;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlockAndTintGetter;
import org.jetbrains.annotations.Nullable;

public final class FramedBlocksRenderExtension implements BlockRenderExtension {

    private static final String MOD_ID = "framedblocks";

    @Override
    public boolean canRender(BlockState state, @Nullable CompoundTag rawBeTag) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        return blockId != null && MOD_ID.equals(blockId.getNamespace());
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
        ModelData modelData = getModelData(localLevel, model, state, localPos);

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

        return true;
    }

    private static ModelData getModelData(
            PreviewBlockAndTintGetter localLevel,
            BakedModel model,
            BlockState state,
            BlockPos localPos
    ) {
        ModelData baseData;

        try {
            baseData = model.getModelData(localLevel, localPos, state, ModelData.EMPTY);
        } catch (Throwable ignored) {
            baseData = ModelData.EMPTY;
        }

        BlockEntity blockEntity = localLevel.getBlockEntity(localPos);
        if (blockEntity == null) {
            return baseData;
        }

        try {
            ModelData blockEntityData = blockEntity.getModelData();
            return blockEntityData != null ? blockEntityData : baseData;
        } catch (Throwable ignored) {
            return baseData;
        }
    }
}