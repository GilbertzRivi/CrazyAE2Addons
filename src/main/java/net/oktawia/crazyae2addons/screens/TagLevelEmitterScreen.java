package net.oktawia.crazyae2addons.screens;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.TagLevelEmitterMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;

public class TagLevelEmitterScreen<C extends TagLevelEmitterMenu> extends UpgradeableScreen<TagLevelEmitterMenu> {

    private final MultilineTextFieldWidget expressionField;
    private final ConfirmableTextField thresholdField;
    private final IconButton confirmBtn;
    private final SettingToggleButton<RedstoneMode> cmpToggle;
    private boolean initialized = false;

    public TagLevelEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        setTextContent("dialog_title", Component.translatable("item.crazyae2addons.tag_level_emitter"));

        expressionField = new MultilineTextFieldWidget(
                Minecraft.getInstance().font,
                0, 0, 160, 90,
                Component.translatable("gui.crazyae2addons.tag_level_emitter.expr_hint"));

        thresholdField = new ConfirmableTextField(style, Minecraft.getInstance().font, 0, 0, 0, Minecraft.getInstance().font.lineHeight);
        thresholdField.setBordered(false);
        thresholdField.setMaxLength(20);
        thresholdField.setOnConfirm(this::sendData);
        thresholdField.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.tag_level_emitter.threshold_tooltip")));

        confirmBtn = new IconButton(Icon.ENTER, btn -> sendData());
        confirmBtn.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.tag_level_emitter.confirm")));

        cmpToggle = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);

        this.widgets.add("input",     expressionField);
        this.widgets.add("threshold", thresholdField);
        this.widgets.add("cmp",       cmpToggle);
        this.widgets.add("confirm",   confirmBtn);

        initialized = false;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        cmpToggle.set(menu.rsMode);

        if (!initialized) {
            expressionField.setValue(menu.expression);
            thresholdField.setValue(String.valueOf(menu.threshold));
            initialized = true;
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (expressionField.isMouseOver(x, y)) return expressionField.mouseScrolled(x, y, delta);
        return super.mouseScrolled(x, y, delta);
    }

    private void sendData() {
        menu.setExpression(expressionField.getValue());
        String rawThreshold = thresholdField.getValue().trim();
        try {
            long value = Long.parseLong(rawThreshold);
            if (value >= 0) menu.setThreshold(value);
        } catch (NumberFormatException ignored) {
        }
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        expressionField.keyPressed(key, sc, mod);
        thresholdField.keyPressed(key, sc, mod);
        if (key == 256) { this.onClose(); return true; }
        return true;
    }
}
