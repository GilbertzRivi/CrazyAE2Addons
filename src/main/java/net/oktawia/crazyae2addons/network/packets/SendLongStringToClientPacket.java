package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SendLongStringToClientPacket {
    private final String data;

    public SendLongStringToClientPacket(String data) {
        this.data = data;
    }

    public static void encode(SendLongStringToClientPacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.data.getBytes(StandardCharsets.UTF_8));
    }

    public static SendLongStringToClientPacket decode(FriendlyByteBuf buf) {
        return new SendLongStringToClientPacket(
                new String(buf.readByteArray(), StandardCharsets.UTF_8)
        );
    }

    public static void handle(SendLongStringToClientPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.setPacketHandled(true);
    }
}