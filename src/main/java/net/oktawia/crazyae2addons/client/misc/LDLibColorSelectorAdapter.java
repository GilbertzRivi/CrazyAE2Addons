package net.oktawia.crazyae2addons.client.misc;

import com.lowdragmc.lowdraglib.gui.widget.HsbColorWidget;
import java.util.function.IntConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class LDLibColorSelectorAdapter extends AbstractWidget {

    private static final int POPUP_W = 172;
    private static final int POPUP_H = 156;
    private static final int POPUP_PAD = 4;
    private static final float POPUP_Z = 800.0f;

    private final HsbColorWidget selector;

    @Nullable
    private Screen lastScreen;
    private int lastScreenWidth = Integer.MIN_VALUE;
    private int lastScreenHeight = Integer.MIN_VALUE;
    private int syncedPopupX = Integer.MIN_VALUE;
    private int syncedPopupY = Integer.MIN_VALUE;

    private int popupX = Integer.MIN_VALUE;
    private int popupY = Integer.MIN_VALUE;

    private boolean open = false;
    private int currentColor = 0xFFFFFFFF;
    private int lastCommittedColor = 0xFFFFFFFF;

    private Runnable onOpen = () -> {};
    private Runnable onClose = () -> {};
    private IntConsumer onColorCommitted = color -> {};

    private boolean pollingDragActive = false;
    private boolean changedSincePress = false;
    private int pollingDragButton = -1;
    private double lastDragMouseX = 0.0;
    private double lastDragMouseY = 0.0;

    public LDLibColorSelectorAdapter(int x, int y, int width, int height, Component narration) {
        super(x, y, width, height, narration);

        this.selector = new HsbColorWidget(0, 0, POPUP_W, POPUP_H)
                .setShowRGB(true)
                .setShowAlpha(true)
                .setOnChanged(color -> {
                    this.currentColor = color;
                    this.changedSincePress = true;
                });

        this.selector.setColor(this.currentColor);
        this.selector.setVisible(false);
        this.selector.setActive(false);
    }

    public LDLibColorSelectorAdapter setColor(int color) {
        this.currentColor = color;
        this.lastCommittedColor = color;
        this.selector.setColor(color);
        return this;
    }

    public int getColor() {
        return this.currentColor;
    }

    public LDLibColorSelectorAdapter setOnOpen(Runnable onOpen) {
        this.onOpen = onOpen == null ? () -> {} : onOpen;
        return this;
    }

    public LDLibColorSelectorAdapter setOnClose(Runnable onClose) {
        this.onClose = onClose == null ? () -> {} : onClose;
        return this;
    }

    public LDLibColorSelectorAdapter setOnColorCommitted(IntConsumer consumer) {
        this.onColorCommitted = consumer == null ? c -> {} : consumer;
        return this;
    }

    public boolean isPopupOpen() {
        return this.open;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible
                && mouseX >= this.getX()
                && mouseY >= this.getY()
                && mouseX < this.getX() + this.width
                && mouseY < this.getY() + this.height;
    }

    public boolean isPopupMouseOver(double mouseX, double mouseY) {
        return this.open
                && mouseX >= this.popupX
                && mouseY >= this.popupY
                && mouseX < this.popupX + POPUP_W
                && mouseY < this.popupY + POPUP_H;
    }

    public void closePopup(boolean commit) {
        if (!this.open) {
            return;
        }

        stopSyntheticDrag();

        if (commit) {
            commitCurrentColor();
        } else {
            this.currentColor = this.lastCommittedColor;
            this.selector.setColor(this.currentColor);
        }

        this.open = false;
        this.selector.setVisible(false);
        this.selector.setActive(false);
        super.setFocused(false);
        this.onClose.run();
    }

    public void tick() {
        if (!this.open) {
            return;
        }

        syncPopup();
        pollSyntheticDrag();
        this.selector.updateScreen();
    }

    public void removed() {
        closePopup(false);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        if (!this.open) {
            return;
        }

        syncPopup();
        this.selector.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible) {
            return false;
        }

        if (this.open) {
            syncPopup();

            if (isPopupMouseOver(mouseX, mouseY)) {
                this.selector.mouseClicked(mouseX, mouseY, button);
                super.setFocused(true);

                if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                    startSyntheticDrag(button, mouseX, mouseY);
                }

                return true;
            }

            if (isMouseOver(mouseX, mouseY)) {
                closePopup(true);
                return true;
            }

            closePopup(true);
            return true;
        }

        if (isMouseOver(mouseX, mouseY)) {
            this.onOpen.run();
            openPopup();
            return true;
        }

        return false;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (!this.open) {
            return false;
        }

        syncPopup();
        this.selector.mouseReleased(mouseX, mouseY, button);

        if (button == this.pollingDragButton) {
            stopSyntheticDrag();
            commitCurrentColor();
        }

        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (!this.open) {
            return false;
        }

        syncPopup();
        this.selector.mouseDragged(mouseX, mouseY, button, dragX, dragY);
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!this.open) {
            return false;
        }

        syncPopup();
        this.selector.mouseMoved(mouseX, mouseY);
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.open) {
            return false;
        }

        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            closePopup(false);
            return true;
        }

        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        return this.open;
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        int border = this.open ? 0xFFFFFFFF : 0xFF808080;
        graphics.fill(getX(), getY(), getX() + this.width, getY() + this.height, 0xFF202020);
        graphics.fill(getX(), getY(), getX() + this.width, getY() + 1, border);
        graphics.fill(getX(), getY() + this.height - 1, getX() + this.width, getY() + this.height, border);
        graphics.fill(getX(), getY(), getX() + 1, getY() + this.height, border);
        graphics.fill(getX() + this.width - 1, getY(), getX() + this.width, getY() + this.height, border);

        graphics.fill(getX() + 2, getY() + 2, getX() + this.width - 2, getY() + this.height - 2, this.currentColor);
    }

    public void renderOverlay(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible || !this.open) {
            return;
        }

        syncPopup();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, POPUP_Z);

        graphics.fill(this.popupX - 2, this.popupY - 2, this.popupX + POPUP_W + 2, this.popupY + POPUP_H + 2, 0xFF000000);
        graphics.fill(this.popupX - 1, this.popupY - 1, this.popupX + POPUP_W + 1, this.popupY + POPUP_H + 1, 0xFF101010);
        graphics.fill(this.popupX, this.popupY, this.popupX + POPUP_W, this.popupY + POPUP_H, 0xFF2A2A2A);

        this.selector.drawInBackground(graphics, mouseX, mouseY, partialTick);
        this.selector.drawInForeground(graphics, mouseX, mouseY, partialTick);

        graphics.pose().popPose();
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, getMessage());
    }

    private void openPopup() {
        this.open = true;
        this.selector.setColor(this.currentColor);
        this.selector.setVisible(true);
        this.selector.setActive(true);
        super.setFocused(true);
        syncPopup();
    }

    private void commitCurrentColor() {
        if (this.currentColor != this.lastCommittedColor) {
            this.lastCommittedColor = this.currentColor;
            this.onColorCommitted.accept(this.currentColor);
        }
        this.changedSincePress = false;
    }

    private void syncPopup() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return;
        }

        computePopupBounds(screen);

        boolean screenChanged =
                screen != this.lastScreen
                        || screen.width != this.lastScreenWidth
                        || screen.height != this.lastScreenHeight;

        boolean boundsChanged =
                this.popupX != this.syncedPopupX
                        || this.popupY != this.syncedPopupY;

        if (!screenChanged && !boundsChanged) {
            return;
        }

        this.lastScreen = screen;
        this.lastScreenWidth = screen.width;
        this.lastScreenHeight = screen.height;

        this.selector.setSelfPosition(this.popupX, this.popupY);
        this.selector.onScreenSizeUpdate(screen.width, screen.height);

        this.syncedPopupX = this.popupX;
        this.syncedPopupY = this.popupY;
    }

    private void computePopupBounds(Screen screen) {
        int minX = POPUP_PAD;
        int minY = POPUP_PAD;
        int maxX = Math.max(minX, screen.width - POPUP_W - POPUP_PAD);
        int maxY = Math.max(minY, screen.height - POPUP_H - POPUP_PAD);

        int preferredLeftX = this.getX() - POPUP_W - POPUP_PAD;
        int preferredRightX = this.getX() + this.width + POPUP_PAD;

        if (preferredLeftX >= minX) {
            this.popupX = preferredLeftX;
        } else if (preferredRightX <= maxX) {
            this.popupX = preferredRightX;
        } else {
            this.popupX = Mth.clamp(preferredLeftX, minX, maxX);
        }

        int preferredY = this.getY() + (this.height / 2) - (POPUP_H / 2);
        this.popupY = Mth.clamp(preferredY, minY, maxY);
    }

    private void startSyntheticDrag(int button, double mouseX, double mouseY) {
        this.pollingDragActive = true;
        this.changedSincePress = false;
        this.pollingDragButton = button;
        this.lastDragMouseX = mouseX;
        this.lastDragMouseY = mouseY;
    }

    private void stopSyntheticDrag() {
        this.pollingDragActive = false;
        this.pollingDragButton = -1;
        this.changedSincePress = false;
    }

    private void pollSyntheticDrag() {
        if (!this.open || !this.pollingDragActive || this.pollingDragButton < 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        long handle = mc.getWindow().getWindow();

        double rawX = mc.mouseHandler.xpos();
        double rawY = mc.mouseHandler.ypos();

        double guiX = rawX * mc.getWindow().getGuiScaledWidth() / mc.getWindow().getScreenWidth();
        double guiY = rawY * mc.getWindow().getGuiScaledHeight() / mc.getWindow().getScreenHeight();

        int buttonState = GLFW.glfwGetMouseButton(handle, this.pollingDragButton);

        syncPopup();
        this.selector.mouseMoved(guiX, guiY);

        if (buttonState == GLFW.GLFW_PRESS) {
            double dragX = guiX - this.lastDragMouseX;
            double dragY = guiY - this.lastDragMouseY;

            this.selector.mouseDragged(guiX, guiY, this.pollingDragButton, dragX, dragY);

            this.lastDragMouseX = guiX;
            this.lastDragMouseY = guiY;
        } else {
            this.selector.mouseReleased(guiX, guiY, this.pollingDragButton);
            stopSyntheticDrag();
            commitCurrentColor();
        }
    }
}