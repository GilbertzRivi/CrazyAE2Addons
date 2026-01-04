package net.oktawia.crazyae2addons.screens;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.slot.AppEngSlot;
import com.google.gson.Gson;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.MultiLevelEmitterMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MultiLevelEmitterScreen<C extends MultiLevelEmitterMenu> extends UpgradeableScreen<C> {

    private static final Gson GSON = new Gson();

    private static final int VISIBLE_ROWS = 6;

    private static final int SLOT_X_FROM_LIMIT_LEFT = 24;
    private static final int SLOT_Y_FROM_LIMIT_TOP = -4;

    private static final String MAX_LONG_STR = Long.toString(Long.MAX_VALUE);
    private static final int MAX_LONG_DIGITS = MAX_LONG_STR.length();

    private Scrollbar scrollbar;
    private Button addRowButton;
    private Button logicButton;

    private final List<AETextField> limitFields = new ArrayList<>(VISIBLE_ROWS);

    // Jeden icon button na wiersz
    private final List<IconButton> compareButtons = new ArrayList<>(VISIBLE_ROWS);

    private final int[] fieldRowIndex = new int[VISIBLE_ROWS];

    public MultiLevelEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.scrollbar = new Scrollbar();
        this.widgets.add("scrollbar", this.scrollbar);

        this.addRowButton = new IconButton(Icon.ENTER, (btn) -> getMenu().requestAddRow());
        this.addRowButton.setTooltip(Tooltip.create(
                Component.translatable("gui.crazyae2addons.multi_emitter.add_row")
        ));
        this.widgets.add("add_row", this.addRowButton);

        this.logicButton = Button.builder(Component.literal("OR"), (btn) -> getMenu().requestToggleLogic())
                .bounds(0, 0, 40, 16)
                .build();
        this.logicButton.setTooltip(Tooltip.create(
                Component.translatable("gui.crazyae2addons.multi_emitter.logic")
        ));
        this.widgets.add("logic", this.logicButton);

        ensureRows();
        Arrays.fill(fieldRowIndex, -1);

        updateScrollbarRange();
        layoutSlotsAndSyncWidgets();
    }

    private static boolean isValidNonNegativeLongUpToMax(String s) {
        if (s == null || s.isEmpty()) return true;

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c < '0' || c > '9') return false;
        }

        if (s.length() < MAX_LONG_DIGITS) return true;
        if (s.length() > MAX_LONG_DIGITS) return false;

        return s.compareTo(MAX_LONG_STR) <= 0;
    }

    private void ensureRows() {
        for (int vis = limitFields.size(); vis < VISIBLE_ROWS; vis++) {
            final int idx = vis;

            // --- limit field
            AETextField tf = new AETextField(style, font, 0, 0, 0, 0);
            tf.setMaxLength(MAX_LONG_DIGITS);
            tf.setBordered(false);
            tf.setFilter(MultiLevelEmitterScreen::isValidNonNegativeLongUpToMax);

            tf.setResponder(s -> {
                if (!tf.isFocused()) return;

                int row = fieldRowIndex[idx];
                if (row < 0) return;

                long units = 0;
                if (s != null && !s.isEmpty()) units = Long.parseLong(s);

                long tech = toTechnicalAmount(row, units);
                getMenu().requestSetLimit(row, tech);
            });

            limitFields.add(tf);
            this.widgets.add("limit_" + idx, tf);

            // --- compare icon button (toggle)
            IconButton cmp = new IconButton(Icon.REDSTONE_HIGH, (b) -> {
                int row = fieldRowIndex[idx];
                if (row >= 0) getMenu().requestToggleCompare(row);
            });

            cmp.setTooltip(Tooltip.create(
                    Component.translatable("gui.crazyae2addons.multi_emitter.cmp_above")
            ));

            compareButtons.add(cmp);
            this.widgets.add("cmp_" + idx, cmp);
        }
    }

    private void updateScrollbarRange() {
        int rows = getMenu().getRows();
        int maxScroll = Math.max(0, rows - VISIBLE_ROWS);
        this.scrollbar.setRange(0, maxScroll, 1);
    }

    private void layoutSlotsAndSyncWidgets() {
        var menu = getMenu();

        int scrollOffset = scrollbar.getCurrentScroll();
        int rows = menu.getRows();

        long[] limits = parseLongArray(menu.limitsJson);
        int[] modes = parseIntArray(menu.modesJson); // 0=ABOVE(>=), 1=BELOW(<)

        // logic button label
        logicButton.setMessage(Component.literal(menu.getLogicMode() == 1 ? "AND" : "OR"));

        for (int vis = 0; vis < VISIBLE_ROWS; vis++) {
            int row = scrollOffset + vis;

            AETextField tf = limitFields.get(vis);
            IconButton cmp = compareButtons.get(vis);

            if (row < rows) {
                fieldRowIndex[vis] = row;

                tf.setVisible(true);
                cmp.visible = true;

                int cm = row < modes.length ? modes[row] : 0;

                if (cm == 1) {
                    // BELOW
                    cmp.setIcon(Icon.REDSTONE_LOW);
                    cmp.setTooltip(Tooltip.create(
                            Component.translatable("gui.crazyae2addons.multi_emitter.cmp_below")
                    ));
                } else {
                    // ABOVE_OR_EQUAL
                    cmp.setIcon(Icon.REDSTONE_HIGH);
                    cmp.setTooltip(Tooltip.create(
                            Component.translatable("gui.crazyae2addons.multi_emitter.cmp_above")
                    ));
                }

                if (!tf.isFocused()) {
                    long tech = row < limits.length ? limits[row] : 0;
                    String target = formatForField(row, tech);
                    if (!tf.getValue().equals(target)) tf.setValue(target);
                }
            } else {
                fieldRowIndex[vis] = -1;

                if (tf.isFocused()) this.setFocused(null);

                tf.setVisible(false);
                cmp.visible = false;

                if (!tf.getValue().isEmpty()) tf.setValue("");
            }
        }

        int baseSlotIndex = menu.getMonitorSlotStart();

        for (int row = 0; row < MultiLevelEmitterMenu.MAX_ROWS; row++) {
            Slot slot = menu.getSlot(baseSlotIndex + row);

            if (!(slot instanceof AppEngSlot appEngSlot)) continue;
            if (!(appEngSlot instanceof IMovableSlot movable)) continue;

            boolean inView = row >= scrollOffset && row < scrollOffset + VISIBLE_ROWS && row < rows;

            if (inView) {
                int vis = row - scrollOffset;

                AETextField anchor = limitFields.get(vis);

                int relX = anchor.getX() - this.leftPos - SLOT_X_FROM_LIMIT_LEFT;
                int relY = anchor.getY() - this.topPos + SLOT_Y_FROM_LIMIT_TOP;

                movable.setX(relX);
                movable.setY(relY);
                appEngSlot.setSlotEnabled(true);
            } else {
                appEngSlot.setSlotEnabled(false);
            }
        }
    }

    private long[] parseLongArray(String json) {
        try {
            if (json == null || json.isBlank()) return new long[0];
            return GSON.fromJson(json, long[].class);
        } catch (Exception ignored) {
            return new long[0];
        }
    }

    private int[] parseIntArray(String json) {
        try {
            if (json == null || json.isBlank()) return new int[0];
            return GSON.fromJson(json, int[].class);
        } catch (Exception ignored) {
            return new int[0];
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateScrollbarRange();
        layoutSlotsAndSyncWidgets();
    }

    private @Nullable AEKey getKeyForRow(int row) {
        int base = getMenu().getMonitorSlotStart();
        ItemStack filter = getMenu().getSlot(base + row).getItem();
        if (filter.isEmpty()) return null;

        var gs = GenericStack.fromItemStack(filter);
        return gs != null ? gs.what() : null;
    }

    private long toTechnicalAmount(int row, long userUnits) {
        AEKey key = getKeyForRow(row);
        if (key == null) return userUnits;

        long perUnit = key.getAmountPerUnit();
        if (perUnit <= 0) return userUnits;

        if (userUnits > Long.MAX_VALUE / perUnit) return Long.MAX_VALUE;
        return userUnits * perUnit;
    }

    private String formatForField(int row, long technical) {
        if (technical <= 0) return "";
        AEKey key = getKeyForRow(row);
        if (key == null) return Long.toString(technical);

        long perUnit = key.getAmountPerUnit();
        if (perUnit > 1 && technical % perUnit == 0) {
            return Long.toString(technical / perUnit);
        }
        return Long.toString(technical);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        for (int vis = 0; vis < VISIBLE_ROWS; vis++) {
            AETextField tf = limitFields.get(vis);
            int row = fieldRowIndex[vis];
            if (row < 0 || !tf.isVisible()) continue;

            if (tf.isFocused() || tf.isMouseOver(mouseX, mouseY)) {
                drawTooltip(graphics, mouseX, mouseY, List.of(limitTooltip(row)));
                break;
            }
        }
    }

    private Component limitTooltip(int row) {
        AEKey key = getKeyForRow(row);

        Component unit = Component.literal("-");
        if (key != null) {
            String sym = key.getUnitSymbol();
            if (sym != null && !sym.isBlank()) {
                unit = Component.literal(sym);
            } else {
                unit = key.getType().getDescription();
            }
        }

        return Component.translatable("gui.crazyae2addons.multi_emitter.unit_line", unit);
    }
}
