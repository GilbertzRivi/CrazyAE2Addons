package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.items.PortableSpatialCloner;
import net.oktawia.crazyae2addons.items.PortableSpatialStorage;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolPreviewDispatcher;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolStackState;
import net.oktawia.crazyae2addons.logic.structuretool.StructureToolUtil;

import java.util.function.Supplier;

public class RequestStructureToolPreviewPacket {

    public static void encode(RequestStructureToolPreviewPacket packet, FriendlyByteBuf buffer) {
    }

    public static RequestStructureToolPreviewPacket decode(FriendlyByteBuf buffer) {
        return new RequestStructureToolPreviewPacket();
    }

    public static void handle(RequestStructureToolPreviewPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender == null) {
                return;
            }

            ItemStack stack = StructureToolUtil.findActive(
                    sender,
                    PortableSpatialStorage.class,
                    PortableSpatialCloner.class
            );
            if (stack.isEmpty()) {
                stack = StructureToolUtil.findHeld(
                        sender,
                        PortableSpatialStorage.class,
                        PortableSpatialCloner.class
                );
            }

            if (stack.isEmpty()) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(sender, null);
                return;
            }

            String structureId = StructureToolStackState.getStructureId(stack);
            if (structureId.isBlank()) {
                StructureToolPreviewDispatcher.sendPreviewToPlayer(sender, null);
                return;
            }

            StructureToolPreviewDispatcher.sendPreviewForStructureId(sender, structureId);
        });
        context.setPacketHandled(true);
    }
}