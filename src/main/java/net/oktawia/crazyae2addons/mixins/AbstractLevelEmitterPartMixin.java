package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.IGridNodeListener;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.parts.automation.StorageLevelEmitterPart;
import net.oktawia.crazyae2addons.interfaces.StorageLevelEmitterUuid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractLevelEmitterPart.class, remap = false)
public abstract class AbstractLevelEmitterPartMixin {

    @Inject(method = "onMainNodeStateChanged", at = @At("TAIL"))
    private void validateUuidAfterNodeStateChange(IGridNodeListener.State reason, CallbackInfo ci) {
        if ((Object) this instanceof StorageLevelEmitterPart
                && (Object) this instanceof StorageLevelEmitterUuid uuidHolder) {
            uuidHolder.getPersistentUuid();
        }
    }
}