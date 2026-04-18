package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

public final class SimpleGradient {
    public static final int START = 0x5CC8FF;
    public static final int END = 0x3B82F6;

    private SimpleGradient() {
    }

    public static int blueGradient(double t) {
        t = Math.clamp(t, 0.0, 1.0);

        int r1 = (START >> 16) & 0xFF;
        int g1 = (START >> 8) & 0xFF;
        int b1 = START & 0xFF;

        int r2 = (END >> 16) & 0xFF;
        int g2 = (END >> 8) & 0xFF;
        int b2 = END & 0xFF;

        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (r << 16) | (g << 8) | b;
    }
}