package net.oktawia.crazyae2addons.misc;

import appeng.client.gui.Icon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.oktawia.crazyae2addons.datavariables.IFlowNode;
import net.oktawia.crazyae2addons.screens.DataflowPatternScreen;

import java.util.List;

public class NodeWidget extends AbstractWidget {
    private final Button editButton;
    public String label;
    public int color;
    private boolean editing = false;
    public final Class<? extends IFlowNode> nodeClass;
    private final Button deleteButton;
    public Integer index;
    public String name = "";
    public String settings = "";


    public NodeWidget(int x, int y, int width, int height, String label, Class<? extends IFlowNode> node, int bgColor, int index) {
        super(x, y, width, height, Component.literal(label));
        this.label = label;
        this.nodeClass = node;
        this.color = bgColor;
        this.index = index;

        this.editButton = Button.builder(Component.literal("🛠"), b -> {
                    if (Minecraft.getInstance().screen instanceof DataflowPatternScreen<?> screen) {
                        screen.openEditorFor(this);
                    }
                }).bounds(x + width - 14, y + 2, 12, 12)
                .tooltip(Tooltip.create(Component.literal("Configure node")))
                .build();

        this.deleteButton = Button.builder(Component.literal("x"), b -> {
            if (Minecraft.getInstance().screen instanceof DataflowPatternScreen<?> screen) {
                screen.removeNodeWidget(this);
                screen.getMenu().saveData(screen.serializeAllNodes());
            }
        }).bounds(x + 2, y + 2, 12, 12)
                .tooltip(Tooltip.create(Component.literal("Delete node")))
                .build();
    }

    private int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r = (int) (r * (1.0f - factor));
        g = (int) (g * (1.0f - factor));
        b = (int) (b * (1.0f - factor));

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        int bgColor = this.color;
        if (label.equalsIgnoreCase("(S) Entrypoint")) {
            bgColor = 0xFF168500;
        }

        guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, bgColor);

        int borderColor = darkenColor(bgColor, 0.2f);
        guiGraphics.fill(getX(), getY(), getX() + width, getY() + 1, borderColor);
        guiGraphics.fill(getX(), getY(), getX() + 1, getY() + height, borderColor);
        guiGraphics.fill(getX() + width - 1, getY(), getX() + width, getY() + height, borderColor);
        guiGraphics.fill(getX(), getY() + height - 1, getX() + width, getY() + height, borderColor);

        var font = Minecraft.getInstance().font;
        int maxTextWidth = getWidth() - 6;
        List<FormattedCharSequence> lines = font.split(Component.literal(getMessage().getString()), maxTextWidth);
        int totalHeight = lines.size() * font.lineHeight;
        int startY = getY() + (getHeight() - totalHeight) / 2;

        java.awt.Rectangle editorBounds = null;
        if (Minecraft.getInstance().screen instanceof DataflowPatternScreen<?> screen) {
            editorBounds = screen.getEditorBounds();
        }

        for (int i = 0; i < lines.size(); i++) {
            FormattedCharSequence line = lines.get(i);
            int lineWidth = font.width(line);
            int x = getX() + (getWidth() - lineWidth) / 2;
            int y = startY + i * font.lineHeight;

            if (editorBounds == null || !editorBounds.intersects(new java.awt.Rectangle(x, y, lineWidth, font.lineHeight))) {
                guiGraphics.drawString(font, line, x, y, 0xFFFFFF, false);
            }
        }

        int centerX = getX() + width / 2;
        int centerY = getY() + height - font.lineHeight;
        String number = String.valueOf(this.index);
        int numberWidth = font.width(number);
        int numberX = centerX - numberWidth / 2;
        if (editorBounds == null || !editorBounds.intersects(new java.awt.Rectangle(numberX, centerY, numberWidth, font.lineHeight))) {
            guiGraphics.drawString(font, number, numberX, centerY, 0xFFFFFF);
        }

        int editX = this.getX() + width - 14;
        int editY = this.getY() + 2;
        if (editorBounds == null || !editorBounds.intersects(new java.awt.Rectangle(editX, editY, 12, 12))) {
            editButton.setPosition(editX, editY);
            editButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }

        int delX = this.getX() + 2;
        int delY = this.getY() + 2;
        if (editorBounds == null || !editorBounds.intersects(new java.awt.Rectangle(delX, delY, 12, 12))) {
            deleteButton.setPosition(delX, delY);
            deleteButton.render(guiGraphics, mouseX, mouseY, partialTick);
        }
    }




    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return deleteButton.mouseClicked(mouseX, mouseY, button) || editButton.mouseClicked(mouseX, mouseY, button)|| super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
