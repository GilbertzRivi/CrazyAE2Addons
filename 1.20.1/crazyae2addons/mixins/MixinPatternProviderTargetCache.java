package net.oktawia.crazyae2addons.mixins;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.helpers.patternprovider.PatternProviderTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderTargetCacheExt;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "appeng.helpers.patternprovider.PatternProviderTargetCache", priority = 890)
public abstract class MixinPatternProviderTargetCache implements IPatternProviderTargetCacheExt {

    @Unique private BlockPos pos = null;
    @Unique private Level lvl = null;
    @Unique private IPatternDetails details = null;

    @Inject(
            method = "<init>(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lappeng/api/networking/security/IActionSource;)V",
            at = @At("RETURN")
    )
    private void atCtorTail(ServerLevel l, BlockPos pos, Direction direction, IActionSource src, CallbackInfo ci){
        this.pos = pos;
        this.lvl = l;
    }

    @Unique
    public PatternProviderTarget find(IPatternDetails patternDetails) {
        this.details = patternDetails;
        return ((PatternProviderTargetCacheAccessor) this).callFind();
    }

    @Unique
    public void setDetails(IPatternDetails details){
        this.details = details;
    }
}