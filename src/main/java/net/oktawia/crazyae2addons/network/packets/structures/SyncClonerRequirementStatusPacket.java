package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.client.misc.ClonerMaterialListWidget;
import net.oktawia.crazyae2addons.client.misc.PortableSpatialClonerRequirementSync;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record SyncClonerRequirementStatusPacket(int containerId, List<Entry> entries) {

    public record Entry(ItemStack stack, long available, long required, boolean craftable) {
    }

    public SyncClonerRequirementStatusPacket(int containerId, List<Entry> entries) {
        this.containerId = containerId;
        this.entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public static void encode(SyncClonerRequirementStatusPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.containerId);
        buf.writeVarInt(packet.entries.size());

        for (Entry entry : packet.entries) {
            buf.writeItem(entry.stack());
            buf.writeLong(entry.available());
            buf.writeLong(entry.required());
            buf.writeBoolean(entry.craftable());
        }
    }

    public static SyncClonerRequirementStatusPacket decode(FriendlyByteBuf buf) {
        int containerId = buf.readVarInt();
        int size = buf.readVarInt();

        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            ItemStack stack = buf.readItem();
            long available = buf.readLong();
            long required = buf.readLong();
            boolean craftable = buf.readBoolean();

            entries.add(new Entry(stack, available, required, craftable));
        }

        return new SyncClonerRequirementStatusPacket(containerId, entries);
    }

    public static void handle(SyncClonerRequirementStatusPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            List<ClonerMaterialListWidget.MaterialEntry> syncedEntries = packet.entries.stream()
                    .map(entry -> new ClonerMaterialListWidget.MaterialEntry(
                            entry.stack().copy(),
                            entry.available(),
                            entry.required(),
                            entry.craftable()
                    ))
                    .toList();

            PortableSpatialClonerRequirementSync.setEntries(packet.containerId, syncedEntries);
        });
        context.setPacketHandled(true);
    }
}