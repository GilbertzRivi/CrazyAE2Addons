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

import java.util.ArrayList;
import java.util.List;

import static net.minecraft.client.gui.screens.Screen.hasShiftDown;

@OnlyIn(Dist.CLIENT)
public class MultilineTextFieldWidget extends AbstractWidget {

    public static final int DEFAULT_MAX_LENGTH = Integer.MAX_VALUE;

    private final Font font;
    private final CachedTextField textField;
    private double scrollAmount;
    private boolean dragging;
    private IStatefulTokenizer tokenizer = SyntaxHighlighter::EmptyTokenizer;


    public MultilineTextFieldWidget(Font font,
                                    int x, int y,
                                    int w, int h,
                                    Component placeholder) {
        super(x, y, w, h, placeholder);
        this.font = font;
        this.textField = new CachedTextField(font, w - 4);
        this.textField.setCharacterLimit(DEFAULT_MAX_LENGTH);
        this.textField.setCursorListener(this::clampScroll);
        this.textField.setValueListener(v -> clampScroll());
        this.textField.setCursorListener(this::ensureCursorVisible);
    }

    public void setTokenizer(IStatefulTokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public String getValue()               { return textField.value(); }
    public void   setValue(String v)       { textField.setValue(v);    }

    public double getScrollAmount()        { return scrollAmount;      }
    public void   setScrollAmount(double a){ scrollAmount = Mth.clamp(a, 0, getMaxScroll()); }

    public double getMaxScroll() {
        int textH = textField.lineCount() * font.lineHeight;
        return Math.max(textH - (height - 4), 0);
    }

    public int getScrollStep() { return this.font.lineHeight; }

    @Override public boolean keyPressed(int key, int sc, int mod) {
        if (!isFocused()) return false;
        if (textField.keyPressed(key) || Minecraft.getInstance().options.keyInventory.matches(key, sc)) {
            clampScroll();
            ensureCursorVisible();
            return true;
        }
        return false;
    }

    @Override public boolean charTyped(char chr, int mods) {
        if (!isFocused()) return false;
        textField.insertText(String.valueOf(chr));
        clampScroll();
        ensureCursorVisible();
        return true;
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (!isActive() || !isValidClickButton(btn) || !clicked(mx, my)) return false;

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
        return super.mouseReleased(mx, my, btn);
    }

    @Override public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (dragging && isFocused()) { moveCursorToMouse(mx, my); return true; }
        return false;
    }

    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        if (!isMouseOver(mx, my)) return false;
        setScrollAmount(scrollAmount - delta * font.lineHeight);
        return true;
    }

    private void ensureCursorVisible() {
        int viewH       = height - 4;
        int caretLine   = textField.lineAtCursor();
        int caretY      = caretLine * font.lineHeight;

        double top      = scrollAmount;
        double bottom   = scrollAmount + viewH - font.lineHeight;

        if (caretY < top) {
            setScrollAmount(caretY);
        } else if (caretY > bottom) {
            setScrollAmount(caretY - (viewH - font.lineHeight));
        }
    }

    @Override
    protected void renderWidget(GuiGraphics g, int mX, int mY, float partial) {
        RenderSystem.enableDepthTest();

        int bg = 0xFF202020, border = isFocused() ? 0xFFFFFFFF : 0xFF808080;
        g.fill(getX(), getY(), getX() + width, getY() + height, bg);
        g.fill(getX(), getY(), getX() + width, getY() + 1, border);                 // ↑
        g.fill(getX(), getY() + height - 1, getX() + width, getY() + height, border);   // ↓
        g.fill(getX(), getY(), getX() + 1, getY() + height, border);                // ←
        g.fill(getX() + width - 1, getY(), getX() + width, getY() + height, border);    // →

        int clipL = getX() + 2, clipT = getY() + 2, clipR = getX() + width - 2, clipB = getY() + height - 2;
        g.enableScissor(clipL, clipT, clipR, clipB);

        int firstLine = (int) (scrollAmount / font.lineHeight);
        int y = clipT - (int) scrollAmount + firstLine * font.lineHeight;

        int[] bracketDepths = new int[3];
        HighlighterState state = new HighlighterState();

        int selectionBegin = textField.hasSelection() ? textField.selection().begin() : -1;
        int selectionEnd = textField.hasSelection() ? textField.selection().end() : -1;
        int selectionColor = 0x80007FFF;

        for (int idx = firstLine; idx < textField.lineCount() && y <= clipB; idx++) {
            Line ln = textField.line(idx);
            String str = textField.value().substring(ln.begin(), ln.end());
            int xOff = clipL;

            if (textField.hasSelection()) {
                int lineStartChar = ln.begin();
                int lineEndChar = ln.end();

                if (!(selectionEnd <= lineStartChar || selectionBegin >= lineEndChar)) {
                    int selStartInLine = Math.max(0, selectionBegin - lineStartChar);
                    int selEndInLine = Math.min(str.length(), selectionEnd - lineStartChar);

                    if (selStartInLine < selEndInLine) {
                        String preSel = str.substring(0, selStartInLine);
                        String selectionText = str.substring(selStartInLine, selEndInLine);

                        int selX = xOff + font.width(preSel);
                        int selW = font.width(selectionText);

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
            int cx = clipL + font.width(textField.value().substring(ln.begin(), textField.cursor()));
            int cy = clipT + curLine * font.lineHeight - (int) scrollAmount;
            if (cy >= clipT && cy < clipB) g.fill(cx, cy, cx + 1, cy + font.lineHeight, 0xFFFFFFFF);
        }

        g.disableScissor();

        if (textField.value().isEmpty() && !isFocused()) {
            g.drawString(font, getMessage(), clipL, clipT, 0xFF808080);
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

    private void clampScroll() { setScrollAmount(scrollAmount); }

    private void moveCursorToMouse(double mx, double my) {
        double relX = mx - (getX() + 2);
        double relY = my - (getY() + 2) + scrollAmount;
        textField.seekCursorToPoint(relX, relY);
    }

    private boolean blink() { return (Util.getMillis() / 500) % 2 == 0; }
}
