package net.oktawia.crazyae2addons.client.misc;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayImageTransfer;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.RequestDisplayImagePacket;

@OnlyIn(Dist.CLIENT)
public final class DisplayImageClientCache {

    private static final long RETRY_DELAY_MS = 10_000L;

    private static final LinkedHashMap<String, byte[]> CACHE = new LinkedHashMap<>(16, 0.75f, true);
    private static final Map<String, ByteArrayOutputStream> INCOMING = new HashMap<>();
    private static final Map<String, Long> REQUESTED = new HashMap<>();

    private static long cachedBytes = 0L;

    private DisplayImageClientCache() {
    }

    @Nullable
    public static byte[] get(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }

        byte[] bytes = CACHE.get(id);
        if (bytes == null) {
            request(id);
        }

        return bytes;
    }

    public static Map<String, byte[]> collect(List<DisplayImageEntry> images) {
        Map<String, byte[]> out = new HashMap<>();

        if (images == null) {
            return out;
        }

        for (DisplayImageEntry image : images) {
            byte[] bytes = get(image.id());
            if (bytes != null) {
                out.put(image.id(), bytes);
            }
        }

        return out;
    }

    public static void accept(int signal, String id, byte[] data) {
        if (id == null || id.isEmpty()) {
            return;
        }

        switch (signal) {
            case DisplayImageTransfer.SIGNAL_BEGIN -> INCOMING.put(id, new ByteArrayOutputStream());
            case DisplayImageTransfer.SIGNAL_DATA -> {
                ByteArrayOutputStream buffer = INCOMING.get(id);
                if (buffer != null && data != null) {
                    buffer.writeBytes(data);
                }
            }
            case DisplayImageTransfer.SIGNAL_END -> {
                ByteArrayOutputStream buffer = INCOMING.remove(id);
                if (buffer != null && buffer.size() > 0) {
                    store(id, buffer.toByteArray());
                }
                REQUESTED.remove(id);
            }
            default -> INCOMING.remove(id);
        }
    }

    public static void invalidate(String id) {
        byte[] removed = CACHE.remove(id);
        if (removed != null) {
            cachedBytes -= removed.length;
        }

        INCOMING.remove(id);
        REQUESTED.remove(id);
    }

    public static void clear() {
        CACHE.clear();
        INCOMING.clear();
        REQUESTED.clear();
        cachedBytes = 0L;
    }

    private static void store(String id, byte[] bytes) {
        byte[] previous = CACHE.put(id, bytes);
        cachedBytes += bytes.length - (previous == null ? 0 : previous.length);
        evictUntilWithinBudget(id);
    }

    private static void evictUntilWithinBudget(String keepId) {
        long budget = CrazyConfig.COMMON.DISPLAY_IMAGES_TOTAL_BYTES.get();
        Iterator<Map.Entry<String, byte[]>> oldest = CACHE.entrySet().iterator();

        while (cachedBytes > budget && oldest.hasNext()) {
            Map.Entry<String, byte[]> entry = oldest.next();
            if (entry.getKey().equals(keepId)) {
                continue;
            }

            cachedBytes -= entry.getValue().length;
            oldest.remove();
        }
    }

    private static void request(String id) {
        long now = System.currentTimeMillis();
        Long lastRequest = REQUESTED.get(id);

        if (lastRequest != null && now - lastRequest < RETRY_DELAY_MS) {
            return;
        }

        REQUESTED.put(id, now);
        NetworkHandler.sendToServer(new RequestDisplayImagePacket(id));
    }
}
