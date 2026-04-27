package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryStore;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolPreviewDispatcher;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.menus.item.PortableSpatialClonerMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.util.TemplateUtil;

import java.util.List;
import java.util.function.Supplier;

public class ImportClonerStructurePacket {

    private static final int MAX_IMPORT_BYTES = 16 * 1024 * 1024;

    private final int containerId;
    private final String name;
    private final byte[] bytes;

    public ImportClonerStructurePacket(int containerId, String name, byte[] bytes) {
        this.containerId = containerId;
        this.name = ClonerStructureLibraryStore.sanitizeName(name);
        this.bytes = bytes == null ? new byte[0] : bytes;
    }

    public static void encode(ImportClonerStructurePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUtf(packet.name, ClonerStructureLibraryStore.MAX_NAME_LENGTH);
        buffer.writeByteArray(packet.bytes);
    }

    public static ImportClonerStructurePacket decode(FriendlyByteBuf buffer) {
        return new ImportClonerStructurePacket(
                buffer.readVarInt(),
                buffer.readUtf(ClonerStructureLibraryStore.MAX_NAME_LENGTH),
                buffer.readByteArray(MAX_IMPORT_BYTES)
        );
    }

    public static void handle(ImportClonerStructurePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();

        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();

            if (player == null || packet.bytes.length == 0) {
                return;
            }

            if (!(player.containerMenu instanceof PortableSpatialClonerMenu menu)
                    || menu.containerId != packet.containerId) {
                return;
            }

            ItemStack stack = menu.getStructureHost().getItemStack();

            try {
                ClonerStructureLibraryStore.Entry entry = ClonerStructureLibraryStore.importBytes(
                        player.server,
                        player.getUUID(),
                        packet.bytes,
                        packet.name
                );

                CompoundTag tag = ClonerStructureLibraryStore.load(
                        player.server,
                        player.getUUID(),
                        entry.id()
                );

                StructureToolStackState.setSelectedClonerLibraryEntry(
                        stack,
                        player.getUUID(),
                        entry.id()
                );

                if (tag != null) {
                    TemplateUtil.copyPreviewTransformState(tag, stack.getOrCreateTag());
                }

                NetworkHandler.sendToPlayer(
                        player,
                        SyncClonerLibraryPacket.fromStoreEntries(
                                ClonerStructureLibraryStore.list(player.server, player.getUUID()),
                                StructureToolStackState.getStructureId(stack)
                        )
                );

                StructureToolPreviewDispatcher.sendPreviewToPlayer(player, tag);
            } catch (Exception ignored) {
                NetworkHandler.sendToPlayer(
                        player,
                        SyncClonerLibraryPacket.fromStoreEntries(List.of(), StructureToolStackState.getStructureId(stack))
                );
            }
        });

        context.setPacketHandled(true);
    }
}