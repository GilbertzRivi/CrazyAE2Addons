package net.oktawia.crazyae2addons.network.packets;

import appeng.api.stacks.GenericStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.client.screens.part.EmitterTerminalScreen;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record EmitterWindowPacket(
        int totalCount,
        int windowOffset,
        int revision,
        List<EmitterTerminalMenu.StorageEmitterInfo> window
) {

    public static void encode(EmitterWindowPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.totalCount);
        buf.writeVarInt(pkt.windowOffset);
        buf.writeVarInt(pkt.revision);
        buf.writeVarInt(pkt.window.size());

        for (var entry : pkt.window) {
            buf.writeUtf(entry.uuid() != null ? entry.uuid() : "");
            buf.writeComponent(entry.name() != null ? entry.name() : Component.empty());

            boolean hasConfig = entry.config() != null;
            buf.writeBoolean(hasConfig);
            if (hasConfig) {
                buf.writeItem(GenericStack.wrapInItemStack(entry.config()));
            }

            buf.writeLong(entry.value() != null ? entry.value() : 0L);
        }
    }

    public static EmitterWindowPacket decode(FriendlyByteBuf buf) {
        int total = buf.readVarInt();
        int offset = buf.readVarInt();
        int revision = buf.readVarInt();
        int size = buf.readVarInt();

        var window = new ArrayList<EmitterTerminalMenu.StorageEmitterInfo>(size);
        for (int i = 0; i < size; i++) {
            String uuid = buf.readUtf();
            Component name = buf.readComponent();

            boolean hasConfig = buf.readBoolean();
            GenericStack config = null;
            if (hasConfig) {
                ItemStack item = buf.readItem();
                config = GenericStack.fromItemStack(item);
            }

            long value = buf.readLong();
            window.add(new EmitterTerminalMenu.StorageEmitterInfo(uuid, name, config, value));
        }

        return new EmitterWindowPacket(total, offset, revision, window);
    }

    public static void handle(EmitterWindowPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var screen = Minecraft.getInstance().screen;
            if (screen instanceof EmitterTerminalScreen<?> s) {
                s.applyEmitterWindow(pkt);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
