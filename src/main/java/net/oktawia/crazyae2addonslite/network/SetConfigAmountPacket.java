package net.oktawia.crazyae2addonslite.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addonslite.menus.EjectorMenu;

import java.util.function.Supplier;

public record SetConfigAmountPacket(int slotIndex, long amount) {
    public static void encode(SetConfigAmountPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.slotIndex());
        buf.writeVarLong(p.amount());
    }

    public static SetConfigAmountPacket decode(FriendlyByteBuf buf) {
        return new SetConfigAmountPacket(buf.readVarInt(), buf.readVarLong());
    }

    public static void handle(SetConfigAmountPacket p, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sp = ctx.get().getSender();
            if (sp != null && sp.containerMenu instanceof EjectorMenu menu) {
                menu.setConfigAmount(p.slotIndex(), p.amount());
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
