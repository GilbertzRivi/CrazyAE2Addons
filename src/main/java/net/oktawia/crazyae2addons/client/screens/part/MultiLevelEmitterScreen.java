package net.oktawia.crazyae2addons.client.screens.part;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.stacks.AEKey;
import appeng.client.gui.Icon;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.part.MultiLevelEmitterMenu;
import net.oktawia.crazyae2addons.parts.MultiLevelEmitter;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class MultiLevelEmitterScreen<C extends MultiLevelEmitterMenu> extends UpgradeableScreen<C> {
    private static final int FILTER_SLOTS = MultiLevelEmitter.FILTER_SLOTS;
    private static final int VISIBLE_ROWS = 6;

    private static final int FILTER_SLOT_X = 10;
    private static final int FILTER_SLOT_Y = 34;
    private static final int ROW_HEIGHT = 18;
    private static final int HIDDEN_POS = -10_000;

    private static final Icon ICON_GE = Icon.REDSTONE_HIGH;
    private static final Icon ICON_LT = Icon.REDSTONE_LOW;

    private final Scrollbar scrollbar = new Scrollbar();
    private final int[] boundSlots = new int[VISIBLE_ROWS];
    private final NumberEntryType[] rowTypes = new NumberEntryType[VISIBLE_ROWS];
    private final ConfirmableTextField[] limitFields = new ConfirmableTextField[VISIBLE_ROWS];
    private final boolean[] suppressChange = new boolean[VISIBLE_ROWS];
    private final ToggleButton[] compareButtons = new ToggleButton[VISIBLE_ROWS];
    private final SettingToggleButton<FuzzyMode> fuzzyModeButton;

    private final DecimalFormat decimalFormat;
    private final int normalTextColor;
    private final int errorTextColor;

    private Button logicButton;

    private int lastOffset = Integer.MIN_VALUE;
    private int lastLeftPos = Integer.MIN_VALUE;
    private int lastTopPos = Integer.MIN_VALUE;
    private int lastWidth = Integer.MIN_VALUE;
    private int lastHeight = Integer.MIN_VALUE;

    public MultiLevelEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        Arrays.fill(boundSlots, -1);
        Arrays.fill(rowTypes, NumberEntryType.of(null));

        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();
        this.errorTextColor = style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();

        this.decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        this.decimalFormat.setParseBigDecimal(true);
        this.decimalFormat.setNegativePrefix("-");

        this.fuzzyModeButton = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        addToLeftToolbar(this.fuzzyModeButton);

        this.scrollbar.setRange(0, Math.max(0, FILTER_SLOTS - VISIBLE_ROWS), 1);
        this.widgets.add("scrollbar", scrollbar);

        createLogicButton();
        createRowWidgets(style);
    }

    @Override
    protected void init() {
        super.init();

        lastOffset = Integer.MIN_VALUE;
        lastLeftPos = Integer.MIN_VALUE;
        lastTopPos = Integer.MIN_VALUE;
        lastWidth = Integer.MIN_VALUE;
        lastHeight = Integer.MIN_VALUE;
    }

    private void createLogicButton() {
        this.logicButton = Button.builder(Component.empty(), button ->
                getMenu().setLogicAnd(!getMenu().isLogicAndClient())
        ).bounds(0, 0, 52, 16).build();

        this.logicButton.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.MULTI_EMITTER_LOGIC.getTranslationKey())
        ));

        this.widgets.add("logic", logicButton);
    }

    private void createRowWidgets(ScreenStyle style) {
        Font font = Minecraft.getInstance().font;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int currentRow = row;

            ConfirmableTextField textField = new ConfirmableTextField(style, font, 0, 0, 0, font.lineHeight);
            textField.setBordered(false);
            textField.setMaxLength(16);
            textField.setTextColor(normalTextColor);
            textField.setVisible(true);
            textField.setResponder(text -> onLimitChanged(currentRow));
            textField.setOnConfirm(this::onClose);
            limitFields[row] = textField;
            this.widgets.add("limit_" + row, textField);

            ToggleButton toggle = new ToggleButton(ICON_GE, ICON_LT, state -> onCompareToggled(currentRow, state));
            compareButtons[row] = toggle;
            this.widgets.add("cmp_" + row, toggle);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        fuzzyModeButton.set(getMenu().getFuzzyMode());
        fuzzyModeButton.setVisibility(getMenu().supportsFuzzySearch());

        boolean craftingCardInstalled = getMenu().hasUpgrade(AEItems.CRAFTING_CARD);
        int scrollOffset = scrollbar.getCurrentScroll();

        updateControls(craftingCardInstalled);

        boolean relayoutNeeded =
                scrollOffset != lastOffset
                        || leftPos != lastLeftPos
                        || topPos != lastTopPos
                        || width != lastWidth
                        || height != lastHeight;

        if (relayoutNeeded) {
            repositionConfigSlots(scrollOffset);
            lastOffset = scrollOffset;
            lastLeftPos = leftPos;
            lastTopPos = topPos;
            lastWidth = width;
            lastHeight = height;
        }

        updateVisibleRows(scrollOffset, craftingCardInstalled);
    }

    private void updateControls(boolean craftingCardInstalled) {
        scrollbar.setVisible(true);
        scrollbar.setRange(0, Math.max(0, FILTER_SLOTS - VISIBLE_ROWS), 1);

        logicButton.visible = true;
        logicButton.active = true;
        logicButton.setMessage(Component.translatable(
                getMenu().isLogicAndClient()
                        ? LangDefs.AND.getTranslationKey()
                        : LangDefs.OR.getTranslationKey()
        ));

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            limitFields[i].setVisible(!craftingCardInstalled);
            limitFields[i].setEditable(!craftingCardInstalled);

            compareButtons[i].visible = true;
            compareButtons[i].active = true;
        }
    }

    private void repositionConfigSlots(int offset) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.CONFIG);

        for (int i = 0; i < FILTER_SLOTS && i < slots.size(); i++) {
            int visibleRow = i - offset;
            boolean inView = visibleRow >= 0 && visibleRow < VISIBLE_ROWS;

            Slot rawSlot = slots.get(i);
            if (!(rawSlot instanceof AppEngSlot slot)) {
                continue;
            }

            if (slot instanceof IMovableSlot movable) {
                if (inView) {
                    movable.setX(FILTER_SLOT_X);
                    movable.setY(FILTER_SLOT_Y + visibleRow * ROW_HEIGHT);
                    slot.setSlotEnabled(true);
                } else {
                    movable.setX(HIDDEN_POS);
                    movable.setY(HIDDEN_POS);
                    slot.setSlotEnabled(false);
                }
            } else {
                slot.setSlotEnabled(inView);
            }
        }
    }

    private void updateVisibleRows(int offset, boolean craftingCardInstalled) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int slotIndex = offset + row;

            if (!isValidSlot(slotIndex)) {
                boundSlots[row] = -1;
                limitFields[row].setVisible(false);
                compareButtons[row].visible = false;
                continue;
            }

            boolean boundChanged = boundSlots[row] != slotIndex;
            boundSlots[row] = slotIndex;

            AEKey key = getMenu().getConfiguredFilter(slotIndex);
            NumberEntryType newType = NumberEntryType.of(key);
            boolean typeChanged = rowTypes[row] == null || !rowTypes[row].equals(newType);

            if (boundChanged || typeChanged) {
                rowTypes[row] = newType;
                suppressChange[row] = true;
                try {
                    setTextFieldLongValue(row, getMenu().getThresholdClient(slotIndex));
                } finally {
                    suppressChange[row] = false;
                }
            }

            if (craftingCardInstalled) {
                boolean whenCrafting = getMenu().isCraftEmitWhenCraftingClient(slotIndex);
                compareButtons[row].setState(whenCrafting);
                compareButtons[row].setTooltip(Tooltip.create(Component.translatable(
                        whenCrafting
                                ? LangDefs.MULTI_EMITTER_EMIT_WHEN_CRAFTING.getTranslationKey()
                                : LangDefs.MULTI_EMITTER_EMIT_WHEN_NOT_CRAFTING.getTranslationKey()
                )));
            } else {
                boolean compareGreaterOrEqual = getMenu().isCompareGeClient(slotIndex);
                compareButtons[row].setState(compareGreaterOrEqual);
                compareButtons[row].setTooltip(Tooltip.create(Component.translatable(
                        compareGreaterOrEqual
                                ? LangDefs.MULTI_EMITTER_CMP_ABOVE.getTranslationKey()
                                : LangDefs.MULTI_EMITTER_CMP_BELOW.getTranslationKey()
                )));
                validateRow(row);
            }
        }
    }

    private void onCompareToggled(int row, boolean state) {
        int slotIndex = boundSlots[row];
        if (!isValidSlot(slotIndex)) {
            return;
        }

        if (getMenu().hasUpgrade(AEItems.CRAFTING_CARD)) {
            getMenu().setCraftEmitWhenCrafting(slotIndex, state);
        } else {
            getMenu().setCompareGe(slotIndex, state);
        }
    }

    private void onLimitChanged(int row) {
        if (suppressChange[row]) {
            return;
        }

        int slotIndex = boundSlots[row];
        if (!isValidSlot(slotIndex) || getMenu().hasUpgrade(AEItems.CRAFTING_CARD)) {
            return;
        }

        validateRow(row);
        getRowLongValue(row).ifPresent(value -> getMenu().setThreshold(slotIndex, value));
    }

    private Optional<BigDecimal> getRowValueInternal(int row) {
        String textValue = limitFields[row].getValue();
        if (textValue.startsWith("=")) {
            textValue = textValue.substring(1);
        }
        return MathExpressionParser.parse(textValue, decimalFormat);
    }

    private boolean isPlainNumber(int row) {
        ParsePosition position = new ParsePosition(0);
        String textValue = limitFields[row].getValue().trim();
        decimalFormat.parse(textValue, position);
        return position.getErrorIndex() == -1 && position.getIndex() == textValue.length();
    }

    private long convertToExternalValue(NumberEntryType type, BigDecimal internalValue) {
        return internalValue
                .multiply(BigDecimal.valueOf(type.amountPerUnit()), MathContext.DECIMAL128)
                .setScale(0, RoundingMode.UP)
                .longValue();
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

        limitFields[row].setValue(decimalFormat.format(internal));
        limitFields[row].moveCursorToEnd();
        limitFields[row].setHighlightPos(0);
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

        limitFields[row].setTextColor(valid ? normalTextColor : errorTextColor);
        limitFields[row].setTooltipMessage(tooltip);
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < FILTER_SLOTS;
    }
}