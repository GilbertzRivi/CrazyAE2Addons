package net.oktawia.crazyae2addons.client.renderer.preview;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PreviewBlockEntityFactory {

    private PreviewBlockEntityFactory() {
    }

    public static @Nullable BlockEntity create(
            ClientLevel level,
            BlockPos pos,
            BlockState state,
            @Nullable CompoundTag tag
    ) {
        if (tag == null) {
            return null;
        }

        CompoundTag copy = tag.copy();
        copy.putInt("x", pos.getX());
        copy.putInt("y", pos.getY());
        copy.putInt("z", pos.getZ());

        try {
            BlockEntity blockEntity = BlockEntity.loadStatic(pos, state, copy);
            if (blockEntity != null) {
                blockEntity.setLevel(level);
            }
            return blockEntity;
        } catch (Exception ignored) {
            return null;
        }
    }
}