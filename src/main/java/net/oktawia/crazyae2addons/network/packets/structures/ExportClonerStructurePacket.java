package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryStore;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialClonerMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.util.function.Supplier;

public class ExportClonerStructurePacket {

    private static final int MAX_EXPORT_BYTES = 16 * 1024 * 1024;

    private final int containerId;
    private final String id;

    public ExportClonerStructurePacket(int containerId, String id) {
        this.containerId = containerId;
        this.id = id == null ? "" : id;
    }

    public static void encode(ExportClonerStructurePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUtf(packet.id, 32767);
    }

    public static ExportClonerStructurePacket decode(FriendlyByteBuf buffer) {
        return new ExportClonerStructurePacket(
                buffer.readVarInt(),
                buffer.readUtf(32767)
        );
    }

    public static void handle(ExportClonerStructurePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null || packet.id.isBlank()) {
                return;
            }

            if (!(player.containerMenu instanceof PortableSpatialClonerMenu menu)
                    || menu.containerId != packet.containerId) {
                return;
            }

            try {
                CompoundTag tag = ClonerStructureLibraryStore.load(
                        player.server,
                        player.getUUID(),
                        packet.id
                );

                if (tag == null) {
                    return;
                }

                byte[] bytes = TemplateUtil.compressNbt(tag);

                if (bytes.length > MAX_EXPORT_BYTES) {
                    return;
                }

                NetworkHandler.sendToPlayer(
                        player,
                        new ExportClonerStructureResultPacket(packet.id, bytes)
                );
            } catch (Exception ignored) {
            }
        });

        context.setPacketHandled(true);
    }
}