package net.oktawia.crazyae2addons.ldlib.accessors;

import appeng.api.stacks.GenericStack;
import appeng.util.ConfigInventory;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IMarkFunction;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ConfigInventoryAccessor
        implements IReadOnlyAccessor<ConfigInventory>, IMarkFunction<ConfigInventory, ListTag> {

    @Override
    public boolean test(Class<?> type) {
        return ConfigInventory.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull ConfigInventory value) {
        ListTag tag = value.writeToTag(extractProvider(op));

        if (isNbtOps(op)) {
            T out = (T) tag;
            return out;
        }

        return NbtOps.INSTANCE.convertTo(op, tag);
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, ConfigInventory value, T payload) {
        Tag tag = isNbtOps(op)
                ? (Tag) payload
                : op.convertTo(NbtOps.INSTANCE, payload);

        value.beginBatch();
        try {
            if (tag instanceof ListTag listTag) {
                value.readFromTag(listTag, extractProvider(op));
            } else {
                value.clear();
            }
        } finally {
            value.endBatch();
        }
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull ConfigInventory value) {
        int size = value.size();
        buffer.writeVarInt(size);

        for (int i = 0; i < size; i++) {
            GenericStack.STREAM_CODEC.encode(buffer, value.getStack(i));
        }
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull ConfigInventory value) {
        int incomingSize = buffer.readVarInt();
        int localSize = value.size();
        int common = Math.min(incomingSize, localSize);

        value.beginBatch();
        try {
            for (int i = 0; i < common; i++) {
                value.setStack(i, GenericStack.STREAM_CODEC.decode(buffer));
            }

            for (int i = common; i < incomingSize; i++) {
                GenericStack.STREAM_CODEC.decode(buffer);
            }

            for (int i = common; i < localSize; i++) {
                value.setStack(i, null);
            }
        } finally {
            value.endBatch();
        }
    }

    @Override
    public @NotNull ListTag obtainManagedMark(@NotNull ConfigInventory value) {
        return value.writeToTag(Platform.getFrozenRegistry());
    }

    @Override
    public boolean areDifferent(@NotNull ListTag managedMark, @NotNull ConfigInventory value) {
        return !Objects.equals(managedMark, value.writeToTag(Platform.getFrozenRegistry()));
    }

    private static HolderLookup.Provider extractProvider(DynamicOps<?> op) {
        if (op instanceof RegistryOps<?> registryOps) {
            return CommonHooks.extractLookupProvider(registryOps);
        }
        return Platform.getFrozenRegistry();
    }

    private static boolean isNbtOps(DynamicOps<?> op) {
        return op == NbtOps.INSTANCE
                || op instanceof DelegatingOpsAccessor<?> accessor
                && accessor.getDelegate() == NbtOps.INSTANCE;
    }
}