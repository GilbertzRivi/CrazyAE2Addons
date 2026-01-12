package net.oktawia.crazyae2addonslite.network;

import appeng.menu.me.crafting.CraftingCPUMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addonslite.interfaces.ICraftingMenuCancelAll;

import java.util.function.Supplier;

public record CancellAllCraftingPacket() {

    public static void encode(CancellAllCraftingPacket pkt, FriendlyByteBuf buf) {}

    public static CancellAllCraftingPacket decode(FriendlyByteBuf buf) {
        return new CancellAllCraftingPacket();
    }

    public static void handle(CancellAllCraftingPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            if (player.containerMenu instanceof CraftingCPUMenu menu && menu instanceof ICraftingMenuCancelAll ca) {
                ca.cancelAllCrafting();
            }
        });
        ctx.setPacketHandled(true);
    }
}