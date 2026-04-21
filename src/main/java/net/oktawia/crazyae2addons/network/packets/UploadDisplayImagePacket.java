package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.menus.part.DisplayImagesSubMenu;

import java.util.function.Supplier;

public record UploadDisplayImagePacket(
        String sourceName,
        byte[] pngBytes,
        int width,
        int height
) {

    public static final int MAX_NAME_LEN = 256;
    public static final int MAX_IMAGE_BYTES = 1024 * 1024;
    public static final int MAX_IMAGE_DIM = 512;

    public static void encode(UploadDisplayImagePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.sourceName, MAX_NAME_LEN);
        buf.writeByteArray(pkt.pngBytes);
        buf.writeVarInt(pkt.width);
        buf.writeVarInt(pkt.height);
    }

    public static UploadDisplayImagePacket decode(FriendlyByteBuf buf) {
        return new UploadDisplayImagePacket(
                buf.readUtf(MAX_NAME_LEN),
                buf.readByteArray(MAX_IMAGE_BYTES),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    public static void handle(UploadDisplayImagePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player == null) {
                return;
            }

            if (!(player.containerMenu instanceof DisplayImagesSubMenu menu)) {
                return;
            }

            if (pkt.sourceName() == null || pkt.sourceName().isBlank()) {
                return;
            }

            if (pkt.pngBytes() == null || pkt.pngBytes().length == 0 || pkt.pngBytes().length > MAX_IMAGE_BYTES) {
                return;
            }

            if (pkt.width() <= 0 || pkt.height() <= 0) {
                return;
            }

            if (pkt.width() > MAX_IMAGE_DIM || pkt.height() > MAX_IMAGE_DIM) {
                return;
            }

            menu.addImage(pkt.sourceName(), pkt.pngBytes(), pkt.width(), pkt.height());
        });

        ctx.setPacketHandled(true);
    }
}