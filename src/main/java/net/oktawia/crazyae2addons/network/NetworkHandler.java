package net.oktawia.crazyae2addons.network;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.oktawia.crazyae2addons.network.packets.SyncBlockClientPacket;
import net.oktawia.crazyae2addons.network.packets.UpdatePatternsPacket;

public final class NetworkHandler {

    private static final String PROTOCOL_VERSION = "1";

    private NetworkHandler() {}

    public static void registerMessages(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar(PROTOCOL_VERSION);

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
    }
}
