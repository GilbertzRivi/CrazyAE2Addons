package net.oktawia.crazyae2addons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.screens.BuilderPatternScreen;
import net.oktawia.crazyae2addons.screens.CrazyPatternProviderScreen;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

public class SendLongStringToClientPacket {
    private final String data;

    public SendLongStringToClientPacket(String data) {
        this.data = data;
    }

    public static void encode(SendLongStringToClientPacket packet, FriendlyByteBuf buf) {
        byte[] bytes = packet.data.getBytes(StandardCharsets.UTF_8);
        buf.writeInt(bytes.length);
        buf.writeByteArray(bytes);
    }

    public static SendLongStringToClientPacket decode(FriendlyByteBuf buf) {
        int length = buf.readInt();
        byte[] bytes = buf.readByteArray(length);
        return new SendLongStringToClientPacket(new String(bytes, StandardCharsets.UTF_8));
    }

    public static void handle(SendLongStringToClientPacket packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        ctxSupplier.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.screen instanceof BuilderPatternScreen<?> screen) {
                screen.setProgram(packet.data);
            }
        });
        ctxSupplier.get().setPacketHandled(true);
    }
}
