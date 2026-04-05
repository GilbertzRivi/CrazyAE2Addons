package net.oktawia.crazyae2addons.mixins;

import java.util.UUID;

import net.oktawia.crazyae2addons.interfaces.StorageLevelEmitterUuid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import appeng.api.parts.IPartItem;
import appeng.parts.automation.StorageLevelEmitterPart;
import net.minecraft.nbt.CompoundTag;

@Mixin(value = StorageLevelEmitterPart.class, remap = false)
public abstract class StorageLevelEmitterPartMixin implements StorageLevelEmitterUuid {

    @Unique
    private static final String UUID_TAG = "crazy_addons_emitter_uuid";

    @Unique
    private UUID persistentUuid;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void init(IPartItem<?> partItem, CallbackInfo ci) {
        ensureUuid();
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void afterReadFromNBT(CompoundTag data, CallbackInfo ci) {
        if (data.hasUUID(UUID_TAG)) {
            this.persistentUuid = data.getUUID(UUID_TAG);
        } else {
            ensureUuid();
        }
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void afterWriteToNBT(CompoundTag data, CallbackInfo ci) {
        data.putUUID(UUID_TAG, ensureUuid());
    }

    @Unique
    private UUID ensureUuid() {
        if (this.persistentUuid == null) {
            this.persistentUuid = UUID.randomUUID();
        }
        return this.persistentUuid;
    }

    @Override
    public UUID getPersistentUuid() {
        return ensureUuid();
    }
}