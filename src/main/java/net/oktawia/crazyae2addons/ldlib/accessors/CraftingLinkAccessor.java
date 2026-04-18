package net.oktawia.crazyae2addons.ldlib.accessors;

import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.storage.StorageHelper;
import com.lowdragmc.lowdraglib2.Platform;
import com.lowdragmc.lowdraglib2.core.mixins.accessor.DelegatingOpsAccessor;
import com.lowdragmc.lowdraglib2.syncdata.accessor.IAccessor;
import com.lowdragmc.lowdraglib2.syncdata.field.ManagedKey;
import com.lowdragmc.lowdraglib2.syncdata.ref.IRef;
import com.lowdragmc.lowdraglib2.syncdata.ref.UniqueDirectRef;
import com.lowdragmc.lowdraglib2.syncdata.var.FieldVar;
import com.mojang.serialization.DynamicOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.neoforged.neoforge.common.CommonHooks;
import net.oktawia.crazyae2addons.CrazyAddons;

import javax.annotation.Nonnull;
import java.util.Objects;

public final class CraftingLinkAccessor implements IAccessor<ICraftingLink> {

    @Override
    public boolean test(Class<?> type) {
        return ICraftingLink.class.isAssignableFrom(type);
    }

    @Override
    public <T> T readField(DynamicOps<T> op, IRef<ICraftingLink> ref) {
        CompoundTag tag = serialize(ref.readRaw());

        if (isNbtOps(op)) {
            T out = (T) tag;
            return out;
        }

        return NbtOps.INSTANCE.convertTo(op, tag);
    }

    @Override
    public <T> void writeField(DynamicOps<T> op, IRef<ICraftingLink> ref, T payload) {
        Tag tag = isNbtOps(op)
                ? (Tag) payload
                : op.convertTo(NbtOps.INSTANCE, payload);

        ICraftingLink value = deserialize(ref, tag instanceof CompoundTag c ? c : new CompoundTag());
        ref.writeRaw(value);
    }

    @Override
    public void readFieldToStream(RegistryFriendlyByteBuf buffer, IRef<ICraftingLink> ref) {
        buffer.writeNbt(serialize(ref.readRaw()));
    }

    @Override
    public void writeFieldFromStream(RegistryFriendlyByteBuf buffer, IRef<ICraftingLink> ref) {
        CompoundTag tag = buffer.readNbt();
        ref.writeRaw(deserialize(ref, tag == null ? new CompoundTag() : tag));
    }

    @Override
    public IRef<ICraftingLink> createRef(ManagedKey managedKey, @Nonnull Object holder) {
        return new CraftingLinkRef(managedKey, holder, this);
    }

    @Override
    public boolean isReadOnly() {
        return false;
    }

    private static CompoundTag serialize(ICraftingLink link) {
        CompoundTag tag = new CompoundTag();
        if (link != null && !link.isCanceled() && !link.isDone()) {
            link.writeToNBT(tag);
        }
        return tag;
    }

    private static ICraftingLink deserialize(IRef<ICraftingLink> ref, CompoundTag tag) {
        if (tag.isEmpty()) {
            return null;
        }

        Object holder = ((CraftingLinkRef) ref).holder;
        if (!(holder instanceof ICraftingRequester requester)) {
            throw new IllegalStateException(
                    "CraftingLinkAccessor requires the field holder to implement ICraftingRequester: " + holder.getClass().getName()
            );
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

    private static boolean isNbtOps(DynamicOps<?> op) {
        return op == NbtOps.INSTANCE
                || op instanceof DelegatingOpsAccessor<?> accessor
                && accessor.getDelegate() == NbtOps.INSTANCE;
    }

    private static final class CraftingLinkRef extends UniqueDirectRef<ICraftingLink> {
        private final Object holder;

        private CraftingLinkRef(ManagedKey key, Object holder, IAccessor<ICraftingLink> accessor) {
            super(FieldVar.of(key, holder), key, accessor);
            this.holder = holder;
        }

        @Override
        protected void updateSync() {
            ICraftingLink oldLink = oldValue;
            ICraftingLink newLink = readRaw();

            if (!sameLink(oldLink, newLink)) {
                oldValue = newLink;
                markAsDirty();
            }
        }

        private static boolean sameLink(ICraftingLink a, ICraftingLink b) {
            if (a == b) return true;
            if (a == null || b == null) return false;

            try {
                return Objects.equals(a.getCraftingID(), b.getCraftingID())
                        && a.isDone() == b.isDone()
                        && a.isCanceled() == b.isCanceled();
            } catch (Throwable e) {
                CrazyAddons.LOGGER.debug("failed to compare crafting links", e);
                return false;
            }
        }
    }
}