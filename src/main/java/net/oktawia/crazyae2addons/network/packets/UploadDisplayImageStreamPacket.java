package net.oktawia.crazyae2addons.network.packets;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import net.oktawia.crazyae2addons.logic.display.DisplayImageTransfer;
import net.oktawia.crazyae2addons.network.transfer.DisplayImageUploadStream;

public record UploadDisplayImageStreamPacket(
        int signal,
        String sourceName,
        int frameCount,
        int columns,
        int frameDurationMs,
        byte[] data) {

    public static UploadDisplayImageStreamPacket begin(
            String sourceName,
            int frameCount,
            int columns,
            int frameDurationMs) {
        return new UploadDisplayImageStreamPacket(
                DisplayImageTransfer.SIGNAL_BEGIN,
                sourceName,
                frameCount,
                columns,
                frameDurationMs,
                new byte[0]);
    }

    public static UploadDisplayImageStreamPacket data(byte[] data) {
        return new UploadDisplayImageStreamPacket(DisplayImageTransfer.SIGNAL_DATA, "", 1, 1, 0, data);
    }

    public static UploadDisplayImageStreamPacket end() {
        return new UploadDisplayImageStreamPacket(DisplayImageTransfer.SIGNAL_END, "", 1, 1, 0, new byte[0]);
    }

    public static void encode(UploadDisplayImageStreamPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.signal);
        buf.writeUtf(pkt.sourceName == null ? "" : pkt.sourceName, DisplayImageTransfer.MAX_NAME_LEN);
        buf.writeVarInt(pkt.frameCount);
        buf.writeVarInt(pkt.columns);
        buf.writeVarInt(pkt.frameDurationMs);
        buf.writeByteArray(pkt.data == null ? new byte[0] : pkt.data);
    }

    public static UploadDisplayImageStreamPacket decode(FriendlyByteBuf buf) {
        return new UploadDisplayImageStreamPacket(
                buf.readVarInt(),
                buf.readUtf(DisplayImageTransfer.MAX_NAME_LEN),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readVarInt(),
                buf.readByteArray(DisplayImageTransfer.CHUNK_BYTES));
    }

    public static void handle(UploadDisplayImageStreamPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            DisplayImageUploadStream.accept(player, pkt);
        });

        ctx.setPacketHandled(true);
    }
}
