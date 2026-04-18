package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;

public final class PreviewRegistry {
    private static final Set<MultiblockPreviewHost> HOSTS =
            Collections.newSetFromMap(new WeakHashMap<>());

    private PreviewRegistry() {
    }

    public static void register(MultiblockPreviewHost host) {
        HOSTS.add(host);
    }

    public static void unregister(MultiblockPreviewHost host) {
        HOSTS.remove(host);
    }

    public static ArrayList<MultiblockPreviewHost> snapshot() {
        return new ArrayList<>(HOSTS);
    }
}