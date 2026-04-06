package net.oktawia.crazyae2addons.screens;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class VisualAssistScreen extends Screen {

    private final BuilderPatternScreen<?> parent;

    private EditBox widthField, heightField, depthField;
    private EditBox placeField, checkField;

    private boolean widthRight   = true;   // R / L
    private boolean heightUp     = true;   // U / D
    private boolean depthForward = true;   // F / B

    // 0 = Always, 1 = Equals (==), 2 = Not Equals (!=)
    private int condition = 0;
    private static final String[] COND_LABELS = {"Always", "= Equals", "≠ Not Equals"};

    // 0 = Place, 1 = Break
    private int actionType = 0;
    private static final String[] ACTION_LABELS = {"Place", "Break"};

    private Button widthDirBtn, heightDirBtn, depthDirBtn, condBtn, actionBtn;

    private static final int PW = 270, PH = 210;
    private static final int COL_LABEL = 8;
    private static final int COL_INPUT = 60;
    private static final int NUM_W     = 44;
    private static final int DIR_W     = 72;
    private static final int BLOCK_W   = PW - COL_INPUT - 12;
    private static final int FIELD_H   = 18;

    private int px, py;

    public VisualAssistScreen(BuilderPatternScreen<?> parent) {
        super(Component.literal("Visual Assistance"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (this.width  - PW) / 2;
        py = (this.height - PH) / 2;

        // Width
        int y = py + 26;
        widthField   = addField(px + COL_INPUT, y, NUM_W, "1");
        widthDirBtn  = addBtn(px + COL_INPUT + NUM_W + 4, y, DIR_W, widthRight ? "→ Right" : "← Left", btn -> {
            widthRight = !widthRight;
            btn.setMessage(Component.literal(widthRight ? "→ Right" : "← Left"));
        });

        // Height
        y = py + 48;
        heightField  = addField(px + COL_INPUT, y, NUM_W, "1");
        heightDirBtn = addBtn(px + COL_INPUT + NUM_W + 4, y, DIR_W, heightUp ? "↑ Up" : "↓ Down", btn -> {
            heightUp = !heightUp;
            btn.setMessage(Component.literal(heightUp ? "↑ Up" : "↓ Down"));
        });

        // Depth
        y = py + 70;
        depthField   = addField(px + COL_INPUT, y, NUM_W, "1");
        depthDirBtn  = addBtn(px + COL_INPUT + NUM_W + 4, y, DIR_W, depthForward ? "↑ Fwd" : "↓ Back", btn -> {
            depthForward = !depthForward;
            btn.setMessage(Component.literal(depthForward ? "↑ Fwd" : "↓ Back"));
        });

        // Action type (Place/Break)
        y = py + 96;
        actionBtn = addBtn(px + COL_INPUT, y, 130, ACTION_LABELS[actionType], btn -> {
            actionType = 1 - actionType;
            btn.setMessage(Component.literal(ACTION_LABELS[actionType]));
            placeField.setVisible(actionType == 0);
        });

        // Place block
        y = py + 118;
        placeField = addField(px + COL_INPUT, y, BLOCK_W, "minecraft:stone");

        // Condition
        y = py + 140;
        condBtn = addBtn(px + COL_INPUT, y, 130, COND_LABELS[condition], btn -> {
            condition = (condition + 1) % 3;
            btn.setMessage(Component.literal(COND_LABELS[condition]));
            checkField.setVisible(condition != 0);
        });

        // Check block
        y = py + 162;
        checkField = addField(px + COL_INPUT, y, BLOCK_W, "minecraft:air");
        checkField.setVisible(false);

        // Bottom buttons
        y = py + PH - 28;
        addRenderableWidget(Button.builder(Component.literal("Cancel"), btn -> minecraft.setScreen(parent))
                .bounds(px + 8, y, 70, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Generate"), btn -> generate())
                .bounds(px + PW - 78, y, 70, 20).build());
    }

    private EditBox addField(int x, int y, int w, String def) {
        EditBox f = new EditBox(this.font, x, y, w, FIELD_H, Component.empty());
        f.setValue(def);
        f.setMaxLength(200);
        return addRenderableWidget(f);
    }

    private Button addBtn(int x, int y, int w, String label, Button.OnPress action) {
        return addRenderableWidget(Button.builder(Component.literal(label), action)
                .bounds(x, y, w, FIELD_H).build());
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx);

        // Panel background + border
        gfx.fill(px,          py,          px + PW,     py + PH,     0xD0101010);
        gfx.fill(px,          py,          px + PW,     py + 1,      0xFFAAAAAA);
        gfx.fill(px,          py + PH - 1, px + PW,     py + PH,     0xFFAAAAAA);
        gfx.fill(px,          py,          px + 1,      py + PH,     0xFFAAAAAA);
        gfx.fill(px + PW - 1, py,          px + PW,     py + PH,     0xFFAAAAAA);

        // Title
        gfx.drawCenteredString(font, "Visual Assistance", px + PW / 2, py + 7, 0xFFFFFF);
        gfx.fill(px + 4, py + 18, px + PW - 4, py + 19, 0xFF555555);

        // Labels
        int lc = 0xAAAAAA;
        gfx.drawString(font, "Width:",      px + COL_LABEL, py + 31,  lc, false);
        gfx.drawString(font, "Height:",     px + COL_LABEL, py + 53,  lc, false);
        gfx.drawString(font, "Depth:",      px + COL_LABEL, py + 75,  lc, false);
        gfx.drawString(font, "Action:",      px + COL_LABEL, py + 101, lc, false);
        if (actionType == 0) {
            gfx.drawString(font, "Place:",      px + COL_LABEL, py + 123, lc, false);
        }
        gfx.drawString(font, "Condition:",  px + COL_LABEL, py + 145, lc, false);
        if (condition != 0) {
            gfx.drawString(font, "Check:", px + COL_LABEL, py + 167, lc, false);
        }

        super.render(gfx, mouseX, mouseY, partialTick);
    }

    private void generate() {
        int w = parsePositive(widthField,  1);
        int h = parsePositive(heightField, 1);
        int d = parsePositive(depthField,  1);

        String place = placeField.getValue().trim();
        String check = checkField.getValue().trim();
        if (place.isEmpty()) place = "minecraft:stone";
        if (check.isEmpty()) check = "minecraft:air";

        String dirW  = widthRight   ? "R" : "L";
        String antiW = widthRight   ? "L" : "R";
        String dirH  = heightUp     ? "U" : "D";
        String antiH = heightUp     ? "D" : "U";
        String dirD  = depthForward ? "F" : "B";

        // Generate instruction based on action type and condition
        String instr;
        String blockMap;
        
        if (actionType == 0) {
            // PLACE action
            instr = switch (condition) {
                case 1  -> "P(0)==(1)";
                case 2  -> "P(0)!=(1)";
                default -> "P(0)";
            };
            blockMap = condition == 0
                    ? "0(" + place + ")"
                    : "0(" + place + "),1(" + check + ")";
        } else {
            // BREAK action
            instr = switch (condition) {
                case 1  -> "X==(1)";
                case 2  -> "X!=(1)";
                default -> "X";
            };
            blockMap = condition == 0
                    ? "0(minecraft:air)"
                    : "0(minecraft:air),1(" + check + ")";
        }

        String row   = w + "{" + instr + dirW + "}" + w + "{" + antiW + "}";
        String slice = h + "{" + row + dirH + "}" + h + "{" + antiH + "}";

        String program;
        if (d == 1 && h == 1) {
            program = "H " + w + "{" + instr + dirW + "}";
        } else if (d == 1) {
            program = "H " + h + "{" + row + dirH + "}";
        } else {
            program = "H " + d + "{" + slice + dirD + "}";
        }

        parent.setGeneratedProgram(blockMap + "\n||\n" + program);
        minecraft.setScreen(parent);
    }

    private static int parsePositive(EditBox f, int fallback) {
        try { return Math.max(1, Integer.parseInt(f.getValue().trim())); }
        catch (Exception e) { return fallback; }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
