package net.oktawia.crazyae2addons.network;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.misc.StockThresholdToast;

import java.util.function.Supplier;

public class StockThresholdToastPacket {
    private final ItemStack filter;
    private final boolean above;
    private final long threshold;
    private final long amount;

    public StockThresholdToastPacket(ItemStack filter, boolean above, long threshold, long amount) {
        this.filter = filter;
        this.above = above;
        this.threshold = threshold;
        this.amount = amount;
    }

    public ItemStack filter() { return filter; }
    public boolean above() { return above; }
    public long threshold() { return threshold; }
    public long amount() { return amount; }

    public static void encode(StockThresholdToastPacket p, FriendlyByteBuf buf) {
        buf.writeItem(p.filter);
        buf.writeBoolean(p.above);
        buf.writeLong(p.threshold);
        buf.writeLong(p.amount);
    }

    public static StockThresholdToastPacket decode(FriendlyByteBuf buf) {
        return new StockThresholdToastPacket(buf.readItem(), buf.readBoolean(), buf.readLong(), buf.readLong());
    }

    public static void handle(StockThresholdToastPacket p, Supplier<NetworkEvent.Context> ctxSupplier) {
        var ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(
                net.minecraftforge.api.distmarker.Dist.CLIENT,
                () -> () -> net.oktawia.crazyae2addons.misc.ClientToastHandler.handle(p)
        ));
        ctx.setPacketHandled(true);
    }
}

