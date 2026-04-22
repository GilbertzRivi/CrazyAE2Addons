package net.oktawia.crazyae2addons.client.renderer.preview;

import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.client.render.cablebus.CableBusRenderState;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.model.data.ModelData;
import net.oktawia.crazyae2addons.CrazyAddons;

import java.util.Map;
import java.util.Set;

public final class PreviewRenderModelDataHelper {

    private PreviewRenderModelDataHelper() {
    }

    public static ModelData getPreviewModelData(
            PreviewStructure structure,
            PreviewBlock previewBlock,
            int[] sideMap,
            String sideMapKey,
            ClientLevel level,
            BakedModel model,
            PreviewBlockAndTintGetter localLevel
    ) {
        return structure.getOrComputeModelData(sideMapKey, previewBlock.pos(), () -> {
            ModelData baseData;
            try {
                baseData = model.getModelData(localLevel, previewBlock.pos(), previewBlock.state(), ModelData.EMPTY);
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
                baseData = ModelData.EMPTY;
            }

            BlockEntity blockEntity = structure.blockEntities(level).get(previewBlock.pos());
            if (blockEntity == null) {
                return baseData;
            }

            ModelData modelData;
            try {
                modelData = blockEntity.getModelData();
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
                return baseData;
            }

            if (!(blockEntity instanceof CableBusBlockEntity)) {
                return modelData != null ? modelData : baseData;
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
        });
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

    private static <V> void remapDirectionMap(Map<Direction, V> map, int[] sideMap) {
        if (map.isEmpty()) return;

        java.util.EnumMap<Direction, V> old = new java.util.EnumMap<>(Direction.class);
        old.putAll(map);
        map.clear();

        for (Map.Entry<Direction, V> entry : old.entrySet()) {
            map.put(mapWithSideMap(entry.getKey(), sideMap), entry.getValue());
        }
    }

    private static void remapDirectionSet(Set<Direction> set, int[] sideMap) {
        if (set.isEmpty()) return;

        java.util.EnumSet<Direction> old = java.util.EnumSet.copyOf(set);
        set.clear();

        for (Direction side : old) {
            set.add(mapWithSideMap(side, sideMap));
        }
    }

    private static Direction mapWithSideMap(Direction side, int[] sideMap) {
        return Direction.values()[sideMap[side.ordinal()]];
    }

    private static void transformCableConnectionsOnly(CableBusRenderState renderState, int[] sideMap) {
        remapDirectionMap(renderState.getConnectionTypes(), sideMap);
        remapDirectionSet(renderState.getCableBusAdjacent(), sideMap);
        remapDirectionMap(renderState.getChannelsOnSide(), sideMap);
    }
}