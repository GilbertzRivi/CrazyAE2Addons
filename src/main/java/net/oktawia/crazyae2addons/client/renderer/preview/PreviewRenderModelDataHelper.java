package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.client.model.data.ModelData;
import net.oktawia.crazyae2addons.CrazyAddons;

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
                baseData = model.getModelData(
                        localLevel,
                        previewBlock.pos(),
                        previewBlock.state(),
                        ModelData.EMPTY
                );
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
                baseData = ModelData.EMPTY;
            }

            BlockEntity blockEntity = structure.blockEntities(level).get(previewBlock.pos());
            if (blockEntity == null) {
                return baseData;
            }

            try {
                ModelData modelData = blockEntity.getModelData();
                return modelData != null ? modelData : baseData;
            } catch (Throwable t) {
                CrazyAddons.LOGGER.debug(t.getLocalizedMessage());
                return baseData;
            }
        });
    }
}