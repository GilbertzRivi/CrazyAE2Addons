package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.IGrid;
import appeng.api.parts.IPartItem;
import appeng.parts.automation.StorageLevelEmitterPart;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.logic.interfaces.StorageLevelEmitterUuid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.UUID;

@Mixin(value = StorageLevelEmitterPart.class, remap = false)
public abstract class MixinStorageLevelEmitterPart implements StorageLevelEmitterUuid {

    @Unique
    private static final String UUID_TAG = "crazy_addons_emitter_uuid";

    @Unique
    private UUID persistentUuid;

    @Unique
    private boolean uuidNeedsValidation = true;

    @Unique
    private boolean uuidValidationInProgress = false;

    @Inject(method = "<init>", at = @At("TAIL"))
    private void crazyAE2Addons$init(IPartItem<?> partItem, CallbackInfo ci) {
        ensureUuid();
        this.uuidNeedsValidation = true;
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void crazyAE2Addons$afterReadFromNBT(CompoundTag data, CallbackInfo ci) {
        if (data.hasUUID(UUID_TAG)) {
            this.persistentUuid = data.getUUID(UUID_TAG);
        } else {
            this.persistentUuid = UUID.randomUUID();
        }

        this.uuidNeedsValidation = true;
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void crazyAE2Addons$afterWriteToNBT(CompoundTag data, CallbackInfo ci) {
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

    @Override
    public UUID getRawPersistentUuid() {
        return ensureUuid();
    }

    @Override
    public void validatePersistentUuidIfPossible() {
        UUID current = ensureUuid();

        if (!this.uuidNeedsValidation || this.uuidValidationInProgress) {
            return;
        }

        StorageLevelEmitterPart self = (StorageLevelEmitterPart) (Object) this;
        if (self.getMainNode() == null || !self.getMainNode().hasGridBooted()) {
            return;
        }

        IGrid grid = self.getMainNode().getGrid();
        if (grid == null) {
            return;
        }

        this.uuidValidationInProgress = true;
        boolean changed = false;

        try {
            var emitters = grid.getMachines(StorageLevelEmitterPart.class);
            if (emitters == null || emitters.isEmpty()) {
                this.uuidNeedsValidation = false;
                return;
            }

            int safety = 0;
            while (safety++ < 128) {
                boolean duplicateFound = false;

                for (var emitter : emitters) {
                    if (emitter == self) {
                        continue;
                    }

                    if (!(emitter instanceof StorageLevelEmitterUuid other)) {
                        continue;
                    }

                    UUID otherUuid = other.getRawPersistentUuid();
                    if (current.equals(otherUuid)) {
                        this.persistentUuid = UUID.randomUUID();
                        current = this.persistentUuid;
                        duplicateFound = true;
                        changed = true;
                        break;
                    }
                }

                if (!duplicateFound) {
                    break;
                }
            }

            this.uuidNeedsValidation = false;

            if (changed && self.getHost() != null) {
                self.getHost().markForSave();
                self.getHost().markForUpdate();
            }
        } finally {
            this.uuidValidationInProgress = false;
        }
    }
}