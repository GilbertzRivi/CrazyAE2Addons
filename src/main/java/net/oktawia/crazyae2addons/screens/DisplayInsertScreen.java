package net.oktawia.crazyae2addons.screens;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.display.DisplayKeyCompatRegistry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class DisplayInsertScreen extends Screen {

    private enum TokenType { ICON, STOCK, DELTA }

    private final Screen parent;
    private final Consumer<String> onInsert;

    private TokenType type = TokenType.STOCK;

    private EditBox itemIdField;

    private int divisorPow = 0;
    private Button divisorBtn;

    private EditBox perNField;
    private EditBox winNField;
    private String perUnit = "s";
    private String winUnit = "s";
    private Button perUnitBtn;
    private Button winUnitBtn;

    private Button btnIcon, btnStock, btnDelta;
    private Button btnInsert, btnBack;

    private List<String> availableTypes;
    private int typeIdx = -1;
    private Button typeBtn;
    private Button typesListBtn;
    private boolean showTypesList = false;
    private int typesScrollOffset = 0;

    private static final int PW = 220, PH = 112;

    private static final int LABEL_X =  5;
    private static final int CTRL_X  = 52;
    private static final int RIGHT_X = 215;

    private static final int DROP_OFF_X = 131;
    private static final int DROP_OFF_Y =  64;
    private static final int DROP_W     =  84;
    private static final int DROP_ROW_H =  11;
    private static final int DROP_MAX   =   8;

    private static final int CS_W     = 180;
    private static final int CS_GAP   =   4;
    private static final int CS_ROW_H =  10;
    private static final int CS_HDR   =  14;

    private static String modForPrefix(String prefix) {
        return switch (prefix) {
            case "gas", "infusion", "pigment", "slurry" -> "mekanism:resource";
            case "flux"          -> "appflux:fe";
            case "source"        -> "ars_nouveau:source";
            case "mana"          -> "botania:mana";
            case "mob"           -> "mod_name:mob_type";
            case "item", "fluid" -> "mod_name:resource";
            default              -> "?";
        };
    }

    public DisplayInsertScreen(Screen parent, Consumer<String> onInsert) {
        super(LangDefs.DISPLAY_INSERT_TITLE.text());
        this.parent = parent;
        this.onInsert = onInsert;
    }

    @Override
    protected void init() {
        int px = (width - PW) / 2;
        int py = (height - PH) / 2;

        btnIcon  = addRenderableWidget(Button.builder(LangDefs.DISPLAY_TYPE_ICON.text(),  b -> setType(TokenType.ICON))
                .pos(px + 5,   py + 18).size(65, 14).build());
        btnStock = addRenderableWidget(Button.builder(LangDefs.DISPLAY_TYPE_STOCK.text(), b -> setType(TokenType.STOCK))
                .pos(px + 75,  py + 18).size(65, 14).build());
        btnDelta = addRenderableWidget(Button.builder(LangDefs.DISPLAY_TYPE_DELTA.text(), b -> setType(TokenType.DELTA))
                .pos(px + 145, py + 18).size(65, 14).build());

        itemIdField = addRenderableWidget(
                new EditBox(font, px + CTRL_X, py + 36, RIGHT_X - CTRL_X, 12, Component.empty()));
        itemIdField.setMaxLength(256);
        setInitialFocus(itemIdField);

        if (availableTypes == null) buildAvailableTypes();

        typeBtn = addRenderableWidget(Button.builder(
                Component.literal(typeLabel()), b -> cycleType())
                .pos(px + CTRL_X - 1, py + 52).size(72, 12).build());
        typesListBtn = addRenderableWidget(Button.builder(
                Component.literal("Key types \u25bc"), b -> toggleTypesList())
                .pos(px + DROP_OFF_X, py + 52).size(DROP_W, 12).build());

        String[] divLabels = {"raw", "/10", "/100", "/1k", "/10k", "/100k", "/1M"};
        divisorBtn = addRenderableWidget(Button.builder(Component.literal(divLabels[0]), b -> {
            divisorPow = (divisorPow + 1) % 7;
            b.setMessage(Component.literal(divLabels[divisorPow]));
        }).pos(px + CTRL_X -1, py + 67).size(60, 12).build());

        perNField  = addRenderableWidget(new EditBox(font, px + CTRL_X,      py + 67, 26, 12, Component.empty()));
        perNField.setMaxLength(8);
        perNField.setValue("1");
        perUnitBtn = addRenderableWidget(Button.builder(Component.literal(perUnit), b -> cycleUnit(true))
                .pos(px + CTRL_X + 28, py + 67).size(18, 12).build());

        winNField  = addRenderableWidget(new EditBox(font, px + 167, py + 67, 26, 12, Component.empty()));
        winNField.setMaxLength(8);
        winNField.setValue("30");
        winUnitBtn = addRenderableWidget(Button.builder(Component.literal(winUnit), b -> cycleUnit(false))
                .pos(px + 195, py + 67).size(18, 12).build());

        btnBack   = addRenderableWidget(Button.builder(LangDefs.DISPLAY_BACK.text(),   b -> onBack())
                .pos(px + 48,  py + 93).size(60, 14).build());
        btnInsert = addRenderableWidget(Button.builder(LangDefs.DISPLAY_INSERT.text(), b -> doInsert())
                .pos(px + 112, py + 93).size(60, 14).build());

        setType(type);
    }


    private void setType(TokenType t) {
        this.type = t;

        divisorBtn.visible = (t == TokenType.STOCK);
        perNField.visible  = (t == TokenType.DELTA);
        perUnitBtn.visible = (t == TokenType.DELTA);
        winNField.visible  = (t == TokenType.DELTA);
        winUnitBtn.visible = (t == TokenType.DELTA);

        btnIcon.active  = (t != TokenType.ICON);
        btnStock.active = (t != TokenType.STOCK);
        btnDelta.active = (t != TokenType.DELTA);
    }

    private void cycleUnit(boolean isPer) {
        String[] u = {"t", "s", "m"};
        if (isPer) {
            perUnit = u[(List.of(u).indexOf(perUnit) + 1) % 3];
            perUnitBtn.setMessage(Component.literal(perUnit));
        } else {
            winUnit = u[(List.of(u).indexOf(winUnit) + 1) % 3];
            winUnitBtn.setMessage(Component.literal(winUnit));
        }
    }

    private void buildAvailableTypes() {
        availableTypes = new ArrayList<>();
        availableTypes.add("item");
        availableTypes.add("fluid");
        availableTypes.addAll(DisplayKeyCompatRegistry.getPrefixes());
        availableTypes.sort(Comparator.naturalOrder());
    }

    private String typeLabel() {
        if (typeIdx < 0) return "Any";
        if (typeIdx >= availableTypes.size()) typeIdx = 0;
        return availableTypes.get(typeIdx);
    }

    private void cycleType() {
        typeIdx = (typeIdx < 0) ? 0 : (typeIdx + 1) % availableTypes.size();
        typeBtn.setMessage(Component.literal(typeLabel()));
    }

    private void toggleTypesList() {
        showTypesList = !showTypesList;
        if (showTypesList) typesScrollOffset = 0;
        typesListBtn.setMessage(Component.literal(showTypesList ? "Key types \u25b2" : "Key types \u25bc"));
    }

    private String currentPrefix() {
        return (typeIdx >= 0 && typeIdx < availableTypes.size()) ? availableTypes.get(typeIdx) + ":" : "";
    }

    private void doInsert() {
        String id = itemIdField.getValue().trim().toLowerCase();
        if (id.isEmpty()) return;
        String prefix = currentPrefix();
        String token = switch (type) {
            case ICON  -> "&i^" + prefix + id;
            case STOCK -> {
                String[] sfx = {"", "%1", "%2", "%3", "%4", "%5", "%6"};
                yield "&s^" + prefix + id + sfx[divisorPow];
            }
            case DELTA -> {
                String pN = perNField.getValue().trim();
                String wN = winNField.getValue().trim();
                if (pN.isEmpty() || wN.isEmpty()) yield null;
                String perPart = (pN.equals("20") && perUnit.equals("t")) ? "" : "%" + pN + perUnit;
                yield "&d^" + prefix + id + perPart + "@" + wN + winUnit;
            }
        };
        if (token == null) return;
        onInsert.accept(token);
        onBack();
    }

    private void onBack() { Minecraft.getInstance().setScreen(parent); }

    @Override
    public void render(GuiGraphics g, int mx, int my, float partial) {
        renderBackground(g);

        int px = (width - PW) / 2;
        int py = (height - PH) / 2;

        g.fill(px, py, px + PW, py + PH, 0xFF252525);
        g.fill(px,          py,          px + PW, py + 1,      0xFF909090);
        g.fill(px,          py + PH - 1, px + PW, py + PH,     0xFF909090);
        g.fill(px,          py,          px + 1,  py + PH,     0xFF909090);
        g.fill(px + PW - 1, py,          px + PW, py + PH,     0xFF909090);

        g.drawCenteredString(font, title, width / 2, py + 6, 0xFFFFFFFF);
        g.drawString(font, "Item ID:", px + LABEL_X, py + 38, 0xFFAAAAAA);
        g.drawString(font, "Type:",    px + LABEL_X, py + 54, 0xFFAAAAAA);

        if (type == TokenType.STOCK) {
            g.drawString(font, "Divisor:", px + LABEL_X, py + 69, 0xFFAAAAAA);
        } else if (type == TokenType.DELTA) {
            g.drawString(font, "Per:", px + LABEL_X, py + 69, 0xFFAAAAAA);
            g.drawString(font, "Window:", px + 132,     py + 69, 0xFFAAAAAA);
        }

        String preview = buildPreview();
        if (preview != null) {
            g.drawString(font, preview, px + LABEL_X, py + 83, 0xFF55FF55);
        }

        super.render(g, mx, my, partial);

        if (availableTypes != null && !availableTypes.isEmpty()) {
            int cx  = px + PW + CS_GAP;
            int cy  = py;
            int cch = CS_HDR + availableTypes.size() * CS_ROW_H + 4;

            g.fill(cx, cy, cx + CS_W, cy + cch, 0xFF252525);
            g.fill(cx,            cy,         cx + CS_W, cy + 1,          0xFF909090);
            g.fill(cx,            cy + cch - 1, cx + CS_W, cy + cch,      0xFF909090);
            g.fill(cx,            cy,         cx + 1,    cy + cch,        0xFF909090);
            g.fill(cx + CS_W - 1, cy,         cx + CS_W, cy + cch,        0xFF909090);

            g.drawCenteredString(font, "Types", cx + CS_W / 2, cy + 3, 0xFFCCCCCC);
            g.fill(cx + 1, cy + CS_HDR - 1, cx + CS_W - 1, cy + CS_HDR, 0xFF444444);

            for (int i = 0; i < availableTypes.size(); i++) {
                String pfx = availableTypes.get(i);
                int ry = cy + CS_HDR + i * CS_ROW_H + 1;
                boolean sel = (i == typeIdx);
                if (sel) g.fill(cx + 1, ry, cx + CS_W - 1, ry + CS_ROW_H, 0xFF3A1A1A);
                g.drawString(font, pfx,               cx + 4,  ry + 1, sel ? 0xFFFF5555 : 0xFFFFFFFF);
                g.drawString(font, "\u2192",           cx + 58, ry + 1, 0xFF555555);
                g.drawString(font, modForPrefix(pfx),  cx + 66, ry + 1, 0xFF888888);
            }
        }

        if (showTypesList && availableTypes != null && !availableTypes.isEmpty()) {
            int lx = px + DROP_OFF_X;
            int ly = py + DROP_OFF_Y;
            int vis = Math.min(availableTypes.size(), DROP_MAX);
            int lh  = vis * DROP_ROW_H;

            g.pose().pushPose();
            g.pose().translate(0, 0, 400);

            g.fill(lx, ly, lx + DROP_W, ly + lh, 0xFF1E1E1E);
            g.fill(lx,              ly,      lx + DROP_W,     ly + 1,          0xFF909090);
            g.fill(lx,              ly + lh - 1, lx + DROP_W, ly + lh,         0xFF909090);
            g.fill(lx,              ly,      lx + 1,          ly + lh,         0xFF909090);
            g.fill(lx + DROP_W - 1, ly,      lx + DROP_W,     ly + lh,         0xFF909090);

            for (int i = 0; i < vis; i++) {
                int idx = i + typesScrollOffset;
                if (idx >= availableTypes.size()) break;
                boolean sel = (idx == typeIdx);
                boolean hov = mx >= lx + 1 && mx < lx + DROP_W - 1
                           && my >= ly + i * DROP_ROW_H && my < ly + (i + 1) * DROP_ROW_H;
                if (sel) g.fill(lx + 1, ly + i * DROP_ROW_H, lx + DROP_W - 1, ly + (i + 1) * DROP_ROW_H, 0xFF3A1A1A);
                else if (hov) g.fill(lx + 1, ly + i * DROP_ROW_H, lx + DROP_W - 1, ly + (i + 1) * DROP_ROW_H, 0xFF2D2D2D);
                int color = sel ? 0xFFFF5555 : (hov ? 0xFFFFFFFF : 0xFFCCCCCC);
                g.drawString(font, availableTypes.get(idx), lx + 4, ly + i * DROP_ROW_H + 2, color);
            }

            g.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (showTypesList && availableTypes != null) {
            int px = (width - PW) / 2, py = (height - PH) / 2;
            int lx = px + DROP_OFF_X, ly = py + DROP_OFF_Y;
            int vis = Math.min(availableTypes.size(), DROP_MAX);
            if (mx >= lx && mx < lx + DROP_W && my >= ly && my < ly + vis * DROP_ROW_H) {
                int idx = ((int) my - ly) / DROP_ROW_H + typesScrollOffset;
                if (idx >= 0 && idx < availableTypes.size()) {
                    typeIdx = idx;
                    typeBtn.setMessage(Component.literal(typeLabel()));
                }
                showTypesList = false;
                typesListBtn.setMessage(Component.literal("Key types \u25bc"));
                return true;
            }
            showTypesList = false;
            typesListBtn.setMessage(Component.literal("Key types \u25bc"));
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double delta) {
        if (showTypesList && availableTypes != null) {
            int max = Math.max(0, availableTypes.size() - DROP_MAX);
            typesScrollOffset = Math.max(0, Math.min(max, typesScrollOffset - (int) delta));
            return true;
        }
        return super.mouseScrolled(mx, my, delta);
    }

    private String buildPreview() {
        String id = itemIdField == null ? "" : itemIdField.getValue().trim().toLowerCase();
        if (id.isEmpty()) return null;
        String prefix = currentPrefix();
        return switch (type) {
            case ICON  -> "&i^" + prefix + id;
            case STOCK -> {
                String[] sfx = {"", "%1", "%2", "%3", "%4", "%5", "%6"};
                yield "&s^" + prefix + id + sfx[divisorPow];
            }
            case DELTA -> {
                String pN = perNField == null ? "" : perNField.getValue().trim();
                String wN = winNField == null ? "" : winNField.getValue().trim();
                if (pN.isEmpty() || wN.isEmpty()) yield null;
                String perPart = (pN.equals("20") && perUnit.equals("t")) ? "" : "%" + pN + perUnit;
                yield "&d^" + prefix + id + perPart + "@" + wN + winUnit;
            }
        };
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == 256) { onBack(); return true; }
        if (key == 258 && type == TokenType.DELTA) {
            if (itemIdField.isFocused()) { setFocused(perNField);  return true; }
            if (perNField.isFocused())   { setFocused(winNField);  return true; }
            if (winNField.isFocused())   { setFocused(itemIdField); return true; }
        }
        itemIdField.keyPressed(key, sc, mod);
        return true;
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
