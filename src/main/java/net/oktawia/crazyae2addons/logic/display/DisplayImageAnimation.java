package net.oktawia.crazyae2addons.logic.display;

import net.oktawia.crazyae2addons.CrazyConfig;

public final class DisplayImageAnimation {

    public record Frame(float u0, float v0, float u1, float v1, int width, int height) {
    }

    private DisplayImageAnimation() {
    }

    public static Frame current(DisplayImageEntry entry, int textureWidth, int textureHeight) {
        int frames = entry == null ? 1 : Math.max(1, entry.frameCount());

        if (frames <= 1) {
            return new Frame(0f, 0f, 1f, 1f, textureWidth, textureHeight);
        }

        int columns = Math.max(1, entry.columns());
        int rows = entry.rows();
        int index = frameIndex(entry, frames);

        int column = index % columns;
        int row = Math.min(rows - 1, index / columns);

        float cellWidth = 1f / columns;
        float cellHeight = 1f / rows;

        float insetU = textureWidth > 0 ? 0.5f / textureWidth : 0f;
        float insetV = textureHeight > 0 ? 0.5f / textureHeight : 0f;

        return new Frame(
                column * cellWidth + insetU,
                row * cellHeight + insetV,
                (column + 1) * cellWidth - insetU,
                (row + 1) * cellHeight - insetV,
                Math.max(1, textureWidth / columns),
                Math.max(1, textureHeight / rows));
    }

    public static int frameIndex(DisplayImageEntry entry) {
        return frameIndex(entry, Math.max(1, entry.frameCount()));
    }

    private static int frameIndex(DisplayImageEntry entry, int frames) {
        if (!entry.animated() || !CrazyConfig.COMMON.DISPLAY_IMAGE_ANIMATION_ENABLED.get()) {
            return 0;
        }

        long elapsed = System.currentTimeMillis() / Math.max(1, entry.frameDurationMs());
        return (int) Math.floorMod(elapsed, frames);
    }
}
