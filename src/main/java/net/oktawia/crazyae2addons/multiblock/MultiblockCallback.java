package net.oktawia.crazyae2addons.multiblock;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public interface MultiblockCallback {
    void setController(@Nullable BlockEntity controller);
    void setState(@Nullable MultiblockState state);
    void unregister(MultiblockState state);
}