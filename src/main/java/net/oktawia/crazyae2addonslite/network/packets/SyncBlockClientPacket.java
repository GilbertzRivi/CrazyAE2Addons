package net.oktawia.crazyae2addonslite.network.packets;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.minecraft.network.codec.StreamCodec;
import net.oktawia.crazyae2addonslite.CrazyAddonslite;
import net.oktawia.crazyae2addonslite.entities.CrazyPatternProviderBE;

public record SyncBlockClientPacket(BlockPos pos, int added) implements CustomPacketPayload {

    public static final Type<SyncBlockClientPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddonslite.MODID, "sync_block_client"));

    public static final StreamCodec<FriendlyByteBuf, SyncBlockClientPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncBlockClientPacket::write, SyncBlockClientPacket::new);

    private SyncBlockClientPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt());
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.added);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }


    public static void handle(final SyncBlockClientPacket pkt, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null || player.level() == null) return;

            var be = player.level().getBlockEntity(pkt.pos);
            if (be instanceof CrazyPatternProviderBE myBe) {
                myBe.setAdded(pkt.added);
            }
        });
    }
}
