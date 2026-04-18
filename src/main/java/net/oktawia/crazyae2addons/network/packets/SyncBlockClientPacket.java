package net.oktawia.crazyae2addons.network.packets;

import appeng.api.parts.IPartHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.parts.CrazyPatternProviderPart;
import org.jetbrains.annotations.Nullable;

public record SyncBlockClientPacket(BlockPos pos, int added, @Nullable Direction side) implements CustomPacketPayload {

    public SyncBlockClientPacket(BlockPos pos, int added) {
        this(pos, added, null);
    }

    public static final Type<SyncBlockClientPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "sync_block_client"));

    public static final StreamCodec<FriendlyByteBuf, SyncBlockClientPacket> STREAM_CODEC =
            StreamCodec.ofMember(SyncBlockClientPacket::write, SyncBlockClientPacket::new);

    private SyncBlockClientPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt(), buf.readBoolean() ? buf.readEnum(Direction.class) : null);
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.added);
        buf.writeBoolean(this.side != null);
        if (this.side != null) buf.writeEnum(this.side);
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
            if (pkt.side != null) {
                if (be instanceof IPartHost host) {
                    var part = host.getPart(pkt.side);
                    if (part instanceof CrazyPatternProviderPart cpp) {
                        cpp.setAdded(pkt.added);
                    }
                }
            } else {
                if (be instanceof CrazyPatternProviderBE myBe) {
                    myBe.setAdded(pkt.added);
                }
            }
        });
    }
}
