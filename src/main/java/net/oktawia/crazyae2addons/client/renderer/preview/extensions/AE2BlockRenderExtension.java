package net.oktawia.crazyae2addons.client.renderer.preview.extensions;

import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.render.cablebus.CableBusRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.oktawia.crazyae2addons.client.renderer.preview.BlockRenderExtension;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewBlockAndTintGetter;
import net.oktawia.crazyae2addons.util.StructureToolKeys;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

public final class AE2BlockRenderExtension implements BlockRenderExtension {

    private static final String AE2_CABLE_BUS_ID = "ae2:cable_bus";

    @Override
    public boolean canRender(BlockState state, @Nullable CompoundTag rawBeTag) {
        return isAe2CableBusTag(rawBeTag);
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
        ModelData ae2ModelData = getCableBusModelData(
                localLevel,
                localPos,
                sideMap,
                modelData
        );

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
                ae2ModelData,
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
        ModelData modelData = getCableBusModelData(
                localLevel,
                localPos,
                sideMap,
                ModelData.EMPTY
        );

        for (RenderType renderType : model.getRenderTypes(
                state,
                RandomSource.create(seed),
                modelData
        )) {
            dispatcher.getModelRenderer().tesselateBlock(
                    localLevel,
                    model,
                    state,
                    localPos,
                    poseStack,
                    bufferSource.getBuffer(toGuiSafeRenderType(renderType)),
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

    private static RenderType toGuiSafeRenderType(RenderType renderType) {
        String name = renderType.toString();

        if (name.contains("translucent")) {
            return RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS);
        }

        return RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS);
    }

    private static ModelData getCableBusModelData(
            PreviewBlockAndTintGetter localLevel,
            BlockPos localPos,
            int[] sideMap,
            ModelData fallback
    ) {
        BlockEntity blockEntity = localLevel.getBlockEntity(localPos);

        if (!(blockEntity instanceof CableBusBlockEntity)) {
            return fallback;
        }

        ModelData modelData;

        try {
            modelData = blockEntity.getModelData();
        } catch (Throwable ignored) {
            return fallback;
        }

        if (modelData == null) {
            return fallback;
        }

        CableBusRenderState renderState = modelData.get(CableBusRenderState.PROPERTY);

        if (renderState == null) {
            return modelData;
        }

        CableBusRenderState transformedState = copyCableBusRenderState(renderState);
        transformCableConnectionsOnly(transformedState, sideMap);

        return ModelData.builder()
                .with(CableBusRenderState.PROPERTY, transformedState)
                .build();
    }

    private static CableBusRenderState copyCableBusRenderState(CableBusRenderState source) {
        CableBusRenderState copy = new CableBusRenderState();

        copy.setCableType(source.getCableType());
        copy.setCoreType(source.getCoreType());
        copy.setCableColor(source.getCableColor());

        copy.getConnectionTypes().putAll(source.getConnectionTypes());
        copy.getCableBusAdjacent().addAll(source.getCableBusAdjacent());
        copy.getChannelsOnSide().putAll(source.getChannelsOnSide());

        copy.getAttachments().putAll(source.getAttachments());
        copy.getAttachmentConnections().putAll(source.getAttachmentConnections());
        copy.getFacades().putAll(source.getFacades());
        copy.getPartModelData().putAll(source.getPartModelData());
        copy.getBoundingBoxes().addAll(source.getBoundingBoxes());

        return copy;
    }

    private static void transformCableConnectionsOnly(
            CableBusRenderState renderState,
            int[] sideMap
    ) {
        remapDirectionMap(renderState.getConnectionTypes(), sideMap);
        remapDirectionSet(renderState.getCableBusAdjacent(), sideMap);
        remapDirectionMap(renderState.getChannelsOnSide(), sideMap);
    }

    private static <V> void remapDirectionMap(Map<Direction, V> map, int[] sideMap) {
        if (map.isEmpty()) {
            return;
        }

        java.util.EnumMap<Direction, V> old = new java.util.EnumMap<>(Direction.class);
        old.putAll(map);
        map.clear();

        for (Map.Entry<Direction, V> entry : old.entrySet()) {
            map.put(mapWithSideMap(entry.getKey(), sideMap), entry.getValue());
        }
    }

    private static void remapDirectionSet(Set<Direction> set, int[] sideMap) {
        if (set.isEmpty()) {
            return;
        }

        java.util.EnumSet<Direction> old = java.util.EnumSet.copyOf(set);
        set.clear();

        for (Direction side : old) {
            set.add(mapWithSideMap(side, sideMap));
        }
    }

    private static Direction mapWithSideMap(Direction side, int[] sideMap) {
        if (sideMap == null || side.ordinal() < 0 || side.ordinal() >= sideMap.length) {
            return side;
        }

        int mappedOrdinal = sideMap[side.ordinal()];
        Direction[] values = Direction.values();

        if (mappedOrdinal < 0 || mappedOrdinal >= values.length) {
            return side;
        }

        return values[mappedOrdinal];
    }

    private static boolean isAe2CableBusTag(@Nullable CompoundTag rawBeTag) {
        if (rawBeTag == null) {
            return false;
        }

        String id = rawBeTag.getString("id");

        if (AE2_CABLE_BUS_ID.equals(id)) {
            return true;
        }

        if (!id.isBlank()) {
            return false;
        }

        for (String key : StructureToolKeys.AE2_CABLE_BUS_KEYS) {
            if (rawBeTag.contains(key)) {
                return true;
            }
        }

        return false;
    }
}