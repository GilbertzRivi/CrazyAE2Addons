package net.oktawia.crazyae2addons.network;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.menus.DataflowPatternMenu;
import net.oktawia.crazyae2addons.screens.DataflowPatternScreen;

import java.util.function.Supplier;

public class TagPacketToServer {
    private final CompoundTag tag;

    public TagPacketToServer(CompoundTag tag) {
        this.tag = tag;
    }

    public static void encode(TagPacketToServer packet, FriendlyByteBuf buf) {
        buf.writeNbt(packet.tag);
    }

    public static TagPacketToServer decode(FriendlyByteBuf buf) {
        return new TagPacketToServer(buf.readNbt());
    }

    public static void handle(TagPacketToServer packet, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            if (player.containerMenu instanceof DataflowPatternMenu menu) {
                menu.saveData(packet.tag);
            }
        });
        ctx.setPacketHandled(true);
    }
}
