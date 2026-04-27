package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialClonerMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;

import java.util.List;
import java.util.function.Supplier;

public class RequestClonerLibraryPacket {

    private final int containerId;

    public RequestClonerLibraryPacket(int containerId) {
        this.containerId = containerId;
    }

    public static void encode(RequestClonerLibraryPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
    }

    public static RequestClonerLibraryPacket decode(FriendlyByteBuf buffer) {
        return new RequestClonerLibraryPacket(buffer.readVarInt());
    }

    public static void handle(RequestClonerLibraryPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null) {
                return;
            }

            if (!(player.containerMenu instanceof PortableSpatialClonerMenu menu)
                    || menu.containerId != packet.containerId) {
                return;
            }

            ItemStack stack = menu.getStructureHost().getItemStack();
            String selectedId = StructureToolStackState.getStructureId(stack);

            try {
                NetworkHandler.sendToPlayer(
                        player,
                        SyncClonerLibraryPacket.fromStoreEntries(
                                ClonerStructureLibraryStore.list(player.server, player.getUUID()),
                                selectedId
                        )
                );
            } catch (Exception ignored) {
                NetworkHandler.sendToPlayer(
                        player,
                        SyncClonerLibraryPacket.fromStoreEntries(List.of(), selectedId)
                );
            }
        });

        context.setPacketHandled(true);
    }
}