package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

public final class PreviewTooltipLayer {
    public static final long DEFAULT_TTL_MS = 150L;

    private static @Nullable String text = null;
    private static @Nullable Integer forceColor = null;
    private static long expireAtMs = 0L;

    private PreviewTooltipLayer() {
    }

    public static void set(@Nullable String msg, @Nullable Integer color, long ttlMs) {
        if (msg == null || msg.isBlank()) {
            text = null;
            forceColor = null;
            expireAtMs = 0L;
            return;
        }

        text = msg;
        forceColor = color;
        expireAtMs = System.currentTimeMillis() + Math.max(50L, ttlMs);
    }

    public static void render(GuiGraphics guiGraphics, int screenWidth, int screenHeight) {
        long now = System.currentTimeMillis();
        if (text == null || now > expireAtMs) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        int x = (int) (screenWidth / 2f + 8f);
        int y = (int) ((screenHeight - font.lineHeight) / 2f + 8f);

        if (forceColor != null) {
            guiGraphics.drawString(font, text, x, y, forceColor, true);
            return;
        }

        int dx = 0;
        for (int i = 0; i < text.length(); i++) {
            double t = text.length() > 1 ? (i / (double) (text.length() - 1)) : 0.0;
            int color = SimpleGradient.blueGradient(t);
            String ch = String.valueOf(text.charAt(i));
            guiGraphics.drawString(font, ch, x + dx, y, color, true);
            dx += font.width(ch);
        }
    }
}