package net.oktawia.crazyae2addons.network;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.network.packets.*;
import net.oktawia.crazyae2addons.network.packets.structures.*;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            CrazyAddons.makeId("main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int nextId = 0;

    private NetworkHandler() {}

    public static void registerMessages() {
        CHANNEL.messageBuilder(SendLongStringToClientPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SendLongStringToClientPacket::encode)
                .decoder(SendLongStringToClientPacket::decode)
                .consumerMainThread(SendLongStringToClientPacket::handle)
                .add();

        CHANNEL.messageBuilder(SendLongStringToServerPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SendLongStringToServerPacket::encode)
                .decoder(SendLongStringToServerPacket::decode)
                .consumerMainThread(SendLongStringToServerPacket::handle)
                .add();

        CHANNEL.messageBuilder(SetConfigAmountPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SetConfigAmountPacket::encode)
                .decoder(SetConfigAmountPacket::decode)
                .consumerMainThread(SetConfigAmountPacket::handle)
                .add();

        CHANNEL.messageBuilder(DisplaySyncPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DisplaySyncPacket::encode)
                .decoder(DisplaySyncPacket::decode)
                .consumerMainThread(DisplaySyncPacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncDisplayImagePreviewPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncDisplayImagePreviewPacket::encode)
                .decoder(SyncDisplayImagePreviewPacket::decode)
                .consumerMainThread(SyncDisplayImagePreviewPacket::handle)
                .add();

        CHANNEL.messageBuilder(UploadDisplayImagePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UploadDisplayImagePacket::encode)
                .decoder(UploadDisplayImagePacket::decode)
                .consumerMainThread(UploadDisplayImagePacket::handle)
                .add();

        CHANNEL.messageBuilder(NotificationHudPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(NotificationHudPacket::encode)
                .decoder(NotificationHudPacket::decode)
                .consumerMainThread(NotificationHudPacket::handle)
                .add();

        CHANNEL.messageBuilder(EmitterWindowPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(EmitterWindowPacket::encode)
                .decoder(EmitterWindowPacket::decode)
                .consumerMainThread(EmitterWindowPacket::handle)
                .add();

        CHANNEL.messageBuilder(RedstoneWindowPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RedstoneWindowPacket::encode)
                .decoder(RedstoneWindowPacket::decode)
                .consumerMainThread(RedstoneWindowPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestStructureToolPreviewPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestStructureToolPreviewPacket::encode)
                .decoder(RequestStructureToolPreviewPacket::decode)
                .consumerMainThread(RequestStructureToolPreviewPacket::handle)
                .add();

        CHANNEL.messageBuilder(ShowHudMessagePacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ShowHudMessagePacket::encode)
                .decoder(ShowHudMessagePacket::decode)
                .consumerMainThread(ShowHudMessagePacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncClonerRequirementStatusPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncClonerRequirementStatusPacket::encode)
                .decoder(SyncClonerRequirementStatusPacket::decode)
                .consumerMainThread(SyncClonerRequirementStatusPacket::handle)
                .add();

        CHANNEL.messageBuilder(CancelAllCraftingPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CancelAllCraftingPacket::encode)
                .decoder(CancelAllCraftingPacket::decode)
                .consumerMainThread(CancelAllCraftingPacket::handle)
                .add();

        CHANNEL.messageBuilder(WirelessNotificationWindowPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(WirelessNotificationWindowPacket::encode)
                .decoder(WirelessNotificationWindowPacket::decode)
                .consumerMainThread(WirelessNotificationWindowPacket::handle)
                .add();

        CHANNEL.messageBuilder(RequestClonerLibraryPacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RequestClonerLibraryPacket::encode)
                .decoder(RequestClonerLibraryPacket::decode)
                .consumerMainThread(RequestClonerLibraryPacket::handle)
                .add();

        CHANNEL.messageBuilder(SelectClonerStructurePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(SelectClonerStructurePacket::encode)
                .decoder(SelectClonerStructurePacket::decode)
                .consumerMainThread(SelectClonerStructurePacket::handle)
                .add();

        CHANNEL.messageBuilder(RenameClonerStructurePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(RenameClonerStructurePacket::encode)
                .decoder(RenameClonerStructurePacket::decode)
                .consumerMainThread(RenameClonerStructurePacket::handle)
                .add();

        CHANNEL.messageBuilder(SyncClonerLibraryPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SyncClonerLibraryPacket::encode)
                .decoder(SyncClonerLibraryPacket::decode)
                .consumerMainThread(SyncClonerLibraryPacket::handle)
                .add();

        CHANNEL.messageBuilder(DeleteClonerStructurePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(DeleteClonerStructurePacket::encode)
                .decoder(DeleteClonerStructurePacket::decode)
                .consumerMainThread(DeleteClonerStructurePacket::handle)
                .add();

        CHANNEL.messageBuilder(ExportClonerStructurePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ExportClonerStructurePacket::encode)
                .decoder(ExportClonerStructurePacket::decode)
                .consumerMainThread(ExportClonerStructurePacket::handle)
                .add();

        CHANNEL.messageBuilder(ExportClonerStructureResultPacket.class, nextId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ExportClonerStructureResultPacket::encode)
                .decoder(ExportClonerStructureResultPacket::decode)
                .consumerMainThread(ExportClonerStructureResultPacket::handle)
                .add();

        CHANNEL.messageBuilder(ImportClonerStructurePacket.class, nextId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ImportClonerStructurePacket::encode)
                .decoder(ImportClonerStructurePacket::decode)
                .consumerMainThread(ImportClonerStructurePacket::handle)
                .add();
    }

    public static void sendToPlayer(ServerPlayer player, Object packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }

    public static void sendToTrackingChunk(LevelChunk chunk, Object packet) {
        CHANNEL.send(PacketDistributor.TRACKING_CHUNK.with(() -> chunk), packet);
    }

    public static void sendToServer(Object packet) {
        CHANNEL.sendToServer(packet);
    }
}