package net.oktawia.crazyae2addons.misc;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemTextButtonBarWidget extends AbstractWidget {

    private ItemStack itemStack;
    private Component centerText;
    private final Button rightButton;

    private boolean enabled = true;

    private int centerTextColor = 0xFFFFFF;

    public ItemTextButtonBarWidget(
            int x, int y, int width, int height,
            ItemStack stack,
            Component text,
            Button rightButton
    ) {
        super(x, y, width, height, text);
        this.itemStack = stack;
        this.centerText = text;
        this.rightButton = rightButton;

        updateButtonPosition();
    }

    public void setBarPosition(int x, int y) {
        this.setX(x);
        this.setY(y);
        updateButtonPosition();
    }

    public void moveBarBy(int dx, int dy) {
        this.setX(this.getX() + dx);
        this.setY(this.getY() + dy);
        updateButtonPosition();
    }

    public void setBarEnabled(boolean enabled) {
        this.enabled = enabled;
        this.active = enabled;
        this.visible = enabled;
    }

    public boolean isBarEnabled() {
        return this.enabled;
    }

    public void setItemStack(ItemStack stack) {
        this.itemStack = stack;
    }

    public void setCenterText(Component text) {
        this.centerText = text;
        this.setMessage(text);
    }

    // ---------- NOWE: obsługa koloru tekstu ----------

    /** Ustawia kolor tekstu w formacie 0xRRGGBB lub 0xAARRGGBB */
    public void setCenterTextColor(int color) {
        this.centerTextColor = color;
    }

    public int getCenterTextColor() {
        return this.centerTextColor;
    }

    /** Helper: ustawienie koloru z RGB (alpha = 255) */
    public void setCenterTextColor(int r, int g, int b) {
        this.centerTextColor =
                (0xFF << 24) |
                        ((r & 0xFF) << 16) |
                        ((g & 0xFF) << 8)  |
                        (b & 0xFF);
    }

    // --------------------------------------------------

    private void updateButtonPosition() {
        if (rightButton == null) {
            return;
        }

        int buttonWidth = rightButton.getWidth();
        int buttonHeight = rightButton.getHeight();

        int buttonX = this.getX() + this.width - buttonWidth - 4;
        int buttonY = this.getY() + (this.height - buttonHeight) / 2;

        rightButton.setPosition(buttonX, buttonY);
    }

    private int getSlotX() {
        return this.getX() + 4;
    }

    private int getSlotY() {
        return this.getY() + (this.height - 16) / 2;
    }

    private boolean isMouseOverSlot(double mouseX, double mouseY) {
        int sx = getSlotX();
        int sy = getSlotY();
        return mouseX >= sx && mouseX < sx + 16
                && mouseY >= sy && mouseY < sy + 16;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || !this.enabled) {
            return;
        }

        int x = this.getX();
        int y = this.getY();

        // Tło paska
        int bgColor = 0xAA909090;
        graphics.fill(x, y, x + this.width, y + this.height, bgColor);

        int outlineColor = this.isHoveredOrFocused() ? 0xFFFFFFFF : 0xFF555555;
        graphics.fill(x, y, x + this.width, y + 1, outlineColor);
        graphics.fill(x, y + this.height - 1, x + this.width, y + this.height, outlineColor);
        graphics.fill(x, y, x + 1, y + this.height, outlineColor);
        graphics.fill(x + this.width - 1, y, x + this.width, y + this.height, outlineColor);

        Font font = Minecraft.getInstance().font;

        int slotX = getSlotX();
        int slotY = getSlotY();
        graphics.fill(slotX - 2, slotY - 2, slotX + 18, slotY + 18, 0xFF777777);
        graphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFFDDDDDD);

        if (itemStack != null && !itemStack.isEmpty()) {
            graphics.renderItem(itemStack, slotX, slotY);
            graphics.renderItemDecorations(font, itemStack, slotX, slotY);
        }

        // Tekst na środku paska (TERAZ z centerTextColor)
        if (centerText != null) {
            int textWidth = font.width(centerText);
            int textX = x + this.width / 2 - textWidth / 2;
            int textY = y + (this.height - font.lineHeight) / 2;
            graphics.drawString(font, centerText, textX, textY, centerTextColor, false);
        }

        // Guzik po prawej (przekazany z zewnątrz)
        if (rightButton != null) {
            updateButtonPosition();
            rightButton.render(graphics, mouseX, mouseY, partialTick);
        }

        // Tooltip dla itemu jak w inventory
        if (itemStack != null && !itemStack.isEmpty() && isMouseOverSlot(mouseX, mouseY)) {
            graphics.renderTooltip(font, itemStack, mouseX, mouseY);
        }
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.enabled || !this.visible) {
            return false;
        }

        if (rightButton != null && rightButton.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }

        // sam pasek nie ma akcji na klik
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (rightButton != null && rightButton.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (rightButton != null) {
            rightButton.mouseMoved(mouseX, mouseY);
        }
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narration) {
    }
}
