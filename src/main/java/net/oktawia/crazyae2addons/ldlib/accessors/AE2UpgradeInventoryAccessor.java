package net.oktawia.crazyae2addons.ldlib.accessors;

import appeng.api.upgrades.IUpgradeInventory;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.readonly.IReadOnlyAccessor;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.CommonHooks;
import org.jetbrains.annotations.NotNull;

public final class AE2UpgradeInventoryAccessor implements IReadOnlyAccessor<IUpgradeInventory> {
    private static final String SUBTAG = "inv";

    @Override
    public boolean test(Class<?> type) {
        return IUpgradeInventory.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readReadOnlyValue(DynamicOps<T> op, @NotNull IUpgradeInventory value) {
        var root = new CompoundTag();
        value.writeToNBT(root, SUBTAG, extractProvider(op));

        if (isNbtOps(op)) {
            T out = (T) root;
            return out;
        }

        return NbtOps.INSTANCE.convertTo(op, root);
    }

    @Override
    public <T> void writeReadOnlyValue(DynamicOps<T> op, IUpgradeInventory value, T payload) {
        Tag tag = isNbtOps(op)
                ? (Tag) payload
                : op.convertTo(NbtOps.INSTANCE, payload);

        CompoundTag root;
        if (tag instanceof CompoundTag compound) {
            root = compound;
        } else {
            root = new CompoundTag();
            root.put(SUBTAG, tag);
        }

        clearInventory(value);
        value.readFromNBT(root, SUBTAG, extractProvider(op));
    }

    @Override
    public void readReadOnlyValueToStream(RegistryFriendlyByteBuf buffer, @NotNull IUpgradeInventory value) {
        var root = new CompoundTag();
        value.writeToNBT(root, SUBTAG, buffer.registryAccess());
        buffer.writeNbt(root);
    }

    @Override
    public void writeReadOnlyValueFromStream(RegistryFriendlyByteBuf buffer, @NotNull IUpgradeInventory value) {
        CompoundTag root = buffer.readNbt();
        clearInventory(value);

        if (root != null) {
            value.readFromNBT(root, SUBTAG, buffer.registryAccess());
        }
    }

    private static void clearInventory(IUpgradeInventory inv) {
        for (int i = 0; i < inv.size(); i++) {
            inv.setItemDirect(i, ItemStack.EMPTY);
        }
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