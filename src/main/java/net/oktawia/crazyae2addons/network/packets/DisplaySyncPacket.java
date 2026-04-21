package net.oktawia.crazyae2addons.network.packets;

import appeng.api.parts.IPartHost;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.parts.Display;

import java.util.function.Supplier;

public record DisplaySyncPacket(BlockPos pos, Direction side, String packed) {

    public static void encode(DisplaySyncPacket pkt, FriendlyByteBuf buf) {
        buf.writeBlockPos(pkt.pos);
        buf.writeEnum(pkt.side);
        buf.writeUtf(pkt.packed, 65535);
    }

    public static DisplaySyncPacket decode(FriendlyByteBuf buf) {
        return new DisplaySyncPacket(
                buf.readBlockPos(),
                buf.readEnum(Direction.class),
                buf.readUtf(65535)
        );
    }

    public static void handle(DisplaySyncPacket pkt, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();

        ctx.enqueueWork(() ->
                DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Client.handle(pkt))
        );

        ctx.setPacketHandled(true);
    }

    @net.minecraftforge.api.distmarker.OnlyIn(Dist.CLIENT)
    private static final class Client {
        private static void handle(DisplaySyncPacket pkt) {
            var mc = net.minecraft.client.Minecraft.getInstance();
            if (mc.level == null) {
                return;
            }

            var be = mc.level.getBlockEntity(pkt.pos);
            if (!(be instanceof IPartHost host)) {
                return;
            }

            if (!(host.getPart(pkt.side) instanceof Display part)) {
                return;
            }

            part.resolvedTokens.clear();

            if (!pkt.packed.isEmpty()) {
                for (String entry : pkt.packed.split("\\|", -1)) {
                    int eq = entry.indexOf('=');
                    if (eq > 0) {
                        part.resolvedTokens.put(entry.substring(0, eq), entry.substring(eq + 1));
                    }
                }
            }
        }
    }
}