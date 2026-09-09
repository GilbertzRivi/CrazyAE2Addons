package net.oktawia.crazyae2addons.network.packets;

import java.util.function.Supplier;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import net.oktawia.crazyae2addons.client.misc.DisplayImageClientCache;
import net.oktawia.crazyae2addons.logic.display.DisplayImageTransfer;

public record DisplayImageStreamPacket(int signal, String imageId, byte[] data) {

    public static DisplayImageStreamPacket begin(String imageId) {
        return new DisplayImageStreamPacket(DisplayImageTransfer.SIGNAL_BEGIN, imageId, new byte[0]);
    }

    public static DisplayImageStreamPacket data(String imageId, byte[] data) {
        return new DisplayImageStreamPacket(DisplayImageTransfer.SIGNAL_DATA, imageId, data);
    }

    public static DisplayImageStreamPacket end(String imageId) {
        return new DisplayImageStreamPacket(DisplayImageTransfer.SIGNAL_END, imageId, new byte[0]);
    }

    public static void encode(DisplayImageStreamPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.signal);
        buf.writeUtf(pkt.imageId == null ? "" : pkt.imageId, DisplayImageTransfer.MAX_ID_LEN);
        buf.writeByteArray(pkt.data == null ? new byte[0] : pkt.data);
    }

    public static DisplayImageStreamPacket decode(FriendlyByteBuf buf) {
        return new DisplayImageStreamPacket(
                buf.readVarInt(),
                buf.readUtf(DisplayImageTransfer.MAX_ID_LEN),
                buf.readByteArray(DisplayImageTransfer.CHUNK_BYTES));
    }

    public static void handle(DisplayImageStreamPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.handle(pkt)));

        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        private static void handle(DisplayImageStreamPacket pkt) {
            DisplayImageClientCache.accept(pkt.signal(), pkt.imageId(), pkt.data());
        }
    }
}
