package net.oktawia.crazyae2addons.network.packets;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import net.oktawia.crazyae2addons.logic.display.DisplayImageStore;
import net.oktawia.crazyae2addons.logic.display.DisplayImageTransfer;
import net.oktawia.crazyae2addons.network.transfer.DisplayImageStreamSender;

public record RequestDisplayImagePacket(String imageId) {

    public static void encode(RequestDisplayImagePacket pkt, FriendlyByteBuf buf) {
        buf.writeUtf(pkt.imageId == null ? "" : pkt.imageId, DisplayImageTransfer.MAX_ID_LEN);
    }

    public static RequestDisplayImagePacket decode(FriendlyByteBuf buf) {
        return new RequestDisplayImagePacket(buf.readUtf(DisplayImageTransfer.MAX_ID_LEN));
    }

    public static void handle(RequestDisplayImagePacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || pkt.imageId() == null || pkt.imageId().isBlank()) {
                return;
            }

            byte[] bytes = DisplayImageStore.get(player.serverLevel()).getImage(pkt.imageId());
            if (bytes == null || bytes.length == 0) {
                return;
            }

            DisplayImageStreamSender.sendImage(player, pkt.imageId(), bytes);
        });

        ctx.setPacketHandled(true);
    }
}
