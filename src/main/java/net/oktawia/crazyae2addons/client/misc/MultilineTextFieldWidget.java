package net.oktawia.crazyae2addons.client.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import lombok.Getter;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

@OnlyIn(Dist.CLIENT)
public class MultilineTextFieldWidget extends AbstractWidget {

    public static final int DEFAULT_MAX_LENGTH = Integer.MAX_VALUE;

    private static final int SCROLLBAR_THICKNESS = 3;
    private static final int SCROLLBAR_TRACK_COLOR = 0x40606060;
    private static final int SCROLLBAR_THUMB_COLOR = 0x80A0A0A0;
    private static final long DOUBLE_CLICK_MS = 250L;

    private enum ScrollbarDrag {
        NONE, VERTICAL, HORIZONTAL
    }

    public record HighlightRule(Pattern pattern, int color) {
        public HighlightRule(String regex, int color) {
            this(Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL), color);
        }
    }

    private record Line(int begin, int end) {}
    private record ColorScope(int tokenStart, int contentStart, int contentEnd, int tokenEnd) {}

    private final Font font;
    private final CachedTextField textField;

    @Getter
    private double scrollAmount;
    private double scrollX;
    private boolean dragging;
    private ScrollbarDrag scrollbarDrag = ScrollbarDrag.NONE;
    private double scrollbarDragMouseStart;
    private double scrollbarDragScrollStart;

    private List<HighlightRule> highlightRules = List.of();
    private int defaultTextColor = 0xFFFFFFFF;

    @Nullable
    private Consumer<String> onValueChanged;

    private String cachedHighlightSource = "";
    private int[] cachedHighlightColors = new int[0];

    private long lastClickTime = 0L;
    private int lastClickButton = -1;

    public MultilineTextFieldWidget(Font font, int x, int y, int w, int h, Component placeholder) {
        super(x, y, w, h, placeholder);
        this.font = font;
        this.textField = new CachedTextField(font, Integer.MAX_VALUE / 2);
        this.textField.setCharacterLimit(DEFAULT_MAX_LENGTH);
        this.textField.setCursorListener(this::onCursorMoved);
        this.textField.setValueListener(v -> {
            clampScroll();
            clampScrollX();
            invalidateHighlightCache();
            notifyValueChanged();
        });
    }

    public void setHighlightRules(List<HighlightRule> rules) {
        this.highlightRules = List.copyOf(rules);
        invalidateHighlightCache();
    }

    public void setDefaultTextColor(int color) {
        this.defaultTextColor = color;
        invalidateHighlightCache();
    }

    public void setOnValueChanged(@Nullable Consumer<String> onValueChanged) {
        this.onValueChanged = onValueChanged;
    }

    private void notifyValueChanged() {
        if (onValueChanged != null) {
            onValueChanged.accept(getValue());
        }
    }

    private void invalidateHighlightCache() {
        cachedHighlightSource = "";
        cachedHighlightColors = new int[0];
    }

    public String getValue() {
        return textField.value();
    }

    public void setValue(String v) {
        textField.setValue(v);
        clampScroll();
        clampScrollX();
        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    public void insertText(String text) {
        textField.insertText(text);
        clampScroll();
        clampScrollX();
        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    public void setScrollAmount(double a) {
        scrollAmount = Mth.clamp(a, 0, getMaxScroll());
    }

    public void setScrollAmountX(double a) {
        scrollX = Mth.clamp(a, 0, getMaxScrollX());
    }

    private int textAreaWidth() {
        return width - 4 - SCROLLBAR_THICKNESS;
    }

    private int textAreaHeight() {
        return height - 4 - SCROLLBAR_THICKNESS;
    }

    public double getMaxScroll() {
        int textH = textField.lineCount() * font.lineHeight;
        return Math.max(textH - textAreaHeight(), 0);
    }

    public double getMaxScrollX() {
        int maxW = 0;
        String val = textField.value();

        for (int i = 0; i < textField.lineCount(); i++) {
            Line ln = textField.line(i);
            int renderEnd = getRenderableLineEnd(val, ln);
            int w = 0;
            try {
                w = font.width(val.substring(ln.begin(), renderEnd));
            } catch (Exception ignored) {
            }
            if (w > maxW) {
                maxW = w;
            }
        }

        return Math.max(maxW - textAreaWidth(), 0);
    }

    public int getScrollStep() {
        return this.font.lineHeight;
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (!isFocused()) {
            return false;
        }

        if (textField.keyPressed(key) || Minecraft.getInstance().options.keyInventory.matches(key, sc)) {
            clampScroll();
            clampScrollX();
            ensureCursorVisible();
            ensureCursorVisibleX();
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int mods) {
        if (!isFocused()) {
            return false;
        }

        textField.insertText(String.valueOf(chr));
        clampScroll();
        clampScrollX();
        ensureCursorVisible();
        ensureCursorVisibleX();
        return true;
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        if (!isActive() || !isValidClickButton(btn) || !clicked(mx, my)) {
            return false;
        }

        if (getMaxScroll() > 0 && isOnVerticalScrollbar(mx, my)) {
            scrollbarDrag = ScrollbarDrag.VERTICAL;
            jumpScrollToMouseY(my);
            scrollbarDragScrollStart = scrollAmount;
            scrollbarDragMouseStart = my;
            return true;
        }

        if (getMaxScrollX() > 0 && isOnHorizontalScrollbar(mx, my)) {
            scrollbarDrag = ScrollbarDrag.HORIZONTAL;
            jumpScrollToMouseX(mx);
            scrollbarDragScrollStart = scrollX;
            scrollbarDragMouseStart = mx;
            return true;
        }

        Minecraft.getInstance().screen.setFocused(this);
        setFocused(true);

        long now = Util.getMillis();
        boolean isDoubleClick = btn == lastClickButton && (now - lastClickTime) <= DOUBLE_CLICK_MS;
        lastClickTime = now;
        lastClickButton = btn;

        if (isDoubleClick) {
            selectTokenAtMouse(mx, my);
            dragging = false;
            return true;
        }

        if (!hasShiftDown()) {
            this.textField.setSelecting(false);
        }

        moveCursorToMouse(mx, my);
        this.textField.setSelecting(true);
        dragging = true;
        return true;
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        dragging = false;
        scrollbarDrag = ScrollbarDrag.NONE;
        textField.setSelecting(false);
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && isFocused()) {
            moveCursorToMouse(mx, my);
            ensureCursorVisible();
            ensureCursorVisibleX();
            return true;
        }
        return false;
    }

    private void updateScrollbarDrag() {
        if (scrollbarDrag == ScrollbarDrag.NONE) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            scrollbarDrag = ScrollbarDrag.NONE;
            return;
        }

        double scale = mc.getWindow().getGuiScale();
        double[] rawX = new double[1];
        double[] rawY = new double[1];
        GLFW.glfwGetCursorPos(window, rawX, rawY);
        double mouseX = rawX[0] / scale;
        double mouseY = rawY[0] / scale;

        if (scrollbarDrag == ScrollbarDrag.VERTICAL) {
            double delta = mouseY - scrollbarDragMouseStart;
            double maxScrollY = getMaxScroll();
            int trackH = verticalTrackHeight();
            int thumbH = verticalThumbHeight(trackH);
            double ratio = (trackH - thumbH) > 0 ? maxScrollY / (trackH - thumbH) : 0;
            setScrollAmount(scrollbarDragScrollStart + delta * ratio);
        } else {
            double delta = mouseX - scrollbarDragMouseStart;
            double maxScrollXVal = getMaxScrollX();
            int trackW = horizontalTrackWidth();
            int thumbW = horizontalThumbWidth(trackW);
            double ratio = (trackW - thumbW) > 0 ? maxScrollXVal / (trackW - thumbW) : 0;
            setScrollAmountX(scrollbarDragScrollStart + delta * ratio);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my)) {
            return false;
        }

        if (hasShiftDown()) {
            setScrollAmountX(scrollX - delta * font.lineHeight);
        } else {
            setScrollAmount(scrollAmount - delta * font.lineHeight);
        }

        return true;
    }

    private void onCursorMoved() {
        clampScroll();
        clampScrollX();
        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    private void ensureCursorVisible() {
        int viewH = textAreaHeight();
        int caretLine = textField.lineAtCursor();
        int caretY = caretLine * font.lineHeight;

        double top = scrollAmount;
        double bottom = scrollAmount + viewH - font.lineHeight;

        if (caretY < top) {
            setScrollAmount(caretY);
        } else if (caretY > bottom) {
            setScrollAmount(caretY - (viewH - font.lineHeight));
        }
    }

    private void ensureCursorVisibleX() {
        int viewW = textAreaWidth();
        int curLine = textField.lineAtCursor();
        Line ln = textField.line(curLine);
        int caretX = 0;
        String text = textField.value();

        try {
            int renderEnd = getRenderableLineEnd(text, ln);
            int cursor = Math.min(textField.cursor(), renderEnd);
            caretX = font.width(text.substring(ln.begin(), cursor));
        } catch (Exception ignored) {
        }

        double left = scrollX;
        double right = scrollX + viewW - font.lineHeight;

        if (caretX < left) {
            setScrollAmountX(caretX);
        } else if (caretX > right) {
            setScrollAmountX(caretX - (viewW - font.lineHeight));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mX, int mY, float partial) {
        updateScrollbarDrag();
        RenderSystem.enableDepthTest();

        int bg = 0xFF202020;
        int border = isFocused() ? 0xFFFFFFFF : 0xFF808080;

        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.fill(getX(), getY(), getX() + width, getY() + 1, border);
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);
        g.fill(getX(), getY(), getX() + 1, getY() + height, border);
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);

        int clipL = getX() + 2;
        int clipT = getY() + 2;
        int clipR = getX() + 2 + textAreaWidth();
        int clipB = getY() + 2 + textAreaHeight();

        g.enableScissor(clipL, clipT, clipR, clipB);

        String fullText = textField.value();
        int[] colors = getHighlightColors(fullText);

        int firstLine = (int) (scrollAmount / font.lineHeight);
        int y = clipT - (int) scrollAmount + firstLine * font.lineHeight;
        int xBase = clipL - (int) scrollX;

        int selectionBegin = textField.hasSelection() ? textField.selection().begin() : -1;
        int selectionEnd = textField.hasSelection() ? textField.selection().end() : -1;
        int selectionColor = 0x80007FFF;

        for (int idx = firstLine; idx < textField.lineCount() && y <= clipB; idx++) {
            Line ln = textField.line(idx);
            int renderEnd = getRenderableLineEnd(fullText, ln);
            int xOff = xBase;

            if (textField.hasSelection()) {
                int lineStartChar = ln.begin();
                int lineEndChar = renderEnd;

                if (!(selectionEnd <= lineStartChar || selectionBegin >= lineEndChar)) {
                    int selStartInLine = Math.max(0, selectionBegin - lineStartChar);
                    int selEndInLine = Math.min(lineEndChar - lineStartChar, selectionEnd - lineStartChar);

                    if (selStartInLine < selEndInLine) {
                        String lineText = fullText.substring(lineStartChar, lineEndChar);
                        String preSel = lineText.substring(0, selStartInLine);
                        String selTxt = lineText.substring(selStartInLine, selEndInLine);
                        int selX = xOff + font.width(preSel);
                        int selW = font.width(selTxt);
                        g.fill(selX, y, selX + selW, y + font.lineHeight, selectionColor);
                    }
                }
            }

            if (renderEnd > ln.begin()) {
                drawHighlightedLine(g, fullText, colors, ln.begin(), renderEnd, xOff, y);
            }

            y += font.lineHeight;
        }

        if (isFocused() && blink()) {
            int curLine = textField.lineAtCursor();
            Line ln = textField.line(curLine);
            int renderEnd = getRenderableLineEnd(fullText, ln);
            int cursor = Math.min(textField.cursor(), renderEnd);
            int cx = xBase + font.width(fullText.substring(ln.begin(), cursor));
            int cy = clipT + curLine * font.lineHeight - (int) scrollAmount;

            if (cy >= clipT && cy < clipB && cx >= clipL && cx <= clipR) {
                g.fill(cx, cy, cx + 1, cy + font.lineHeight, 0xFFFFFFFF);
            }
        }

        g.disableScissor();

        if (textField.value().isEmpty() && !isFocused()) {
            g.drawString(font, getMessage(), clipL, clipT, 0xFF808080);
        }

        double maxScrollY = getMaxScroll();
        if (maxScrollY > 0) {
            int trackX = getX() + width - 1 - SCROLLBAR_THICKNESS;
            int trackT = getY() + 1;
            int trackB = getY() + height - 1 - SCROLLBAR_THICKNESS;
            int trackH = verticalTrackHeight();
            int thumbH = verticalThumbHeight(trackH);
            int thumbT = trackT + (int) ((trackH - thumbH) * scrollAmount / maxScrollY);

            g.fill(trackX, trackT, trackX + SCROLLBAR_THICKNESS, trackB, SCROLLBAR_TRACK_COLOR);
            g.fill(trackX, thumbT, trackX + SCROLLBAR_THICKNESS, thumbT + thumbH, SCROLLBAR_THUMB_COLOR);
        }

        double maxScrollXVal = getMaxScrollX();
        if (maxScrollXVal > 0) {
            int trackY = getY() + height - 1 - SCROLLBAR_THICKNESS;
            int trackL = getX() + 1;
            int trackR = getX() + width - 1 - SCROLLBAR_THICKNESS;
            int trackW = horizontalTrackWidth();
            int thumbW = horizontalThumbWidth(trackW);
            int thumbL = trackL + (int) ((trackW - thumbW) * scrollX / maxScrollXVal);

            g.fill(trackL, trackY, trackR, trackY + SCROLLBAR_THICKNESS, SCROLLBAR_TRACK_COLOR);
            g.fill(thumbL, trackY, thumbL + thumbW, trackY + SCROLLBAR_THICKNESS, SCROLLBAR_THUMB_COLOR);
        }
    }

    private int[] getHighlightColors(String text) {
        if (text.equals(cachedHighlightSource) && cachedHighlightColors.length == text.length()) {
            return cachedHighlightColors;
        }

        int[] colors = new int[text.length()];
        Arrays.fill(colors, defaultTextColor);

        for (HighlightRule rule : highlightRules) {
            Matcher matcher = rule.pattern().matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int end = matcher.end();
                if (start < 0 || end <= start) {
                    continue;
                }
                for (int i = start; i < end && i < colors.length; i++) {
                    colors[i] = rule.color();
                }
            }
        }

        cachedHighlightSource = text;
        cachedHighlightColors = colors;
        return colors;
    }

    private void drawHighlightedLine(GuiGraphics g, String fullText, int[] colors, int start, int end, int x, int y) {
        if (start < 0 || end <= start || end > fullText.length()) {
            return;
        }

        int runStart = start;
        int currentColor = colors[start];

        for (int i = start + 1; i < end; i++) {
            if (colors[i] != currentColor) {
                String part = fullText.substring(runStart, i);
                g.drawString(font, part, x, y, currentColor);
                x += font.width(part);
                runStart = i;
                currentColor = colors[i];
            }
        }

        String lastPart = fullText.substring(runStart, end);
        g.drawString(font, lastPart, x, y, currentColor);
    }

    private int getRenderableLineEnd(String fullText, Line ln) {
        int renderEnd = ln.end();
        if (renderEnd > ln.begin() && renderEnd <= fullText.length() && fullText.charAt(renderEnd - 1) == '\n') {
            renderEnd--;
        }
        return renderEnd;
    }

    private void selectTokenAtMouse(double mx, double my) {
        String fullText = textField.value();
        if (fullText.isEmpty() || textField.lineCount() <= 0) {
            return;
        }

        int lineIndex = getLineIndexAtMouse(my);
        Line ln = textField.line(lineIndex);
        int renderEnd = getRenderableLineEnd(fullText, ln);
        if (renderEnd <= ln.begin()) {
            return;
        }

        String lineText = fullText.substring(ln.begin(), renderEnd);
        int column = getColumnFromMouse(lineText, mx);

        if (column >= lineText.length()) {
            column = lineText.length() - 1;
        }
        if (column < 0) {
            return;
        }

        int start = column;
        int end = column;

        if (isTokenChar(lineText.charAt(column))) {
            while (start > 0 && isTokenChar(lineText.charAt(start - 1))) {
                start--;
            }
            while (end + 1 < lineText.length() && isTokenChar(lineText.charAt(end + 1))) {
                end++;
            }
            end++;
        } else {
            end = Math.min(column + 1, lineText.length());
        }

        double yWithinContent = lineIndex * font.lineHeight + (font.lineHeight / 2.0);
        double startX = font.width(lineText.substring(0, start));
        double endX = font.width(lineText.substring(0, end));

        textField.setSelecting(false);
        moveCursorToContent(startX, yWithinContent);
        textField.setSelecting(true);
        moveCursorToContent(endX, yWithinContent);
        textField.setSelecting(false);

        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    private int getLineIndexAtMouse(double my) {
        double relY = my - (getY() + 2) + scrollAmount;
        return Mth.clamp((int) (relY / font.lineHeight), 0, Math.max(0, textField.lineCount() - 1));
    }

    private int getColumnFromMouse(String lineText, double mx) {
        double relX = mx - (getX() + 2) + scrollX;
        if (lineText.isEmpty()) {
            return 0;
        }

        int bestCol = lineText.length();
        int bestDist = Integer.MAX_VALUE;

        for (int i = 0; i <= lineText.length(); i++) {
            int width = font.width(lineText.substring(0, i));
            int dist = Math.abs((int) relX - width);
            if (dist < bestDist) {
                bestDist = dist;
                bestCol = i;
            }
        }

        return bestCol;
    }

    private boolean isTokenChar(char c) {
        return !Character.isWhitespace(c)
                && "[](){},".indexOf(c) < 0;
    }

    private void moveCursorToMouse(double mx, double my) {
        double relX = mx - (getX() + 2) + scrollX;
        double relY = my - (getY() + 2) + scrollAmount;
        textField.seekCursorToPoint(relX, relY);
    }

    private void moveCursorToContent(double relX, double relY) {
        textField.seekCursorToPoint(relX, relY);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, getMessage());
    }

    private static final class CachedTextField extends MultilineTextField {
        private List<Line> cache = new ArrayList<>();

        record Selection(int begin, int end) {}

        CachedTextField(Font font, int w) {
            super(font, w);
            rebuild();
        }

        int lineCount() {
            return cache.size();
        }

        Line line(int idx) {
            return cache.get(Mth.clamp(idx, 0, cache.size() - 1));
        }

        int lineAtCursor() {
            return super.getLineAtCursor();
        }

        public boolean hasSelection() {
            return super.hasSelection();
        }

        Selection selection() {
            var sv = super.getSelected();
            int a = sv.beginIndex();
            int b = sv.endIndex();
            return new Selection(Math.min(a, b), Math.max(a, b));
        }

        @Override
        public void setValue(String v) {
            super.setValue(v);
            rebuild();
        }

        @Override
        public void insertText(String t) {
            super.insertText(t);
            rebuild();
        }

        private void rebuild() {
            if (cache == null) {
                cache = new ArrayList<>();
            }
            cache.clear();
            super.iterateLines().forEach(sv -> cache.add(new Line(sv.beginIndex(), sv.endIndex())));
        }
    }

    private boolean isOnVerticalScrollbar(double mx, double my) {
        int trackX = getX() + width - 1 - SCROLLBAR_THICKNESS;
        int trackT = getY() + 1;
        int trackB = getY() + height - 1 - SCROLLBAR_THICKNESS;
        return mx >= trackX && mx < trackX + SCROLLBAR_THICKNESS && my >= trackT && my < trackB;
    }

    private boolean isOnHorizontalScrollbar(double mx, double my) {
        int trackY = getY() + height - 1 - SCROLLBAR_THICKNESS;
        int trackL = getX() + 1;
        int trackR = getX() + width - 1 - SCROLLBAR_THICKNESS;
        return my >= trackY && my < trackY + SCROLLBAR_THICKNESS && mx >= trackL && mx < trackR;
    }

    private int verticalTrackHeight() {
        return (getY() + height - 1 - SCROLLBAR_THICKNESS) - (getY() + 1);
    }

    private int verticalThumbHeight(int trackH) {
        int totalH = textField.lineCount() * font.lineHeight;
        return Math.max(4, trackH * textAreaHeight() / Math.max(1, totalH));
    }

    private int horizontalTrackWidth() {
        return (getX() + width - 1 - SCROLLBAR_THICKNESS) - (getX() + 1);
    }

    private int horizontalThumbWidth(int trackW) {
        int totalW = (int) (getMaxScrollX() + textAreaWidth());
        return Math.max(4, trackW * textAreaWidth() / Math.max(1, totalW));
    }

    private void jumpScrollToMouseY(double my) {
        int trackT = getY() + 1;
        int trackH = verticalTrackHeight();
        int thumbH = verticalThumbHeight(trackH);
        double ratio = (trackH - thumbH) > 0 ? (my - trackT - thumbH / 2.0) / (trackH - thumbH) : 0;
        setScrollAmount(ratio * getMaxScroll());
    }

    private void jumpScrollToMouseX(double mx) {
        int trackL = getX() + 1;
        int trackW = horizontalTrackWidth();
        int thumbW = horizontalThumbWidth(trackW);
        double ratio = (trackW - thumbW) > 0 ? (mx - trackL - thumbW / 2.0) / (trackW - thumbW) : 0;
        setScrollAmountX(ratio * getMaxScrollX());
    }

    private void clampScroll() {
        setScrollAmount(scrollAmount);
    }

    private void clampScrollX() {
        setScrollAmountX(scrollX);
    }

    private boolean blink() {
        return (Util.getMillis() / 500) % 2 == 0;
    }

    public int getCursorPos() {
        return textField.cursor();
    }

    public boolean hasSelectionRange() {
        return textField.hasSelection();
    }

    public int getSelectionStart() {
        if (!textField.hasSelection()) {
            return textField.cursor();
        }
        return textField.selection().begin();
    }

    public int getSelectionEnd() {
        if (!textField.hasSelection()) {
            return textField.cursor();
        }
        return textField.selection().end();
    }

    public void insertAtCursor(int cursorPos, String text) {
        String current = getValue();
        int pos = Mth.clamp(cursorPos, 0, current.length());
        String updated = current.substring(0, pos) + text + current.substring(pos);
        setValue(updated);
        moveCursorToIndex(pos + text.length());
        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    public void applySelectedTextColor(int argb) {
        String text = getValue();
        if (text.isEmpty() || !textField.hasSelection()) {
            return;
        }

        CachedTextField.Selection selection = textField.selection();
        int begin = Mth.clamp(selection.begin(), 0, text.length());
        int end = Mth.clamp(selection.end(), 0, text.length());

        if (end <= begin) {
            return;
        }

        String hex = String.format(Locale.ROOT, "%06X", argb & 0xFFFFFF);
        String colorToken = "&c" + hex;

        ColorScope wrapped = findWrappingColorScope(text, begin, end);

        int replaceStart;
        int replaceEnd;
        String innerText;

        if (wrapped != null) {
            replaceStart = wrapped.tokenStart();
            replaceEnd = wrapped.tokenEnd();
            innerText = text.substring(wrapped.contentStart(), wrapped.contentEnd());
        } else {
            replaceStart = begin;
            replaceEnd = end;
            innerText = text.substring(begin, end);
        }

        String replacement = colorToken + "(" + innerText + ")";
        String updated = text.substring(0, replaceStart) + replacement + text.substring(replaceEnd);

        setValue(updated);

        int newSelStart = replaceStart + colorToken.length() + 1;
        int newSelEnd = newSelStart + innerText.length();

        setSelectionRange(newSelStart, newSelEnd);
        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    private void setSelectionRange(int begin, int end) {
        String text = textField.value();
        int len = text.length();

        begin = Mth.clamp(begin, 0, len);
        end = Mth.clamp(end, 0, len);

        if (end < begin) {
            int tmp = begin;
            begin = end;
            end = tmp;
        }

        textField.setSelecting(false);
        moveCursorToIndex(begin);
        textField.setSelecting(true);
        moveCursorToIndex(end);
        textField.setSelecting(false);

        setFocused(true);
    }

    private void moveCursorToIndex(int index) {
        String text = textField.value();
        int len = text.length();

        index = Mth.clamp(index, 0, len);

        if (textField.lineCount() <= 0) {
            return;
        }

        int targetLine = 0;
        Line target = textField.line(0);

        for (int i = 0; i < textField.lineCount(); i++) {
            Line ln = textField.line(i);
            int renderEnd = getRenderableLineEnd(text, ln);

            if (index >= ln.begin() && index <= renderEnd) {
                targetLine = i;
                target = ln;
                break;
            }

            if (i == textField.lineCount() - 1) {
                targetLine = i;
                target = ln;
            }
        }

        int renderEnd = getRenderableLineEnd(text, target);
        int clampedIndex = Mth.clamp(index, target.begin(), renderEnd);

        double relX = font.width(text.substring(target.begin(), clampedIndex));
        double relY = targetLine * font.lineHeight + font.lineHeight / 2.0;

        textField.seekCursorToPoint(relX, relY);
    }

    @Nullable
    private ColorScope findWrappingColorScope(String text, int begin, int end) {
        if (begin < 9 || end > text.length()) {
            return null;
        }

        int tokenStart = begin - 9;
        int openParen = begin - 1;

        if (openParen < 0 || text.charAt(openParen) != '(') {
            return null;
        }

        if (!isColorTokenAt(text, tokenStart)) {
            return null;
        }

        int closeParen = findMatchingParen(text, openParen);
        if (closeParen < 0) {
            return null;
        }

        if (closeParen != end) {
            return null;
        }

        return new ColorScope(tokenStart, begin, end, closeParen + 1);
    }

    private boolean isColorTokenAt(String text, int index) {
        if (index < 0 || index + 9 > text.length()) {
            return false;
        }

        if (text.charAt(index) != '&') {
            return false;
        }

        char c = text.charAt(index + 1);
        if (c != 'c' && c != 'C') {
            return false;
        }

        for (int i = 0; i < 6; i++) {
            char ch = text.charAt(index + 2 + i);
            boolean hex =
                    (ch >= '0' && ch <= '9')
                            || (ch >= 'a' && ch <= 'f')
                            || (ch >= 'A' && ch <= 'F');
            if (!hex) {
                return false;
            }
        }

        return text.charAt(index + 8) == '(';
    }

    private int findMatchingParen(String text, int openParenIndex) {
        if (openParenIndex < 0 || openParenIndex >= text.length() || text.charAt(openParenIndex) != '(') {
            return -1;
        }

        int depth = 0;
        for (int i = openParenIndex + 1; i < text.length(); i++) {
            char ch = text.charAt(i);

            if (ch == '(') {
                depth++;
            } else if (ch == ')') {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }

        return -1;
    }
}