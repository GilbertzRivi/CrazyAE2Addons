package net.oktawia.crazyae2addons.network.transfer;

import java.util.Arrays;

import net.minecraft.server.level.ServerPlayer;

import net.oktawia.crazyae2addons.logic.display.DisplayImageTransfer;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.DisplayImageStreamPacket;

public final class DisplayImageStreamSender {

    private DisplayImageStreamSender() {
    }

    public static void sendImage(ServerPlayer player, String imageId, byte[] bytes) {
        if (player == null || imageId == null || imageId.isBlank() || bytes == null || bytes.length == 0) {
            return;
        }

        NetworkHandler.sendToPlayer(player, DisplayImageStreamPacket.begin(imageId));

        for (int offset = 0; offset < bytes.length; offset += DisplayImageTransfer.CHUNK_BYTES) {
            int end = Math.min(bytes.length, offset + DisplayImageTransfer.CHUNK_BYTES);
            NetworkHandler.sendToPlayer(player, DisplayImageStreamPacket.data(
                    imageId,
                    Arrays.copyOfRange(bytes, offset, end)));
        }

        NetworkHandler.sendToPlayer(player, DisplayImageStreamPacket.end(imageId));
    }
}
