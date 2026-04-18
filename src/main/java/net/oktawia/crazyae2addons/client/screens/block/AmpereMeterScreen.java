package net.oktawia.crazyae2addons.client.screens.block;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.block.AmpereMeterMenu;
import net.oktawia.crazyae2addons.util.MathParser;
import net.oktawia.crazyae2addons.util.Utils;

import java.util.List;

public class AmpereMeterScreen<C extends AmpereMeterMenu> extends AEBaseScreen<C> {

    public ToggleButton direction;
    public AETextField minFe;
    public AETextField maxFe;

    private boolean lastMinFocused = false;
    private boolean lastMaxFocused = false;

    public AmpereMeterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        direction = new ToggleButton(Icon.ARROW_RIGHT, Icon.ARROW_LEFT, this::toggleDirection);
        direction.setTooltipOn(List.of(Component.translatable(LangDefs.AMPERE_METER_DIRECTION_LEFT_TO_RIGHT.getTranslationKey())));
        direction.setTooltipOff(List.of(Component.translatable(LangDefs.AMPERE_METER_DIRECTION_RIGHT_TO_LEFT.getTranslationKey())));
        this.widgets.add("direction", direction);

        minFe = new AETextField(style, this.font, 0, 0, 64, 12);
        minFe.setBordered(false);
        minFe.setFilter(s -> s.isEmpty() || s.matches("[0-9kKmMgGtTeE+\\-*/%().\\s]*"));
        minFe.setPlaceholder(Component.translatable(LangDefs.MIN.getTranslationKey()));
        minFe.setMaxLength(24);
        minFe.setResponder(this::onMinChanged);
        this.widgets.add("min_fe", minFe);

        maxFe = new AETextField(style, this.font, 0, 0, 64, 12);
        maxFe.setBordered(false);
        maxFe.setFilter(s -> s.isEmpty() || s.matches("[0-9kKmMgGtTeE+\\-*/%().\\s]*"));
        maxFe.setPlaceholder(Component.translatable(LangDefs.MAX.getTranslationKey()));
        maxFe.setMaxLength(24);
        maxFe.setResponder(this::onMaxChanged);
        this.widgets.add("max_fe", maxFe);

        refreshThresholdTooltips();
    }

    @Override
    protected void init() {
        super.init();

        if (minFe != null) {
            minFe.setValue(formatThreshold(getMenu().getHost().getMinFePerTick()));
        }

        if (maxFe != null) {
            maxFe.setValue(formatThreshold(getMenu().getHost().getMaxFePerTick()));
        }

        lastMinFocused = false;
        lastMaxFocused = false;

        refreshThresholdTooltips();
    }

    private void refreshThresholdTooltips() {
        boolean useAmps = getMenu().getHost().isAmperesMode();

        Component tooltip = Component.translatable(
                useAmps
                        ? LangDefs.AMPERES.getTranslationKey()
                        : LangDefs.FE_PER_TICK.getTranslationKey()
        );

        if (minFe != null) {
            minFe.setTooltipMessage(List.of(tooltip));
        }

        if (maxFe != null) {
            maxFe.setTooltipMessage(List.of(tooltip));
        }
    }

    private int parseThreshold(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }

        String trimmed = value.trim();

        if (trimmed.equals("-") || trimmed.equals("+") || trimmed.equals("(") || trimmed.equals(")")) {
            throw new IllegalArgumentException("Incomplete expression");
        }

        double parsed = MathParser.parse(trimmed);

        if (Double.isNaN(parsed) || Double.isInfinite(parsed)) {
            throw new IllegalArgumentException("Invalid numeric value: " + value);
        }

        long rounded = Math.round(parsed);

        if (rounded < 0L) {
            return 0;
        }
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }

        return (int) rounded;
    }

    private String formatThreshold(int value) {
        return value <= 0 ? "0" : Utils.shortenNumber(value);
    }

    private void onMinChanged(String value) {
        if (minFe == null || !minFe.isFocused()) {
            return;
        }

        int parsed;
        try {
            parsed = parseThreshold(value);
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("invalid numeric input in ampere meter min field", e);
            return;
        }

        if (parsed != getMenu().getHost().getMinFePerTick()) {
            getMenu().changeMin(parsed);
        }
    }

    private void onMaxChanged(String value) {
        if (maxFe == null || !maxFe.isFocused()) {
            return;
        }

        int parsed;
        try {
            parsed = parseThreshold(value);
        } catch (Exception e) {
            CrazyAddons.LOGGER.debug("invalid numeric input in ampere meter max field", e);
            return;
        }

        if (parsed != getMenu().getHost().getMaxFePerTick()) {
            getMenu().changeMax(parsed);
        }
    }

    private void toggleDirection(boolean dir) {
        this.direction.setState(dir);
        this.getMenu().changeDirection(dir);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        direction.setState(getMenu().getHost().isDirection());
        refreshThresholdTooltips();

        setTextContent(
                "energy",
                Component.literal("Transferring: " + getMenu().getHost().getTransfer() + " " + getMenu().getHost().getUnit())
        );

        if (minFe != null) {
            boolean focused = minFe.isFocused();

            if (focused && !lastMinFocused) {
                minFe.setValue(String.valueOf(getMenu().getHost().getMinFePerTick()));
            } else if (!focused) {
                String v = formatThreshold(getMenu().getHost().getMinFePerTick());
                if (!v.equals(minFe.getValue())) {
                    minFe.setValue(v);
                }
            }

            lastMinFocused = focused;
        }

        if (maxFe != null) {
            boolean focused = maxFe.isFocused();

            if (focused && !lastMaxFocused) {
                maxFe.setValue(String.valueOf(getMenu().getHost().getMaxFePerTick()));
            } else if (!focused) {
                String v = formatThreshold(getMenu().getHost().getMaxFePerTick());
                if (!v.equals(maxFe.getValue())) {
                    maxFe.setValue(v);
                }
            }

            lastMaxFocused = focused;
        }
    }
}