package net.oktawia.crazyae2addons.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.oktawia.crazyae2addons.network.packets.*;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    private NetworkHandler() {}

    public static void registerMessages(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

        registrar.playToClient(
                DisplaySyncPacket.TYPE,
                DisplaySyncPacket.STREAM_CODEC,
                DisplaySyncPacket::handle
        );

        registrar.playToClient(
                SyncBlockClientPacket.TYPE,
                SyncBlockClientPacket.STREAM_CODEC,
                SyncBlockClientPacket::handle
        );

        registrar.playToClient(
                UpdatePatternsPacket.TYPE,
                UpdatePatternsPacket.STREAM_CODEC,
                UpdatePatternsPacket::handle
        );

        registrar.playToClient(
                SendLongStringToClientPacket.TYPE,
                SendLongStringToClientPacket.STREAM_CODEC,
                SendLongStringToClientPacket::handle
        );

        registrar.playToServer(
                SendLongStringToServerPacket.TYPE,
                SendLongStringToServerPacket.STREAM_CODEC,
                SendLongStringToServerPacket::handle
        );

        registrar.playToServer(
                SetConfigAmountPacket.TYPE,
                SetConfigAmountPacket.STREAM_CODEC,
                SetConfigAmountPacket::handle
        );

        registrar.playToServer(
                UploadDisplayImagePacket.TYPE,
                UploadDisplayImagePacket.STREAM_CODEC,
                UploadDisplayImagePacket::handle
        );

        registrar.playToClient(
                SyncDisplayImagePreviewPacket.TYPE,
                SyncDisplayImagePreviewPacket.STREAM_CODEC,
                SyncDisplayImagePreviewPacket::handle
        );
    }
}
