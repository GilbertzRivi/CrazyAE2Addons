package net.oktawia.crazyae2addons.client.misc;

import com.lowdragmc.lowdraglib2.gui.ui.ModularUI;
import com.lowdragmc.lowdraglib2.gui.ui.UI;
import com.lowdragmc.lowdraglib2.gui.ui.UIElement;
import com.lowdragmc.lowdraglib2.gui.ui.data.Cursor;
import com.lowdragmc.lowdraglib2.gui.ui.data.ScrollerMode;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.ILanguageDefinition;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.StyleManager;
import com.mojang.blaze3d.platform.Window;
import dev.vfyjxf.taffy.style.TaffyPosition;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

@OnlyIn(Dist.CLIENT)
public class LDLibCodeEditorAdapter extends AbstractWidget {

    private final UIElement root;
    private final UIElement host;
    private final ExposedCodeEditor editor;
    private final ModularUI modularUI;
    private final GuiEventListener guiDelegate;
    private final Renderable renderDelegate;

    @Nullable
    private Screen lastScreen;
    private int lastScreenWidth = Integer.MIN_VALUE;
    private int lastScreenHeight = Integer.MIN_VALUE;

    private int syncedX = Integer.MIN_VALUE;
    private int syncedY = Integer.MIN_VALUE;
    private int syncedWidth = Integer.MIN_VALUE;
    private int syncedHeight = Integer.MIN_VALUE;

    private boolean pollingDragActive = false;
    private int pollingDragButton = -1;
    private double lastDragMouseX = 0.0;
    private double lastDragMouseY = 0.0;
    private int rememberedColorSelStart = -1;
    private int rememberedColorSelEnd = -1;

    public LDLibCodeEditorAdapter(int x, int y, int width, int height, Component placeholder) {
        super(x, y, width, height, placeholder);

        this.root = new UIElement();
        this.root.setFocusable(false);
        this.root.setOverflowVisible(false);

        this.host = new UIElement();
        this.host.setId("ae2_host");
        this.host.setFocusable(true);
        this.host.setOverflowVisible(false);

        this.editor = new ExposedCodeEditor();
        this.editor.setId("editor");
        this.editor.setFocusable(true);
        this.editor.layout(layout -> {
            layout.widthPercent(100);
            layout.heightPercent(100);
        });
        this.editor.textAreaStyle(style -> {
            style.placeholder(placeholder);
            style.viewMode(ScrollerMode.BOTH);
            style.fontSize(8.0f);
            style.lineSpacing(0.0f);
        });

        this.host.addChildren(this.editor);
        this.root.addChildren(this.host);

        this.modularUI = ModularUI.of(UI.of(this.root));

        var widget = this.modularUI.getWidget();
        this.guiDelegate = widget;
        this.renderDelegate = widget;
    }

    public LDLibCodeEditorAdapter setLanguage(@Nullable ILanguageDefinition language) {
        this.editor.setLanguage(language);
        return this;
    }

    public LDLibCodeEditorAdapter setStyleManager(@Nullable StyleManager styleManager) {
        this.editor.setStyleManager(styleManager == null ? StyleManager.DEFAULT : styleManager);
        return this;
    }

    public CodeEditor getEditor() {
        return this.editor;
    }

    public String getValue() {
        return String.join("\n", this.editor.getValue());
    }

    public void setValue(@Nullable String value) {
        this.editor.setValue(splitLines(value), true);
    }

    public void insertText(@Nullable String text) {
        this.editor.insertTextPublic(text);
    }

    public int getCursorPos() {
        String[] lines = this.editor.getValue();
        int line = this.editor.getCursorLine();
        int col = this.editor.getCursorCol();

        int flat = 0;
        for (int i = 0; i < line && i < lines.length; i++) {
            flat += lines[i].length();
            flat += 1;
        }
        return flat + col;
    }

    public boolean hasSelection() {
        return this.editor.hasSelectionPublic();
    }

    public void applySelectedTextColor(int rgb) {
        rgb &= 0xFFFFFF;

        String current = getValue();

        int start = -1;
        int end = -1;

        if (this.editor.hasSelectionPublic()) {
            start = lineColToFlat(
                    this.editor.getValue(),
                    this.editor.getSelStartLine(),
                    this.editor.getSelStartCol()
            );
            end = lineColToFlat(
                    this.editor.getValue(),
                    this.editor.getSelEndLine(),
                    this.editor.getSelEndCol()
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
                this.editor.setSelectionFlatPublic(updated, wrap.innerStart(), wrap.innerEnd());
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
            this.editor.setSelectionFlatPublic(updated, newStart, newEnd);
            this.rememberedColorSelStart = newStart;
            this.rememberedColorSelEnd = newEnd;
            return;
        }

        int cursor = getCursorPos();
        ColorWrap wrap = findContainingColorWrapAtCursor(current, cursor);
        if (wrap != null) {
            String updated = replaceColorWrap(current, wrap, rgb);
            setValue(updated);
            int[] lc = flatToLineCol(updated, Math.clamp(cursor, wrap.innerStart(), wrap.innerEnd()));
            this.editor.setCursor(lc[0], lc[1]);
            return;
        }
    }

    public void tick() {
        syncModularUI();
        pollSyntheticDrag();
        this.modularUI.tick();
    }

    public void removed() {
        stopSyntheticDrag();
        this.modularUI.onRemoved();
    }

    public void invalidateLayout() {
        this.syncedX = Integer.MIN_VALUE;
        this.syncedY = Integer.MIN_VALUE;
        this.syncedWidth = Integer.MIN_VALUE;
        this.syncedHeight = Integer.MIN_VALUE;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        syncModularUI();
        this.guiDelegate.setFocused(focused);

        if (focused) {
            this.modularUI.requestFocus(this.editor);
        } else {
            stopSyntheticDrag();
            this.modularUI.clearFocus();
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
        syncModularUI();
        this.guiDelegate.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!this.active || !this.visible || !isMouseOver(mouseX, mouseY)) {
            return false;
        }

        syncModularUI();
        this.guiDelegate.mouseMoved(mouseX, mouseY);

        boolean handled = this.guiDelegate.mouseClicked(mouseX, mouseY, button);
        if (handled) {
            setFocused(true);
            startSyntheticDrag(button, mouseX, mouseY);
        }
        return handled;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        syncModularUI();
        this.guiDelegate.mouseMoved(mouseX, mouseY);

        boolean handled = this.guiDelegate.mouseReleased(mouseX, mouseY, button);
        if (button == this.pollingDragButton) {
            stopSyntheticDrag();
        }
        return handled;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        syncModularUI();
        this.guiDelegate.mouseMoved(mouseX, mouseY);
        return this.guiDelegate.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!isMouseOver(mouseX, mouseY)) {
            return false;
        }

        syncModularUI();
        this.guiDelegate.mouseMoved(mouseX, mouseY);
        return this.guiDelegate.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!isFocused()) {
            return false;
        }

        syncModularUI();
        return this.guiDelegate.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!isFocused()) {
            return false;
        }

        syncModularUI();
        return this.guiDelegate.charTyped(codePoint, modifiers);
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!this.visible) {
            return;
        }

        syncModularUI();
        this.guiDelegate.mouseMoved(mouseX, mouseY);
        this.renderDelegate.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, Component.translatable("narration.edit_box", getValue()));
        if (isFocused()) {
            out.add(NarratedElementType.USAGE, Component.translatable("narration.edit_box.usage"));
        }
    }

    private void syncModularUI() {
        Screen screen = Minecraft.getInstance().screen;
        if (screen == null) {
            return;
        }

        boolean screenChanged =
                screen != this.lastScreen
                        || screen.width != this.lastScreenWidth
                        || screen.height != this.lastScreenHeight;

        boolean boundsChanged =
                getX() != this.syncedX
                        || getY() != this.syncedY
                        || this.width != this.syncedWidth
                        || this.height != this.syncedHeight;

        if (!screenChanged && !boundsChanged) {
            return;
        }

        this.lastScreen = screen;
        this.lastScreenWidth = screen.width;
        this.lastScreenHeight = screen.height;

        applyBounds(screen);

        this.modularUI.setScreen(screen);
        this.modularUI.init(screen.width, screen.height);

        this.syncedX = getX();
        this.syncedY = getY();
        this.syncedWidth = this.width;
        this.syncedHeight = this.height;
    }

    private void applyBounds(Screen screen) {
        final float screenW = (float) screen.width;
        final float screenH = (float) screen.height;

        final float x = (float) getX();
        final float y = (float) getY();
        final float w = (float) this.width;
        final float h = (float) this.height;

        this.root.layout(layout -> {
            layout.width(screenW);
            layout.height(screenH);
        });

        this.host.layout(layout -> {
            layout.positionType(TaffyPosition.ABSOLUTE);
            layout.left(x);
            layout.top(y);
            layout.width(w);
            layout.height(h);
        });

        this.root.markTaffyStyleDirty();
        this.host.markTaffyStyleDirty();
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

        syncModularUI();
        this.guiDelegate.mouseMoved(guiX, guiY);

        if (buttonState == GLFW.GLFW_PRESS) {
            double dragX = guiX - this.lastDragMouseX;
            double dragY = guiY - this.lastDragMouseY;

            this.guiDelegate.mouseDragged(guiX, guiY, this.pollingDragButton, dragX, dragY);

            this.lastDragMouseX = guiX;
            this.lastDragMouseY = guiY;
        } else {
            this.guiDelegate.mouseReleased(guiX, guiY, this.pollingDragButton);
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
        if (!this.editor.hasSelectionPublic()) {
            this.rememberedColorSelStart = -1;
            this.rememberedColorSelEnd = -1;
            return;
        }

        int start = lineColToFlat(
                this.editor.getValue(),
                this.editor.getSelStartLine(),
                this.editor.getSelStartCol()
        );
        int end = lineColToFlat(
                this.editor.getValue(),
                this.editor.getSelEndLine(),
                this.editor.getSelEndCol()
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

    private static final class ExposedCodeEditor extends CodeEditor {
        public void insertTextPublic(@Nullable String text) {
            super.insertText(text);
        }

        public boolean hasSelectionPublic() {
            return hasSelection();
        }

        public void wrapSelectionWithColorPublic(int rgb) {
            String prefix = String.format(Locale.ROOT, "&c%06X(", rgb & 0xFFFFFF);
            String suffix = ")";

            if (hasSelectionPublic()) {
                int sl = getSelStartLine();
                int sc = getSelStartCol();
                int el = getSelEndLine();
                int ec = getSelEndCol();

                if (sl > el || (sl == el && sc > ec)) {
                    int tsl = sl; sl = el; el = tsl;
                    int tsc = sc; sc = ec; ec = tsc;
                }

                String selected;
                if (sl == el) {
                    String line = lines.get(sl);
                    selected = line.substring(Math.min(sc, line.length()), Math.min(ec, line.length()));
                } else {
                    StringBuilder sb = new StringBuilder();
                    String first = lines.get(sl);
                    sb.append(first.substring(Math.min(sc, first.length()))).append('\n');
                    for (int i = sl + 1; i < el; i++) {
                        sb.append(lines.get(i)).append('\n');
                    }
                    String last = lines.get(el);
                    sb.append(last, 0, Math.min(ec, last.length()));
                    selected = sb.toString();
                }

                super.insertText(prefix + selected + suffix);
            } else {
                int line = getCursorLine();
                int col = getCursorCol();
                super.insertText(prefix + suffix);
                setCursor(line, col + prefix.length());
            }
        }

        public void setSelectionFlatPublic(String text, int startFlat, int endFlat) {
            int[] a = flatToLineCol(text, startFlat);
            int[] b = flatToLineCol(text, endFlat);
            setSelection(new Cursor(a[0], a[1]), new Cursor(b[0], b[1]));
            setCursor(b[0], b[1]);
        }
    }
}