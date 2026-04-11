package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.menus.DisplayImagesSubMenu;

public record UploadDisplayImagePacket(
        String sourceName,
        byte[] pngBytes,
        int width,
        int height
) implements CustomPacketPayload {

    public static final int MAX_NAME_LEN = 256;
    public static final int MAX_IMAGE_BYTES = 1024 * 1024;
    public static final int MAX_IMAGE_DIM = 512;

    public static final Type<UploadDisplayImagePacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "upload_display_image"));

    public static final StreamCodec<FriendlyByteBuf, UploadDisplayImagePacket> STREAM_CODEC =
            StreamCodec.ofMember(UploadDisplayImagePacket::write, UploadDisplayImagePacket::new);

    private UploadDisplayImagePacket(FriendlyByteBuf buf) {
        this(
                buf.readUtf(MAX_NAME_LEN),
                buf.readByteArray(MAX_IMAGE_BYTES),
                buf.readVarInt(),
                buf.readVarInt()
        );
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(sourceName, MAX_NAME_LEN);
        buf.writeByteArray(pngBytes);
        buf.writeVarInt(width);
        buf.writeVarInt(height);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UploadDisplayImagePacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
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
    }
}