package net.oktawia.crazyae2addons.network.transfer;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.server.level.ServerPlayer;

import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.display.DisplayImagePng;
import net.oktawia.crazyae2addons.logic.display.DisplayImageStore;
import net.oktawia.crazyae2addons.logic.display.DisplayImageTransfer;
import net.oktawia.crazyae2addons.logic.display.DisplayImageUploadStatus;
import net.oktawia.crazyae2addons.menus.part.DisplayImagesSubMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.DisplayImageUploadResultPacket;
import net.oktawia.crazyae2addons.network.packets.UploadDisplayImageStreamPacket;

public final class DisplayImageUploadStream {

    private static final Map<UUID, Incoming> INCOMING = new HashMap<>();

    private DisplayImageUploadStream() {
    }

    private static final class Incoming {
        private final String sourceName;
        private final int frameCount;
        private final int columns;
        private final int frameDurationMs;
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private Incoming(UploadDisplayImageStreamPacket pkt) {
            this.sourceName = pkt.sourceName();
            this.frameCount = pkt.frameCount();
            this.columns = pkt.columns();
            this.frameDurationMs = pkt.frameDurationMs();
        }
    }

    public static void accept(ServerPlayer player, UploadDisplayImageStreamPacket pkt) {
        UUID key = player.getUUID();

        if (!CrazyConfig.COMMON.DISPLAY_ENABLED.get() || !CrazyConfig.COMMON.DISPLAY_IMAGES_ENABLED.get()) {
            fail(player, DisplayImageUploadStatus.DISABLED, 0);
            return;
        }

        switch (pkt.signal()) {
            case DisplayImageTransfer.SIGNAL_BEGIN -> INCOMING.put(key, new Incoming(pkt));
            case DisplayImageTransfer.SIGNAL_DATA -> append(player, pkt.data());
            case DisplayImageTransfer.SIGNAL_END -> commit(player);
            default -> fail(player, DisplayImageUploadStatus.FAILED, 0);
        }
    }

    public static void forget(UUID playerId) {
        INCOMING.remove(playerId);
    }

    private static void append(ServerPlayer player, byte[] data) {
        Incoming incoming = INCOMING.get(player.getUUID());

        if (incoming == null || data == null) {
            return;
        }

        int maxBytes = DisplayImageStore.maxImageBytes();

        if (incoming.buffer.size() + data.length > maxBytes) {
            fail(player, DisplayImageUploadStatus.TOO_LARGE, maxBytes / 1024);
            return;
        }

        incoming.buffer.writeBytes(data);
    }

    private static void commit(ServerPlayer player) {
        Incoming incoming = INCOMING.remove(player.getUUID());

        if (incoming == null) {
            return;
        }

        byte[] bytes = incoming.buffer.toByteArray();
        DisplayImagePng.Size size = DisplayImagePng.readSize(bytes);
        int maxDimension = DisplayImageStore.maxImageDimension();

        if (size == null || size.width() > maxDimension || size.height() > maxDimension) {
            reply(player, DisplayImageUploadStatus.INVALID_IMAGE, maxDimension);
            return;
        }

        if (!(player.containerMenu instanceof DisplayImagesSubMenu menu)) {
            reply(player, DisplayImageUploadStatus.FAILED, 0);
            return;
        }

        DisplayImageUploadStatus status = menu.addImage(
                incoming.sourceName,
                bytes,
                incoming.frameCount,
                incoming.columns,
                incoming.frameDurationMs);
        int detail = status == DisplayImageUploadStatus.STORAGE_FULL
                ? (int) (DisplayImageStore.budgetBytes() / 1024)
                : 0;

        reply(player, status, detail);
    }

    private static void fail(ServerPlayer player, DisplayImageUploadStatus status, int detailKilobytes) {
        INCOMING.remove(player.getUUID());
        reply(player, status, detailKilobytes);
    }

    private static void reply(ServerPlayer player, DisplayImageUploadStatus status, int detailKilobytes) {
        NetworkHandler.sendToPlayer(player, DisplayImageUploadResultPacket.of(status, detailKilobytes));
    }
}
