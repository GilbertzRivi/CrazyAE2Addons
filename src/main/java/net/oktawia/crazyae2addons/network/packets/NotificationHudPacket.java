package net.oktawia.crazyae2addons.network.packets;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.client.renderer.overlay.NotificationHudOverlay;

public record NotificationHudPacket(List<Entry> entries, byte hudX, byte hudY, byte hudScale) {

    public record Entry(ItemStack icon, long amount, long threshold) {
    }

    public static void encode(NotificationHudPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.hudX);
        buf.writeByte(msg.hudY);
        buf.writeByte(msg.hudScale);

        buf.writeVarInt(msg.entries.size());
        for (Entry entry : msg.entries) {
            buf.writeItem(entry.icon);
            buf.writeVarLong(entry.amount);
            buf.writeVarLong(entry.threshold);
        }
    }

    public static NotificationHudPacket decode(FriendlyByteBuf buf) {
        byte hudX = buf.readByte();
        byte hudY = buf.readByte();
        byte hudScale = buf.readByte();

        int size = buf.readVarInt();
        List<Entry> entries = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            ItemStack icon = buf.readItem();
            long amount = buf.readVarLong();
            long threshold = buf.readVarLong();
            entries.add(new Entry(icon, amount, threshold));
        }

        return new NotificationHudPacket(entries, hudX, hudY, hudScale);
    }

    public static void handle(NotificationHudPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        NetworkEvent.Context ctx = ctxSup.get();
        ctx.enqueueWork(() -> NotificationHudOverlay.update(msg.entries, msg.hudX, msg.hudY, msg.hudScale));
        ctx.setPacketHandled(true);
    }
}