package net.oktawia.crazyae2addons.network;

import appeng.blockentity.networking.CableBusBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.parts.CrazyPatternProviderPart;

import java.util.function.Supplier;

public class SyncBlockClientPacket {
    private final BlockPos pos;
    private final Integer added;
    private final Direction direction;

    public SyncBlockClientPacket(BlockPos pos, Integer added, Direction direction) {
        this.pos = pos;
        this.added = added;
        this.direction = direction;
    }

    public static void encode(SyncBlockClientPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.added);
        buf.writeEnum(packet.direction);
    }

    public static SyncBlockClientPacket decode(FriendlyByteBuf buf) {
        return new SyncBlockClientPacket(buf.readBlockPos(), buf.readInt(), buf.readEnum(Direction.class));
    }

    public static void handle(SyncBlockClientPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            BlockEntity be = mc.level.getBlockEntity(pkt.pos);
            if (be instanceof CrazyPatternProviderBE myBe) {
                myBe.setAdded(pkt.added);
            } else if (be instanceof CableBusBlockEntity cbbe) {
                var part = cbbe.getPart(pkt.direction);
                if (part instanceof CrazyPatternProviderPart myPart) {
                    myPart.setAdded(pkt.added);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
