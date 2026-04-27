package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.client.misc.ClonerStructureFileTransferClient;

import java.util.function.Supplier;

public class ExportClonerStructureResultPacket {

    private static final int MAX_EXPORT_BYTES = 16 * 1024 * 1024;

    private final String id;
    private final byte[] bytes;

    public ExportClonerStructureResultPacket(String id, byte[] bytes) {
        this.id = id == null ? "" : id;
        this.bytes = bytes == null ? new byte[0] : bytes;
    }

    public static void encode(ExportClonerStructureResultPacket packet, FriendlyByteBuf buffer) {
        buffer.writeUtf(packet.id, 32767);
        buffer.writeByteArray(packet.bytes);
    }

    public static ExportClonerStructureResultPacket decode(FriendlyByteBuf buffer) {
        return new ExportClonerStructureResultPacket(
                buffer.readUtf(32767),
                buffer.readByteArray(MAX_EXPORT_BYTES)
        );
    }

    public static void handle(ExportClonerStructureResultPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> ClonerStructureFileTransferClient.completeExport(packet.id, packet.bytes));

        context.setPacketHandled(true);
    }
}