package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.menus.EjectorMenu;

public record SetConfigAmountPacket(int slotIndex, long amount) implements CustomPacketPayload {

    public static final Type<SetConfigAmountPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "set_config_amount"));

    public static final StreamCodec<FriendlyByteBuf, SetConfigAmountPacket> STREAM_CODEC =
            StreamCodec.ofMember(SetConfigAmountPacket::write, SetConfigAmountPacket::new);

    private SetConfigAmountPacket(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readVarLong());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeVarLong(amount);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetConfigAmountPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player != null && player.containerMenu instanceof EjectorMenu menu) {
                menu.setConfigAmount(pkt.slotIndex(), pkt.amount());
            }
        });
    }
}
