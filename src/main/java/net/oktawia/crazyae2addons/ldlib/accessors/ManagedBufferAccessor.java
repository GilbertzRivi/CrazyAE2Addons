package net.oktawia.crazyae2addons.ldlib.accessors;

import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;

public final class ManagedBufferAccessor implements IAccessor {
    private byte defaultType = -1;

    @Override
    public boolean hasPredicate() {
        return true;
    }

    @Override
    public boolean test(Class<?> type) {
        return ManagedBuffer.class.isAssignableFrom(type);
    }

    @Override
    public boolean isManaged() {
        return false;
    }

    @Override
    public void setDefaultType(byte payloadType) {
        this.defaultType = payloadType;
    }

    @Override
    public byte getDefaultType() {
        return this.defaultType;
    }

    @Override
    public ITypedPayload<?> readField(AccessorOp op, IRef field) {
        ManagedBuffer value = field.readRaw();
        if (value == null) {
            return PrimitiveTypedPayload.ofNull();
        }
        return NbtTagPayload.of(value.toTag());
    }

    @Override
    public void writeField(AccessorOp op, IRef field, ITypedPayload<?> payload) {
        ManagedBuffer value = field.readRaw();
        if (value == null) {
            return;
        }

        if (payload.serializeNBT() instanceof CompoundTag tag) {
            value.fromTag(tag);
        } else {
            value.fromTag(new CompoundTag());
        }
    }
}