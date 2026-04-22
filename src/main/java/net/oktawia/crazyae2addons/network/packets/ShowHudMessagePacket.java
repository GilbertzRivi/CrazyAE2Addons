package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.client.renderer.message.ClientHudPacketHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record ShowHudMessagePacket(int durationTicks, List<Line> lines) {

    public record Line(Component text, int color) {
    }

    public static void encode(ShowHudMessagePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.durationTicks);
        buf.writeVarInt(msg.lines.size());

        for (Line line : msg.lines) {
            buf.writeComponent(line.text());
            buf.writeInt(line.color());
        }
    }

    public static ShowHudMessagePacket decode(FriendlyByteBuf buf) {
        int durationTicks = buf.readVarInt();
        int size = buf.readVarInt();

        List<Line> lines = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            lines.add(new Line(buf.readComponent(), buf.readInt()));
        }

        return new ShowHudMessagePacket(durationTicks, lines);
    }

    public static void handle(ShowHudMessagePacket msg, Supplier<NetworkEvent.Context> ctx) {
        NetworkEvent.Context context = ctx.get();

        context.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientHudPacketHandler.handleShowHudMessage(msg))
        );

        context.setPacketHandled(true);
    }
}