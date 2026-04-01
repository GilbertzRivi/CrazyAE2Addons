package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oktawia.crazyae2addons.CrazyAddons;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

public record SendLongStringToClientPacket(String data) implements CustomPacketPayload {

    public static final Type<SendLongStringToClientPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "send_string_to_client"));

    public static final StreamCodec<FriendlyByteBuf, SendLongStringToClientPacket> STREAM_CODEC =
            StreamCodec.ofMember(SendLongStringToClientPacket::write, SendLongStringToClientPacket::new);

    private SendLongStringToClientPacket(FriendlyByteBuf buf) {
        this(new String(buf.readByteArray(), StandardCharsets.UTF_8));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeByteArray(data.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static volatile Consumer<String> clientHandler = null;

    public static void handle(SendLongStringToClientPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            Consumer<String> h = clientHandler;
            if (h != null) h.accept(pkt.data());
        });
    }
}
