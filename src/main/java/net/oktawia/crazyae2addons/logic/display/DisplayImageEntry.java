package net.oktawia.crazyae2addons.logic.display;

public record DisplayImageEntry(
        String id,
        String sourceName,
        int x,
        int y,
        int width,
        int height,
        int frameCount,
        int columns,
        int frameDurationMs) {

    public DisplayImageEntry withBounds(int x, int y, int width, int height) {
        return new DisplayImageEntry(id, sourceName, x, y, width, height, frameCount, columns, frameDurationMs);
    }

    public boolean animated() {
        return frameCount > 1 && columns > 0 && frameDurationMs > 0;
    }

    public int rows() {
        return Math.max(1, (Math.max(1, frameCount) + Math.max(1, columns) - 1) / Math.max(1, columns));
    }
}
