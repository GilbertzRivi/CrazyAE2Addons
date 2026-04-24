package net.oktawia.crazyae2addons.logic.structuretool;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.SendLongStringToClientPacket;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class StructureToolPreviewDispatcher {

    private static final int PREVIEW_CHUNK_SIZE = 1_000_000;

    private StructureToolPreviewDispatcher() {
    }

    public static void sendPreviewToPlayer(ServerPlayer player, @Nullable CompoundTag structureTag) {
        NetworkHandler.sendToPlayer(player, new SendLongStringToClientPacket("__RESET__"));

        if (structureTag != null) {
            try {
                byte[] bytes = TemplateUtil.compressNbt(structureTag);
                String base64 = TemplateUtil.toBase64(bytes);

                byte[] raw = base64.getBytes(StandardCharsets.UTF_8);
                int total = (int) Math.ceil((double) raw.length / PREVIEW_CHUNK_SIZE);

                for (int i = 0; i < total; i++) {
                    int start = i * PREVIEW_CHUNK_SIZE;
                    int end = Math.min(raw.length, (i + 1) * PREVIEW_CHUNK_SIZE);
                    byte[] part = Arrays.copyOfRange(raw, start, end);

                    NetworkHandler.sendToPlayer(
                            player,
                            new SendLongStringToClientPacket(new String(part, StandardCharsets.UTF_8))
                    );
                }
            } catch (Exception ignored) {
            }
        }

        NetworkHandler.sendToPlayer(player, new SendLongStringToClientPacket("__END__"));
    }

    public static void sendPreviewForStructureId(ServerPlayer player, String structureId) {
        if (structureId == null || structureId.isBlank()) {
            sendPreviewToPlayer(player, null);
            return;
        }

        try {
            CompoundTag tag = StructureToolStructureStore.load(player.server, structureId);
            sendPreviewToPlayer(player, tag);
        } catch (Exception ignored) {
            sendPreviewToPlayer(player, null);
        }
    }
}