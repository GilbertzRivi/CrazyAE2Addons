package net.oktawia.crazyae2addons.util;

import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedAccessor;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IAsyncAutoSyncBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.blockentity.IRPCBlockEntity;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface IManagedBEHelper extends IManaged, IAsyncAutoSyncBlockEntity, IRPCBlockEntity {
    String MANAGED_TAG = "managed";

    FieldManagedStorage getSyncStorage();

    @Override
    default IManagedStorage getRootStorage() {
        return getSyncStorage();
    }

    @Override
    default void onChanged() {
        ((BlockEntity) this).setChanged();
    }

    default void saveManagedData(CompoundTag tag) {
        tag.put(MANAGED_TAG, IManagedAccessor.readManagedFields(this, new CompoundTag()));
    }

    default void loadManagedData(CompoundTag tag) {
        if (tag.contains(MANAGED_TAG, Tag.TAG_COMPOUND)) {
            IManagedAccessor.writePersistedFields(
                    tag.getCompound(MANAGED_TAG),
                    getSyncStorage().getPersistedFields()
            );
        }
    }

    default void markManagedDirty(String fieldName) {
        getSyncStorage()
                .getFieldByKey(getFieldHolder().getSyncedFieldIndex(fieldName))
                .markAsDirty();
    }

    default void syncManaged() {
        if (getSelf().getLevel() != null && !getSelf().getLevel().isClientSide()) {
            syncNow(false);
        }
    }

    default void forceSyncManaged() {
        syncNow(true);
    }
}