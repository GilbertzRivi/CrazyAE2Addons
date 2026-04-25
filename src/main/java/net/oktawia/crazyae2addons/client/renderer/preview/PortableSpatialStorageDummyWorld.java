package net.oktawia.crazyae2addons.client.renderer.preview;

import com.lowdragmc.lowdraglib.utils.TrackedDummyWorld;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class PortableSpatialStorageDummyWorld extends TrackedDummyWorld {

    private final Map<BlockPos, BlockEntity> previewBlockEntities = new HashMap<>();

    public void loadPreviewStructure(@Nullable PreviewStructure structure) {
        this.clear();
        this.previewBlockEntities.clear();

        if (structure == null) {
            return;
        }

        ClientLevel clientLevel = Minecraft.getInstance().level;
        if (clientLevel == null) {
            return;
        }

        for (PreviewBlock block : structure.blocks()) {
            this.setBlock(block.pos(), block.state(), 3);
        }

        Map<BlockPos, BlockEntity> builtBlockEntities = structure.blockEntities(clientLevel);
        for (Map.Entry<BlockPos, BlockEntity> entry : builtBlockEntities.entrySet()) {
            BlockEntity blockEntity = entry.getValue();

            try {
                blockEntity.setLevel(this);
            } catch (Throwable ignored) {
            }

            try {
                blockEntity.clearRemoved();
            } catch (Throwable ignored) {
            }

            try {
                blockEntity.onLoad();
            } catch (Throwable ignored) {
            }

            this.previewBlockEntities.put(entry.getKey(), blockEntity);
        }
    }

    @Override
    public @Nullable BlockEntity getBlockEntity(BlockPos pos) {
        BlockEntity preview = this.previewBlockEntities.get(pos);
        if (preview != null) {
            return preview;
        }
        return super.getBlockEntity(pos);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return super.getBlockState(pos);
    }
}