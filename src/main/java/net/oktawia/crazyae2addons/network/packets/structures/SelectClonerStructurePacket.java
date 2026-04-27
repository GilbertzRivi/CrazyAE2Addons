package net.oktawia.crazyae2addons.network.packets.structures;

import net.minecraft.core.BlockPos;
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

import java.util.function.Supplier;

public class SelectClonerStructurePacket {

    private final int containerId;
    private final String id;

    public SelectClonerStructurePacket(int containerId, String id) {
        this.containerId = containerId;
        this.id = id == null ? "" : id;
    }

    public static void encode(SelectClonerStructurePacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.containerId);
        buffer.writeUtf(packet.id, 32767);
    }

    public static SelectClonerStructurePacket decode(FriendlyByteBuf buffer) {
        return new SelectClonerStructurePacket(
                buffer.readVarInt(),
                buffer.readUtf(32767)
        );
    }

    public static void handle(SelectClonerStructurePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
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

            try {
                CompoundTag previewTag = null;

                if (packet.id.isBlank()) {
                    StructureToolStackState.clearSelectedClonerLibraryEntry(stack);

                    TemplateUtil.setTemplateOffset(stack.getOrCreateTag(), BlockPos.ZERO);
                    TemplateUtil.setEnergyOrigin(stack.getOrCreateTag(), BlockPos.ZERO);
                } else {
                    CompoundTag tag = ClonerStructureLibraryStore.load(
                            player.server,
                            player.getUUID(),
                            packet.id
                    );

                    if (tag == null) {
                        return;
                    }

                    StructureToolStackState.setSelectedClonerLibraryEntry(
                            stack,
                            player.getUUID(),
                            packet.id
                    );

                    TemplateUtil.copyPreviewTransformState(tag, stack.getOrCreateTag());

                    previewTag = tag;
                }

                NetworkHandler.sendToPlayer(
                        player,
                        SyncClonerLibraryPacket.fromStoreEntries(
                                ClonerStructureLibraryStore.list(player.server, player.getUUID()),
                                StructureToolStackState.getStructureId(stack)
                        )
                );

                StructureToolPreviewDispatcher.sendPreviewToPlayer(player, previewTag);
            } catch (Exception ignored) {
            }
        });

        context.setPacketHandled(true);
    }
}