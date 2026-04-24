package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.IGridNodeListener;
import appeng.parts.automation.AbstractLevelEmitterPart;
import net.oktawia.crazyae2addons.logic.interfaces.StorageLevelEmitterUuid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AbstractLevelEmitterPart.class, remap = false)
public abstract class MixinAbstractLevelEmitterPart {

    @Inject(method = "onMainNodeStateChanged", at = @At("TAIL"))
    private void crazyAE2Addons$validateUuidAfterNodeStateChanged(IGridNodeListener.State reason, CallbackInfo ci) {
        if ((Object) this instanceof StorageLevelEmitterUuid uuidHolder) {
            uuidHolder.validatePersistentUuidIfPossible();
        }
    }
}