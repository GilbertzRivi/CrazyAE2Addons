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

    public static final IGuiOverlay TOOLTIP = ((gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
        if (text == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        PoseStack pose = guiGraphics.pose();
        pose.pushPose();
        pose.translate((float) screenWidth / 2 + 8, (float) (screenHeight - font.lineHeight) / 2 + 8, 0.0F);
        drawTextWithGradient(guiGraphics, font, text, new float[]{0.0f, 0.5f, 1.0f}, new float[]{1.0f, 0.5f, 0.0f}, true);
        pose.popPose();
        text = null;
    });

    private static void drawTextWithGradient(GuiGraphics guiGraphics, Font font, String text, float[] rgb1, float[] rgb2, boolean shadow) {
        char[] arr = text.toCharArray();
        int dx = 0;
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            float[] color = LabGradient.labGradient(rgb1, rgb2, i / (float) (arr.length - 1));
            int colorInt = (int) (color[0] * 255) << 16 | (int) (color[1] * 255) << 8 | (int) (color[2] * 255);
            guiGraphics.drawString(font, String.valueOf(ch), dx, 0, colorInt, shadow);
            dx += font.width(String.valueOf(ch));
        }
    }
}
