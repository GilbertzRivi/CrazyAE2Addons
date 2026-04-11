package net.oktawia.crazyae2addons.ldlib.accessors;

import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.mojang.serialization.DynamicOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.oktawia.crazyae2addons.defs.components.AEItemBufferData;
import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ManagedBufferAccessor implements IReadOnlyAccessor<ManagedBuffer>, IMarkFunction<ManagedBuffer, AEItemBufferData> {

    @Override
    public boolean test(Class<?> type) {
        return ManagedBuffer.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull ManagedBuffer value) {
        return AEItemBufferData.CODEC.encodeStart(op, value.toData()).getOrThrow();
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, ManagedBuffer value, T payload) {
        var data = AEItemBufferData.CODEC.parse(op, payload).getOrThrow();
        value.fromData(data);
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull ManagedBuffer value) {
        AEItemBufferData.STREAM_CODEC.encode(buffer, value.toData());
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull ManagedBuffer value) {
        var data = AEItemBufferData.STREAM_CODEC.decode(buffer);
        value.fromData(data);
    }

    @Override
    public @NotNull AEItemBufferData obtainManagedMark(@NotNull ManagedBuffer value) {
        return value.toData();
    }

    @Override
    public boolean areDifferent(@NotNull AEItemBufferData managedMark, @NotNull ManagedBuffer value) {
        return !Objects.equals(managedMark, value.toData());
    }
}