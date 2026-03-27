package net.oktawia.crazyae2addons.mixins;

import appeng.util.BlockApiCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = BlockApiCache.class, remap = false)
public interface BlockApiCacheAccessor {
    @Accessor("level")
    ServerLevel getLevel();
    @Accessor("fromPos")
    BlockPos getFromPos();
}
