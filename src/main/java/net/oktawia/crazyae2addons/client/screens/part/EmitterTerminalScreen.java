package net.oktawia.crazyae2addons.client.screens.part;

import appeng.api.behaviors.ContainerItemStrategies;
import appeng.api.behaviors.EmptyingAction;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.GenericStack;
import appeng.client.Point;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
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
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;
import net.oktawia.crazyae2addons.network.packets.EmitterWindowPacket;

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

public class EmitterTerminalScreen<C extends EmitterTerminalMenu> extends AEBaseScreen<C> {
    private static final int VISIBLE_ROWS = EmitterTerminalMenu.VISIBLE_ROWS;
    private static final int SLOT_X = 10;
    private static final int SLOT_Y = 34;
    private static final int ROW_HEIGHT = 18;
    private static final int VALUE_WIDTH = 40;
    private static final int HIDDEN_POS = -10_000;
    private static final int MAX_DISPLAY_NAME_LENGTH = 13;

    private final Scrollbar scrollbar = new Scrollbar();
    private final ConfirmableTextField[] valueFields = new ConfirmableTextField[VISIBLE_ROWS];
    private final String[] boundUuids = new String[VISIBLE_ROWS];
    private final boolean[] suppressChange = new boolean[VISIBLE_ROWS];
    private final NumberEntryType[] rowTypes = new NumberEntryType[VISIBLE_ROWS];

    private final int errorTextColor;
    private final int normalTextColor;
    private final DecimalFormat decimalFormat;

    private int lastSentOffset = -1;
    private int lastEmitterCount = -1;
    private int lastAppliedRevision = -1;
    private boolean needsRefresh = false;
    private ConfirmableTextField searchField;

    public EmitterTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.errorTextColor = style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();
        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();

        this.decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        this.decimalFormat.setParseBigDecimal(true);
        this.decimalFormat.setNegativePrefix("-");

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            this.rowTypes[i] = NumberEntryType.of(null);
        }

        setupGui();
    }

    private void setupGui() {
        this.searchField = new ConfirmableTextField(this.style, Minecraft.getInstance().font, 0, 0, 0, 0);
        searchField.setBordered(false);
        searchField.setMaxLength(99);
        searchField.setPlaceholder(Component.translatable(LangDefs.EMITTER_TERMINAL_SEARCH.getTranslationKey()));
        searchField.setResponder(newValue -> {
            getMenu().search(newValue);
            scrollbar.setCurrentScroll(0);
            lastSentOffset = -1;
            lastAppliedRevision = -1;
        });
        this.widgets.add("search", searchField);

        this.scrollbar.setRange(0, 0, 1);
        this.widgets.add("scrollbar", this.scrollbar);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int currentRow = row;

            ConfirmableTextField textField = new ConfirmableTextField(
                    this.style,
                    Minecraft.getInstance().font,
                    0,
                    0,
                    VALUE_WIDTH,
                    Minecraft.getInstance().font.lineHeight
            );
            textField.setBordered(false);
            textField.setMaxLength(16);
            textField.setTextColor(normalTextColor);
            textField.setResponder(text -> onValueChanged(currentRow));
            textField.setOnConfirm(this::onClose);

            this.valueFields[row] = textField;
            this.widgets.add("value_" + row, textField);
        }
    }

    public void applyEmitterWindow(EmitterWindowPacket packet) {
        int currentOffset = this.scrollbar.getCurrentScroll();
        if (packet.windowOffset() != currentOffset) {
            return;
        }
        if (packet.revision() < lastAppliedRevision) {
            return;
        }

        lastAppliedRevision = packet.revision();
        getMenu().applyClientWindow(packet);
        needsRefresh = true;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        int totalEmitters = getMenu().totalEmitterCount;
        int maxStart = Math.max(0, totalEmitters - VISIBLE_ROWS);

        this.scrollbar.setRange(0, maxStart, 1);

        int offset = Math.min(this.scrollbar.getCurrentScroll(), maxStart);
        this.scrollbar.setCurrentScroll(offset);

        if (totalEmitters != lastEmitterCount) {
            lastEmitterCount = totalEmitters;
            needsRefresh = true;
        }

        if (offset != lastSentOffset) {
            getMenu().onScroll(offset);
            lastSentOffset = offset;
        }

        if (needsRefresh) {
            List<EmitterTerminalMenu.StorageEmitterInfo> window = getMenu().clientWindow;
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

        InternalInventory inventory = appEngSlot.getInventory();
        EmptyingAction emptyingAction = ContainerItemStrategies.getEmptyingAction(carried);
        if (emptyingAction == null) {
            return null;
        }

        ItemStack wrappedStack = GenericStack.wrapInItemStack(new GenericStack(emptyingAction.what(), 1));
        return inventory.isItemValid(slot.getSlotIndex(), wrappedStack) ? emptyingAction : null;
    }

    private void repositionConfigSlots(int windowSize) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.CONFIG);

        for (int i = 0; i < slots.size(); i++) {
            Slot rawSlot = slots.get(i);
            if (!(rawSlot instanceof AppEngSlot slot)) {
                continue;
            }

            boolean visible = i < windowSize;

            if (slot instanceof IMovableSlot movable) {
                if (visible) {
                    movable.setX(SLOT_X);
                    movable.setY(SLOT_Y + i * ROW_HEIGHT);
                    slot.setSlotEnabled(true);
                    slot.setActive(true);
                } else {
                    movable.setX(HIDDEN_POS);
                    movable.setY(HIDDEN_POS);
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
                clearRow(row);
                continue;
            }

            EmitterTerminalMenu.StorageEmitterInfo emitter = window.get(row);

            String oldUuid = boundUuids[row];
            NumberEntryType oldType = rowTypes[row];

            boundUuids[row] = emitter.uuid();
            rowTypes[row] = NumberEntryType.of(emitter.config() != null ? emitter.config().what() : null);

            boolean uuidChanged = !Objects.equals(oldUuid, emitter.uuid());
            boolean typeChanged = oldType == null || !oldType.equals(rowTypes[row]);

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

    private void clearRow(int row) {
        boundUuids[row] = null;
        rowTypes[row] = NumberEntryType.of(null);
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
    }

    private void onValueChanged(int row) {
        if (suppressChange[row]) {
            return;
        }

        String uuid = boundUuids[row];
        if (uuid == null || uuid.isBlank()) {
            return;
        }

        String text = valueFields[row].getValue();
        if (text == null) {
            return;
        }

        if (text.isBlank()) {
            getMenu().setValue(uuid + "|");
            return;
        }

        validateRow(row);
        getRowLongValue(row).ifPresent(value -> getMenu().setValue(uuid + "|" + value));
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

    private boolean isPlainNumber(int row) {
        ParsePosition position = new ParsePosition(0);
        String textValue = valueFields[row].getValue().trim();
        decimalFormat.parse(textValue, position);
        return position.getErrorIndex() == -1 && position.getIndex() == textValue.length();
    }

    private long convertToExternalValue(NumberEntryType type, BigDecimal internalValue) {
        BigDecimal value = internalValue.multiply(BigDecimal.valueOf(type.amountPerUnit()), MathContext.DECIMAL128);
        return value.setScale(0, RoundingMode.UP).longValue();
    }

    private BigDecimal convertToInternalValue(NumberEntryType type, long externalValue) {
        return BigDecimal.valueOf(externalValue)
                .divide(BigDecimal.valueOf(type.amountPerUnit()), MathContext.DECIMAL128);
    }

    private Optional<Long> getRowLongValue(int row) {
        NumberEntryType type = rowTypes[row] != null ? rowTypes[row] : NumberEntryType.of(null);
        Optional<BigDecimal> internal = getRowValueInternal(row);

        if (internal.isEmpty()) {
            return Optional.empty();
        }
        if (type.amountPerUnit() == 1 && internal.get().scale() > 0) {
            return Optional.empty();
        }

        long external = convertToExternalValue(type, internal.get());
        return external < 0 ? Optional.empty() : Optional.of(external);
    }

    private void setTextFieldLongValue(int row, long value) {
        NumberEntryType type = rowTypes[row] != null ? rowTypes[row] : NumberEntryType.of(null);
        BigDecimal internal = convertToInternalValue(type, Math.max(0L, value));

        valueFields[row].setValue(decimalFormat.format(internal));
        valueFields[row].moveCursorToEnd();
        valueFields[row].setHighlightPos(0);
    }

    private String formatExternalValue(int row, long value) {
        NumberEntryType type = rowTypes[row] != null ? rowTypes[row] : NumberEntryType.of(null);
        return decimalFormat.format(convertToInternalValue(type, Math.max(0L, value)));
    }

    private void validateRow(int row) {
        NumberEntryType type = rowTypes[row] != null ? rowTypes[row] : NumberEntryType.of(null);

        boolean valid = true;
        List<Component> tooltip = new ArrayList<>();

        Object unitArg = type.unit() == null
                ? Component.translatable(LangDefs.ITEMS.getTranslationKey())
                : type.unit();

        tooltip.add(Component.translatable(
                LangDefs.MULTI_EMITTER_UNIT_LINE.getTranslationKey(),
                unitArg
        ));

        Optional<BigDecimal> internal = getRowValueInternal(row);
        if (internal.isEmpty()) {
            valid = false;
            tooltip.add(Component.translatable(LangDefs.INVALID_NUMBER.getTranslationKey()));
        } else if (type.amountPerUnit() == 1 && internal.get().scale() > 0) {
            valid = false;
            tooltip.add(Component.translatable(LangDefs.INVALID_NUMBER.getTranslationKey()));
        } else {
            long external = convertToExternalValue(type, internal.get());
            if (external < 0) {
                valid = false;
                tooltip.add(Component.translatable(LangDefs.INVALID_NUMBER.getTranslationKey()));
            } else if (!isPlainNumber(row)) {
                tooltip.add(Component.literal("= " + decimalFormat.format(internal.get())));
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
        if (text.length() > MAX_DISPLAY_NAME_LENGTH) {
            text = text.substring(0, MAX_DISPLAY_NAME_LENGTH) + "...";
        }

        return Component.literal(text);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        boolean anyFieldFocused = false;

        for (ConfirmableTextField field : valueFields) {
            if (field.isFocused()) {
                anyFieldFocused = true;
                break;
            }
        }

        if (!anyFieldFocused) {
            return scrollbar.onMouseWheel(
                    new Point((int) Math.round(x - leftPos), (int) Math.round(y - topPos)),
                    delta
            );
        }

        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) {
            if (searchField.isMouseOver(mouseX, mouseY)) {
                searchField.setValue("");
                searchField.setFocused(true);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }
}