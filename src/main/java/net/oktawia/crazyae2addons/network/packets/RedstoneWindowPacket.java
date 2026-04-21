package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.client.screens.part.RedstoneTerminalScreen;
import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public record RedstoneWindowPacket(
        int totalCount,
        int windowOffset,
        int revision,
        List<RedstoneTerminalMenu.EmitterInfo> window
) {

    public static void encode(RedstoneWindowPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.totalCount());
        buf.writeVarInt(packet.windowOffset());
        buf.writeVarInt(packet.revision());
        buf.writeVarInt(packet.window().size());

        for (var entry : packet.window()) {
            buf.writeBlockPos(entry.pos());
            buf.writeUtf(entry.name() != null ? entry.name() : "");
            buf.writeBoolean(entry.active());
        }
    }

    public static RedstoneWindowPacket decode(FriendlyByteBuf buf) {
        int totalCount = buf.readVarInt();
        int windowOffset = buf.readVarInt();
        int revision = buf.readVarInt();
        int size = buf.readVarInt();

        List<RedstoneTerminalMenu.EmitterInfo> window = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            BlockPos pos = buf.readBlockPos();
            String name = buf.readUtf();
            boolean active = buf.readBoolean();
            window.add(new RedstoneTerminalMenu.EmitterInfo(pos, name, active));
        }

        return new RedstoneWindowPacket(totalCount, windowOffset, revision, window);
    }

    public static void handle(RedstoneWindowPacket packet, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var screen = Minecraft.getInstance().screen;
            if (screen instanceof RedstoneTerminalScreen<?> terminalScreen) {
                terminalScreen.applyWindow(packet);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}