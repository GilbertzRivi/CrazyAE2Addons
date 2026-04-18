package net.oktawia.crazyae2addons.screens;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.EmitterTerminalMenu;
import net.oktawia.crazyae2addons.network.EmitterWindowPacket;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class EmitterTerminalScreen<C extends EmitterTerminalMenu> extends UpgradeableScreen<C> {

    private static final int VISIBLE_ROWS = EmitterTerminalMenu.VISIBLE_ROWS;
    private static final int SLOT_X = 10;
    private static final int SLOT_Y0 = 34;
    private static final int ROW_H = 18;
    private static final int VALUE_W = 40;

    private final Scrollbar scrollbar = new Scrollbar();
    private final ConfirmableTextField[] valueFields = new ConfirmableTextField[VISIBLE_ROWS];
    private final String[] boundUuid = new String[VISIBLE_ROWS];
    private final boolean[] suppressChange = new boolean[VISIBLE_ROWS];
    private final NumberEntryType[] rowType = new NumberEntryType[VISIBLE_ROWS];

    private final int errorTextColor;
    private final int normalTextColor;
    private final DecimalFormat decimalFormat;

    private int lastSentOffset = -1;
    private int lastEmitterCount = -1;
    private int lastAppliedRevision = -1;
    private boolean needsRefresh = false;

    public EmitterTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.errorTextColor = style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();
        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();

        this.decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        this.decimalFormat.setParseBigDecimal(true);
        this.decimalFormat.setNegativePrefix("-");

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            this.rowType[i] = NumberEntryType.of(null);
        }

        setupGui();
    }

    private void setupGui() {
        var searchField = new ConfirmableTextField(this.style, Minecraft.getInstance().font, 0, 0, 0, 0);
        searchField.setBordered(false);
        searchField.setMaxLength(99);
        searchField.setPlaceholder(Component.translatable("gui.crazyae2addons.redstone_terminal_search"));
        searchField.setResponder(newVal -> {
            getMenu().search(newVal);
            scrollbar.setCurrentScroll(0);
            lastSentOffset = -1;
            lastAppliedRevision = -1;
        });
        this.widgets.add("search", searchField);

        this.scrollbar.setRange(0, 0, 1);
        this.widgets.add("scrollbar", this.scrollbar);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int r = row;

            var tf = new ConfirmableTextField(
                    this.style,
                    Minecraft.getInstance().font,
                    0, 0,
                    VALUE_W,
                    Minecraft.getInstance().font.lineHeight
            );
            tf.setBordered(false);
            tf.setMaxLength(16);
            tf.setTextColor(normalTextColor);
            tf.setResponder(text -> onValueChanged(r));
            tf.setOnConfirm(this::onClose);

            this.valueFields[row] = tf;
            this.widgets.add("value_" + row, tf);
        }
    }

    public void applyEmitterWindow(EmitterWindowPacket pkt) {
        int currentOffset = this.scrollbar.getCurrentScroll();

        if (pkt.windowOffset() != currentOffset) {
            return;
        }

        if (pkt.revision() < lastAppliedRevision) {
            return;
        }

        lastAppliedRevision = pkt.revision();
        getMenu().applyClientWindow(pkt);
        needsRefresh = true;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        int total = getMenu().totalEmitterCount;
        int maxStart = Math.max(0, total - VISIBLE_ROWS);

        this.scrollbar.setRange(0, maxStart, 1);

        int offset = Math.min(this.scrollbar.getCurrentScroll(), maxStart);
        this.scrollbar.setCurrentScroll(offset);

        if (total != lastEmitterCount) {
            needsRefresh = true;
            lastEmitterCount = total;
        }

        if (offset != lastSentOffset) {
            getMenu().onScroll(offset);
            lastSentOffset = offset;
        }

        if (needsRefresh) {
            var window = getMenu().clientWindow;
            repositionConfigSlots(window.size());
            refreshRows(window);
            needsRefresh = false;
        }
    }

    @Override
    protected EmptyingAction getEmptyingAction(Slot slot, ItemStack carried) {
        if (!(slot instanceof AppEngSlot appEngSlot) || carried.isEmpty()) {
            return null;
        }

        if (getMenu().getSlotSemantic(slot) != SlotSemantics.CONFIG) {
            return null;
        }

        InternalInventory inv = appEngSlot.getInventory();

        var emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
        if (emptyingAction == null) {
            return null;
        }

        var wrappedStack = GenericStack.wrapInItemStack(new GenericStack(emptyingAction.what(), 1));
        return inv.isItemValid(slot.getSlotIndex(), wrappedStack) ? emptyingAction : null;
    }

    private void repositionConfigSlots(int windowSize) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.CONFIG);

        for (int i = 0; i < slots.size(); i++) {
            Slot s = slots.get(i);
            if (!(s instanceof AppEngSlot slot)) {
                continue;
            }

            boolean visible = i < windowSize;

            if (slot instanceof IMovableSlot movable) {
                if (visible) {
                    movable.setX(SLOT_X);
                    movable.setY(SLOT_Y0 + i * ROW_H);
                    slot.setSlotEnabled(true);
                    slot.setActive(true);
                } else {
                    movable.setX(-10000);
                    movable.setY(-10000);
                    slot.setSlotEnabled(false);
                    slot.setActive(false);
                }
            } else {
                slot.setSlotEnabled(visible);
                slot.setActive(visible);
            }
        }
    }

    private void refreshRows(List<EmitterTerminalMenu.StorageEmitterInfo> window) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            if (row >= window.size()) {
                boundUuid[row] = null;
                rowType[row] = NumberEntryType.of(null);
                setTextContent("label_" + row, Component.empty());

                if (!valueFields[row].isFocused()) {
                    suppressChange[row] = true;
                    try {
                        valueFields[row].setValue("");
                    } finally {
                        suppressChange[row] = false;
                    }
                }

                valueFields[row].setTooltipMessage(List.of());
                valueFields[row].setTextColor(normalTextColor);
                valueFields[row].setVisible(true);
                valueFields[row].setEditable(false);
                valueFields[row].active = false;
                continue;
            }

            var emitter = window.get(row);

            String oldUuid = boundUuid[row];
            NumberEntryType oldType = rowType[row];

            boundUuid[row] = emitter.uuid();
            rowType[row] = NumberEntryType.of(emitter.config() != null ? emitter.config().what() : null);

            boolean uuidChanged = !Objects.equals(oldUuid, emitter.uuid());
            boolean typeChanged = oldType == null || !oldType.equals(rowType[row]);

            long rawValue = emitter.value() == null ? 0L : emitter.value();
            String serverValue = formatExternalValue(row, rawValue);
            boolean fieldOutOfSync = !Objects.equals(valueFields[row].getValue(), serverValue);

            if (uuidChanged || typeChanged || (fieldOutOfSync && !valueFields[row].isFocused())) {
                setTextContent("label_" + row, getDisplayName(emitter));

                if (!valueFields[row].isFocused()) {
                    suppressChange[row] = true;
                    try {
                        setTextFieldLongValue(row, rawValue);
                    } finally {
                        suppressChange[row] = false;
                    }
                }
            }

            valueFields[row].setVisible(true);
            valueFields[row].setEditable(true);
            valueFields[row].active = true;
            validateRow(row);
        }
    }

    private void onValueChanged(int row) {
        if (suppressChange[row]) {
            return;
        }

        String uuid = boundUuid[row];
        if (uuid == null || uuid.isBlank()) {
            return;
        }

        validateRow(row);

        Optional<Long> value = getRowLongValue(row);
        value.ifPresent(aLong -> getMenu().setValue(uuid + "|" + aLong));
    }

    private Optional<BigDecimal> getRowValueInternal(int row) {
        String textValue = valueFields[row].getValue();
        if (textValue == null) {
            return Optional.empty();
        }
        if (textValue.startsWith("=")) {
            textValue = textValue.substring(1);
        }
        return MathExpressionParser.parse(textValue, decimalFormat);
    }

    private boolean isNumber(int row) {
        var position = new ParsePosition(0);
        var textValue = valueFields[row].getValue().trim();
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
        var type = rowType[row] != null ? rowType[row] : NumberEntryType.of(null);
        var internal = getRowValueInternal(row);
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
        if (value < 0) {
            value = 0;
        }

        var type = rowType[row] != null ? rowType[row] : NumberEntryType.of(null);
        var internal = convertToInternalValue(type, value);

        valueFields[row].setValue(decimalFormat.format(internal));
        valueFields[row].moveCursorToEnd();
        valueFields[row].setHighlightPos(0);
    }

    private String formatExternalValue(int row, long value) {
        if (value < 0) {
            value = 0;
        }

        var type = rowType[row] != null ? rowType[row] : NumberEntryType.of(null);
        var internal = convertToInternalValue(type, value);
        return decimalFormat.format(internal);
    }

    private void validateRow(int row) {
        var type = rowType[row] != null ? rowType[row] : NumberEntryType.of(null);

        boolean valid = true;
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(Component.translatable(
                "gui.crazyae2addons.multi_emitter.unit_line",
                type.unit() == null ? "Items" : type.unit()
        ));

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
                } else if (!isNumber(row)) {
                    tooltip.add(Component.literal("= " + decimalFormat.format(internal.get())));
                }
            }
        }

        valueFields[row].setTextColor(valid ? normalTextColor : errorTextColor);
        valueFields[row].setTooltipMessage(tooltip);
    }

    private Component getDisplayName(EmitterTerminalMenu.StorageEmitterInfo emitter) {
        if (emitter == null || emitter.name() == null) {
            return Component.empty();
        }

        String text = emitter.name().getString().trim();
        if (text.isBlank()) {
            return Component.empty();
        }

        if (text.length() > 13) {
            text = text.substring(0, 13) + "...";
        }

        return Component.literal(text);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        boolean focus = false;
        for (ConfirmableTextField valueField : valueFields) {
            if (valueField.isFocused()) {
                focus = true;
                break;
            }
        }
        if (!focus) {
            return scrollbar.onMouseWheel(new Point((int) Math.round(x - leftPos), (int) Math.round(y - topPos)), delta);
        }
        return true;
    }
}