package net.oktawia.crazyae2addonslite.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.network.NetworkEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class ClipboardPacket {
    private String data;
    private static StringBuilder packetAccumulator = new StringBuilder();

    public ClipboardPacket(String data) {
        this.data = data;
    }

    public static void encode(ClipboardPacket packet, FriendlyByteBuf buf) {
        buf.writeUtf(packet.data);
    }

    public static ClipboardPacket decode(FriendlyByteBuf buf) {
        return new ClipboardPacket(buf.readUtf());
    }

    public static void handle(ClipboardPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> setClipboard(packet.data));
        ctx.get().setPacketHandled(true);
    }

    @OnlyIn(Dist.CLIENT)
    private static void setClipboard(String data) {
        packetAccumulator.append(data);
        String completeData = packetAccumulator.toString();
        packetAccumulator.setLength(0);

        try {
            Minecraft.getInstance().keyboardHandler.setClipboard(completeData);
        } catch (Exception e) {
            try {
                Path path = Paths.get("structure_output.json");
                Files.writeString(path, completeData);
                Minecraft.getInstance().keyboardHandler.setClipboard("Data saved to: " + path.toAbsolutePath());
            } catch (Exception ex) {
                Minecraft.getInstance().keyboardHandler.setClipboard("Failed to copy or save structure data.");
            }
        }
    }
}