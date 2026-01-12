package net.oktawia.crazyae2addonslite.misc;

import appeng.api.stacks.GenericStack;
import net.minecraft.client.Minecraft;
import net.oktawia.crazyae2addonslite.network.StockThresholdToastPacket;

public final class ClientToastHandler {
    private ClientToastHandler() {}

    public static void handle(StockThresholdToastPacket p) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var gs = GenericStack.fromItemStack(p.filter());
        if (gs == null) return;

        mc.getToasts().addToast(new StockThresholdToast(gs.what(), p.above(), p.threshold(), p.amount()));
    }
}
