package net.oktawia.crazyae2addonslite.screens;

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

import appeng.api.stacks.AEKey;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.client.Point;
import appeng.client.gui.MathExpressionParser;
import appeng.client.gui.NumberEntryType;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.PaletteColor;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.style.WidgetStyle;
import appeng.client.gui.widgets.ConfirmableTextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import appeng.client.gui.widgets.ToggleButton;
import appeng.client.gui.Icon;
import appeng.core.definitions.AEItems;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;

import net.oktawia.crazyae2addonslite.interfaces.IMovableSlot;
import net.oktawia.crazyae2addonslite.menus.MultiLevelEmitterMenu;

public class MultiLevelEmitterScreen<C extends MultiLevelEmitterMenu> extends UpgradeableScreen<MultiLevelEmitterMenu> {

    private static final int FILTER_SLOTS = 16;
    private static final int VISIBLE_ROWS = 6;
    private static final int FILTER_SLOT_X = 10;
    private static final int FILTER_SLOT_Y0 = 34;
    private static final int ROW_H = 18;

    private static final Icon ICON_GE = Icon.REDSTONE_HIGH;
    private static final Icon ICON_LT = Icon.REDSTONE_LOW;
    private final Font font;
    private final Scrollbar scrollbar = new Scrollbar();
    private int lastOffset = -1;
    private final int[] boundSlot = new int[VISIBLE_ROWS];
    private final NumberEntryType[] rowType = new NumberEntryType[VISIBLE_ROWS];
    private final ConfirmableTextField[] limitFields = new ConfirmableTextField[VISIBLE_ROWS];
    private final WidgetStyle[] limitStyles = new WidgetStyle[VISIBLE_ROWS];
    private final boolean[] suppressChange = new boolean[VISIBLE_ROWS];
    private final ToggleButton[] cmpToggles = new ToggleButton[VISIBLE_ROWS];
    private Button logicToggle;
    private final SettingToggleButton<FuzzyMode> fuzzyMode;
    private final int errorTextColor;
    private final int normalTextColor;
    private final DecimalFormat decimalFormat;

    public MultiLevelEmitterScreen(C menu, Inventory playerInventory, Component title,
                                   ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.font = Minecraft.getInstance().font;

        this.errorTextColor = style.getColor(PaletteColor.TEXTFIELD_ERROR).toARGB();
        this.normalTextColor = style.getColor(PaletteColor.TEXTFIELD_TEXT).toARGB();

        this.decimalFormat = new DecimalFormat("#.######", new DecimalFormatSymbols());
        this.decimalFormat.setParseBigDecimal(true);
        this.decimalFormat.setNegativePrefix("-");

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            boundSlot[i] = -1;
            rowType[i] = NumberEntryType.of(null);
        }

        this.fuzzyMode = new ServerSettingToggleButton<>(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        this.addToLeftToolbar(this.fuzzyMode);

        this.scrollbar.setRange(0, Math.max(0, FILTER_SLOTS - VISIBLE_ROWS), 1);
        this.widgets.add("scrollbar", scrollbar);

        this.logicToggle = Button.builder(Component.literal(""), btn -> {
            this.menu.setLogicAnd(!this.menu.isLogicAndClient());
        }).bounds(0, 0, 40, 16).build();

        this.logicToggle.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.multi_emitter.logic")));
        this.widgets.add("logic", logicToggle);

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            final int r = row;

            this.limitStyles[row] = style.getWidget("limit_" + row);

            var tf = new ConfirmableTextField(style, this.font, 0, 0, 0, this.font.lineHeight);
            tf.setBordered(false);
            tf.setMaxLength(16);
            tf.setTextColor(normalTextColor);
            tf.setVisible(true);
            tf.setResponder(text -> onLimitChanged(r));
            tf.setOnConfirm(this::onClose);
            this.limitFields[row] = tf;
            this.widgets.add("limit_" + row, tf);

            var tg = new ToggleButton(ICON_GE, ICON_LT, (state) -> onCmpToggled(r, state)
            );
            this.cmpToggles[row] = tg;
            this.widgets.add("cmp_" + row, tg);
        }
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.fuzzyMode.set(menu.getFuzzyMode());
        this.fuzzyMode.setVisibility(menu.supportsFuzzySearch());

        final boolean hasCraftingCard = menu.hasUpgrade(AEItems.CRAFTING_CARD);

        this.scrollbar.setVisible(true);
        this.logicToggle.visible = true;
        this.logicToggle.active = true;

        for (int i = 0; i < VISIBLE_ROWS; i++) {
            this.limitFields[i].setVisible(!hasCraftingCard);
            this.limitFields[i].setEditable(!hasCraftingCard);

            this.cmpToggles[i].visible = true;
            this.cmpToggles[i].active = true;
        }

        this.logicToggle.setMessage(Component.literal(menu.isLogicAndClient() ? "AND" : "OR"));

        this.scrollbar.setRange(0, Math.max(0, FILTER_SLOTS - VISIBLE_ROWS), 1);
        int offset = this.scrollbar.getCurrentScroll();

        if (offset != lastOffset) {
            repositionConfigSlots(offset);
            lastOffset = offset;
        }

        updateVisibleRows(offset, hasCraftingCard);
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

    private void updateVisibleRows(int offset, boolean hasCraftingCard) {
        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int slotIndex = offset + row;
            boolean valid = slotIndex >= 0 && slotIndex < FILTER_SLOTS;

            if (!valid) {
                boundSlot[row] = -1;
                limitFields[row].setVisible(false);
                cmpToggles[row].visible = false;
                continue;
            }

            boolean boundChanged = (boundSlot[row] != slotIndex);
            boundSlot[row] = slotIndex;

            AEKey key = menu.getConfiguredFilter(slotIndex);

            NumberEntryType newType = NumberEntryType.of(key);
            boolean typeChanged = (rowType[row] == null) || !rowType[row].equals(newType);

            if (boundChanged || typeChanged) {
                rowType[row] = newType;
            }
            if (boundChanged || typeChanged) {
                suppressChange[row] = true;
                try {
                    setTextFieldLongValue(row, menu.getThresholdClient(slotIndex));
                } finally {
                    suppressChange[row] = false;
                }
            }

            if (hasCraftingCard) {
                boolean whenCrafting = menu.isCraftEmitWhenCraftingClient(slotIndex);
                cmpToggles[row].setState(whenCrafting);

                Component tip = Component.literal(whenCrafting ? "Emit when crafting" : "Emit when NOT crafting");
                cmpToggles[row].setTooltip(Tooltip.create(tip));
            } else {
                boolean ge = menu.isCompareGeClient(slotIndex);
                cmpToggles[row].setState(ge);

                Component tip = Component.translatable(
                        ge ? "gui.crazyae2addons.multi_emitter.cmp_above"
                                : "gui.crazyae2addons.multi_emitter.cmp_below"
                );
                cmpToggles[row].setTooltip(Tooltip.create(tip));
            }

            if (!hasCraftingCard && limitFields[row].visible) {
                validateRow(row);
            }
        }
    }



    private void onCmpToggled(int row, boolean state) {
        int slotIndex = boundSlot[row];
        if (slotIndex < 0) return;

        if (menu.hasUpgrade(AEItems.CRAFTING_CARD)) {
            menu.setCraftEmitWhenCrafting(slotIndex, state);
        } else {
            menu.setCompareGe(slotIndex, state);
        }
    }

    private void onLimitChanged(int row) {
        if (suppressChange[row]) return;

        int slotIndex = boundSlot[row];
        if (slotIndex < 0) return;

        if (menu.hasUpgrade(AEItems.CRAFTING_CARD)) {
            return;
        }

        validateRow(row);

        var v = getRowLongValue(row);
        v.ifPresent(aLong -> menu.setThreshold(slotIndex, aLong));
    }

    private Optional<BigDecimal> getRowValueInternal(int row) {
        String textValue = limitFields[row].getValue();
        if (textValue.startsWith("=")) {
            textValue = textValue.substring(1);
        }
        return MathExpressionParser.parse(textValue, decimalFormat);
    }

    private boolean isNumber(int row) {
        var position = new ParsePosition(0);
        var textValue = limitFields[row].getValue().trim();
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
        if (internal.isEmpty()) return Optional.empty();

        if (type.amountPerUnit() == 1 && internal.get().scale() > 0) {
            return Optional.empty();
        }

        long external = convertToExternalValue(type, internal.get());
        if (external < 0) return Optional.empty();

        return Optional.of(external);
    }

    private void setTextFieldLongValue(int row, long value) {
        if (value < 0) value = 0;

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

        tooltip.add(Component.translatable("gui.crazyae2addons.multi_emitter.unit_line", type.unit() == null ? "Items" : type.unit()));

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

        limitFields[row].setTextColor(valid ? normalTextColor : errorTextColor);
        limitFields[row].setTooltipMessage(tooltip);
    }
}
