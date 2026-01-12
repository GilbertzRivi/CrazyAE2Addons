package net.oktawia.crazyae2addonslite.mixins;

import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.util.BlockApiCache;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "appeng.helpers.patternprovider.PatternProviderTargetCache")
public interface PatternProviderTargetCacheAccessor {
    @Invoker("find")
    PatternProviderTarget callFind();
    @Accessor("direction")
    Direction getDirection();
    @Accessor("cache")
    BlockApiCache<?> getCache();
}