package net.oktawia.crazyae2addons.client.renderer.preview;

import java.util.HashMap;
import java.util.Map;

public final class PortableSpatialStoragePreviewCache {

    private static final Map<String, PreviewStructure> CACHE = new HashMap<>();

    private PortableSpatialStoragePreviewCache() {
    }

    public static void put(String structureId, PreviewStructure structure) {
        if (structureId == null || structureId.isBlank() || structure == null) {
            return;
        }
        CACHE.put(structureId, structure);
    }

    public static PreviewStructure get(String structureId) {
        if (structureId == null || structureId.isBlank()) {
            return null;
        }
        return CACHE.get(structureId);
    }

    public static void clear(String structureId) {
        if (structureId == null || structureId.isBlank()) {
            return;
        }
        CACHE.remove(structureId);
    }

    public static void clearAll() {
        CACHE.clear();
    }
}