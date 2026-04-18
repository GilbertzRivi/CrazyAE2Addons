package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternMenu;

import java.nio.charset.StandardCharsets;

public record SendLongStringToServerPacket(String data) implements CustomPacketPayload {

    public static final Type<SendLongStringToServerPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "send_string_to_server"));

    public static final StreamCodec<FriendlyByteBuf, SendLongStringToServerPacket> STREAM_CODEC =
            StreamCodec.ofMember(SendLongStringToServerPacket::write, SendLongStringToServerPacket::new);

    private SendLongStringToServerPacket(FriendlyByteBuf buf) {
        this(new String(buf.readByteArray(), StandardCharsets.UTF_8));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeByteArray(data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SendLongStringToServerPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player != null && player.containerMenu instanceof BuilderPatternMenu menu) {
                menu.receiveProgram(pkt.data());
            }
        });
    }
}
