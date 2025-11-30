package net.oktawia.crazyae2addons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.menus.BuilderPatternMenu;
import net.oktawia.crazyae2addons.menus.CopyGadgetMenu;
import net.oktawia.crazyae2addons.menus.GadgetMenu;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SendLongStringToServerPacket {
    private final String data;

    public SendLongStringToServerPacket(String data) {
        this.data = data;
    }

    public static void encode(SendLongStringToServerPacket packet, FriendlyByteBuf buf) {
        byte[] bytes = packet.data.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeByteArray(bytes);
    }

    public static SendLongStringToServerPacket decode(FriendlyByteBuf buf) {
        int length = buf.readInt();
        byte[] bytes = buf.readByteArray(length);
        return new SendLongStringToServerPacket(new String(bytes, StandardCharsets.UTF_8));
    }

    public static void handle(SendLongStringToServerPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer sender = ctx.getSender();
            if (sender != null && sender.containerMenu instanceof BuilderPatternMenu menu) {
                menu.updateData(packet.data);
            } else if (sender != null && sender.containerMenu instanceof GadgetMenu menu) {
                menu.updateData(packet.data);
            } else if (sender != null && sender.containerMenu instanceof CopyGadgetMenu menu) {
                menu.updateData(packet.data);
            }
        });
        ctx.setPacketHandled(true);
    }
}
