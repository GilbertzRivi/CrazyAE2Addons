package net.oktawia.crazyae2addons.ldlib.accessors;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.storage.StorageHelper;
import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.IAccessor;
import com.lowdragmc.lowdraglib.syncdata.managed.IManagedVar;
import com.lowdragmc.lowdraglib.syncdata.managed.IRef;
import com.lowdragmc.lowdraglib.syncdata.managed.ManagedRef;
import com.lowdragmc.lowdraglib.syncdata.payload.ITypedPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import com.lowdragmc.lowdraglib.syncdata.payload.PrimitiveTypedPayload;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.CrazyAddons;

import java.lang.reflect.Field;

public final class CraftingLinkAccessor implements IAccessor {
    private byte defaultType = -1;

    @Override
    public boolean hasPredicate() {
        return true;
    }

    @Override
    public boolean test(Class<?> type) {
        return ICraftingLink.class.isAssignableFrom(type);
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
        ICraftingLink value = field.readRaw();
        if (value == null) {
            return PrimitiveTypedPayload.ofNull();
        }
        return NbtTagPayload.of(serialize(value));
    }

    @Override
    @SuppressWarnings("unchecked")
    public void writeField(AccessorOp op, IRef field, ITypedPayload<?> payload) {
        if (!(field instanceof ManagedRef managedRef)) {
            return;
        }

        CompoundTag tag = payload.serializeNBT() instanceof CompoundTag c ? c : new CompoundTag();
        ICraftingLink link = deserialize(field, tag);

        IManagedVar<ICraftingLink> managedVar = managedRef.getField();
        managedVar.set(link);
    }

    private static CompoundTag serialize(ICraftingLink link) {
        CompoundTag tag = new CompoundTag();
        if (link != null && !link.isCanceled() && !link.isDone()) {
            link.writeToNBT(tag);
        }
        return tag;
    }

    private static ICraftingLink deserialize(IRef ref, CompoundTag tag) {
        if (tag.isEmpty()) {
            return null;
        }

        Object holder = extractHolder(ref);
        if (!(holder instanceof ICraftingRequester requester)) {
            return null;
        }

        try {
            ICraftingLink link = StorageHelper.loadCraftingLink(tag, requester);
            if (link == null || link.isCanceled() || link.isDone()) {
                return null;
            }
            return link;
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to read crafting link from NBT", e);
            return null;
        }
    }

    private static Object extractHolder(IRef ref) {
        if (!(ref instanceof ManagedRef managedRef)) {
            return null;
        }

        Object fieldObj = managedRef.getField();
        Class<?> type = fieldObj.getClass();

        while (type != null) {
            try {
                Field instanceField = type.getDeclaredField("instance");
                instanceField.setAccessible(true);
                return instanceField.get(fieldObj);
            } catch (NoSuchFieldException ignored) {
                type = type.getSuperclass();
            } catch (Throwable e) {
                CrazyAddons.LOGGER.debug("failed to extract managed field holder", e);
                return null;
            }
        }

        return null;
    }
}