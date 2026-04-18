package net.oktawia.crazyae2addons.logic.display;

public record DisplayImageEntry(
        String id,
        String sourceName,
        int x,
        int y,
        int width,
        int height
) {
    public DisplayImageEntry withBounds(int x, int y, int width, int height) {
        return new DisplayImageEntry(id, sourceName, x, y, width, height);
    }
}