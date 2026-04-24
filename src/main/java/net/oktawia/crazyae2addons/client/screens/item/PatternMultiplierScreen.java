package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.PatternMultiplierMenu;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PatternMultiplierScreen<C extends PatternMultiplierMenu> extends AEBaseScreen<C> {

    private final IconButton clear;
    private final IconButton confirm;
    private final AETextField value;
    private final AETextField limit;

    private final DecimalFormat decimalFormat;
    private final int normalTextColor;
    private final int errorTextColor;

    private boolean initialized = false;

    public PatternMultiplierScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();
        this.errorTextColor = style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();

        this.decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        this.decimalFormat.setParseBigDecimal(true);
        this.decimalFormat.setNegativePrefix("-");

        this.confirm = new IconButton(Icon.ENTER, this::modify);
        this.confirm.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.APPLY_MULTIPLIER.getTranslationKey())
        ));

        this.clear = new IconButton(Icon.CLEAR, this::clear);
        this.clear.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.CLEAR_ALL_PATTERNS.getTranslationKey())
        ));

        this.value = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.value.setBordered(false);
        this.value.setMaxLength(50);
        this.value.setPlaceholder(Component.translatable(LangDefs.MULTIPLIER.getTranslationKey()));
        this.value.setResponder(text -> validateValueField());

        this.limit = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.limit.setBordered(false);
        this.limit.setMaxLength(32);
        this.limit.setPlaceholder(Component.translatable(LangDefs.OUTPUT_LIMIT.getTranslationKey()));
        this.limit.setResponder(text -> {
            validateLimitField();
            getParsedLimit().ifPresent(getMenu()::setLimit);
        });

        this.widgets.add("confirm", this.confirm);
        this.widgets.add("value", this.value);
        this.widgets.add("clear", this.clear);
        this.widgets.add("limit", this.limit);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!this.initialized) {
            this.value.setValue(formatDecimal(getMenu().mult));
            this.limit.setValue(Integer.toString(getMenu().limit));
            this.initialized = true;

            validateValueField();
            validateLimitField();
        }
    }

    public void clear(Button button) {
        getMenu().clearPatterns();
    }

    public void modify(Button button) {
        Optional<Double> parsedValue = getParsedMultiplier();
        if (parsedValue.isEmpty()) {
            validateValueField();
            return;
        }

        this.value.setTextColor(normalTextColor);
        getMenu().modifyPatterns(parsedValue.get());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (button == 1 && this.value.isMouseOver(mouseX, mouseY)) {
            this.value.setValue("");
            validateValueField();
            return true;
        }

        if (button == 1 && this.limit.isMouseOver(mouseX, mouseY)) {
            this.limit.setValue("0");
            validateLimitField();
            getMenu().setLimit(0);
            return true;
        }

        return handled;
    }

    private void validateValueField() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(LangDefs.PATTERN_MULTIPLIER_VALUE_TOOLTIP.getTranslationKey()));

        Optional<BigDecimal> parsed = parseExpression(this.value.getValue());
        boolean valid = parsed.isPresent() && parsed.get().doubleValue() > 0;

        if (parsed.isEmpty()) {
            tooltip.add(Component.translatable(LangDefs.INVALID_NUMBER.getTranslationKey()));
        } else {
            BigDecimal result = parsed.get();

            if (result.doubleValue() <= 0) {
                tooltip.add(Component.translatable(LangDefs.INVALID_NUMBER.getTranslationKey()));
            } else if (!isPlainNumber(this.value.getValue())) {
                tooltip.add(Component.literal("= " + formatDecimal(result.doubleValue())));
            }
        }

        this.value.setTextColor(valid ? normalTextColor : errorTextColor);
        this.value.setTooltipMessage(tooltip);
    }

    private void validateLimitField() {
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(LangDefs.OUTPUT_LIMIT.getTranslationKey()));

        Optional<BigDecimal> parsed = parseExpression(this.limit.getValue());
        boolean valid = false;

        if (parsed.isEmpty()) {
            tooltip.add(Component.translatable(LangDefs.INVALID_NUMBER.getTranslationKey()));
        } else {
            BigDecimal result = parsed.get();

            if (result.scale() > 0 || result.intValue() < 0) {
                tooltip.add(Component.translatable(LangDefs.INVALID_NUMBER.getTranslationKey()));
            } else {
                valid = true;
                if (!isPlainNumber(this.limit.getValue())) {
                    tooltip.add(Component.literal("= " + result.intValue()));
                }
            }
        }

        this.limit.setTextColor(valid ? normalTextColor : errorTextColor);
        this.limit.setTooltipMessage(tooltip);
    }

    private Optional<Double> getParsedMultiplier() {
        Optional<BigDecimal> parsed = parseExpression(this.value.getValue());
        if (parsed.isEmpty()) {
            return Optional.empty();
        }

        double result = parsed.get().doubleValue();
        return result > 0 ? Optional.of(result) : Optional.empty();
    }

    private Optional<Integer> getParsedLimit() {
        Optional<BigDecimal> parsed = parseExpression(this.limit.getValue());
        if (parsed.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal result = parsed.get();
        if (result.scale() > 0 || result.intValue() < 0) {
            return Optional.empty();
        }

        return Optional.of(result.intValue());
    }

    private Optional<BigDecimal> parseExpression(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            return Optional.empty();
        }

        if (value.startsWith("=")) {
            value = value.substring(1).trim();
        }

        return MathExpressionParser.parse(value, decimalFormat);
    }

    private boolean isPlainNumber(String input) {
        String value = input == null ? "" : input.trim();
        if (value.isEmpty()) {
            return false;
        }

        ParsePosition position = new ParsePosition(0);
        decimalFormat.parse(value, position);
        return position.getErrorIndex() == -1 && position.getIndex() == value.length();
    }

    private String formatDecimal(double value) {
        return decimalFormat.format(value);
    }
}