package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternMenu;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SendLongStringToServerPacket {
    private final String data;

    public SendLongStringToServerPacket(String data) {
        this.data = data;
    }

    public static void encode(SendLongStringToServerPacket packet, FriendlyByteBuf buf) {
        buf.writeByteArray(packet.data.getBytes(StandardCharsets.UTF_8));
    }

    public static SendLongStringToServerPacket decode(FriendlyByteBuf buf) {
        return new SendLongStringToServerPacket(
                new String(buf.readByteArray(), StandardCharsets.UTF_8)
        );
    }

    public static void handle(SendLongStringToServerPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null && sender.containerMenu instanceof BuilderPatternMenu menu) {
                menu.updateData(packet.data);
            }
        });
        ctx.setPacketHandled(true);
    }
}