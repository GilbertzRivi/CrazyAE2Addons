package net.oktawia.crazyae2addons.client.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AE2Button;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.display.keytypes.DisplayKeyCompatRegistry;
import net.oktawia.crazyae2addons.menus.DisplaySubMenu;

import java.util.*;
import java.util.LinkedHashMap;

public class DisplaySubScreen extends AEBaseScreen<DisplaySubMenu> {

    private enum TokenType { ICON, STOCK, DELTA }
    private TokenType type = TokenType.STOCK;

    private EditBox   itemIdField;
    private int       divisorPow  = 0;
    private AE2Button divisorBtn;
    private EditBox   perNField, winNField;
    private int       perUnitIdx  = 1;
    private int       winUnitIdx  = 1;
    private AE2Button perUnitBtn, winUnitBtn;
    private AE2Button typeBtn, typesDropBtn;

    private List<String> availableTypes;
    private int     typeIdx       = -1;
    private boolean dropdownOpen  = false;
    private int     dropScrollOff = 0;

    private static final int DIVISOR_X  = 52,  ROW3_Y = 66;
    private static final int PERN_X     = 52;
    private static final int PERUNIT_X  = 80;
    private static final int WINN_X     = 167;
    private static final int WINUNIT_X  = 195;
    private static final int OFF        = -2000;

    private static final int DROP_MAX   = 8;
    private static final int DROP_ROW_H = 11;
    private static final int DROP_W     = 84;
    private static final int DROP_BTN_X = 130;
    private static final int DROP_LIST_Y = 62;

    private static final Component[] DIV_LABELS = {
        Component.translatable(LangDefs.DIV_RAW.getTranslationKey()),
        Component.translatable(LangDefs.DIV_10.getTranslationKey()),
        Component.translatable(LangDefs.DIV_100.getTranslationKey()),
        Component.translatable(LangDefs.DIV_1K.getTranslationKey()),
        Component.translatable(LangDefs.DIV_10K.getTranslationKey()),
        Component.translatable(LangDefs.DIV_100K.getTranslationKey()),
        Component.translatable(LangDefs.DIV_1M.getTranslationKey()),
    };

    public DisplaySubScreen(DisplaySubMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);
        widgets.add("back", new TabButton(Icon.BACK,
                Component.translatable(LangDefs.GO_BACK.getTranslationKey()),
                btn -> AESubScreen.goBack()));
    }

    @Override
    protected void init() {
        super.init();
        buildAvailableTypes();

        addRenderableWidget(new AE2Button(leftPos + 5,   topPos + 18, 65, 14,
                Component.translatable(LangDefs.ICON.getTranslationKey()),  b -> setType(TokenType.ICON)));
        addRenderableWidget(new AE2Button(leftPos + 75,  topPos + 18, 65, 14,
                Component.translatable(LangDefs.STOCK.getTranslationKey()), b -> setType(TokenType.STOCK)));
        addRenderableWidget(new AE2Button(leftPos + 145, topPos + 18, 65, 14,
                Component.translatable(LangDefs.DELTA.getTranslationKey()), b -> setType(TokenType.DELTA)));

        itemIdField = addRenderableWidget(new EditBox(font, leftPos + 52, topPos + 34, 163, 12, Component.empty()));
        itemIdField.setMaxLength(256);
        setInitialFocus(itemIdField);

        typeBtn = addRenderableWidget(new AE2Button(leftPos + 52, topPos + 50, 72, 12,
                Component.translatable(LangDefs.ANY.getTranslationKey()), b -> cycleType()));
        updateTypeBtn();

        typesDropBtn = addRenderableWidget(new AE2Button(leftPos + DROP_BTN_X, topPos + 50, DROP_W, 12,
                Component.translatable(LangDefs.KEY_TYPES_COLLAPSED.getTranslationKey()),
                b -> toggleDropdown()));

        divisorBtn = addRenderableWidget(new AE2Button(leftPos + DIVISOR_X, topPos + ROW3_Y, 60, 12,
                DIV_LABELS[0], b -> {
                    divisorPow = (divisorPow + 1) % 7;
                    b.setMessage(DIV_LABELS[divisorPow]);
                }));

        perNField = addRenderableWidget(new EditBox(font, leftPos + PERN_X, topPos + ROW3_Y, 26, 12, Component.empty()));
        perNField.setMaxLength(8); perNField.setValue("1");

        perUnitBtn = addRenderableWidget(new AE2Button(leftPos + PERUNIT_X, topPos + ROW3_Y, 18, 12,
                unitLabel(perUnitIdx), b -> { perUnitIdx = (perUnitIdx + 1) % 3; b.setMessage(unitLabel(perUnitIdx)); }));

        winNField = addRenderableWidget(new EditBox(font, leftPos + WINN_X, topPos + ROW3_Y, 26, 12, Component.empty()));
        winNField.setMaxLength(8); winNField.setValue("30");

        winUnitBtn = addRenderableWidget(new AE2Button(leftPos + WINUNIT_X, topPos + ROW3_Y, 18, 12,
                unitLabel(winUnitIdx), b -> { winUnitIdx = (winUnitIdx + 1) % 3; b.setMessage(unitLabel(winUnitIdx)); }));

        addRenderableWidget(new AE2Button(leftPos + 85, topPos + 100, 75, 14,
                Component.translatable(LangDefs.INSERT.getTranslationKey()), b -> doInsert()));

        addRenderableWidget(new LabelsWidget());

        setType(type);
    }

    private static final Map<String, String> TYPE_EXAMPLES = new LinkedHashMap<>();
    static {
        TYPE_EXAMPLES.put("item",     "mod_name:resource");
        TYPE_EXAMPLES.put("fluid",    "mod_name:resource");
        TYPE_EXAMPLES.put("gas",      "mekanism:resource");
        TYPE_EXAMPLES.put("infusion", "mekanism:resource");
        TYPE_EXAMPLES.put("pigment",  "mekanism:resource");
        TYPE_EXAMPLES.put("slurry",   "mekanism:resource");
        TYPE_EXAMPLES.put("flux",     "appflux:fe");
        TYPE_EXAMPLES.put("source",   "ars_nouveau:source");
        TYPE_EXAMPLES.put("mana",     "botania:mana");
        TYPE_EXAMPLES.put("mob",      "mod_name:mob_type");
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);
        if (availableTypes == null || availableTypes.isEmpty()) return;

        int cx   = leftPos + imageWidth + 4;
        int cy   = topPos;
        int cw   = 160;
        int rowH = 10;
        int hdr  = 14;
        int cch  = hdr + availableTypes.size() * rowH + 4;

        g.fill(cx, cy, cx + cw, cy + cch, 0xFF252525);
        g.fill(cx,       cy,        cx + cw, cy + 1,       0xFF909090);
        g.fill(cx,       cy+cch-1,  cx + cw, cy + cch,     0xFF909090);
        g.fill(cx,       cy,        cx + 1,  cy + cch,     0xFF909090);
        g.fill(cx+cw-1,  cy,        cx + cw, cy + cch,     0xFF909090);

        g.drawCenteredString(font,
                Component.translatable(LangDefs.TYPES_HEADER.getTranslationKey()),
                cx + cw / 2, cy + 3, 0xFFCCCCCC);
        g.fill(cx + 1, cy + hdr - 1, cx + cw - 1, cy + hdr, 0xFF444444);

        for (int i = 0; i < availableTypes.size(); i++) {
            String t   = availableTypes.get(i);
            String ex  = TYPE_EXAMPLES.getOrDefault(t, ":mod_name:resource");
            int ry     = cy + hdr + i * rowH + 1;
            boolean sel = (i == typeIdx);
            if (sel) g.fill(cx + 1, ry, cx + cw - 1, ry + rowH, 0xFF3A1A1A);
            g.drawString(font, t + ":" + ex,  cx + 4, ry + 1, sel ? 0xFFFF5555 : 0xFFAAAAAA, false);
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dropdownOpen) {
            int lx  = leftPos + DROP_BTN_X;
            int ly  = topPos  + DROP_LIST_Y;
            int vis = Math.min(availableTypes.size(), DROP_MAX);
            boolean inList   = mx >= lx && mx < lx + DROP_W && my >= ly && my < ly + vis * DROP_ROW_H;
            boolean onToggle = mx >= typesDropBtn.getX() && mx < typesDropBtn.getX() + typesDropBtn.getWidth()
                            && my >= typesDropBtn.getY() && my < typesDropBtn.getY() + typesDropBtn.getHeight();

            if (inList) {
                int idx = ((int) my - ly) / DROP_ROW_H + dropScrollOff;
                if (idx >= 0 && idx < availableTypes.size()) {
                    typeIdx = idx;
                    updateTypeBtn();
                }
                closeDropdown();
                return true;
            }
            if (!onToggle) {
                closeDropdown();
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (dropdownOpen) {
            int max = Math.max(0, availableTypes.size() - DROP_MAX);
            dropScrollOff = Math.clamp(dropScrollOff - (int) scrollY, 0, max);
            return true;
        }
        return super.mouseScrolled(mx, my, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == 256) { AESubScreen.goBack(); return true; }
        if (key == 258 && type == TokenType.DELTA) {
            if (itemIdField.isFocused()) { setFocused(perNField);   return true; }
            if (perNField.isFocused())   { setFocused(winNField);   return true; }
            if (winNField.isFocused())   { setFocused(itemIdField); return true; }
        }
        itemIdField.keyPressed(key, sc, mod);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    private void setType(TokenType t) {
        this.type = t;
        if (divisorBtn == null) return;
        divisorBtn.setX(t == TokenType.STOCK ? leftPos + DIVISOR_X : OFF);
        perNField.setX( t == TokenType.DELTA  ? leftPos + PERN_X    : OFF);
        perUnitBtn.setX(t == TokenType.DELTA  ? leftPos + PERUNIT_X : OFF);
        winNField.setX( t == TokenType.DELTA  ? leftPos + WINN_X    : OFF);
        winUnitBtn.setX(t == TokenType.DELTA  ? leftPos + WINUNIT_X : OFF);
    }

    private Component unitLabel(int idx) {
        return switch (idx) {
            case 0  -> Component.translatable(LangDefs.UNIT_TICKS.getTranslationKey());
            case 1  -> Component.translatable(LangDefs.UNIT_SECONDS.getTranslationKey());
            default -> Component.translatable(LangDefs.UNIT_MINUTES.getTranslationKey());
        };
    }

    private String unitCode(int idx) { return new String[]{"t", "s", "m"}[idx]; }

    private void buildAvailableTypes() {
        availableTypes = new ArrayList<>();
        availableTypes.add("item");
        availableTypes.add("fluid");
        availableTypes.addAll(DisplayKeyCompatRegistry.getPrefixes());
        availableTypes.sort(Comparator.naturalOrder());
    }

    private void updateTypeBtn() {
        if (typeBtn == null) return;
        typeBtn.setMessage(typeIdx < 0 || typeIdx >= availableTypes.size()
                ? Component.translatable(LangDefs.ANY.getTranslationKey())
                : Component.literal(availableTypes.get(typeIdx)));
    }

    private void cycleType() {
        typeIdx = (typeIdx < 0) ? 0 : (typeIdx + 1) % availableTypes.size();
        updateTypeBtn();
    }

    private void toggleDropdown() {
        dropdownOpen = !dropdownOpen;
        if (dropdownOpen) dropScrollOff = 0;
        typesDropBtn.setMessage(dropdownOpen
                ? Component.translatable(LangDefs.KEY_TYPES_EXPANDED.getTranslationKey())
                : Component.translatable(LangDefs.KEY_TYPES_COLLAPSED.getTranslationKey()));
    }

    private void closeDropdown() {
        dropdownOpen = false;
        typesDropBtn.setMessage(Component.translatable(LangDefs.KEY_TYPES_COLLAPSED.getTranslationKey()));
    }

    private String currentPrefix() {
        return (typeIdx >= 0 && typeIdx < availableTypes.size()) ? availableTypes.get(typeIdx) + ":" : "";
    }

    private void doInsert() {
        String id = itemIdField.getValue().trim().toLowerCase();
        if (id.isEmpty()) return;
        String token = buildToken(id, currentPrefix());
        if (token == null) return;
        getMenu().doInsert(token);
    }

    private String buildToken(String id, String prefix) {
        return switch (type) {
            case ICON  -> "&i^" + prefix + id;
            case STOCK -> {
                String[] sfx = {"", "%1", "%2", "%3", "%4", "%5", "%6"};
                yield "&s^" + prefix + id + sfx[divisorPow];
            }
            case DELTA -> {
                String pN = perNField.getValue().trim();
                String wN = winNField.getValue().trim();
                if (pN.isEmpty() || wN.isEmpty()) yield null;
                String perPart = (pN.equals("20") && perUnitIdx == 0) ? "" : "%" + pN + unitCode(perUnitIdx);
                yield "&d^" + prefix + id + perPart + "@" + wN + unitCode(winUnitIdx);
            }
        };
    }

    private class LabelsWidget extends AbstractWidget {

        LabelsWidget() {
            super(leftPos, topPos, imageWidth, imageHeight, Component.empty());
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            g.drawString(font, Component.translatable(LangDefs.ITEM_ID_LABEL.getTranslationKey()),
                    leftPos + 5, topPos + 36, 0xFFAAAAAA, false);
            g.drawString(font, Component.translatable(LangDefs.TYPE_LABEL.getTranslationKey()),
                    leftPos + 5, topPos + 52, 0xFFAAAAAA, false);

            if (type == TokenType.STOCK) {
                g.drawString(font, Component.translatable(LangDefs.DIVISOR_LABEL.getTranslationKey()),
                        leftPos + 5, topPos + 68, 0xFFAAAAAA, false);
            } else if (type == TokenType.DELTA) {
                g.drawString(font, Component.translatable(LangDefs.PER_LABEL.getTranslationKey()),
                        leftPos + 5,   topPos + 68, 0xFFAAAAAA, false);
                g.drawString(font, Component.translatable(LangDefs.WINDOW_LABEL.getTranslationKey()),
                        leftPos + 132, topPos + 68, 0xFFAAAAAA, false);
            }

            if (itemIdField != null) {
                String preview = buildToken(itemIdField.getValue().trim().toLowerCase(), currentPrefix());
                if (preview != null && !preview.isBlank()) {
                    g.drawString(font, preview, leftPos + 5, topPos + 83, 0xFF55FF55, false);
                }
            }

            if (dropdownOpen && availableTypes != null && !availableTypes.isEmpty()) {
                int lx  = leftPos + DROP_BTN_X;
                int ly  = topPos  + DROP_LIST_Y;
                int vis = Math.min(availableTypes.size(), DROP_MAX);
                int lh  = vis * DROP_ROW_H;

                g.pose().pushPose();
                g.pose().translate(0, 0, 400);

                g.fill(lx, ly, lx + DROP_W, ly + lh, 0xFF1E1E1E);
                g.fill(lx,           ly,      lx + DROP_W, ly + 1,     0xFF909090);
                g.fill(lx,           ly+lh-1, lx + DROP_W, ly + lh,    0xFF909090);
                g.fill(lx,           ly,      lx + 1,      ly + lh,    0xFF909090);
                g.fill(lx + DROP_W-1,ly,      lx + DROP_W, ly + lh,    0xFF909090);

                for (int i = 0; i < vis; i++) {
                    int idx = i + dropScrollOff;
                    if (idx >= availableTypes.size()) break;
                    boolean sel = (idx == typeIdx);
                    boolean hov = mx >= lx+1 && mx < lx+DROP_W-1
                               && my >= ly + i*DROP_ROW_H && my < ly + (i+1)*DROP_ROW_H;
                    if (sel)      g.fill(lx+1, ly+i*DROP_ROW_H, lx+DROP_W-1, ly+(i+1)*DROP_ROW_H, 0xFF3A1A1A);
                    else if (hov) g.fill(lx+1, ly+i*DROP_ROW_H, lx+DROP_W-1, ly+(i+1)*DROP_ROW_H, 0xFF2D2D2D);
                    g.drawString(font, availableTypes.get(idx),
                            lx + 4, ly + i*DROP_ROW_H + 2,
                            sel ? 0xFFFF5555 : (hov ? 0xFFFFFFFF : 0xFFCCCCCC), false);
                }

                g.pose().popPose();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {}
    }
}
