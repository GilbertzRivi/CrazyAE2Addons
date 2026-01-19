package net.oktawia.crazyae2addonslite.network.packets;

import net.minecraft.client.Minecraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.client.screens.CrazyPatternProviderScreen;

import java.util.ArrayList;
import java.util.List;

public record UpdatePatternsPacket(int startIndex, List<ItemStack> patterns) implements CustomPacketPayload {

    public static final Type<UpdatePatternsPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddonslite.MODID, "update_patterns"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdatePatternsPacket> STREAM_CODEC =
            StreamCodec.of(UpdatePatternsPacket::encode, UpdatePatternsPacket::decode);

    private static void encode(RegistryFriendlyByteBuf buf, UpdatePatternsPacket pkt) {
        buf.writeVarInt(pkt.startIndex);
        buf.writeVarInt(pkt.patterns.size());
        for (ItemStack stack : pkt.patterns) {
            ItemStack.STREAM_CODEC.encode(buf, stack);
        }
    }

    private static UpdatePatternsPacket decode(RegistryFriendlyByteBuf buf) {
        int start = buf.readVarInt();
        int size = buf.readVarInt();

        var patterns = new ArrayList<ItemStack>(Math.max(0, size));
        for (int i = 0; i < size; i++) {
            patterns.add(ItemStack.STREAM_CODEC.decode(buf));
        }
        return new UpdatePatternsPacket(start, patterns);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdatePatternsPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null) return;

            if (Minecraft.getInstance().screen instanceof CrazyPatternProviderScreen<?> screen) {
                screen.updatePatternsFromServer(pkt.startIndex, pkt.patterns);
            }
        });
    }
}
