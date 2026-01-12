package net.oktawia.crazyae2addonslite.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addonslite.entities.CrazyPatternProviderBE;
import java.util.function.Supplier;

public class SyncBlockClientPacket {
    private final BlockPos pos;
    private final Integer added;

    public SyncBlockClientPacket(BlockPos pos, Integer added) {
        this.pos = pos;
        this.added = added;
    }

    public static void encode(SyncBlockClientPacket packet, FriendlyByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeInt(packet.added);
    }

    public static SyncBlockClientPacket decode(FriendlyByteBuf buf) {
        return new SyncBlockClientPacket(buf.readBlockPos(), buf.readInt());
    }

    public static void handle(SyncBlockClientPacket pkt, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.level == null) return;

            BlockEntity be = mc.level.getBlockEntity(pkt.pos);
            if (be instanceof CrazyPatternProviderBE myBe) {
                myBe.setAdded(pkt.added);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
