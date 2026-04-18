package net.oktawia.crazyae2addons.client.misc;

import com.lowdragmc.lowdraglib.gui.widget.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.CodeEditorWidget;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.Cursor;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.language.ILanguageDefinition;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.language.Languages;
import com.lowdragmc.lowdraglib.gui.widget.codeeditor.language.StyleManager;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.BooleanSupplier;

@OnlyIn(Dist.CLIENT)
public class LDLibCodeEditorAdapter extends AbstractWidget {

    private final ExposedCodeEditorWidget widget;

    private boolean pollingDragActive = false;
    private int pollingDragButton = -1;
    private double lastDragMouseX = 0.0;
    private double lastDragMouseY = 0.0;
    private int rememberedColorSelStart = -1;
    private int rememberedColorSelEnd = -1;

    public LDLibCodeEditorAdapter(int x, int y, int width, int height, Component placeholder) {
        super(x, y, width, height, placeholder);

        this.widget = new ExposedCodeEditorWidget(x, y, width, height, this::isFocused);
        this.widget.setLines(Arrays.asList(splitLines("")));
        syncWidgetBounds();
    }

    public LDLibCodeEditorAdapter setLanguage(@NotNull ILanguageDefinition language) {
        this.widget.codeEditor.setLanguageDefinition(language);
        return this;
    }

    public LDLibCodeEditorAdapter setStyleManager(@Nullable StyleManager styleManager) {
        StyleManager src = styleManager == null ? new StyleManager() : styleManager;
        StyleManager dst = this.widget.codeEditor.getStyleManager();
        dst.getStyleMap().clear();
        dst.getStyleMap().putAll(src.getStyleMap());
        dst.setDefaultStyle(src.getDefaultStyle());
        return this;
    }

    public CodeEditor getEditor() {
        return this.widget.codeEditor;
    }

    public String getValue() {
        return String.join("\n", this.widget.codeEditor.getLines());
    }

    public void setValue(@Nullable String value) {
        this.widget.setLines(Arrays.asList(splitLines(value)));
    }

    public void insertText(@Nullable String text) {
        this.widget.codeEditor.insertText(text == null ? "" : text);
    }

    public int getCursorPos() {
        String[] lines = splitLines(getValue());
        int line = this.widget.codeEditor.getCursor().line();
        int col = this.widget.codeEditor.getCursor().column();

        int flat = 0;
        for (int i = 0; i < line && i < lines.length; i++) {
            flat += lines[i].length();
            flat += 1;
        }
        return flat + col;
    }

    public boolean hasSelection() {
        return this.widget.hasSelectionPublic();
    }

    public void applySelectedTextColor(int rgb) {
        rgb &= 0xFFFFFF;

        String current = getValue();

        int start = -1;
        int end = -1;

        int[] selectionRange = this.widget.getSelectionRangePublic();
        if (selectionRange != null) {
            start = lineColToFlat(
                    this.widget.codeEditor.getLines().toArray(String[]::new),
                    selectionRange[0],
                    selectionRange[1]
            );
            end = lineColToFlat(
                    this.widget.codeEditor.getLines().toArray(String[]::new),
                    selectionRange[2],
                    selectionRange[3]
            );

            if (end < start) {
                int t = start;
                start = end;
                end = t;
            }

            if (end > start) {
                this.rememberedColorSelStart = start;
                this.rememberedColorSelEnd = end;
            }
        } else if (this.rememberedColorSelStart >= 0
                && this.rememberedColorSelEnd > this.rememberedColorSelStart
                && this.rememberedColorSelEnd <= current.length()) {
            start = this.rememberedColorSelStart;
            end = this.rememberedColorSelEnd;
        }

        if (start >= 0 && end > start) {
            ColorWrap wrap = findExactColorWrapAroundRange(current, start, end);
            if (wrap != null) {
                String updated = replaceColorWrap(current, wrap, rgb);
                setValue(updated);
                this.widget.setSelectionFlatPublic(updated, wrap.innerStart(), wrap.innerEnd());
                this.rememberedColorSelStart = wrap.innerStart();
                this.rememberedColorSelEnd = wrap.innerEnd();
                return;
            }

            String prefix = String.format(Locale.ROOT, "&c%06X(", rgb);
            String suffix = ")";

            String updated = current.substring(0, start)
                    + prefix
                    + current.substring(start, end)
                    + suffix
                    + current.substring(end);

            setValue(updated);

            int newStart = start + prefix.length();
            int newEnd = newStart + (end - start);
            this.widget.setSelectionFlatPublic(updated, newStart, newEnd);
            this.rememberedColorSelStart = newStart;
            this.rememberedColorSelEnd = newEnd;
            return;
        }

        int cursor = getCursorPos();
        ColorWrap wrap = findContainingColorWrapAtCursor(current, cursor);
        if (wrap != null) {
            String updated = replaceColorWrap(current, wrap, rgb);
            setValue(updated);
            int[] lc = flatToLineCol(updated, Mth.clamp(cursor, wrap.innerStart(), wrap.innerEnd()));
            this.widget.codeEditor.setCursor(lc[0], lc[1]);
        }
    }

    public void tick() {
        syncWidgetBounds();
        pollSyntheticDrag();
        this.widget.updateScreen();
    }

    public void removed() {
        stopSyntheticDrag();
    }

    public void invalidateLayout() {
        syncWidgetBounds();
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            stopSyntheticDrag();
        }
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return this.visible
                && mouseX >= this.getX()
                && mouseY >= this.getY()
                && mouseX < this.getX() + this.width
                && mouseY < this.getY() + this.height;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        syncWidgetBounds();
        this.widget.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }

        syncWidgetBounds();

        boolean handled = this.widget.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            setFocused(true);
            startSyntheticDrag(button, mouseX, mouseY);
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        syncWidgetBounds();

        boolean handled = this.widget.mouseReleased(mouseX, mouseY, button);
        if (button == this.pollingDragButton) {
            stopSyntheticDrag();
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        syncWidgetBounds();
        return this.widget.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        syncWidgetBounds();
        return this.widget.mouseWheelMove(mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }

        syncWidgetBounds();
        return this.widget.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }

        syncWidgetBounds();
        return this.widget.keyReleased(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused()) {
            return false;
        }

        syncWidgetBounds();
        return this.widget.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        syncWidgetBounds();
        this.widget.drawInBackground(guiGraphics, mouseX, mouseY, partialTick);
        this.widget.drawInForeground(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.translatable("narration.edit_box", getValue()));
        if (isFocused()) {
            out.add(NarratedElementType.USAGE, Component.translatable("narration.edit_box.usage"));
        }
    }

    private void syncWidgetBounds() {
        this.widget.setVisible(this.visible);
        this.widget.setActive(this.active);
        this.widget.setSelfPosition(getX(), getY());
        this.widget.setSize(this.width, this.height);
    }

    private void startSyntheticDrag(int button, double mouseX, double mouseY) {
        this.pollingDragActive = true;
        this.pollingDragButton = button;
        this.lastDragMouseX = mouseX;
        this.lastDragMouseY = mouseY;
    }

    private void stopSyntheticDrag() {
        this.pollingDragActive = false;
        this.pollingDragButton = -1;
    }

    private void pollSyntheticDrag() {
        if (!this.pollingDragActive || this.pollingDragButton < 0) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        Window window = mc.getWindow();
        long handle = window.getWindow();

        double rawX = mc.mouseHandler.xpos();
        double rawY = mc.mouseHandler.ypos();

        double guiX = rawX * window.getGuiScaledWidth() / window.getScreenWidth();
        double guiY = rawY * window.getGuiScaledHeight() / window.getScreenHeight();

        int buttonState = GLFW.glfwGetMouseButton(handle, this.pollingDragButton);

        syncWidgetBounds();
        this.widget.mouseMoved(guiX, guiY);

        if (buttonState == GLFW.GLFW_PRESS) {
            double dragX = guiX - this.lastDragMouseX;
            double dragY = guiY - this.lastDragMouseY;

            this.widget.mouseDragged(guiX, guiY, this.pollingDragButton, dragX, dragY);

            this.lastDragMouseX = guiX;
            this.lastDragMouseY = guiY;
        } else {
            this.widget.mouseReleased(guiX, guiY, this.pollingDragButton);
            stopSyntheticDrag();
        }
    }

    private record ColorWrap(int wrapperStart, int innerStart, int innerEnd, int closeParenIndex) {}

    @Nullable
    private static ColorWrap findExactColorWrapAroundRange(String text, int start, int end) {
        if (start < 0 || end < start || end > text.length()) {
            return null;
        }

        for (int i = 0; i + 9 < text.length(); i++) {
            if (!isTextColorWrapPrefixAt(text, i)) {
                continue;
            }

            int close = findMatchingParen(text, i + 8);
            if (close < 0) {
                continue;
            }

            int innerStart = i + 9;
            int innerEnd = close;

            if (innerStart == start && innerEnd == end) {
                return new ColorWrap(i, innerStart, innerEnd, close);
            }
        }

        return null;
    }

    @Nullable
    private static ColorWrap findContainingColorWrapAtCursor(String text, int cursor) {
        if (cursor < 0 || cursor > text.length()) {
            return null;
        }

        ColorWrap best = null;

        for (int i = 0; i + 9 < text.length(); i++) {
            if (!isTextColorWrapPrefixAt(text, i)) {
                continue;
            }

            int close = findMatchingParen(text, i + 8);
            if (close < 0) {
                continue;
            }

            int innerStart = i + 9;
            int innerEnd = close;

            if (cursor >= innerStart && cursor <= innerEnd) {
                if (best == null || innerStart >= best.innerStart()) {
                    best = new ColorWrap(i, innerStart, innerEnd, close);
                }
            }
        }

        return best;
    }

    private static String replaceColorWrap(String text, ColorWrap wrap, int rgb) {
        String prefix = String.format(Locale.ROOT, "&c%06X(", rgb & 0xFFFFFF);
        return text.substring(0, wrap.wrapperStart())
                + prefix
                + text.substring(wrap.innerStart(), wrap.innerEnd())
                + ")"
                + text.substring(wrap.closeParenIndex() + 1);
    }

    private static boolean isTextColorWrapPrefixAt(String text, int idx) {
        return idx >= 0
                && idx + 9 <= text.length()
                && text.charAt(idx) == '&'
                && (text.charAt(idx + 1) == 'c' || text.charAt(idx + 1) == 'C')
                && isHex6(text, idx + 2)
                && text.charAt(idx + 8) == '(';
    }

    private static boolean isHex6(String text, int idx) {
        if (idx < 0 || idx + 6 > text.length()) {
            return false;
        }

        for (int i = 0; i < 6; i++) {
            char ch = Character.toUpperCase(text.charAt(idx + i));
            if (!((ch >= '0' && ch <= '9') || (ch >= 'A' && ch <= 'F'))) {
                return false;
            }
        }

        return true;
    }

    private static int findMatchingParen(String text, int openParenIndex) {
        if (openParenIndex < 0 || openParenIndex >= text.length() || text.charAt(openParenIndex) != '(') {
            return -1;
        }

        int depth = 0;
        for (int i = openParenIndex + 1; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }

        return -1;
    }

    private static int lineColToFlat(String[] lines, int line, int col) {
        int flat = 0;
        int lim = Math.max(0, Math.min(line, lines.length));
        for (int i = 0; i < lim; i++) {
            flat += lines[i].length();
            flat += 1;
        }
        if (line >= 0 && line < lines.length) {
            flat += Math.max(0, Math.min(col, lines[line].length()));
        }
        return flat;
    }

    private static int[] flatToLineCol(String text, int flat) {
        int idx = Math.max(0, Math.min(flat, text.length()));
        int line = 0;
        int col = 0;

        for (int i = 0; i < idx; i++) {
            char c = text.charAt(i);
            if (c == '\n') {
                line++;
                col = 0;
            } else {
                col++;
            }
        }

        return new int[]{line, col};
    }

    private static String[] splitLines(@Nullable String text) {
        if (text == null || text.isEmpty()) {
            return new String[]{""};
        }
        return text.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
    }

    public void rememberSelectionForColorApply() {
        int[] range = this.widget.getSelectionRangePublic();
        if (range == null) {
            this.rememberedColorSelStart = -1;
            this.rememberedColorSelEnd = -1;
            return;
        }

        int start = lineColToFlat(
                this.widget.codeEditor.getLines().toArray(String[]::new),
                range[0],
                range[1]
        );
        int end = lineColToFlat(
                this.widget.codeEditor.getLines().toArray(String[]::new),
                range[2],
                range[3]
        );

        if (end < start) {
            int t = start;
            start = end;
            end = t;
        }

        if (end <= start) {
            this.rememberedColorSelStart = -1;
            this.rememberedColorSelEnd = -1;
            return;
        }

        this.rememberedColorSelStart = start;
        this.rememberedColorSelEnd = end;
    }

    private static final class ExposedCodeEditorWidget extends CodeEditorWidget {
        private final BooleanSupplier focusedSupplier;

        private ExposedCodeEditorWidget(int x, int y, int width, int height, BooleanSupplier focusedSupplier) {
            super(x, y, width, height);
            this.focusedSupplier = focusedSupplier;
        }

        @Override
        public boolean canConsumeInput() {
            return this.isVisible() && this.isActive() && this.focusedSupplier.getAsBoolean();
        }

        public boolean hasSelectionPublic() {
            return this.codeEditor.isSelectionValid();
        }

        @Nullable
        public int[] getSelectionRangePublic() {
            if (!this.codeEditor.isSelectionValid() || this.codeEditor.getSelection() == null) {
                return null;
            }
            return this.codeEditor.getSelection().getSelectionRange();
        }

        public void setSelectionFlatPublic(String text, int startFlat, int endFlat) {
            int[] a = flatToLineCol(text, startFlat);
            int[] b = flatToLineCol(text, endFlat);

            this.codeEditor.setCursor(new Cursor(a[0], a[1]));
            this.codeEditor.startSelection();
            this.codeEditor.setCursor(new Cursor(b[0], b[1]));
            this.codeEditor.updateSelection();
            this.codeEditor.endSelection();
        }
    }
}