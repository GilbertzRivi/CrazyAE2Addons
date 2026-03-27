package net.oktawia.crazyae2addons.screens;

import appeng.api.stacks.AEKey;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import de.mari_023.ae2wtlib.wut.CycleTerminalButton;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.WirelessNotificationTerminalMenu;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WirelessNotificationTerminalScreen<C extends WirelessNotificationTerminalMenu>
        extends AEBaseScreen<C> implements IUniversalTerminalCapable {

    private static final int FILTER_SLOTS = WirelessNotificationTerminalMenu.FILTER_SLOTS;
    private static final int VISIBLE_ROWS = WirelessNotificationTerminalMenu.VISIBLE_ROWS;

    private static final int FILTER_SLOT_X = 10;
    private static final int FILTER_SLOT_Y0 = 34;
    private static final int ROW_H = 18;

    private final Scrollbar scrollbar = new Scrollbar();
    private int lastOffset = -1;

    private final int[] boundSlot = new int[VISIBLE_ROWS];
    private final NumberEntryType[] rowType = new NumberEntryType[VISIBLE_ROWS];
    private final ConfirmableTextField[] limitFields = new ConfirmableTextField[VISIBLE_ROWS];
    private final boolean[] suppressChange = new boolean[VISIBLE_ROWS];

    private AETextField hudXField;
    private AETextField hudYField;
    private AECheckbox hideAboveCb;
    private AECheckbox hideBelowCb;

    private final int errorTextColor;
    private final int normalTextColor;
    private final DecimalFormat decimalFormat;
    private boolean initialized = false;

    public WirelessNotificationTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        Font font = Minecraft.getInstance().font;

        if (this.getMenu().isWUT()) {
            this.addToLeftToolbar(new CycleTerminalButton((btn) -> this.cycleTerminal()));
        }

        this.errorTextColor = style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();
        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();

        this.decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        this.decimalFormat.setParseBigDecimal(true);
        this.decimalFormat.setNegativePrefix("-");

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            boundSlot[i] = -1;
            rowType[i] = NumberEntryType.of(null);
        }

        this.scrollbar.setRange(0, Math.max(0, FILTER_SLOTS - VISIBLE_ROWS), 1);
        this.widgets.add("scrollbar", scrollbar);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int r = row;

            var tf = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
            tf.setBordered(false);
            tf.setMaxLength(16);
            tf.setResponder(text -> onLimitChanged(r));
            tf.setOnConfirm(this::onClose);

            this.limitFields[row] = tf;
            this.widgets.add("limit_" + row, tf);
        }

        this.hudXField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
        hudXField.setBordered(false);
        hudXField.setMaxLength(3);
        hudXField.setFilter((x) -> x.chars().allMatch(Character::isDigit));
        hudXField.setResponder(x -> {
            if (x.isEmpty()) return;
            getMenu().setHudX(Math.max(0, Math.min(100, Integer.parseInt(x))));
        });
        hudXField.setTooltip(Tooltip.create(Component.literal("Hud X in % (0-100)")));
        this.widgets.add("hud_x", hudXField);

        this.hudYField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
        hudYField.setBordered(false);
        hudYField.setMaxLength(3);
        hudYField.setFilter((x) -> x.chars().allMatch(Character::isDigit));
        hudYField.setResponder(x -> {
            if (x.isEmpty()) return;
            getMenu().setHudY(Math.max(0, Math.min(100, Integer.parseInt(x))));
        });
        hudYField.setTooltip(Tooltip.create(Component.literal("Hud Y in % (0-100)")));
        this.widgets.add("hud_y", hudYField);

        hideAboveCb = new AECheckbox(0, 0, 170, 14, style, Component.empty());
        hideAboveCb.setTooltip(Tooltip.create(Component.literal("Do not render entries that are >= threshold")));
        hideAboveCb.setChangeListener(() -> getMenu().setHideAbove(hideAboveCb.isSelected()));
        this.widgets.add("hide_above", hideAboveCb);

        hideBelowCb = new AECheckbox(0, 0, 170, 14, style, Component.empty());
        hideBelowCb.setTooltip(Tooltip.create(Component.literal("Do not render entries that are < threshold")));
        hideBelowCb.setChangeListener(() -> getMenu().setHideBelow(hideBelowCb.isSelected()));
        this.widgets.add("hide_below", hideBelowCb);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            hudYField.setValue(String.valueOf(getMenu().getHudY()));
            hudXField.setValue(String.valueOf(getMenu().getHudX()));

            hideAboveCb.setSelected(getMenu().isHideAbove());
            hideBelowCb.setSelected(getMenu().isHideBelow());

            initialized = true;
        }

        this.scrollbar.setRange(0, Math.max(0, FILTER_SLOTS - VISIBLE_ROWS), 1);
        int offset = this.scrollbar.getCurrentScroll();

        if (offset != lastOffset) {
            repositionConfigSlots(offset);
            lastOffset = offset;
        }

        updateVisibleRows(offset);
    }

    private void repositionConfigSlots(int offset) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.CONFIG);

        for (int i = 0; i < FILTER_SLOTS && i < slots.size(); i++) {
            int row = i - offset;

            Slot s = slots.get(i);
            if (!(s instanceof AppEngSlot slot)) continue;

            boolean inView = row >= 0 && row < VISIBLE_ROWS;

            if (slot instanceof IMovableSlot movable) {
                if (inView) {
                    movable.setX(FILTER_SLOT_X);
                    movable.setY(FILTER_SLOT_Y0 + row * ROW_H);
                    slot.setSlotEnabled(true);
                } else {
                    movable.setX(-10000);
                    movable.setY(-10000);
                    slot.setSlotEnabled(false);
                }
            } else {
                slot.setSlotEnabled(inView);
            }
        }
    }

    private void updateVisibleRows(int offset) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int slotIndex = offset + row;
            boolean valid = slotIndex >= 0 && slotIndex < FILTER_SLOTS;

            if (!valid) {
                boundSlot[row] = -1;
                limitFields[row].setVisible(false);
                continue;
            }

            boolean boundChanged = (boundSlot[row] != slotIndex);
            boundSlot[row] = slotIndex;

            AEKey key = menu.getConfiguredFilter(slotIndex);

            NumberEntryType newType = NumberEntryType.of(key);
            boolean typeChanged = (rowType[row] == null) || !rowType[row].equals(newType);

            if (boundChanged || typeChanged) {
                rowType[row] = newType;

                suppressChange[row] = true;
                try {
                    setTextFieldLongValue(row, menu.getThresholdClient(slotIndex));
                } finally {
                    suppressChange[row] = false;
                }
            }

            limitFields[row].setVisible(true);
            limitFields[row].setEditable(true);
            validateRow(row);
        }
    }

    private void onLimitChanged(int row) {
        if (suppressChange[row]) return;

        int slotIndex = boundSlot[row];
        if (slotIndex < 0) return;

        validateRow(row);

        var v = getRowLongValue(row);
        v.ifPresent(val -> menu.setThreshold(slotIndex, val));
    }

    private Optional<BigDecimal> getRowValueInternal(int row) {
        String textValue = limitFields[row].getValue().trim();
        if (textValue.isEmpty()) return Optional.empty();
        if (textValue.startsWith("=")) textValue = textValue.substring(1);
        return MathExpressionParser.parse(textValue, decimalFormat);
    }

    private boolean isNumberLiteral(int row) {
        var position = new ParsePosition(0);
        var textValue = limitFields[row].getValue().trim();
        if (textValue.startsWith("=")) return false;
        decimalFormat.parse(textValue, position);
        return position.getErrorIndex() == -1 && position.getIndex() == textValue.length();
    }

    private long convertToExternalValue(NumberEntryType type, BigDecimal internalValue) {
        var multiplicand = BigDecimal.valueOf(type.amountPerUnit());
        var value = internalValue.multiply(multiplicand, MathContext.DECIMAL128);
        value = value.setScale(0, RoundingMode.UP);
        return value.longValue();
    }

    private BigDecimal convertToInternalValue(NumberEntryType type, long externalValue) {
        var divisor = BigDecimal.valueOf(type.amountPerUnit());
        return BigDecimal.valueOf(externalValue).divide(divisor, MathContext.DECIMAL128);
    }

    private Optional<Long> getRowLongValue(int row) {
        var text = limitFields[row].getValue().trim();
        if (text.isEmpty()) return Optional.of(0L);

        var type = rowType[row] != null ? rowType[row] : NumberEntryType.of(null);
        var internal = getRowValueInternal(row);
        if (internal.isEmpty()) return Optional.empty();

        if (type.amountPerUnit() == 1 && internal.get().scale() > 0) return Optional.empty();

        long external = convertToExternalValue(type, internal.get());
        if (external < 0) return Optional.empty();
        return Optional.of(external);
    }

    private void setTextFieldLongValue(int row, long value) {
        if (value <= 0) {
            limitFields[row].setValue("");
            limitFields[row].moveCursorToEnd();
            limitFields[row].setHighlightPos(0);
            return;
        }

        var type = rowType[row] != null ? rowType[row] : NumberEntryType.of(null);
        var internal = convertToInternalValue(type, value);
        limitFields[row].setValue(decimalFormat.format(internal));
        limitFields[row].moveCursorToEnd();
        limitFields[row].setHighlightPos(0);
    }

    private void validateRow(int row) {
        var type = rowType[row] != null ? rowType[row] : NumberEntryType.of(null);

        boolean valid = true;
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(
                "gui.crazyae2addons.notification_terminal.unit_line",
                type.unit() == null ? "Items" : type.unit()
        ));

        var text = limitFields[row].getValue().trim();
        if (text.isEmpty()) {
            tooltip.add(Component.literal("Disabled"));
            limitFields[row].setTextColor(normalTextColor);
            limitFields[row].setTooltipMessage(tooltip);
            return;
        }

        var internal = getRowValueInternal(row);
        if (internal.isEmpty()) {
            valid = false;
            tooltip.add(Component.literal("Invalid number"));
        } else {
            if (type.amountPerUnit() == 1 && internal.get().scale() > 0) {
                valid = false;
                tooltip.add(Component.literal("Invalid number"));
            } else {
                long external = convertToExternalValue(type, internal.get());
                if (external < 0) {
                    valid = false;
                    tooltip.add(Component.literal("Invalid number"));
                } else if (!isNumberLiteral(row)) {
                    tooltip.add(Component.literal("= " + decimalFormat.format(internal.get())));
                }
            }
        }

        limitFields[row].setTextColor(valid ? normalTextColor : errorTextColor);
        limitFields[row].setTooltipMessage(tooltip);
    }

    @Override
    public void storeState() {}
}
