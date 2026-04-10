package net.oktawia.crazyae2addons.client.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.TabButton;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.BuilderPatternSubMenu;

public class BuilderPatternSubScreen<C extends BuilderPatternSubMenu> extends AEBaseScreen<C> {

    private static final int COL_INPUT = 60;
    private static final int NUM_W = 44;
    private static final int BLOCK_W = 256 - COL_INPUT - 12;
    private static final int FIELD_H = 18;

    private static final String[] COND_LABELS = {
            LangDefs.ALWAYS.getTranslationKey(),
            LangDefs.EQUALS.getTranslationKey(),
            LangDefs.NOT_EQUALS.getTranslationKey()
    };

    private static final String[] ACTION_LABELS = {
            LangDefs.PLACE.getTranslationKey(),
            LangDefs.BREAK.getTranslationKey()
    };

    private AETextField widthField;
    private AETextField heightField;
    private AETextField depthField;
    private AETextField placeField;
    private AETextField checkField;

    public BuilderPatternSubScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        var font = Minecraft.getInstance().font;

        widthField = new AETextField(style, font, 0, 0, NUM_W, FIELD_H);
        heightField = new AETextField(style, font, 0, 0, NUM_W, FIELD_H);
        depthField = new AETextField(style, font, 0, 0, NUM_W, FIELD_H);
        placeField = new AETextField(style, font, 0, 0, BLOCK_W, FIELD_H);
        checkField = new AETextField(style, font, 0, 0, BLOCK_W, FIELD_H);

        widthField.setBordered(false);
        heightField.setBordered(false);
        depthField.setBordered(false);
        placeField.setBordered(false);
        checkField.setBordered(false);

        widthField.setValue(Integer.toString(getMenu().width));
        heightField.setValue(Integer.toString(getMenu().height));
        depthField.setValue(Integer.toString(getMenu().depth));
        placeField.setValue(getMenu().placeBlock);
        checkField.setValue(getMenu().checkBlock);

        widthField.setResponder(value -> {
            int parsed = parsePositiveInt(value, getMenu().width);
            if (parsed != getMenu().width) {
                getMenu().setWidth(parsed);
            }
        });

        heightField.setResponder(value -> {
            int parsed = parsePositiveInt(value, getMenu().height);
            if (parsed != getMenu().height) {
                getMenu().setHeight(parsed);
            }
        });

        depthField.setResponder(value -> {
            int parsed = parsePositiveInt(value, getMenu().depth);
            if (parsed != getMenu().depth) {
                getMenu().setDepth(parsed);
            }
        });

        placeField.setResponder(value -> {
            if (!value.equals(getMenu().placeBlock)) {
                getMenu().setPlaceBlock(value);
            }
        });

        checkField.setResponder(value -> {
            if (!value.equals(getMenu().checkBlock)) {
                getMenu().setCheckBlock(value);
            }
        });

        widgets.add("width", widthField);
        widgets.add("height", heightField);
        widgets.add("depth", depthField);
        widgets.add("place", placeField);
        widgets.add("check", checkField);

        widgets.addButton(
                "widthDir",
                Component.translatable(getMenu().right
                        ? LangDefs.RIGHT.getTranslationKey()
                        : LangDefs.LEFT.getTranslationKey()),
                btn -> {
                    boolean newRight = !getMenu().right;
                    getMenu().setRight(newRight);
                    btn.setMessage(Component.translatable(newRight
                            ? LangDefs.RIGHT.getTranslationKey()
                            : LangDefs.LEFT.getTranslationKey()));
                }
        );

        widgets.addButton(
                "heightDir",
                Component.translatable(getMenu().up
                        ? LangDefs.UP.getTranslationKey()
                        : LangDefs.DOWN.getTranslationKey()),
                btn -> {
                    boolean newUp = !getMenu().up;
                    getMenu().setUp(newUp);
                    btn.setMessage(Component.translatable(newUp
                            ? LangDefs.UP.getTranslationKey()
                            : LangDefs.DOWN.getTranslationKey()));
                }
        );

        widgets.addButton(
                "depthDir",
                Component.translatable(getMenu().forward
                        ? LangDefs.FORWARDS.getTranslationKey()
                        : LangDefs.BACKWARDS.getTranslationKey()),
                btn -> {
                    boolean newForward = !getMenu().forward;
                    getMenu().setForward(newForward);
                    btn.setMessage(Component.translatable(newForward
                            ? LangDefs.FORWARDS.getTranslationKey()
                            : LangDefs.BACKWARDS.getTranslationKey()));
                }
        );

        widgets.addButton(
                "action",
                Component.translatable(ACTION_LABELS[getMenu().actionType]),
                btn -> {
                    int newType = 1 - getMenu().actionType;
                    getMenu().setActionType(newType);
                    btn.setMessage(Component.translatable(ACTION_LABELS[newType]));
                    updateFieldVisibility();
                }
        );

        widgets.addButton(
                "condition",
                Component.translatable(COND_LABELS[getMenu().condition]),
                btn -> {
                    int newCondition = (getMenu().condition + 1) % COND_LABELS.length;
                    getMenu().setCondition(newCondition);
                    btn.setMessage(Component.translatable(COND_LABELS[newCondition]));
                    updateFieldVisibility();
                }
        );

        widgets.addButton(
                "generate",
                Component.literal("Generate"),
                btn -> getMenu().generate()
        );

        widgets.add(
                "back",
                new TabButton(
                        Icon.BACK,
                        Component.translatable(LangDefs.GO_BACK.getTranslationKey()),
                        btn -> AESubScreen.goBack()
                )
        );

        updateFieldVisibility();
    }

    private void updateFieldVisibility() {
        placeField.setVisible(getMenu().actionType == 0);
        checkField.setVisible(getMenu().condition != 0);
    }

    private static int parsePositiveInt(String value, int fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }

        try {
            return Math.max(1, Integer.parseInt(value.trim()));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }
}