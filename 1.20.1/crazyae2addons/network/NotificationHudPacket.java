package net.oktawia.crazyae2addons.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.misc.NotificationHudOverlay;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record NotificationHudPacket(List<Entry> entries, byte hudX, byte hudY) {

    public record Entry(ItemStack icon, long amount, long threshold) {
    }

    public static void encode(NotificationHudPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.hudX);
        buf.writeByte(msg.hudY);

        buf.writeVarInt(msg.entries.size());
        for (var e : msg.entries) {
            buf.writeItem(e.icon);
            buf.writeVarLong(e.amount);
            buf.writeVarLong(e.threshold);
        }
    }

    public static NotificationHudPacket decode(FriendlyByteBuf buf) {
        byte x = buf.readByte();
        byte y = buf.readByte();

        int n = buf.readVarInt();
        var list = new ArrayList<Entry>(n);
        for (int i = 0; i < n; i++) {
            ItemStack icon = buf.readItem();
            long amount = buf.readVarLong();
            long threshold = buf.readVarLong();
            list.add(new Entry(icon, amount, threshold));
        }
        return new NotificationHudPacket(list, x, y);
    }

    public static void handle(NotificationHudPacket msg, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() ->
                NotificationHudOverlay.update(msg.entries, msg.hudX, msg.hudY)
        );
        ctx.setPacketHandled(true);
    }
}
