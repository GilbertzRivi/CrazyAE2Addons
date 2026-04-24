package net.oktawia.crazyae2addons.client.misc;

import lombok.Getter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Supplier;

@Getter
@Accessors(chain = true)
public class SimpleProgressBarWidget extends AbstractWidget {

    private double progress;

    private int backgroundColor = 0xFF1E1E1E;
    private int borderColor = 0xFF8B8B8B;

    private boolean useGradient = true;
    private int fillColor = 0xFF56E2F5;
    private int gradientStartColor = 0xFF56E2F5;
    private int gradientEndColor = 0xFF00BAD4;

    private boolean drawBorder = true;
    private boolean drawBackground = true;

    private List<Component> tooltipLines = List.of();
    @Nullable
    private Supplier<List<Component>> tooltipSupplier = null;

    public SimpleProgressBarWidget(int x, int y, int width, int height) {
        this(x, y, width, height, Component.empty());
    }

    public SimpleProgressBarWidget(int x, int y, int width, int height, Component message) {
        super(x, y, width, height, message);
        this.progress = 0.0D;
        this.active = true;
    }

    public SimpleProgressBarWidget setProgress(double progress) {
        this.progress = Mth.clamp(progress, 0.0D, 1.0D);
        return this;
    }

    public boolean isPointOver(double mouseX, double mouseY) {
        return this.visible
                && mouseX >= this.getX()
                && mouseX < this.getX() + this.getWidth()
                && mouseY >= this.getY()
                && mouseY < this.getY() + this.getHeight();
    }

    public SimpleProgressBarWidget setProgress(float progress) {
        return setProgress((double) progress);
    }

    public SimpleProgressBarWidget setProgress(int current, int max) {
        if (max <= 0) {
            this.progress = 0.0D;
            return this;
        }

        return setProgress((double) current / (double) max);
    }

    public SimpleProgressBarWidget setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        return this;
    }

    public SimpleProgressBarWidget setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        return this;
    }

    public SimpleProgressBarWidget setFillColor(int fillColor) {
        this.fillColor = fillColor;
        this.useGradient = false;
        return this;
    }

    public SimpleProgressBarWidget setGradient(int startColor, int endColor) {
        this.gradientStartColor = startColor;
        this.gradientEndColor = endColor;
        this.useGradient = true;
        return this;
    }

    public SimpleProgressBarWidget setUseGradient(boolean useGradient) {
        this.useGradient = useGradient;
        return this;
    }

    public SimpleProgressBarWidget setDrawBorder(boolean drawBorder) {
        this.drawBorder = drawBorder;
        return this;
    }

    public SimpleProgressBarWidget setDrawBackground(boolean drawBackground) {
        this.drawBackground = drawBackground;
        return this;
    }

    public SimpleProgressBarWidget setTooltip(Component... lines) {
        this.tooltipSupplier = null;
        this.tooltipLines = List.of(lines);
        return this;
    }

    public SimpleProgressBarWidget setTooltipLines(List<Component> lines) {
        this.tooltipSupplier = null;
        this.tooltipLines = List.copyOf(lines);
        return this;
    }

    public SimpleProgressBarWidget setTooltipSupplier(@Nullable Supplier<List<Component>> tooltipSupplier) {
        this.tooltipSupplier = tooltipSupplier;
        return this;
    }

    public List<Component> getTooltipToRender() {
        if (this.tooltipSupplier != null) {
            List<Component> supplied = this.tooltipSupplier.get();
            return supplied == null ? List.of() : supplied;
        }
        return this.tooltipLines;
    }

    public boolean hasTooltip() {
        return !getTooltipToRender().isEmpty();
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = this.width;
        int h = this.height;

        if (this.drawBackground) {
            guiGraphics.fill(x, y, x + w, y + h, this.backgroundColor);
        }

        int innerX = x;
        int innerY = y;
        int innerW = w;
        int innerH = h;

        if (this.drawBorder) {
            guiGraphics.fill(x, y, x + w, y + 1, this.borderColor);
            guiGraphics.fill(x, y + h - 1, x + w, y + h, this.borderColor);
            guiGraphics.fill(x, y, x + 1, y + h, this.borderColor);
            guiGraphics.fill(x + w - 1, y, x + w, y + h, this.borderColor);

            innerX += 1;
            innerY += 1;
            innerW -= 2;
            innerH -= 2;
        }

        if (innerW <= 0 || innerH <= 0) {
            return;
        }

        int fillWidth = Mth.clamp((int) Math.round(innerW * this.progress), 0, innerW);
        if (fillWidth <= 0) {
            return;
        }

        if (this.useGradient) {
            guiGraphics.fillGradient(
                    innerX,
                    innerY,
                    innerX + fillWidth,
                    innerY + innerH,
                    this.gradientStartColor,
                    this.gradientEndColor
            );
        } else {
            guiGraphics.fill(
                    innerX,
                    innerY,
                    innerX + fillWidth,
                    innerY + innerH,
                    this.fillColor
            );
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
    }
}