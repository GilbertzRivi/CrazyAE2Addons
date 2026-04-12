package net.oktawia.crazyae2addons.ldlib.accessors;

import appeng.util.inv.AppEngInternalInventory;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.mojang.serialization.DynamicOps;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.NotNull;

public final class InventoryAccessor implements IReadOnlyAccessor<AppEngInternalInventory> {

    @Override
    public boolean test(Class<?> type) {
        return AppEngInternalInventory.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull AppEngInternalInventory value) {
        CompoundTag root = new CompoundTag();
        value.writeToNBT(root, "inv", net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.of()));
        Tag payload = root.contains("inv") ? root.get("inv") : new net.minecraft.nbt.ListTag();

        if (op == NbtOps.INSTANCE
            || op instanceof DelegatingOpsAccessor<?> a && a.getDelegate() == NbtOps.INSTANCE) {
            T out = (T) payload;
            return out;
        }

        return NbtOps.INSTANCE.convertTo(op, payload);
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, AppEngInternalInventory value, T payload) {
        Tag nbt = (op == NbtOps.INSTANCE
            || op instanceof DelegatingOpsAccessor<?> a && a.getDelegate() == NbtOps.INSTANCE)
            ? (Tag) payload
            : op.convertTo(NbtOps.INSTANCE, payload);

        CompoundTag root = new CompoundTag();
        root.put("inv", nbt);

        for (int i = 0; i < value.size(); i++) {
            value.setItemDirect(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        value.readFromNBT(root, "inv", net.minecraft.core.HolderLookup.Provider.create(java.util.stream.Stream.of()));
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull AppEngInternalInventory value) {
        CompoundTag root = new CompoundTag();
        value.writeToNBT(root, "inv", buffer.registryAccess());
        buffer.writeNbt(root);
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull AppEngInternalInventory value) {
        CompoundTag root = buffer.readNbt();
        if (root == null) return;

        for (int i = 0; i < value.size(); i++) {
            value.setItemDirect(i, net.minecraft.world.item.ItemStack.EMPTY);
        }
        value.readFromNBT(root, "inv", buffer.registryAccess());
    }
}