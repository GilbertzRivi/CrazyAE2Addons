package net.oktawia.crazyae2addons.client.screens.part;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.Icon;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.part.TagLevelEmitterMenu;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Optional;

public class TagLevelEmitterScreen<C extends TagLevelEmitterMenu> extends UpgradeableScreen<C> {

    private final MultilineTextFieldWidget expressionField;
    private final ConfirmableTextField thresholdField;
    private final SettingToggleButton<RedstoneMode> redstoneModeButton;

    private final DecimalFormat decimalFormat;
    private final int normalTextColor;
    private final int errorTextColor;

    private boolean initialized = false;

    public TagLevelEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();
        this.errorTextColor = style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();

        this.decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        this.decimalFormat.setParseBigDecimal(true);
        this.decimalFormat.setNegativePrefix("-");

        this.expressionField = new MultilineTextFieldWidget(
                Minecraft.getInstance().font,
                0,
                0,
                160,
                90,
                Component.translatable(LangDefs.EXPRESSION_HINT.getTranslationKey())
        );

        this.thresholdField = new ConfirmableTextField(
                style,
                Minecraft.getInstance().font,
                0,
                0,
                0,
                Minecraft.getInstance().font.lineHeight
        );
        this.thresholdField.setBordered(false);
        this.thresholdField.setMaxLength(20);
        this.thresholdField.setOnConfirm(this::sendData);
        this.thresholdField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.THRESHOLD_TOOLTIP.getTranslationKey())
        ));

        IconButton confirmButton = new IconButton(Icon.ENTER, button -> sendData());
        confirmButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.APPLY.getTranslationKey())
        ));

        this.redstoneModeButton = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);

        this.widgets.add("input", this.expressionField);
        this.widgets.add("threshold", this.thresholdField);
        this.widgets.add("cmp", this.redstoneModeButton);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.redstoneModeButton.set(menu.rsMode);

        if (!initialized) {
            this.expressionField.setValue(menu.expression);
            this.thresholdField.setValue(String.valueOf(menu.threshold));
            validateThreshold();
            initialized = true;
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (this.expressionField.isMouseOver(x, y)) {
            return this.expressionField.mouseScrolled(x, y, delta);
        }
        return super.mouseScrolled(x, y, delta);
    }

    private void sendData() {
        menu.setExpression(expressionField.getValue());
        parseThreshold().ifPresent(menu::setThreshold);
        validateThreshold();
    }

    private Optional<Long> parseThreshold() {
        String raw = thresholdField.getValue().trim();
        if (raw.isEmpty()) {
            return Optional.of(0L);
        }

        String expression = raw.startsWith("=") ? raw.substring(1) : raw;
        Optional<BigDecimal> parsed = MathExpressionParser.parse(expression, decimalFormat);
        if (parsed.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal value = parsed.get();
        if (value.scale() > 0 || value.signum() < 0) {
            return Optional.empty();
        }

        try {
            return Optional.of(value.longValueExact());
        } catch (ArithmeticException ignored) {
            return Optional.empty();
        }
    }

    private void validateThreshold() {
        boolean valid = parseThreshold().isPresent();
        thresholdField.setTextColor(valid ? normalTextColor : errorTextColor);
    }

    @Override
    public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 256) {
            onClose();
            return true;
        }

        if (expressionField.keyPressed(key, scanCode, modifiers)) {
            return true;
        }

        if (thresholdField.keyPressed(key, scanCode, modifiers)) {
            validateThreshold();
            return true;
        }

        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (expressionField.charTyped(codePoint, modifiers)) {
            return true;
        }

        if (thresholdField.charTyped(codePoint, modifiers)) {
            validateThreshold();
            return true;
        }

        return super.charTyped(codePoint, modifiers);
    }
}