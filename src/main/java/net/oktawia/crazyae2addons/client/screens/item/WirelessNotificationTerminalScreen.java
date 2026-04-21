package net.oktawia.crazyae2addons.client.screens.item;

import appeng.api.stacks.AEKey;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.*;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import de.mari_023.ae2wtlib.wut.CycleTerminalButton;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.item.WirelessNotificationTerminalMenu;

public class WirelessNotificationTerminalScreen<C extends WirelessNotificationTerminalMenu>
        extends UpgradeableScreen<C> implements IUniversalTerminalCapable {

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

    private final AETextField hudXField;
    private final AETextField hudYField;
    private final AECheckbox hideAboveCb;
    private final AECheckbox hideBelowCb;

    private final int errorTextColor;
    private final int normalTextColor;
    private final DecimalFormat decimalFormat;

    private boolean initialized = false;

    public WirelessNotificationTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        Font font = Minecraft.getInstance().font;

        if (getMenu().isWUT()) {
            addToLeftToolbar(new CycleTerminalButton(btn -> cycleTerminal()));
        }
        this.widgets.add("singularityBackground", new BackgroundPanel(style.getImage("singularityBackground")));

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
        this.widgets.add("scrollbar", this.scrollbar);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int rowIndex = row;

            ConfirmableTextField field = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
            field.setBordered(false);
            field.setMaxLength(16);
            field.setResponder(text -> onLimitChanged(rowIndex));
            field.setOnConfirm(this::onClose);

            this.limitFields[row] = field;
            this.widgets.add("limit_" + row, field);
        }

        this.hudXField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
        this.hudXField.setBordered(false);
        this.hudXField.setMaxLength(3);
        this.hudXField.setFilter(text -> text.chars().allMatch(Character::isDigit));
        this.hudXField.setResponder(text -> {
            if (text.isEmpty()) {
                return;
            }
            getMenu().setHudX(Math.max(0, Math.min(100, Integer.parseInt(text))));
        });
        this.hudXField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.NOTIFICATION_TERMINAL_HUD_X_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("hud_x", this.hudXField);

        this.hudYField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
        this.hudYField.setBordered(false);
        this.hudYField.setMaxLength(3);
        this.hudYField.setFilter(text -> text.chars().allMatch(Character::isDigit));
        this.hudYField.setResponder(text -> {
            if (text.isEmpty()) {
                return;
            }
            getMenu().setHudY(Math.max(0, Math.min(100, Integer.parseInt(text))));
        });
        this.hudYField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.NOTIFICATION_TERMINAL_HUD_Y_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("hud_y", this.hudYField);

        this.hideAboveCb = new AECheckbox(0, 0, 170, 14, style, Component.empty());
        this.hideAboveCb.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.NOTIFICATION_TERMINAL_HIDE_ABOVE_TOOLTIP.getTranslationKey())
        ));
        this.hideAboveCb.setChangeListener(() -> getMenu().setHideAbove(this.hideAboveCb.isSelected()));
        this.widgets.add("hide_above", this.hideAboveCb);

        this.hideBelowCb = new AECheckbox(0, 0, 170, 14, style, Component.empty());
        this.hideBelowCb.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.NOTIFICATION_TERMINAL_HIDE_BELOW_TOOLTIP.getTranslationKey())
        ));
        this.hideBelowCb.setChangeListener(() -> getMenu().setHideBelow(this.hideBelowCb.isSelected()));
        this.widgets.add("hide_below", this.hideBelowCb);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            this.hudXField.setValue(String.valueOf(getMenu().getHudX()));
            this.hudYField.setValue(String.valueOf(getMenu().getHudY()));
            this.hideAboveCb.setSelected(getMenu().isHideAbove());
            this.hideBelowCb.setSelected(getMenu().isHideBelow());
            this.initialized = true;
        }

        this.scrollbar.setRange(0, Math.max(0, FILTER_SLOTS - VISIBLE_ROWS), 1);
        int offset = this.scrollbar.getCurrentScroll();

        if (offset != this.lastOffset) {
            repositionConfigSlots(offset);
            this.lastOffset = offset;
        }

        updateVisibleRows(offset);
    }

    private void repositionConfigSlots(int offset) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.CONFIG);

        for (int i = 0; i < FILTER_SLOTS && i < slots.size(); i++) {
            int row = i - offset;

            Slot slotRaw = slots.get(i);
            if (!(slotRaw instanceof AppEngSlot slot)) {
                continue;
            }

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
                this.boundSlot[row] = -1;
                this.limitFields[row].setVisible(false);
                continue;
            }

            boolean boundChanged = this.boundSlot[row] != slotIndex;
            this.boundSlot[row] = slotIndex;

            AEKey key = this.menu.getConfiguredFilter(slotIndex);
            NumberEntryType newType = NumberEntryType.of(key);
            boolean typeChanged = this.rowType[row] == null || !this.rowType[row].equals(newType);

            if (boundChanged || typeChanged) {
                this.rowType[row] = newType;

                this.suppressChange[row] = true;
                try {
                    setTextFieldLongValue(row, this.menu.getThresholdClient(slotIndex));
                } finally {
                    this.suppressChange[row] = false;
                }
            }

            this.limitFields[row].setVisible(true);
            this.limitFields[row].setEditable(true);
            validateRow(row);
        }
    }

    private void onLimitChanged(int row) {
        if (this.suppressChange[row]) {
            return;
        }

        int slotIndex = this.boundSlot[row];
        if (slotIndex < 0) {
            return;
        }

        validateRow(row);

        Optional<Long> value = getRowLongValue(row);
        value.ifPresent(v -> this.menu.setThreshold(slotIndex, v));
    }

    private Optional<BigDecimal> getRowValueInternal(int row) {
        String textValue = this.limitFields[row].getValue().trim();
        if (textValue.isEmpty()) {
            return Optional.empty();
        }
        if (textValue.startsWith("=")) {
            textValue = textValue.substring(1);
        }
        return MathExpressionParser.parse(textValue, this.decimalFormat);
    }

    private boolean isNumberLiteral(int row) {
        ParsePosition position = new ParsePosition(0);
        String textValue = this.limitFields[row].getValue().trim();
        if (textValue.startsWith("=")) {
            return false;
        }

        this.decimalFormat.parse(textValue, position);
        return position.getErrorIndex() == -1 && position.getIndex() == textValue.length();
    }

    private long convertToExternalValue(NumberEntryType type, BigDecimal internalValue) {
        BigDecimal multiplicand = BigDecimal.valueOf(type.amountPerUnit());
        BigDecimal value = internalValue.multiply(multiplicand, MathContext.DECIMAL128);
        value = value.setScale(0, RoundingMode.UP);
        return value.longValue();
    }

    private BigDecimal convertToInternalValue(NumberEntryType type, long externalValue) {
        BigDecimal divisor = BigDecimal.valueOf(type.amountPerUnit());
        return BigDecimal.valueOf(externalValue).divide(divisor, MathContext.DECIMAL128);
    }

    private Optional<Long> getRowLongValue(int row) {
        String text = this.limitFields[row].getValue().trim();
        if (text.isEmpty()) {
            return Optional.of(0L);
        }

        NumberEntryType type = this.rowType[row] != null ? this.rowType[row] : NumberEntryType.of(null);
        Optional<BigDecimal> internal = getRowValueInternal(row);
        if (internal.isEmpty()) {
            return Optional.empty();
        }

        if (type.amountPerUnit() == 1 && internal.get().scale() > 0) {
            return Optional.empty();
        }

        long external = convertToExternalValue(type, internal.get());
        if (external < 0) {
            return Optional.empty();
        }

        return Optional.of(external);
    }

    private void setTextFieldLongValue(int row, long value) {
        if (value <= 0) {
            this.limitFields[row].setValue("");
            this.limitFields[row].moveCursorToEnd();
            this.limitFields[row].setHighlightPos(0);
            return;
        }

        NumberEntryType type = this.rowType[row] != null ? this.rowType[row] : NumberEntryType.of(null);
        BigDecimal internal = convertToInternalValue(type, value);

        this.limitFields[row].setValue(this.decimalFormat.format(internal));
        this.limitFields[row].moveCursorToEnd();
        this.limitFields[row].setHighlightPos(0);
    }

    private void validateRow(int row) {
        NumberEntryType type = this.rowType[row] != null ? this.rowType[row] : NumberEntryType.of(null);

        boolean valid = true;
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(
                LangDefs.NOTIFICATION_TERMINAL_UNIT_LINE.getTranslationKey(),
                type.unit() == null ? "Items" : type.unit()
        ));

        String text = this.limitFields[row].getValue().trim();
        if (text.isEmpty()) {
            tooltip.add(Component.translatable(LangDefs.NOTIFICATION_TERMINAL_DISABLED.getTranslationKey()));
            this.limitFields[row].setTextColor(this.normalTextColor);
            this.limitFields[row].setTooltipMessage(tooltip);
            return;
        }

        Optional<BigDecimal> internal = getRowValueInternal(row);
        if (internal.isEmpty()) {
            valid = false;
            tooltip.add(Component.translatable(LangDefs.NOTIFICATION_TERMINAL_INVALID_NUMBER.getTranslationKey()));
        } else {
            if (type.amountPerUnit() == 1 && internal.get().scale() > 0) {
                valid = false;
                tooltip.add(Component.translatable(LangDefs.NOTIFICATION_TERMINAL_INVALID_NUMBER.getTranslationKey()));
            } else {
                long external = convertToExternalValue(type, internal.get());
                if (external < 0) {
                    valid = false;
                    tooltip.add(Component.translatable(LangDefs.NOTIFICATION_TERMINAL_INVALID_NUMBER.getTranslationKey()));
                } else if (!isNumberLiteral(row)) {
                    tooltip.add(Component.literal("= " + this.decimalFormat.format(internal.get())));
                }
            }
        }

        this.limitFields[row].setTextColor(valid ? this.normalTextColor : this.errorTextColor);
        this.limitFields[row].setTooltipMessage(tooltip);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void storeState() {
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (this.hudXField != null && this.hudXField.isMouseOver(mouseX, mouseY)) {
                this.hudXField.setValue("");
                this.hudXField.setFocused(true);
                return true;
            }

            if (this.hudYField != null && this.hudYField.isMouseOver(mouseX, mouseY)) {
                this.hudYField.setValue("");
                this.hudYField.setFocused(true);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}