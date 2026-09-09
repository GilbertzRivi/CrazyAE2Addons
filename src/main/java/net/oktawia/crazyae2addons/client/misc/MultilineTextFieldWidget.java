package net.oktawia.crazyae2addons.client.misc;

import static net.minecraft.client.gui.screens.Screen.hasControlDown;
import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.blaze3d.systems.RenderSystem;

import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.MultilineTextField;
import net.minecraft.client.gui.components.Whence;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import lombok.Getter;

@OnlyIn(Dist.CLIENT)
public class MultilineTextFieldWidget extends AbstractWidget {

    public static final int DEFAULT_MAX_LENGTH = Integer.MAX_VALUE;

    private static final int SCROLLBAR_THICKNESS = 3;
    private static final int SCROLLBAR_TRACK_COLOR = 0x40606060;
    private static final int SCROLLBAR_THUMB_COLOR = 0x80A0A0A0;
    private static final long DOUBLE_CLICK_MS = 250L;
    private static final char ASCII_START = ' ';
    private static final char ASCII_END = 127;

    private enum ScrollbarDrag {
        NONE, VERTICAL, HORIZONTAL
    }

    public record HighlightRule(Pattern pattern, int color) {
        public HighlightRule(String regex, int color) {
            this(Pattern.compile(regex, Pattern.MULTILINE | Pattern.DOTALL), color);
        }
    }

    private record Line(int begin, int end) {
    }

    private record ColorScope(int tokenStart, int contentStart, int contentEnd, int tokenEnd) {
    }

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

    @Nullable
    private Function<String, List<HighlightRule>> dynamicHighlightRules;

    private int defaultTextColor = 0xFFFFFFFF;
    private float textScale = 1.0f;
    private Style textStyle = Style.EMPTY;
    private Component[] asciiGlyphs = new Component[ASCII_END - ASCII_START];
    private int[] glyphOffsets = new int[ASCII_END - ASCII_START];
    private int cellAdvance;
    private boolean monospace;

    @Nullable
    private Consumer<String> onValueChanged;

    private String cachedHighlightSource = "";
    private int[] cachedHighlightColors = new int[0];

    private long lastClickTime = 0L;
    private int lastClickButton = -1;
    private int clickCount = 0;

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

    public void setDynamicHighlightRules(@Nullable Function<String, List<HighlightRule>> rules) {
        this.dynamicHighlightRules = rules;
        invalidateHighlightCache();
    }

    public void setDefaultTextColor(int color) {
        this.defaultTextColor = color;
        invalidateHighlightCache();
    }

    public void setTextScale(float scale) {
        this.textScale = Mth.clamp(scale, 0.25f, 2.0f);
        clampScroll();
        clampScrollX();
    }

    public void setMonospace(boolean monospace) {
        this.monospace = monospace;
        clampScrollX();
    }

    public void setFontId(@Nullable ResourceLocation fontId) {
        this.textStyle = fontId == null ? Style.EMPTY : Style.EMPTY.withFont(fontId);
        this.asciiGlyphs = new Component[ASCII_END - ASCII_START];
        this.glyphOffsets = new int[ASCII_END - ASCII_START];
        this.cellAdvance = 0;
        clampScrollX();
    }

    private float lineHeight() {
        return font.lineHeight * textScale;
    }

    private Component styled(String s) {
        return Component.literal(s).withStyle(textStyle);
    }

    private Component glyph(char c) {
        if (c < ASCII_START || c >= ASCII_END) {
            return styled(String.valueOf(c));
        }

        Component cached = asciiGlyphs[c - ASCII_START];
        if (cached == null) {
            cached = styled(String.valueOf(c));
            asciiGlyphs[c - ASCII_START] = cached;
        }

        return cached;
    }

    private int cellAdvance() {
        if (cellAdvance <= 0) {
            int widest = 1;
            for (char c = ASCII_START; c < ASCII_END; c++) {
                widest = Math.max(widest, font.width(glyph(c)));
            }

            cellAdvance = widest;

            for (char c = ASCII_START; c < ASCII_END; c++) {
                glyphOffsets[c - ASCII_START] = centeringOffset(c);
            }
        }

        return cellAdvance;
    }

    private int centeringOffset(char c) {
        return Math.max(0, (cellAdvance - Math.max(0, font.width(glyph(c)) - 1)) / 2);
    }

    private int glyphOffset(char c) {
        cellAdvance();
        return c < ASCII_START || c >= ASCII_END ? centeringOffset(c) : glyphOffsets[c - ASCII_START];
    }

    private float cellWidth() {
        return cellAdvance() * textScale;
    }

    private float columnX(String lineText, int columns) {
        int clamped = Mth.clamp(columns, 0, lineText.length());

        return monospace
                ? clamped * cellWidth()
                : font.width(styled(lineText.substring(0, clamped))) * textScale;
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
        float textH = textField.lineCount() * lineHeight();
        return Math.max(textH - textAreaHeight(), 0);
    }

    public double getMaxScrollX() {
        float maxW = 0f;
        String val = textField.value();

        for (int i = 0; i < textField.lineCount(); i++) {
            Line ln = textField.line(i);
            String lineText = val.substring(ln.begin(), getRenderableLineEnd(val, ln));
            maxW = Math.max(maxW, columnX(lineText, lineText.length()));
        }

        return Math.max(maxW - textAreaWidth(), 0);
    }

    public int getScrollStep() {
        return Math.max(1, Math.round(lineHeight()));
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
        boolean chained = btn == lastClickButton && (now - lastClickTime) <= DOUBLE_CLICK_MS;
        clickCount = chained ? Math.min(clickCount + 1, 3) : 1;
        lastClickTime = now;
        lastClickButton = btn;

        if (clickCount >= 3) {
            selectLineAtMouse(my);
            dragging = false;
            return true;
        }

        if (clickCount == 2) {
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
            setScrollAmountX(scrollX - delta * getScrollStep());
        } else {
            setScrollAmount(scrollAmount - delta * getScrollStep());
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
        float caretY = caretLine * lineHeight();

        double top = scrollAmount;
        double bottom = scrollAmount + viewH - lineHeight();

        if (caretY < top) {
            setScrollAmount(caretY);
        } else if (caretY > bottom) {
            setScrollAmount(caretY - (viewH - lineHeight()));
        }
    }

    private void ensureCursorVisibleX() {
        int viewW = textAreaWidth();
        int curLine = textField.lineAtCursor();
        Line ln = textField.line(curLine);
        String text = textField.value();
        int renderEnd = getRenderableLineEnd(text, ln);
        String lineText = text.substring(ln.begin(), renderEnd);
        float caretX = columnX(lineText, Math.min(textField.cursor(), renderEnd) - ln.begin());

        double left = scrollX;
        double right = scrollX + viewW - lineHeight();

        if (caretX < left) {
            setScrollAmountX(caretX);
        } else if (caretX > right) {
            setScrollAmountX(caretX - (viewW - lineHeight()));
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

        int firstLine = (int) (scrollAmount / lineHeight());
        float y = clipT - (float) scrollAmount + firstLine * lineHeight();
        float xBase = clipL - (float) scrollX;

        int selectionBegin = textField.hasSelection() ? textField.selection().begin() : -1;
        int selectionEnd = textField.hasSelection() ? textField.selection().end() : -1;
        int selectionColor = 0x80007FFF;

        for (int idx = firstLine; idx < textField.lineCount() && y <= clipB; idx++) {
            Line ln = textField.line(idx);
            int renderEnd = getRenderableLineEnd(fullText, ln);

            g.pose().pushPose();
            g.pose().translate(Math.round(xBase), Math.round(y), 0f);
            g.pose().scale(textScale, textScale, 1f);

            if (textField.hasSelection()) {
                int lineStartChar = ln.begin();
                int lineEndChar = renderEnd;

                if (!(selectionEnd <= lineStartChar || selectionBegin >= lineEndChar)) {
                    int selStartInLine = Math.max(0, selectionBegin - lineStartChar);
                    int selEndInLine = Math.min(lineEndChar - lineStartChar, selectionEnd - lineStartChar);

                    if (selStartInLine < selEndInLine) {
                        String lineText = fullText.substring(lineStartChar, lineEndChar);
                        int selX = rawColumnX(lineText, selStartInLine);
                        int selW = rawColumnX(lineText, selEndInLine) - selX;
                        g.fill(selX, 0, selX + selW, font.lineHeight, selectionColor);
                    }
                }
            }

            if (renderEnd > ln.begin()) {
                drawHighlightedLine(g, fullText, colors, ln.begin(), renderEnd);
            }

            g.pose().popPose();

            y += lineHeight();
        }

        if (isFocused() && blink()) {
            int curLine = textField.lineAtCursor();
            Line ln = textField.line(curLine);
            int renderEnd = getRenderableLineEnd(fullText, ln);
            int cursor = Math.min(textField.cursor(), renderEnd);
            float cx = xBase + columnX(fullText.substring(ln.begin(), renderEnd), cursor - ln.begin());
            float cy = clipT + curLine * lineHeight() - (float) scrollAmount;

            if (cy >= clipT && cy < clipB && cx >= clipL && cx <= clipR) {
                g.fill(Math.round(cx), Math.round(cy), Math.round(cx) + 1, Math.round(cy + lineHeight()), 0xFFFFFFFF);
            }
        }

        g.disableScissor();

        if (textField.value().isEmpty() && !isFocused()) {
            g.pose().pushPose();
            g.pose().translate(clipL, clipT, 0f);
            g.pose().scale(textScale, textScale, 1f);
            g.drawString(font, getMessage(), 0, 0, 0xFF808080);
            g.pose().popPose();
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
            applyHighlightRule(colors, text, rule);
        }

        if (dynamicHighlightRules != null) {
            for (HighlightRule rule : dynamicHighlightRules.apply(text)) {
                applyHighlightRule(colors, text, rule);
            }
        }

        cachedHighlightSource = text;
        cachedHighlightColors = colors;
        return colors;
    }

    private static void applyHighlightRule(int[] colors, String text, HighlightRule rule) {
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

    private void drawHighlightedLine(GuiGraphics g, String fullText, int[] colors, int start, int end) {
        if (start < 0 || end <= start || end > fullText.length()) {
            return;
        }

        if (monospace) {
            int advance = cellAdvance();

            for (int i = start; i < end; i++) {
                char c = fullText.charAt(i);
                if (c == ' ' || c == '\t') {
                    continue;
                }

                g.drawString(font, glyph(c), (i - start) * advance + glyphOffset(c), 0, colors[i]);
            }

            return;
        }

        int x = 0;
        int runStart = start;
        int currentColor = colors[start];

        for (int i = start + 1; i < end; i++) {
            if (colors[i] != currentColor) {
                String part = fullText.substring(runStart, i);
                g.drawString(font, styled(part), x, 0, currentColor);
                x += font.width(styled(part));
                runStart = i;
                currentColor = colors[i];
            }
        }

        g.drawString(font, styled(fullText.substring(runStart, end)), x, 0, currentColor);
    }

    private int rawColumnX(String lineText, int columns) {
        int clamped = Mth.clamp(columns, 0, lineText.length());
        return monospace ? clamped * cellAdvance() : font.width(styled(lineText.substring(0, clamped)));
    }

    private int getRenderableLineEnd(String fullText, Line ln) {
        int renderEnd = ln.end();
        if (renderEnd > ln.begin() && renderEnd <= fullText.length() && fullText.charAt(renderEnd - 1) == '\n') {
            renderEnd--;
        }
        return renderEnd;
    }

    private void selectLineAtMouse(double my) {
        if (textField.lineCount() <= 0) {
            return;
        }

        Line ln = textField.line(getLineIndexAtMouse(my));
        int end = getRenderableLineEnd(textField.value(), ln);

        textField.setSelecting(false);
        textField.seekCursor(Whence.ABSOLUTE, ln.begin());
        textField.setSelecting(true);
        textField.seekCursor(Whence.ABSOLUTE, end);
        textField.setSelecting(false);

        ensureCursorVisible();
        ensureCursorVisibleX();
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

        if (CachedTextField.isMarker(lineText, column)) {
            char marker = lineText.charAt(column);
            while (start > 0 && lineText.charAt(start - 1) == marker
                    && CachedTextField.isMarker(lineText, start - 1)) {
                start--;
            }
            while (end + 1 < lineText.length() && lineText.charAt(end + 1) == marker
                    && CachedTextField.isMarker(lineText, end + 1)) {
                end++;
            }
            end++;
        } else if (isTokenChar(lineText, column)) {
            while (start > 0 && isTokenChar(lineText, start - 1)) {
                start--;
            }
            while (end + 1 < lineText.length() && isTokenChar(lineText, end + 1)) {
                end++;
            }
            end++;
        } else {
            end = Math.min(column + 1, lineText.length());
        }

        textField.setSelecting(false);
        textField.seekCursor(Whence.ABSOLUTE, ln.begin() + start);
        textField.setSelecting(true);
        textField.seekCursor(Whence.ABSOLUTE, ln.begin() + end);
        textField.setSelecting(false);

        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    private int getLineIndexAtMouse(double my) {
        double relY = my - (getY() + 2) + scrollAmount;
        return Mth.clamp((int) (relY / lineHeight()), 0, Math.max(0, textField.lineCount() - 1));
    }

    private int getColumnFromMouse(String lineText, double mx) {
        double relX = mx - (getX() + 2) + scrollX;

        if (monospace) {
            return Mth.clamp((int) Math.round(relX / cellWidth()), 0, lineText.length());
        }

        int bestCol = lineText.length();
        double bestDist = Double.MAX_VALUE;

        for (int i = 0; i <= lineText.length(); i++) {
            double dist = Math.abs(relX - columnX(lineText, i));
            if (dist < bestDist) {
                bestDist = dist;
                bestCol = i;
            }
        }

        return bestCol;
    }

    private boolean isTokenChar(String lineText, int index) {
        char c = lineText.charAt(index);
        return !Character.isWhitespace(c)
                && "[](){},".indexOf(c) < 0
                && !CachedTextField.isMarker(lineText, index);
    }

    private void moveCursorToMouse(double mx, double my) {
        String fullText = textField.value();
        int lineIndex = getLineIndexAtMouse(my);
        Line ln = textField.line(lineIndex);
        int renderEnd = getRenderableLineEnd(fullText, ln);

        String lineText = fullText.substring(ln.begin(), renderEnd);
        textField.seekCursor(Whence.ABSOLUTE, ln.begin() + getColumnFromMouse(lineText, mx));
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE, getMessage());
    }

    private static final class CachedTextField extends MultilineTextField {
        private final List<Line> cache = new ArrayList<>();
        private Consumer<String> valueListener = v -> {
        };

        record Selection(int begin, int end) {
        }

        private static final String WORD_EXTRA = ".+-?";
        private static final String MARKERS = "*_~`#>|";

        CachedTextField(Font font, int w) {
            super(font, w);
            super.setValueListener(v -> {
                rebuild();
                valueListener.accept(v);
            });
            rebuild();
        }

        @Override
        public boolean keyPressed(int key) {
            if (!hasControlDown() && (key == GLFW.GLFW_KEY_UP || key == GLFW.GLFW_KEY_DOWN)) {
                setSelecting(hasShiftDown());
                seekCursorLineKeepingColumn(key == GLFW.GLFW_KEY_DOWN ? 1 : -1);
                return true;
            }

            if (hasControlDown() && (key == GLFW.GLFW_KEY_LEFT || key == GLFW.GLFW_KEY_RIGHT)) {
                setSelecting(hasShiftDown());
                String text = value();
                int target = key == GLFW.GLFW_KEY_RIGHT
                        ? nextTokenBoundary(text, cursor())
                        : prevTokenBoundary(text, cursor());
                seekCursor(Whence.ABSOLUTE, target);
                return true;
            }
            return super.keyPressed(key);
        }

        private void seekCursorLineKeepingColumn(int delta) {
            int target = getLineAtCursor() + delta;
            if (target < 0 || target >= cache.size()) {
                return;
            }

            int column = cursor() - line(getLineAtCursor()).begin();
            Line next = line(target);

            seekCursor(Whence.ABSOLUTE, next.begin() + Math.min(column, visibleLength(next)));
        }

        private int visibleLength(Line ln) {
            String v = value();
            int end = ln.end();

            if (end > ln.begin() && end <= v.length() && v.charAt(end - 1) == '\n') {
                end--;
            }

            return end - ln.begin();
        }

        static boolean isMarker(String s, int i) {
            char c = s.charAt(i);
            if (MARKERS.indexOf(c) < 0) {
                return false;
            }
            return c != '_' || !isInsideWord(s, i);
        }

        private static boolean isInsideWord(String s, int i) {
            return i > 0 && i + 1 < s.length()
                    && isPlainWordChar(s.charAt(i - 1))
                    && isPlainWordChar(s.charAt(i + 1));
        }

        private static boolean isPlainWordChar(char c) {
            return Character.isLetterOrDigit(c) || WORD_EXTRA.indexOf(c) >= 0;
        }

        private static int charClass(String s, int i) {
            char c = s.charAt(i);
            if (Character.isWhitespace(c)) {
                return 0;
            }
            if (isMarker(s, i)) {
                return 3;
            }
            if (isPlainWordChar(c) || c == '_') {
                return 1;
            }
            return 2;
        }

        private static int nextTokenBoundary(String s, int cursor) {
            int n = s.length();
            int i = Mth.clamp(cursor, 0, n);
            if (i >= n) {
                return n;
            }
            int cls = charClass(s, i);
            if (cls == 1) {
                while (i < n && charClass(s, i) == 1) {
                    i++;
                }
            } else if (cls == 3) {
                char marker = s.charAt(i);
                while (i < n && s.charAt(i) == marker && charClass(s, i) == 3) {
                    i++;
                }
            } else if (cls == 2) {
                i++;
            }
            while (i < n && charClass(s, i) == 0) {
                i++;
            }
            return i;
        }

        private static int prevTokenBoundary(String s, int cursor) {
            int i = Mth.clamp(cursor, 0, s.length());
            while (i > 0 && charClass(s, i - 1) == 0) {
                i--;
            }
            if (i <= 0) {
                return 0;
            }
            int cls = charClass(s, i - 1);
            if (cls == 1) {
                while (i > 0 && charClass(s, i - 1) == 1) {
                    i--;
                }
            } else if (cls == 3) {
                char marker = s.charAt(i - 1);
                while (i > 0 && s.charAt(i - 1) == marker && charClass(s, i - 1) == 3) {
                    i--;
                }
            } else {
                i--;
            }
            return i;
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
        public void setValueListener(Consumer<String> listener) {
            this.valueListener = listener;
        }

        private void rebuild() {
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
        int totalH = Math.round(textField.lineCount() * lineHeight());
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

    public void setValueKeepingCursor(String v) {
        int cursor = textField.cursor();
        setValue(v);
        moveCursorToIndex(Mth.clamp(cursor, 0, v.length()));
        ensureCursorVisible();
        ensureCursorVisibleX();
    }

    private void moveCursorToIndex(int index) {
        textField.seekCursor(Whence.ABSOLUTE, Mth.clamp(index, 0, textField.value().length()));
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
            boolean hex = (ch >= '0' && ch <= '9')
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
