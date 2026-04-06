package net.oktawia.crazyae2addons.misc;

import com.mojang.blaze3d.systems.RenderSystem;
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
import net.oktawia.crazyae2addons.interfaces.IStatefulTokenizer;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

@OnlyIn(Dist.CLIENT)
public class MultilineTextFieldWidget extends AbstractWidget {

    public static final int DEFAULT_MAX_LENGTH = Integer.MAX_VALUE;

    private static final int SCROLLBAR_THICKNESS = 3;
    private static final int SCROLLBAR_TRACK_COLOR = 0x40606060;
    private static final int SCROLLBAR_THUMB_COLOR  = 0x80A0A0A0;

    private enum ScrollbarDrag { NONE, VERTICAL, HORIZONTAL }

    private final Font font;
    private final CachedTextField textField;
    private double scrollAmount;
    private double scrollX;
    private boolean dragging;
    private ScrollbarDrag scrollbarDrag = ScrollbarDrag.NONE;
    private double scrollbarDragMouseStart;
    private double scrollbarDragScrollStart;
    private IStatefulTokenizer tokenizer = SyntaxHighlighter::EmptyTokenizer;


    public MultilineTextFieldWidget(Font font,
                                    int x, int y,
                                    int w, int h,
                                    Component placeholder) {
        super(x, y, w, h, placeholder);
        this.font = font;
        // Use a very large width to prevent automatic line wrapping.
        this.textField = new CachedTextField(font, Integer.MAX_VALUE / 2);
        this.textField.setCharacterLimit(DEFAULT_MAX_LENGTH);
        this.textField.setCursorListener(this::onCursorMoved);
        this.textField.setValueListener(v -> { clampScroll(); clampScrollX(); });
    }

    public void setTokenizer(IStatefulTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public String getValue()               { return textField.value(); }
    public void   setValue(String v)       { textField.setValue(v); clampScroll(); clampScrollX(); }
    public void   insertText(String text)  { textField.insertText(text); clampScroll(); clampScrollX(); ensureCursorVisible(); ensureCursorVisibleX(); }

    public double getScrollAmount()         { return scrollAmount; }
    public void   setScrollAmount(double a) { scrollAmount = Mth.clamp(a, 0, getMaxScroll()); }
    public void   setScrollAmountX(double a){ scrollX      = Mth.clamp(a, 0, getMaxScrollX()); }

    private int textAreaWidth()  { return width  - 4 - SCROLLBAR_THICKNESS; }
    private int textAreaHeight() { return height - 4 - SCROLLBAR_THICKNESS; }

    public double getMaxScroll() {
        int textH = textField.lineCount() * font.lineHeight;
        return Math.max(textH - textAreaHeight(), 0);
    }

    public double getMaxScrollX() {
        int maxW = 0;
        String val = textField.value();
        for (int i = 0; i < textField.lineCount(); i++) {
            Line ln = textField.line(i);
            int w = 0;
            try {
                w = font.width(val.substring(ln.begin(), Math.max(ln.end() - 1, 0)));
            } catch (Exception ignored){}
            if (w > maxW) maxW = w;
        }
        return Math.max(maxW - textAreaWidth(), 0);
    }

    public int getScrollStep() { return this.font.lineHeight; }

    @Override public boolean keyPressed(int key, int sc, int mod) {
        if (!isFocused()) return false;
        if (textField.keyPressed(key) || Minecraft.getInstance().options.keyInventory.matches(key, sc)) {
            clampScroll();
            clampScrollX();
            ensureCursorVisible();
            ensureCursorVisibleX();
            return true;
        }
        return false;
    }

    @Override public boolean charTyped(char chr, int mods) {
        if (!isFocused()) return false;
        textField.insertText(String.valueOf(chr));
        clampScroll();
        clampScrollX();
        ensureCursorVisible();
        ensureCursorVisibleX();
        return true;
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (!isActive() || !isValidClickButton(btn) || !clicked(mx, my)) return false;

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

        if (!hasShiftDown()) {
            this.textField.setSelecting(false);
        }

        moveCursorToMouse(mx, my);
        dragging = true;
        return true;
    }

    @Override public boolean mouseReleased(double mx, double my, int btn) {
        dragging = false;
        scrollbarDrag = ScrollbarDrag.NONE;
        return super.mouseReleased(mx, my, btn);
    }

    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && isFocused()) { moveCursorToMouse(mx, my); return true; }
        return false;
    }

    private void updateScrollbarDrag() {
        if (scrollbarDrag == ScrollbarDrag.NONE) return;

        Minecraft mc = Minecraft.getInstance();
        long window = mc.getWindow().getWindow();
        if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            scrollbarDrag = ScrollbarDrag.NONE;
            return;
        }

        double scale = mc.getWindow().getGuiScale();
        double[] rawX = new double[1], rawY = new double[1];
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

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my)) return false;
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
        int viewH     = textAreaHeight();
        int caretLine = textField.lineAtCursor();
        int caretY    = caretLine * font.lineHeight;

        double top    = scrollAmount;
        double bottom = scrollAmount + viewH - font.lineHeight;

        if (caretY < top) {
            setScrollAmount(caretY);
        } else if (caretY > bottom) {
            setScrollAmount(caretY - (viewH - font.lineHeight));
        }
    }

    private void ensureCursorVisibleX() {
        int viewW   = textAreaWidth();
        int curLine = textField.lineAtCursor();
        Line ln     = textField.line(curLine);
        int caretX = 0;
        try {
            caretX  = font.width(textField.value().substring(Math.min(ln.begin(), textField.value().length()), textField.cursor()));
        } catch (Exception ignored) {}

        double left  = scrollX;
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

        int bg = 0xFF202020, border = isFocused() ? 0xFFFFFFFF : 0xFF808080;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.fill(getX(), getY(),                 getX() + width,     getY() + 1,          border); // top
        g.fill(getX(), getY() + height - 1,    getX() + width,     getY() + height,     border); // bottom
        g.fill(getX(), getY(),                 getX() + 1,         getY() + height,     border); // left
        g.fill(getX() + width - 1, getY(),     getX() + width,     getY() + height,     border); // right

        int clipL = getX() + 2;
        int clipT = getY() + 2;
        int clipR = getX() + 2 + textAreaWidth();
        int clipB = getY() + 2 + textAreaHeight();

        g.enableScissor(clipL, clipT, clipR, clipB);

        int firstLine = (int) (scrollAmount / font.lineHeight);
        int y    = clipT - (int) scrollAmount + firstLine * font.lineHeight;
        int xBase = clipL - (int) scrollX;

        int[] bracketDepths = new int[3];
        HighlighterState state = new HighlighterState();

        int selectionBegin = textField.hasSelection() ? textField.selection().begin() : -1;
        int selectionEnd   = textField.hasSelection() ? textField.selection().end()   : -1;
        int selectionColor = 0x80007FFF;

        for (int idx = firstLine; idx < textField.lineCount() && y <= clipB; idx++) {
            Line ln  = textField.line(idx);
            String str = textField.value().substring(ln.begin(), ln.end());
            int xOff = xBase;

            if (textField.hasSelection()) {
                int lineStartChar = ln.begin();
                int lineEndChar   = ln.end();
                if (!(selectionEnd <= lineStartChar || selectionBegin >= lineEndChar)) {
                    int selStartInLine = Math.max(0, selectionBegin - lineStartChar);
                    int selEndInLine   = Math.min(str.length(), selectionEnd - lineStartChar);
                    if (selStartInLine < selEndInLine) {
                        String preSel = str.substring(0, selStartInLine);
                        String selTxt = str.substring(selStartInLine, selEndInLine);
                        int selX = xOff + font.width(preSel);
                        int selW = font.width(selTxt);
                        g.fill(selX, y, selX + selW, y + font.lineHeight, selectionColor);
                    }
                }
            }

            for (SyntaxHighlighter.Tok t : tokenizer.tokenize(str, bracketDepths, state)) {
                g.drawString(font, t.s(), xOff, y, t.col());
                xOff += font.width(t.s());
            }

            y += font.lineHeight;
        }

        if (isFocused() && blink()) {
            int curLine = textField.lineAtCursor();
            Line ln = textField.line(curLine);
            int cx  = xBase + font.width(textField.value().substring(ln.begin(), textField.cursor()));
            int cy  = clipT + curLine * font.lineHeight - (int) scrollAmount;
            if (cy >= clipT && cy < clipB && cx >= clipL && cx <= clipR)
                g.fill(cx, cy, cx + 1, cy + font.lineHeight, 0xFFFFFFFF);
        }

        g.disableScissor();

        if (textField.value().isEmpty() && !isFocused()) {
            g.drawString(font, getMessage(), clipL, clipT, 0xFF808080);
        }

        // --- Vertical scrollbar ---
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

        // --- Horizontal scrollbar ---
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


    @Override
    protected void updateWidgetNarration(NarrationElementOutput out) {
        out.add(NarratedElementType.TITLE,
                Component.translatable("narration.edit_box", getValue()));
        if (isFocused())
            out.add(NarratedElementType.USAGE,
                    Component.translatable("narration.edit_box.usage"));
    }

    private record Line(int begin, int end) {}

    private static final class CachedTextField extends MultilineTextField {
        private List<Line> cache = new ArrayList<>();
        record Selection(int begin, int end) {}

        CachedTextField(Font font, int w) {
            super(font, w);
            rebuild();
        }

        int  lineCount()    { return cache.size(); }
        Line line(int idx)  { return cache.get(Mth.clamp(idx, 0, cache.size()-1)); }
        int  lineAtCursor() { return super.getLineAtCursor(); }

        public boolean hasSelection() { return super.hasSelection(); }
        Selection selection() {
            var sv = super.getSelected();
            return new Selection(sv.beginIndex(), sv.endIndex());
        }

        @Override public void setValue(String v)   { super.setValue(v);   rebuild(); }
        @Override public void insertText(String t) { super.insertText(t); rebuild(); }

        private void rebuild() {
            if (cache == null) cache = new ArrayList<>();
            cache.clear();
            super.iterateLines().forEach(sv ->
                    cache.add(new Line(sv.beginIndex(), sv.endIndex()))
            );
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

    private void clampScroll()  { setScrollAmount(scrollAmount); }
    private void clampScrollX() { setScrollAmountX(scrollX); }

    private void moveCursorToMouse(double mx, double my) {
        double relX = mx - (getX() + 2) + scrollX;
        double relY = my - (getY() + 2) + scrollAmount;
        textField.seekCursorToPoint(relX, relY);
    }

    private boolean blink() { return (Util.getMillis() / 500) % 2 == 0; }
}