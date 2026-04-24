package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.BackgroundPanel;
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
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.item.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addons.network.packets.WirelessNotificationWindowPacket;
import net.oktawia.crazyae2addons.util.MathParser;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class WirelessNotificationTerminalScreen<C extends WirelessNotificationTerminalMenu>
        extends UpgradeableScreen<C> implements IUniversalTerminalCapable {

    private static final int SLOT_X = 10;
    private static final int SLOT_Y = 34;
    private static final int ROW_HEIGHT = 18;
    private static final int HIDDEN_POS = -10_000;

    private static final int VISIBLE_ROWS = WirelessNotificationTerminalMenu.VISIBLE_ROWS;

    private final Scrollbar scrollbar = new Scrollbar();

    private final int[] boundSlot = new int[VISIBLE_ROWS];
    private final NumberEntryType[] rowType = new NumberEntryType[VISIBLE_ROWS];
    private final ConfirmableTextField[] limitFields = new ConfirmableTextField[VISIBLE_ROWS];
    private final boolean[] suppressChange = new boolean[VISIBLE_ROWS];

    private final ConfirmableTextField hudXField;
    private final ConfirmableTextField hudYField;
    private final ConfirmableTextField hudScaleField;
    private final AECheckbox hideAboveCb;
    private final AECheckbox hideBelowCb;

    private final int errorTextColor;
    private final int normalTextColor;
    private final DecimalFormat decimalFormat;

    private boolean initialized = false;
    private int lastSentOffset = -1;
    private int lastTotalCount = -1;
    private int lastAppliedRevision = -1;
    private boolean needsRefresh = true;

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

        this.scrollbar.setRange(0, 0, 1);
        this.widgets.add("scrollbar", this.scrollbar);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int rowIndex = row;

            ConfirmableTextField field = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
            field.setBordered(false);
            field.setMaxLength(32);
            field.setTextColor(normalTextColor);
            field.setResponder(text -> onLimitChanged(rowIndex));
            field.setOnConfirm(this::onClose);

            this.limitFields[row] = field;
            this.widgets.add("limit_" + row, field);
        }
        this.hudXField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
        this.hudXField.setBordered(false);
        this.hudXField.setMaxLength(32);
        this.hudXField.setResponder(text ->
                parsePercentField(text).ifPresent(getMenu()::setHudX)
        );
        this.hudXField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.NOTIFICATION_TERMINAL_HUD_X_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("hud_x", this.hudXField);

        this.hudYField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
        this.hudYField.setBordered(false);
        this.hudYField.setMaxLength(32);
        this.hudYField.setResponder(text ->
                parsePercentField(text).ifPresent(getMenu()::setHudY)
        );
        this.hudYField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.NOTIFICATION_TERMINAL_HUD_Y_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("hud_y", this.hudYField);

        this.hudScaleField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
        this.hudScaleField.setBordered(false);
        this.hudScaleField.setMaxLength(32);
        this.hudScaleField.setResponder(text ->
                parsePercentField(text).ifPresent(getMenu()::setHudScale)
        );
        this.hudScaleField.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.NOTIFICATION_TERMINAL_HUD_SCALE_TOOLTIP.getTranslationKey())
        ));
        this.widgets.add("hud_scale", this.hudScaleField);

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

    private void layoutConfigSlots() {
        var configSlots = getMenu().getSlots(SlotSemantics.CONFIG);
        int visibleCount = Math.min(VISIBLE_ROWS, Math.max(0, getMenu().totalCount));

        for (int row = 0; row < configSlots.size(); row++) {
            var slot = configSlots.get(row);
            boolean visible = row < visibleCount;

            if (!(slot instanceof AppEngSlot aeSlot)) {
                continue;
            }

            if (slot instanceof IMovableSlot movable) {
                if (visible) {
                    movable.setX(SLOT_X);
                    movable.setY(SLOT_Y + row * ROW_HEIGHT);
                    aeSlot.setSlotEnabled(true);
                    aeSlot.setActive(true);
                } else {
                    movable.setX(HIDDEN_POS);
                    movable.setY(HIDDEN_POS);
                    aeSlot.setSlotEnabled(false);
                    aeSlot.setActive(false);
                }
            } else {
                aeSlot.setSlotEnabled(visible);
                aeSlot.setActive(visible);
            }
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        layoutConfigSlots();

        if (!initialized) {
            this.hudXField.setValue(String.valueOf(getMenu().getHudX()));
            this.hudYField.setValue(String.valueOf(getMenu().getHudY()));
            this.hudScaleField.setValue(String.valueOf(getMenu().getHudScale()));
            this.hideAboveCb.setSelected(getMenu().isHideAbove());
            this.hideBelowCb.setSelected(getMenu().isHideBelow());
            this.initialized = true;
            this.needsRefresh = true;
        }

        if (!hudXField.isFocused()) {
            String expected = String.valueOf(getMenu().getHudX());
            if (!expected.equals(hudXField.getValue())) {
                hudXField.setValue(expected);
            }
        }

        if (!hudYField.isFocused()) {
            String expected = String.valueOf(getMenu().getHudY());
            if (!expected.equals(hudYField.getValue())) {
                hudYField.setValue(expected);
            }
        }

        if (!hudScaleField.isFocused()) {
            String expected = String.valueOf(getMenu().getHudScale());
            if (!expected.equals(hudScaleField.getValue())) {
                hudScaleField.setValue(expected);
            }
        }

        if (hideAboveCb.isSelected() != getMenu().isHideAbove()) {
            hideAboveCb.setSelected(getMenu().isHideAbove());
        }

        if (hideBelowCb.isSelected() != getMenu().isHideBelow()) {
            hideBelowCb.setSelected(getMenu().isHideBelow());
        }

        int totalCount = Math.max(0, getMenu().totalCount);
        int maxOffset = Math.max(0, totalCount - VISIBLE_ROWS);

        this.scrollbar.setRange(0, maxOffset, 1);

        int offset = Math.min(this.scrollbar.getCurrentScroll(), maxOffset);
        this.scrollbar.setCurrentScroll(offset);

        if (offset != lastSentOffset) {
            getMenu().onScroll(offset);
            lastSentOffset = offset;
        }

        if (totalCount != lastTotalCount) {
            lastTotalCount = totalCount;
            needsRefresh = true;
        }

        if (getMenu().clientWindowRevision != lastAppliedRevision) {
            lastAppliedRevision = getMenu().clientWindowRevision;
            needsRefresh = true;
        }

        if (needsRefresh) {
            refreshVisibleRows(getMenu().clientWindowOffset, getMenu().clientWindow, totalCount);
            needsRefresh = false;
        }
    }

    private void refreshVisibleRows(
            int windowOffset,
            List<WirelessNotificationTerminalMenu.NotificationSlotInfo> window,
            int totalCount
    ) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int absoluteSlot = windowOffset + row;
            boolean valid = row < window.size() && absoluteSlot < totalCount;

            if (!valid) {
                clearRow(row);
                continue;
            }

            var entry = window.get(row);

            int oldBoundSlot = boundSlot[row];
            NumberEntryType oldType = rowType[row];

            boundSlot[row] = absoluteSlot;
            rowType[row] = NumberEntryType.of(entry.config() != null ? entry.config().what() : null);

            boolean slotChanged = oldBoundSlot != absoluteSlot;
            boolean typeChanged = oldType == null || !oldType.equals(rowType[row]);

            if (slotChanged || typeChanged || !limitFields[row].isFocused()) {
                suppressChange[row] = true;
                try {
                    setTextFieldLongValue(row, Math.max(0L, entry.threshold()));
                } finally {
                    suppressChange[row] = false;
                }
            }

            limitFields[row].setVisible(true);
            limitFields[row].setEditable(true);
            limitFields[row].active = true;

            validateRow(row);
        }
    }

    private void clearRow(int row) {
        boundSlot[row] = -1;
        rowType[row] = NumberEntryType.of(null);

        suppressChange[row] = true;
        try {
            limitFields[row].setValue("");
        } finally {
            suppressChange[row] = false;
        }

        limitFields[row].setTooltipMessage(List.of());
        limitFields[row].setTextColor(normalTextColor);
        limitFields[row].setVisible(false);
        limitFields[row].setEditable(false);
        limitFields[row].active = false;
    }

    private void onLimitChanged(int row) {
        if (suppressChange[row]) {
            return;
        }

        int slotIndex = boundSlot[row];
        if (slotIndex < 0) {
            return;
        }

        validateRow(row);

        Optional<Long> value = getRowLongValue(row);
        value.ifPresent(v -> getMenu().setThreshold(slotIndex, v));
    }

    private Optional<BigDecimal> getRowValueInternal(int row) {
        String textValue = this.limitFields[row].getValue();
        if (textValue == null) {
            return Optional.empty();
        }

        textValue = textValue.trim();
        if (textValue.isEmpty()) {
            return Optional.of(BigDecimal.ZERO);
        }

        if (textValue.startsWith("=")) {
            textValue = textValue.substring(1).trim();
        }

        if (!MathParser.canParse(textValue)) {
            return Optional.empty();
        }

        try {
            return Optional.of(BigDecimal.valueOf(MathParser.parse(textValue)).stripTrailingZeros());
        } catch (Throwable ignored) {
            return Optional.empty();
        }
    }

    private boolean isNumberLiteral(int row) {
        String textValue = this.limitFields[row].getValue();
        if (textValue == null) {
            return false;
        }

        textValue = textValue.trim();
        if (textValue.isEmpty() || textValue.startsWith("=")) {
            return false;
        }

        return MathParser.isLiteralNumber(textValue);
    }

    private Optional<Integer> parsePercentField(String text) {
        if (text == null) {
            return Optional.empty();
        }

        text = text.trim();
        if (text.isEmpty()) {
            return Optional.empty();
        }

        if (text.startsWith("=")) {
            text = text.substring(1).trim();
        }

        if (!MathParser.canParse(text)) {
            return Optional.empty();
        }

        try {
            double parsed = MathParser.parse(text);
            if (!Double.isFinite(parsed)) {
                return Optional.empty();
            }

            return Optional.of(Math.max(0, Math.min(100, (int) Math.round(parsed))));
        } catch (Throwable ignored) {
            return Optional.empty();
        }
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
            return;
        }

        NumberEntryType type = this.rowType[row] != null ? this.rowType[row] : NumberEntryType.of(null);
        BigDecimal internal = convertToInternalValue(type, value);

        this.limitFields[row].setValue(this.decimalFormat.format(internal));
    }

    private void validateRow(int row) {
        NumberEntryType type = this.rowType[row] != null ? this.rowType[row] : NumberEntryType.of(null);

        boolean valid = true;
        List<Component> tooltip = new ArrayList<>();
        tooltip.add(Component.translatable(
                LangDefs.NOTIFICATION_TERMINAL_UNIT_LINE.getTranslationKey(),
                type.unit() == null
                        ? Component.translatable(LangDefs.ITEMS.getTranslationKey())
                        : type.unit()
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
        if (button == 0) {
            if (this.hudXField != null && this.hudXField.visible && this.hudXField.active && this.hudXField.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.hudXField);
                this.hudXField.setFocused(true);
                return true;
            }

            if (this.hudYField != null && this.hudYField.visible && this.hudYField.active && this.hudYField.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.hudYField);
                this.hudYField.setFocused(true);
                return true;
            }

            if (this.hudScaleField != null && this.hudScaleField.visible && this.hudScaleField.active && this.hudScaleField.mouseClicked(mouseX, mouseY, button)) {
                this.setFocused(this.hudScaleField);
                this.hudScaleField.setFocused(true);
                return true;
            }

            for (ConfirmableTextField field : this.limitFields) {
                if (field != null && field.visible && field.active && field.mouseClicked(mouseX, mouseY, button)) {
                    this.setFocused(field);
                    field.setFocused(true);
                    return true;
                }
            }
        }

        if (button == 1) {
            if (this.hudXField != null && this.hudXField.isMouseOver(mouseX, mouseY)) {
                this.hudXField.setValue("");
                this.setFocused(this.hudXField);
                this.hudXField.setFocused(true);
                return true;
            }

            if (this.hudYField != null && this.hudYField.isMouseOver(mouseX, mouseY)) {
                this.hudYField.setValue("");
                this.setFocused(this.hudYField);
                this.hudYField.setFocused(true);
                return true;
            }

            if (this.hudScaleField != null && this.hudScaleField.isMouseOver(mouseX, mouseY)) {
                this.hudScaleField.setValue("");
                this.setFocused(this.hudScaleField);
                this.hudScaleField.setFocused(true);
                return true;
            }

            for (ConfirmableTextField field : this.limitFields) {
                if (field != null && field.visible && field.isMouseOver(mouseX, mouseY)) {
                    field.setValue("");
                    this.setFocused(field);
                    field.setFocused(true);
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void applyClientWindow(WirelessNotificationWindowPacket pkt) {
        int currentOffset = this.scrollbar.getCurrentScroll();
        if (pkt.windowOffset() != currentOffset) {
            return;
        }
        if (pkt.revision() < lastAppliedRevision) {
            return;
        }

        getMenu().applyClientWindow(pkt);
        lastAppliedRevision = pkt.revision();
        needsRefresh = true;
    }
}