package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialClonerMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;

import java.util.function.Supplier;

public class RenameClonerStructurePacket {

    private final int containerId;
    private final String id;
    private final String name;

    public RenameClonerStructurePacket(int containerId, String id, String name) {
        this.containerId = containerId;
        this.id = id == null ? "" : id;
        this.name = name == null ? "" : name;
    }

    public static void encode(RenameClonerStructurePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUtf(packet.id, 32767);
        buffer.writeUtf(packet.name, ClonerStructureLibraryStore.MAX_NAME_LENGTH);
    }

    public static RenameClonerStructurePacket decode(FriendlyByteBuf buffer) {
        return new RenameClonerStructurePacket(
                buffer.readVarInt(),
                buffer.readUtf(32767),
                buffer.readUtf(ClonerStructureLibraryStore.MAX_NAME_LENGTH)
        );
    }

    public static void handle(RenameClonerStructurePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

            try {
                ClonerStructureLibraryStore.rename(
                        player.server,
                        player.getUUID(),
                        packet.id,
                        packet.name
                );

                NetworkHandler.sendToPlayer(
                        player,
                        SyncClonerLibraryPacket.fromStoreEntries(
                                ClonerStructureLibraryStore.list(player.server, player.getUUID()),
                                StructureToolStackState.getStructureId(menu.getStructureHost().getItemStack())
                        )
                );
            } catch (Exception ignored) {
            }
        });

        context.setPacketHandled(true);
    }
}