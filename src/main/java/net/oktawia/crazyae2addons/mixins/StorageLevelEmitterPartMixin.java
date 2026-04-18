package net.oktawia.crazyae2addons.mixins;

import java.util.UUID;

import appeng.api.parts.IPartItem;
import appeng.parts.automation.StorageLevelEmitterPart;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.logic.interfaces.IStorageLevelEmitterUuid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageLevelEmitterPart.class, remap = false)
public abstract class StorageLevelEmitterPartMixin implements IStorageLevelEmitterUuid {

    @Unique
    private static final String UUID_TAG = "crazy_addons_emitter_uuid";

    @Unique
    private UUID crazyae2addons$persistentUuid;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void crazyae2addons$init(IPartItem<?> partItem, CallbackInfo ci) {
        crazyae2addons$ensureUuid();
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void crazyae2addons$afterReadFromNBT(
            CompoundTag data,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        if (data.hasUUID(UUID_TAG)) {
            this.crazyae2addons$persistentUuid = data.getUUID(UUID_TAG);
        } else {
            crazyae2addons$ensureUuid();
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void crazyae2addons$afterWriteToNBT(
            CompoundTag data,
            HolderLookup.Provider registries,
            CallbackInfo ci
    ) {
        data.putUUID(UUID_TAG, crazyae2addons$ensureUuid());
    }

    @Unique
    private UUID crazyae2addons$ensureUuid() {
        if (this.crazyae2addons$persistentUuid == null) {
            this.crazyae2addons$persistentUuid = UUID.randomUUID();
        }
        return this.crazyae2addons$persistentUuid;
    }

    @Override
    public UUID getPersistentUuid() {
        return crazyae2addons$ensureUuid();
    }
}