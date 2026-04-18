package net.oktawia.crazyae2addons.ldlib.accessors;

import appeng.util.inv.AppEngInternalInventory;
import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.stream.Stream;

public final class InventoryAccessor implements IAccessor {
    private static final String SUBTAG = "inv";

    private byte defaultType = -1;

    @Override
    public boolean hasPredicate() {
        return true;
    }

    @Override
    public boolean test(Class<?> type) {
        return AppEngInternalInventory.class.isAssignableFrom(type);
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
        AppEngInternalInventory value = field.readRaw();
        if (value == null) {
            return PrimitiveTypedPayload.ofNull();
        }

        CompoundTag root = new CompoundTag();
        value.writeToNBT(root, SUBTAG);
        return NbtTagPayload.of(root);
    }

    @Override
    public void writeField(AccessorOp op, IRef field, ITypedPayload<?> payload) {
        AppEngInternalInventory value = field.readRaw();
        if (value == null) {
            return;
        }

        Tag tag = payload.serializeNBT();
        CompoundTag root;

        if (tag instanceof CompoundTag compound) {
            root = compound;
        } else {
            root = new CompoundTag();
            root.put(SUBTAG, tag);
        }

        for (int i = 0; i < value.size(); i++) {
            value.setItemDirect(i, ItemStack.EMPTY);
        }
        value.readFromNBT(root, SUBTAG);
    }
}