package net.oktawia.crazyae2addons.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.gui.overlay.IGuiOverlay;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT)
public class PreviewTooltipRenderer {
    public static String text = null;
    public static Integer forceColor = null;
    public static long expireAtMs = 0L;
    public static final long DEFAULT_TTL_MS = 150L;

    public static final IGuiOverlay TOOLTIP = ((gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        long now = System.currentTimeMillis();
        if (text == null || now > expireAtMs) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        float x = screenWidth / 2f + 8f;
        float y = (screenHeight - font.lineHeight) / 2f + 8f;
        pose.translate(x, y, 0.0F);
        drawText(guiGraphics, font, text);
        pose.popPose();

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        com.mojang.blaze3d.systems.RenderSystem.defaultBlendFunc();
        com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
    });

    public static void set(String msg, Integer color, long ttlMs) {
        text = msg;
        forceColor = color;
        expireAtMs = System.currentTimeMillis() + Math.max(50L, ttlMs);
    }

    private static void drawText(GuiGraphics g, Font font, String txt) {
        if (forceColor != null) {
            g.drawString(font, txt, 0, 0, forceColor, true);
            return;
        }
        char[] arr = txt.toCharArray();
        int dx = 0;
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            double t = (arr.length > 1) ? (i / (double) (arr.length - 1)) : 0.0;
            int colorInt = SimpleGradient.blueGradient(t);
            g.drawString(font, String.valueOf(ch), dx, 0, colorInt, true);
            dx += font.width(String.valueOf(ch));
        }
    }
}
