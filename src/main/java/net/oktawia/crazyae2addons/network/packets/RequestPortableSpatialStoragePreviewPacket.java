package net.oktawia.crazyae2addons.network.packets;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.oktawia.crazyae2addons.logic.cutpaste.PortableSpatialStoragePreviewDispatcher;

import java.util.function.Supplier;

public class RequestPortableSpatialStoragePreviewPacket {

    public static void encode(RequestPortableSpatialStoragePreviewPacket packet, FriendlyByteBuf buffer) {}

    public static RequestPortableSpatialStoragePreviewPacket decode(FriendlyByteBuf buffer) {
        return new RequestPortableSpatialStoragePreviewPacket();
    }

    public static void handle(RequestPortableSpatialStoragePreviewPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer sender = context.getSender();
            if (sender != null) {
                PortableSpatialStoragePreviewDispatcher.sendPreviewForHeldItem(sender);
            }
        });
        context.setPacketHandled(true);
    }
}