package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.MathParser;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.CrazyPatternMultiplierMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class CrazyPatternMultiplierScreen<C extends CrazyPatternMultiplierMenu> extends AEBaseScreen<C> implements CrazyScreen {

    private static final String NAME = "crazy_pattern_multiplier";
    public IconButton clear;
    public IconButton circuit;
    public IconButton confirm;
    public AETextField value;
    public AETextField circuitValue;
    public AETextField limit;
    private boolean initialized = false;

    static {
        CrazyScreen.i18n(NAME, "multiply", "Multiply all patterns by selected value");
        CrazyScreen.i18n(NAME, "clear", "Clear all patterns");
        CrazyScreen.i18n(NAME, "set_circuit", "Set selected circuit to all patterns");
        CrazyScreen.i18n(NAME, "circuit_tooltip", "Input desired circuit number (0-32)");
        CrazyScreen.i18n(NAME, "multiplier_placeholder", "Multiplier");
        CrazyScreen.i18n(NAME, "multiplier_tooltip", "Input the amount by which you want to multiply your patterns, can also be an equation like 2*(3/4)");
        CrazyScreen.i18n(NAME, "limit_tooltip", "Limit above which patterns won't get multiplied, 0 means limit disabled");
    }

    public CrazyPatternMultiplierScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.confirm = new IconButton(Icon.ENTER, this::modify);
        this.confirm.setTooltip(Tooltip.create(l10n(NAME, "multiply")));
        this.clear = new IconButton(Icon.CLEAR, this::clear);
        this.clear.setTooltip(Tooltip.create(l10n(NAME, "clear")));
        this.circuit = new IconButton(Icon.ENTER, this::circuit);
        this.circuit.setTooltip(Tooltip.create(l10n(NAME, "set_circuit")));
        this.circuitValue = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.circuitValue.setBordered(false);
        this.circuitValue.setMaxLength(2);
        this.circuitValue.setTooltip(Tooltip.create(l10n(NAME, "circuit_tooltip")));
        this.value = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.value.setBordered(false);
        this.value.setMaxLength(50);
        this.value.setPlaceholder(l10n(NAME, "multiplier_placeholder"));
        this.value.setTooltip(Tooltip.create(l10n(NAME, "multiplier_tooltip")));
        this.limit = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.limit.setBordered(false);
        this.limit.setMaxLength(16);
        this.limit.setTooltip(Tooltip.create(l10n(NAME, "limit_tooltip")));
        this.limit.setResponder(val -> {
            try {
                int parsed = Integer.parseInt(val);
                getMenu().setLimit(Math.max(parsed, 0));
            } catch (Exception ignored) {}
        });
        this.widgets.add("confirm", this.confirm);
        this.widgets.add("value", this.value);
        this.widgets.add("clear", this.clear);
        this.widgets.add("circuit", this.circuit);
        this.widgets.add("circuitVal", this.circuitValue);
        this.widgets.add("limit", this.limit);
    }

    private void circuit(Button button) {
        if (circuitValue.getValue().isEmpty()) {
            this.getMenu().setCircuit(-1);
        } else if ((circuitValue.getValue().chars().allMatch(Character::isDigit) && !circuitValue.getValue().isEmpty() && Integer.parseInt(circuitValue.getValue()) <= 32)) {
            this.getMenu().setCircuit(Integer.parseInt(circuitValue.getValue()));
        }
    }

    public void clear(Button button) {
        getMenu().clearPatterns();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.initialized) {
            this.value.setValue(String.valueOf(getMenu().mult));
            this.limit.setValue(String.valueOf(getMenu().limit));
            this.initialized = true;
        }
    }

    public void modify(Button btn) {
        double evaled = 0;
        try {
            evaled = MathParser.parse(value.getValue());
            if (evaled <= 0) {
                value.setTextColor(0xFF0000);
                Runnable col = () -> value.setTextColor(0xFFFFFF);
                Utils.asyncDelay(col, 1);
                return;
            }
        } catch (Exception ignored) {
            value.setTextColor(0xFF0000);
            Runnable col = () -> value.setTextColor(0xFFFFFF);
            Utils.asyncDelay(col, 1);
            return;
        }
        value.setTextColor(0x00FF00);
        Runnable col = () -> value.setTextColor(0xFFFFFF);
        Utils.asyncDelay(col, 1);
        this.getMenu().modifyPatterns(evaled);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (button == 1 && this.circuitValue != null && this.circuitValue.isMouseOver(mouseX, mouseY)) {
            this.circuitValue.setValue("");
            return true;
        }
        if (button == 1 && this.value != null && this.value.isMouseOver(mouseX, mouseY)) {
            this.value.setValue("");
            return true;
        }
        if (button == 1 && this.limit != null && this.limit.isMouseOver(mouseX, mouseY)) {
            this.limit.setValue("0");
            return true;
        }

        return handled;
    }
}
