package net.oktawia.crazyae2addons.client.screens.part;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterMenu;
import net.oktawia.crazyae2addons.menus.part.TagLevelEmitterMenu;
import net.oktawia.crazyae2addons.util.MathParser;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Optional;

public class TagLevelEmitterScreen<C extends TagLevelEmitterMenu> extends UpgradeableScreen<C> {

    private final MultilineTextFieldWidget expressionField;
    private final ConfirmableTextField thresholdField;
    private final SettingToggleButton<RedstoneMode> redstoneModeButton;
    private final IconButton analogModeButton;

    private final DecimalFormat decimalFormat;
    private final int normalTextColor;
    private final int errorTextColor;
    private boolean suppressThresholdChange = false;

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
        this.expressionField.setHighlightRules(List.of(
                new MultilineTextFieldWidget.HighlightRule("[^\\s#!&|^()]+", 0xFF00FFC8),
                new MultilineTextFieldWidget.HighlightRule("\\*", 0xFF55FF88),
                new MultilineTextFieldWidget.HighlightRule("&&|\\|\\||[!&|^]", 0xFFFFC800),
                new MultilineTextFieldWidget.HighlightRule("[()]", 0xFFFFDD55),
                new MultilineTextFieldWidget.HighlightRule("#[^\\s#!&|^()]*|#", 0xFFFF5555)
        ));

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
        this.thresholdField.setTextColor(normalTextColor);
        this.thresholdField.setResponder(text -> onThresholdChanged());
        this.thresholdField.setOnConfirm(this::sendData);
        this.thresholdField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.THRESHOLD_TOOLTIP.getTranslationKey())
        ));

        IconButton confirmButton = new IconButton(Icon.ENTER, button -> sendData());
        confirmButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.APPLY.getTranslationKey())
        ));

        this.redstoneModeButton = new ServerSettingToggleButton<>(Settings.REDSTONE_EMITTER, RedstoneMode.HIGH_SIGNAL);

        this.analogModeButton = new IconButton(Icon.REDSTONE_LOW, button -> {
                getMenu().crazyAE2Addons$toggleAnalogLogarithmicMode();
                syncAnalogModeButton(getMenu());
        });
        this.analogModeButton.setVisibility(false);

        this.widgets.add("input", this.expressionField);
        this.widgets.add("threshold", this.thresholdField);
        this.widgets.add("cmp", this.redstoneModeButton);
        this.widgets.add("analogMode", this.analogModeButton);
        this.widgets.add("confirm", confirmButton);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.redstoneModeButton.set(menu.rsMode);

        boolean visible = getMenu().crazyAE2Addons$hasAnalogCard();
        this.analogModeButton.setVisibility(visible);

        if (visible) {
            syncAnalogModeButton(getMenu());
        }


        if (!initialized) {
            this.expressionField.setValue(menu.expression);

            suppressThresholdChange = true;
            try {
                setThresholdLongValue(menu.threshold);
            } finally {
                suppressThresholdChange = false;
            }

            validateThreshold();
            initialized = true;
        }

        if (!thresholdField.isFocused()) {
            String expected = formatThreshold(menu.threshold);
            if (!expected.equals(thresholdField.getValue())) {
                suppressThresholdChange = true;
                try {
                    setThresholdLongValue(menu.threshold);
                } finally {
                    suppressThresholdChange = false;
                }

                validateThreshold();
            }
        }
    }

    private void syncAnalogModeButton(IAnalogLevelEmitterMenu analogMenu) {
        boolean logarithmic = analogMenu.crazyAE2Addons$isAnalogLogarithmicMode();

        this.analogModeButton.setIcon(logarithmic ? Icon.REDSTONE_HIGH : Icon.REDSTONE_LOW);

        Component message = Component.empty()
                .append(Component.translatable(LangDefs.ANALOG_OUTPUT_MODE.getTranslationKey()))
                .append("\n")
                .append(Component.translatable(
                        logarithmic
                                ? LangDefs.ANALOG_OUTPUT_LOGARITHMIC_DESC.getTranslationKey()
                                : LangDefs.ANALOG_OUTPUT_LINEAR_DESC.getTranslationKey()
                ).withStyle(ChatFormatting.GRAY));

        this.analogModeButton.setTooltip(Tooltip.create(message));
    }

    private void onThresholdChanged() {
        if (suppressThresholdChange) {
            return;
        }

        validateThreshold();

        parseThreshold().ifPresent(menu::setThreshold);
    }

    private void setThresholdLongValue(long value) {
        if (value <= 0) {
            this.thresholdField.setValue("");
            this.thresholdField.moveCursorToEnd();
            this.thresholdField.setHighlightPos(0);
            return;
        }

        this.thresholdField.setValue(formatThreshold(value));
        this.thresholdField.moveCursorToEnd();
        this.thresholdField.setHighlightPos(0);
    }

    private String formatThreshold(long value) {
        if (value <= 0) {
            return "";
        }

        return this.decimalFormat.format(BigDecimal.valueOf(value));
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
        onThresholdChanged();
    }

    private Optional<Long> parseThreshold() {
        String raw = thresholdField.getValue();

        if (raw == null) {
            return Optional.empty();
        }

        raw = raw.trim();

        if (raw.isEmpty()) {
            return Optional.of(0L);
        }

        if (raw.startsWith("=")) {
            raw = raw.substring(1).trim();
        }

        if (!MathParser.canParse(raw)) {
            return Optional.empty();
        }

        try {
            double parsed = MathParser.parse(raw);

            if (!Double.isFinite(parsed)) {
                return Optional.empty();
            }

            BigDecimal value = BigDecimal.valueOf(parsed).stripTrailingZeros();

            if (value.scale() > 0 || value.signum() < 0) {
                return Optional.empty();
            }

            return Optional.of(value.longValueExact());
        } catch (Throwable ignored) {
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (this.thresholdField != null
                    && this.thresholdField.visible
                    && this.thresholdField.active
                    && this.thresholdField.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.thresholdField);
                this.thresholdField.setFocused(true);
                return true;
            }
        }

        if (button == 1) {
            if (this.thresholdField != null
                    && this.thresholdField.visible
                    && this.thresholdField.isMouseOver(mouseX, mouseY)) {
                this.thresholdField.setValue("");
                this.setFocused(this.thresholdField);
                this.thresholdField.setFocused(true);
                validateThreshold();
                parseThreshold().ifPresent(menu::setThreshold);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}