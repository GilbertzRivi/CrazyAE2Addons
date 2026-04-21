package net.oktawia.crazyae2addons.client.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class AETextButton extends Button {

    public AETextButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }

    public AETextButton(Component message, OnPress onPress) {
        this(0, 0, 0, 0, message, onPress);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;

        int bg;
        int border;
        int textColor;

        if (!this.active) {
            bg = 0xFF2A2A2A;
            border = 0xFF555555;
            textColor = 0xFF777777;
        } else if (this.isHoveredOrFocused()) {
            bg = 0xFF3A4654;
            border = 0xFF8AA9C7;
            textColor = 0xFFFFFFFF;
        } else {
            bg = 0xFF353535;
            border = 0xFF909090;
            textColor = 0xFFE0E0E0;
        }

        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bg);
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, border);
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, border);
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        renderCenteredScrollingText(guiGraphics, font, textColor);
    }

    private void renderCenteredScrollingText(GuiGraphics guiGraphics, Font font, int color) {
        Component text = getMessage();
        int innerLeft = getX() + 3;
        int innerRight = getX() + width - 3;
        int innerTop = getY();
        int innerBottom = getY() + height;

        int textWidth = font.width(text);
        int available = innerRight - innerLeft;
        int textY = innerTop + (height - 8) / 2;

        if (textWidth <= available) {
            int centerX = Mth.clamp(getX() + width / 2, innerLeft + textWidth / 2, innerRight - textWidth / 2);
            guiGraphics.drawCenteredString(font, text, centerX, textY, color);
            return;
        }

        guiGraphics.enableScissor(innerLeft, innerTop, innerRight, innerBottom);

        long time = System.currentTimeMillis();
        int overflow = textWidth - available;
        double speed = Math.max(overflow * 0.5, 3.0);
        double phase = (time / 1000.0) / speed;
        double offset = (Math.sin(phase * Math.PI * 2.0) * 0.5 + 0.5) * overflow;

        guiGraphics.drawString(font, text, innerLeft - (int) offset, textY, color, false);
        guiGraphics.disableScissor();
    }
}