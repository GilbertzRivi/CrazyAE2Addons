package net.oktawia.crazyae2addons.client.renderer.preview;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class BlockRenderExtensions {
    private static final List<BlockRenderExtension> EXTENSIONS = new CopyOnWriteArrayList<>();

    private BlockRenderExtensions() {
    }

    public static void register(BlockRenderExtension extension) {
        if (extension == null || EXTENSIONS.contains(extension)) {
            return;
        }

        EXTENSIONS.add(extension);
    }

    public static List<BlockRenderExtension> all() {
        return EXTENSIONS;
    }
}