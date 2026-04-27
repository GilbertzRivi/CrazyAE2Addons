package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryClientCache;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryStore;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class SyncClonerLibraryPacket {

    private final List<Entry> entries;
    private final String selectedId;

    public SyncClonerLibraryPacket(
            List<Entry> entries,
            String selectedId
    ) {
        this.entries = List.copyOf(entries);
        this.selectedId = selectedId == null ? "" : selectedId;
    }

    public record Entry(
            String id,
            String name,
            long created,
            long updated,
            int blockCount
    ) {
    }

    public static SyncClonerLibraryPacket fromStoreEntries(
            List<ClonerStructureLibraryStore.Entry> storeEntries,
            String selectedId
    ) {
        List<Entry> entries = new ArrayList<>();

        for (ClonerStructureLibraryStore.Entry entry : storeEntries) {
            entries.add(new Entry(
                    entry.id(),
                    entry.name(),
                    entry.created(),
                    entry.updated(),
                    entry.blockCount()
            ));
        }

        return new SyncClonerLibraryPacket(entries, selectedId);
    }

    public static void encode(SyncClonerLibraryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.selectedId, 32767);
        buffer.writeVarInt(packet.entries.size());

        for (Entry entry : packet.entries) {
            buffer.writeUtf(entry.id(), 32767);
            buffer.writeUtf(entry.name(), ClonerStructureLibraryStore.MAX_NAME_LENGTH);
            buffer.writeLong(entry.created());
            buffer.writeLong(entry.updated());
            buffer.writeVarInt(Math.max(0, entry.blockCount()));
        }
    }

    public static SyncClonerLibraryPacket decode(FriendlyByteBuf buffer) {
        String selectedId = buffer.readUtf(32767);
        int size = buffer.readVarInt();

        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < size; i++) {
            entries.add(new Entry(
                    buffer.readUtf(32767),
                    buffer.readUtf(ClonerStructureLibraryStore.MAX_NAME_LENGTH),
                    buffer.readLong(),
                    buffer.readLong(),
                    buffer.readVarInt()
            ));
        }

        return new SyncClonerLibraryPacket(entries, selectedId);
    }

    public static void handle(SyncClonerLibraryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            List<ClonerStructureLibraryClientCache.Entry> cached = new ArrayList<>();

            for (Entry entry : packet.entries) {
                cached.add(new ClonerStructureLibraryClientCache.Entry(
                        entry.id(),
                        entry.name(),
                        entry.created(),
                        entry.updated(),
                        entry.blockCount()
                ));
            }

            ClonerStructureLibraryClientCache.set(cached, packet.selectedId);
        });

        context.setPacketHandled(true);
    }
}