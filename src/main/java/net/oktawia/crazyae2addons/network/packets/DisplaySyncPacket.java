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
import net.oktawia.crazyae2addons.parts.DisplayPart;

public record DisplaySyncPacket(BlockPos pos, Direction side, String packed) implements CustomPacketPayload {

    public static final Type<DisplaySyncPacket> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(CrazyAddons.MODID, "display_sync"));

    public static final StreamCodec<FriendlyByteBuf, DisplaySyncPacket> STREAM_CODEC =
            StreamCodec.ofMember(DisplaySyncPacket::write, DisplaySyncPacket::new);

    private DisplaySyncPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos(), buf.readEnum(Direction.class), buf.readUtf(65535));
    }

    private void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeEnum(this.side);
        buf.writeUtf(this.packed, 65535);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(final DisplaySyncPacket pkt, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            var player = ctx.player();
            if (player == null || player.level() == null) return;

            var be = player.level().getBlockEntity(pkt.pos);
            if (!(be instanceof IPartHost host)) return;
            if (!(host.getPart(pkt.side) instanceof DisplayPart part)) return;

            part.resolvedTokens.clear();
            if (!pkt.packed.isEmpty()) {
                for (String entry : pkt.packed.split("\\|", -1)) {
                    int eq = entry.indexOf('=');
                    if (eq > 0) part.resolvedTokens.put(entry.substring(0, eq), entry.substring(eq + 1));
                }
            }
        });
    }
}
