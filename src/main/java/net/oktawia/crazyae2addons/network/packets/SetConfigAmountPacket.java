package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.menus.block.EjectorMenu;

import java.util.function.Supplier;

public record SetConfigAmountPacket(int slotIndex, long amount) {

    public static void encode(SetConfigAmountPacket pkt, FriendlyByteBuf buf) {
        buf.writeVarInt(pkt.slotIndex);
        buf.writeVarLong(pkt.amount);
    }

    public static SetConfigAmountPacket decode(FriendlyByteBuf buf) {
        return new SetConfigAmountPacket(buf.readVarInt(), buf.readVarLong());
    }

    public static void handle(SetConfigAmountPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            var player = ctx.getSender();
            if (player != null && player.containerMenu instanceof EjectorMenu menu) {
                menu.setConfigAmount(pkt.slotIndex(), pkt.amount());
            }
        });
        ctx.setPacketHandled(true);
    }
}