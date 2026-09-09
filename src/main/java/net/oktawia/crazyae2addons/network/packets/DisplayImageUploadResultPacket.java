package net.oktawia.crazyae2addons.network.packets;

import java.util.function.Supplier;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import net.oktawia.crazyae2addons.client.screens.part.DisplayImagesSubScreen;
import net.oktawia.crazyae2addons.logic.display.DisplayImageUploadStatus;

public record DisplayImageUploadResultPacket(int status, int detailKilobytes) {

    public static DisplayImageUploadResultPacket of(DisplayImageUploadStatus status, int detailKilobytes) {
        return new DisplayImageUploadResultPacket(status.ordinal(), detailKilobytes);
    }

    public static void encode(DisplayImageUploadResultPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.status);
        buf.writeVarInt(Math.max(0, pkt.detailKilobytes));
    }

    public static DisplayImageUploadResultPacket decode(FriendlyByteBuf buf) {
        return new DisplayImageUploadResultPacket(buf.readVarInt(), buf.readVarInt());
    }

    public static void handle(DisplayImageUploadResultPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.handle(pkt)));

        ctx.setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static final class Client {
        private static void handle(DisplayImageUploadResultPacket pkt) {
            if (Minecraft.getInstance().screen instanceof DisplayImagesSubScreen screen) {
                screen.applyUploadResult(DisplayImageUploadStatus.byId(pkt.status()), pkt.detailKilobytes());
            }
        }
    }
}
