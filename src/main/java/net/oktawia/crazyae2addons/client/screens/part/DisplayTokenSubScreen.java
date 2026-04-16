package net.oktawia.crazyae2addons.client.screens.part;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AE2Button;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.display.keytypes.DisplayKeyCompatRegistry;
import net.oktawia.crazyae2addons.menus.part.DisplayTokenSubMenu;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public class DisplayTokenSubScreen extends AEBaseScreen<DisplayTokenSubMenu> {

    private enum TokenType { ICON, STOCK, DELTA }

    private TokenType type = TokenType.STOCK;

    private EditBox itemIdField;
    private int divisorPow = 0;
    private AE2Button divisorBtn;
    private EditBox perNField;
    private EditBox winNField;
    private int perUnitIdx = 1;
    private int winUnitIdx = 1;
    private AE2Button perUnitBtn;
    private AE2Button winUnitBtn;
    private AE2Button typeBtn;
    private AE2Button typesDropBtn;
    private AE2Button insertBtn;

    private List<String> availableTypes = Collections.emptyList();
    private int typeIdx = -1;
    private boolean dropdownOpen = false;
    private int dropScrollOff = 0;

    private static final int DROP_MAX = 8;
    private static final int DROP_ROW_H = 11;

    private static final Component[] DIV_LABELS = {
            Component.translatable(LangDefs.DIV_RAW.getTranslationKey()),
            Component.translatable(LangDefs.DIV_10.getTranslationKey()),
            Component.translatable(LangDefs.DIV_100.getTranslationKey()),
            Component.translatable(LangDefs.DIV_1K.getTranslationKey()),
            Component.translatable(LangDefs.DIV_10K.getTranslationKey()),
            Component.translatable(LangDefs.DIV_100K.getTranslationKey()),
            Component.translatable(LangDefs.DIV_1M.getTranslationKey()),
    };

    private static final Map<String, String> TYPE_EXAMPLES = new LinkedHashMap<>();
    static {
        TYPE_EXAMPLES.put("item", "mod_name:resource");
        TYPE_EXAMPLES.put("fluid", "mod_name:resource");
        TYPE_EXAMPLES.put("gas", "mekanism:resource");
        TYPE_EXAMPLES.put("flux", "appflux:fe");
        TYPE_EXAMPLES.put("source", "ars_nouveau:source");
        TYPE_EXAMPLES.put("mana", "botania:mana");
        TYPE_EXAMPLES.put("mob", "mod_name:mob_type");
    }

    public DisplayTokenSubScreen(DisplayTokenSubMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);

        var font = Minecraft.getInstance().font;

        widgets.add("back", new TabButton(
                Icon.BACK,
                Component.translatable(LangDefs.BACK.getTranslationKey()),
                btn -> AESubScreen.goBack()
        ));

        widgets.add("icon", new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.ICON.getTranslationKey()),
                btn -> setType(TokenType.ICON)
        ));

        widgets.add("stock", new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.STOCK.getTranslationKey()),
                btn -> setType(TokenType.STOCK)
        ));

        widgets.add("delta", new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.DELTA.getTranslationKey()),
                btn -> setType(TokenType.DELTA)
        ));

        itemIdField = new EditBox(font, 0, 0, 0, 0, Component.empty());
        itemIdField.setBordered(false);
        itemIdField.setMaxLength(256);
        widgets.add("itemId", itemIdField);

        typeBtn = new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.ANY.getTranslationKey()),
                btn -> cycleType()
        );
        widgets.add("type", typeBtn);

        typesDropBtn = new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.KEY_TYPES_COLLAPSED.getTranslationKey()),
                btn -> toggleDropdown()
        );
        widgets.add("typeDropdown", typesDropBtn);

        divisorBtn = new AE2Button(
                0, 0, 0, 0,
                DIV_LABELS[0],
                btn -> {
                    divisorPow = (divisorPow + 1) % DIV_LABELS.length;
                    btn.setMessage(DIV_LABELS[divisorPow]);
                }
        );
        widgets.add("divisor", divisorBtn);

        perNField = new EditBox(font, 0, 0, 0, 0, Component.empty());
        perNField.setMaxLength(8);
        widgets.add("perN", perNField);

        perUnitBtn = new AE2Button(
                0, 0, 0, 0,
                unitLabel(perUnitIdx),
                btn -> {
                    perUnitIdx = (perUnitIdx + 1) % 3;
                    btn.setMessage(unitLabel(perUnitIdx));
                }
        );
        widgets.add("perUnit", perUnitBtn);

        winNField = new EditBox(font, 0, 0, 0, 0, Component.empty());
        winNField.setMaxLength(8);
        widgets.add("winN", winNField);

        winUnitBtn = new AE2Button(
                0, 0, 0, 0,
                unitLabel(winUnitIdx),
                btn -> {
                    winUnitIdx = (winUnitIdx + 1) % 3;
                    btn.setMessage(unitLabel(winUnitIdx));
                }
        );
        widgets.add("winUnit", winUnitBtn);

        insertBtn = new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.INSERT.getTranslationKey()),
                btn -> doInsert()
        );
        widgets.add("insert", insertBtn);
    }

    @Override
    protected void init() {
        super.init();

        buildAvailableTypes();
        updateTypeBtn();
        closeDropdown();

        if (perNField.getValue().isEmpty()) {
            perNField.setValue("1");
        }

        if (winNField.getValue().isEmpty()) {
            winNField.setValue("30");
        }

        setInitialFocus(itemIdField);
        addRenderableWidget(new LabelsWidget());

        setType(type);
    }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        super.render(g, mx, my, partial);

        if (availableTypes.isEmpty()) {
            return;
        }

        int cx = leftPos + imageWidth + 4;
        int cy = topPos;
        int cw = 160;
        int rowH = 10;
        int hdr = 14;
        int cch = hdr + availableTypes.size() * rowH + 4;

        g.fill(cx, cy, cx + cw, cy + cch, 0xFF252525);
        g.fill(cx, cy, cx + cw, cy + 1, 0xFF909090);
        g.fill(cx, cy + cch - 1, cx + cw, cy + cch, 0xFF909090);
        g.fill(cx, cy, cx + 1, cy + cch, 0xFF909090);
        g.fill(cx + cw - 1, cy, cx + cw, cy + cch, 0xFF909090);

        g.drawCenteredString(
                font,
                Component.translatable(LangDefs.TYPES_HEADER.getTranslationKey()),
                cx + cw / 2,
                cy + 3,
                0xFFCCCCCC
        );
        g.fill(cx + 1, cy + hdr - 1, cx + cw - 1, cy + hdr, 0xFF444444);

        for (int i = 0; i < availableTypes.size(); i++) {
            String t = availableTypes.get(i);
            String ex = TYPE_EXAMPLES.getOrDefault(t, ":mod_name:resource");
            int ry = cy + hdr + i * rowH + 1;
            boolean sel = i == typeIdx;

            if (sel) {
                g.fill(cx + 1, ry, cx + cw - 1, ry + rowH, 0xFF3A1A1A);
            }

            g.drawString(
                    font,
                    t + ":" + ex,
                    cx + 4,
                    ry + 1,
                    sel ? 0xFFFF5555 : 0xFFAAAAAA,
                    false
            );
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (dropdownOpen) {
            int lx = dropdownLeft();
            int ly = dropdownTop();
            int lw = dropdownWidth();
            int vis = Math.min(availableTypes.size(), DROP_MAX);

            boolean inList = mx >= lx && mx < lx + lw && my >= ly && my < ly + vis * DROP_ROW_H;
            boolean onToggle = mx >= typesDropBtn.getX()
                    && mx < typesDropBtn.getX() + typesDropBtn.getWidth()
                    && my >= typesDropBtn.getY()
                    && my < typesDropBtn.getY() + typesDropBtn.getHeight();

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
        if (key == 256) {
            AESubScreen.goBack();
            return true;
        }

        if (key == 258 && type == TokenType.DELTA) {
            if (itemIdField.isFocused()) {
                setFocused(perNField);
                return true;
            }
            if (perNField.isFocused()) {
                setFocused(winNField);
                return true;
            }
            if (winNField.isFocused()) {
                setFocused(itemIdField);
                return true;
            }
        }

        if (itemIdField != null && itemIdField.keyPressed(key, sc, mod)) {
            return true;
        }
        if (perNField != null && perNField.visible && perNField.keyPressed(key, sc, mod)) {
            return true;
        }
        if (winNField != null && winNField.visible && winNField.keyPressed(key, sc, mod)) {
            return true;
        }

        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void setType(TokenType t) {
        this.type = t;

        boolean stock = t == TokenType.STOCK;
        boolean delta = t == TokenType.DELTA;

        setWidgetVisibility(divisorBtn, stock);
        setWidgetVisibility(perNField, delta);
        setWidgetVisibility(perUnitBtn, delta);
        setWidgetVisibility(winNField, delta);
        setWidgetVisibility(winUnitBtn, delta);

        if (!delta && (perNField.isFocused() || winNField.isFocused())) {
            setFocused(itemIdField);
        }
    }

    private void setWidgetVisibility(AbstractWidget widget, boolean visible) {
        widget.visible = visible;
        widget.active = visible;

        if (widget instanceof EditBox editBox) {
            editBox.setEditable(visible);
        }
    }

    private Component unitLabel(int idx) {
        return switch (idx) {
            case 0 -> Component.translatable(LangDefs.UNIT_TICKS.getTranslationKey());
            case 1 -> Component.translatable(LangDefs.UNIT_SECONDS.getTranslationKey());
            default -> Component.translatable(LangDefs.UNIT_MINUTES.getTranslationKey());
        };
    }

    private String unitCode(int idx) {
        return switch (idx) {
            case 0 -> "t";
            case 1 -> "s";
            default -> "m";
        };
    }

    private void buildAvailableTypes() {
        var dedup = new LinkedHashSet<String>();
        dedup.add("item");
        dedup.add("fluid");
        dedup.addAll(DisplayKeyCompatRegistry.getPrefixes());

        availableTypes = new ArrayList<>(dedup);
        availableTypes.sort(Comparator.naturalOrder());
    }

    private void updateTypeBtn() {
        if (typeBtn == null) {
            return;
        }

        typeBtn.setMessage(
                typeIdx < 0 || typeIdx >= availableTypes.size()
                        ? Component.translatable(LangDefs.ANY.getTranslationKey())
                        : Component.literal(availableTypes.get(typeIdx))
        );
    }

    private void cycleType() {
        if (availableTypes.isEmpty()) {
            return;
        }

        typeIdx = (typeIdx < 0) ? 0 : (typeIdx + 1) % availableTypes.size();
        updateTypeBtn();
    }

    private void toggleDropdown() {
        dropdownOpen = !dropdownOpen;

        if (dropdownOpen) {
            dropScrollOff = 0;
        }

        typesDropBtn.setMessage(
                dropdownOpen
                        ? Component.translatable(LangDefs.KEY_TYPES_EXPANDED.getTranslationKey())
                        : Component.translatable(LangDefs.KEY_TYPES_COLLAPSED.getTranslationKey())
        );
    }

    private void closeDropdown() {
        dropdownOpen = false;
        typesDropBtn.setMessage(Component.translatable(LangDefs.KEY_TYPES_COLLAPSED.getTranslationKey()));
    }

    private String currentPrefix() {
        return (typeIdx >= 0 && typeIdx < availableTypes.size())
                ? availableTypes.get(typeIdx) + ":"
                : "";
    }

    private void doInsert() {
        String id = itemIdField.getValue().trim().toLowerCase();
        if (id.isEmpty()) {
            return;
        }

        String token = buildToken(id, currentPrefix());
        if (token == null) {
            return;
        }

        getMenu().doInsert(token);
    }

    private String buildToken(String id, String prefix) {
        return switch (type) {
            case ICON -> "&i^" + prefix + id;

            case STOCK -> {
                String[] sfx = {"", "%1", "%2", "%3", "%4", "%5", "%6"};
                yield "&s^" + prefix + id + sfx[divisorPow];
            }

            case DELTA -> {
                String pN = perNField.getValue().trim();
                String wN = winNField.getValue().trim();

                if (pN.isEmpty() || wN.isEmpty()) {
                    yield null;
                }

                String perPart = (pN.equals("20") && perUnitIdx == 0)
                        ? ""
                        : "%" + pN + unitCode(perUnitIdx);

                yield "&d^" + prefix + id + perPart + "@" + wN + unitCode(winUnitIdx);
            }
        };
    }

    private int dropdownLeft() {
        return typesDropBtn.getX();
    }

    private int dropdownTop() {
        return typesDropBtn.getY() + typesDropBtn.getHeight();
    }

    private int dropdownWidth() {
        return typesDropBtn.getWidth();
    }

    private class LabelsWidget extends AbstractWidget {

        LabelsWidget() {
            super(0, 0, 0, 0, Component.empty());
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float partial) {
            g.drawString(
                    font,
                    Component.translatable(LangDefs.ITEM_ID_LABEL.getTranslationKey()),
                    leftPos + 5,
                    itemIdField.getY() + 2,
                    0xFFAAAAAA,
                    false
            );

            g.drawString(
                    font,
                    Component.translatable(LangDefs.TYPE_LABEL.getTranslationKey()),
                    leftPos + 5,
                    typeBtn.getY() + 2,
                    0xFFAAAAAA,
                    false
            );

            if (type == TokenType.STOCK) {
                g.drawString(
                        font,
                        Component.translatable(LangDefs.DIVISOR_LABEL.getTranslationKey()),
                        leftPos + 5,
                        divisorBtn.getY() + 2,
                        0xFFAAAAAA,
                        false
                );
            } else if (type == TokenType.DELTA) {
                g.drawString(
                        font,
                        Component.translatable(LangDefs.PER_LABEL.getTranslationKey()),
                        leftPos + 5,
                        perNField.getY() + 2,
                        0xFFAAAAAA,
                        false
                );

                g.drawString(
                        font,
                        Component.translatable(LangDefs.WINDOW_LABEL.getTranslationKey()),
                        winNField.getX() - 36,
                        winNField.getY() + 2,
                        0xFFAAAAAA,
                        false
                );
            }

            if (itemIdField != null) {
                String preview = buildToken(itemIdField.getValue().trim().toLowerCase(), currentPrefix());
                if (preview != null && !preview.isBlank()) {
                    g.drawString(
                            font,
                            preview,
                            leftPos + 5,
                            insertBtn.getY() - 17,
                            0xFF55FF55,
                            false
                    );
                }
            }

            if (dropdownOpen && !availableTypes.isEmpty()) {
                int lx = dropdownLeft();
                int ly = dropdownTop();
                int lw = dropdownWidth();
                int vis = Math.min(availableTypes.size(), DROP_MAX);
                int lh = vis * DROP_ROW_H;

                g.pose().pushPose();
                g.pose().translate(0, 0, 400);

                g.fill(lx, ly, lx + lw, ly + lh, 0xFF1E1E1E);
                g.fill(lx, ly, lx + lw, ly + 1, 0xFF909090);
                g.fill(lx, ly + lh - 1, lx + lw, ly + lh, 0xFF909090);
                g.fill(lx, ly, lx + 1, ly + lh, 0xFF909090);
                g.fill(lx + lw - 1, ly, lx + lw, ly + lh, 0xFF909090);

                for (int i = 0; i < vis; i++) {
                    int idx = i + dropScrollOff;
                    if (idx >= availableTypes.size()) {
                        break;
                    }

                    boolean sel = idx == typeIdx;
                    boolean hov = mx >= lx + 1
                            && mx < lx + lw - 1
                            && my >= ly + i * DROP_ROW_H
                            && my < ly + (i + 1) * DROP_ROW_H;

                    if (sel) {
                        g.fill(lx + 1, ly + i * DROP_ROW_H, lx + lw - 1, ly + (i + 1) * DROP_ROW_H, 0xFF3A1A1A);
                    } else if (hov) {
                        g.fill(lx + 1, ly + i * DROP_ROW_H, lx + lw - 1, ly + (i + 1) * DROP_ROW_H, 0xFF2D2D2D);
                    }

                    g.drawString(
                            font,
                            availableTypes.get(idx),
                            lx + 4,
                            ly + i * DROP_ROW_H + 2,
                            sel ? 0xFFFF5555 : (hov ? 0xFFFFFFFF : 0xFFCCCCCC),
                            false
                    );
                }

                g.pose().popPose();
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
        }
    }
}