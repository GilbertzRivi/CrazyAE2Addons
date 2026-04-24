package net.oktawia.crazyae2addons.network.packets;

import appeng.api.stacks.GenericStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.client.screens.item.WirelessNotificationTerminalScreen;
import net.oktawia.crazyae2addons.menus.item.WirelessNotificationTerminalMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record WirelessNotificationWindowPacket(
        int totalCount,
        int windowOffset,
        int revision,
        List<WirelessNotificationTerminalMenu.NotificationSlotInfo> window
) {

    public static void encode(WirelessNotificationWindowPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.totalCount);
        buf.writeVarInt(pkt.windowOffset);
        buf.writeVarInt(pkt.revision);
        buf.writeVarInt(pkt.window.size());

        for (var entry : pkt.window) {
            boolean hasConfig = entry.config() != null;
            buf.writeBoolean(hasConfig);
            if (hasConfig) {
                buf.writeItem(GenericStack.wrapInItemStack(entry.config()));
            }

            buf.writeLong(entry.threshold());
        }
    }

    public static WirelessNotificationWindowPacket decode(FriendlyByteBuf buf) {
        int total = buf.readVarInt();
        int offset = buf.readVarInt();
        int revision = buf.readVarInt();
        int size = buf.readVarInt();

        var window = new ArrayList<WirelessNotificationTerminalMenu.NotificationSlotInfo>(size);
        for (int i = 0; i < size; i++) {
            GenericStack config = null;

            boolean hasConfig = buf.readBoolean();
            if (hasConfig) {
                ItemStack item = buf.readItem();
                config = GenericStack.fromItemStack(item);
            }

            long threshold = buf.readLong();
            window.add(new WirelessNotificationTerminalMenu.NotificationSlotInfo(config, threshold));
        }

        return new WirelessNotificationWindowPacket(total, offset, revision, window);
    }

    public static void handle(WirelessNotificationWindowPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var screen = Minecraft.getInstance().screen;
            if (screen instanceof WirelessNotificationTerminalScreen<?> s) {
                s.applyClientWindow(pkt);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}