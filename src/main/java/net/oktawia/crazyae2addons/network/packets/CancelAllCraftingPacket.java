package net.oktawia.crazyae2addons.network.packets;

import appeng.menu.me.crafting.CraftingCPUMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.logic.interfaces.ICancelAllCraftingMenu;

import java.util.function.Supplier;

public record CancelAllCraftingPacket() {

    public static void encode(CancelAllCraftingPacket pkt, FriendlyByteBuf buf) {
    }

    public static CancelAllCraftingPacket decode(FriendlyByteBuf buf) {
        return new CancelAllCraftingPacket();
    }

    public static void handle(CancelAllCraftingPacket pkt, Supplier<NetworkEvent.Context> ctxSup) {
        var ctx = ctxSup.get();

        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) {
                return;
            }

            if (player.containerMenu instanceof CraftingCPUMenu menu
                    && (Object) menu instanceof ICancelAllCraftingMenu cancelMenu) {
                cancelMenu.cancelAllCrafting();
            }
        });

        ctx.setPacketHandled(true);
    }
}