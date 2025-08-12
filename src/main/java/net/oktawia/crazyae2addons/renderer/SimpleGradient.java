package net.oktawia.crazyae2addons.renderer;

public class SimpleGradient {
    public static final int START = 0x56E2F5; // jasny niebieski
    public static final int END   = 0x00BAD4; // turkus

    public static int blueGradient(double t) {
        t = Math.max(0, Math.min(1, t));
        int r1 = (START >> 16) & 0xFF, g1 = (START >> 8) & 0xFF, b1 = START & 0xFF;
        int r2 = (END >> 16) & 0xFF,   g2 = (END >> 8) & 0xFF,   b2 = END & 0xFF;
        int r = (int)(r1 + (r2 - r1) * t);
        int g = (int)(g1 + (g2 - g1) * t);
        int b = (int)(b1 + (b2 - b1) * t);
        return (r << 16) | (g << 8) | b;
    }
}