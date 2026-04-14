package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.client.screens.part.DisplayImagesSubScreen;
import net.oktawia.crazyae2addons.client.screens.part.DisplayScreen;

public record SyncDisplayImagePreviewPacket(
        String imageId,
        byte[] pngBytes
) implements CustomPacketPayload {

    private static final int MAX_ID_LEN = 128;

    public static final Type<SyncDisplayImagePreviewPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "sync_display_image_preview"));

    public static final StreamCodec<FriendlyByteBuf, SyncDisplayImagePreviewPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncDisplayImagePreviewPacket::write, SyncDisplayImagePreviewPacket::new);

    private SyncDisplayImagePreviewPacket(FriendlyByteBuf buf) {
        this(
                buf.readUtf(MAX_ID_LEN),
                buf.readByteArray(UploadDisplayImagePacket.MAX_IMAGE_BYTES)
        );
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeUtf(imageId == null ? "" : imageId, MAX_ID_LEN);
        buf.writeByteArray(pngBytes == null ? new byte[0] : pngBytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SyncDisplayImagePreviewPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> Client.handle(pkt));
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        private static void handle(SyncDisplayImagePreviewPacket pkt) {
            if (Minecraft.getInstance().screen instanceof DisplayImagesSubScreen screen) {
                screen.applyPreviewFromServer(pkt.imageId(), pkt.pngBytes());
            }

            if (Minecraft.getInstance().screen instanceof DisplayScreen<?> screen) {
                screen.applyPreviewImageFromServer(pkt.imageId(), pkt.pngBytes());
            }
        }
    }
}