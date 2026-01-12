package net.oktawia.crazyae2addonslite.screens;

import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.BackgroundPanel;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.slot.AppEngSlot;
import com.google.gson.Gson;
import de.mari_023.ae2wtlib.wut.CycleTerminalButton;
import de.mari_023.ae2wtlib.wut.IUniversalTerminalCapable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addonslite.interfaces.IMovableSlot;
import net.oktawia.crazyae2addonslite.menus.WirelessNotificationTerminalMenu;
import net.oktawia.crazyae2addonslite.misc.IconButton;
import net.oktawia.crazyae2addonslite.misc.StockThresholdToast;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WirelessNotificationTerminalScreen<C extends WirelessNotificationTerminalMenu>
        extends UpgradeableScreen<C> implements IUniversalTerminalCapable {

    private static final Gson GSON = new Gson();

    private static final int VISIBLE_ROWS = 6;

    private static final int SLOT_X_FROM_THRESHOLD_LEFT = 24;
    private static final int SLOT_Y_FROM_THRESHOLD_TOP = -4;

    private static final String TR_UNIT_LINE =
            "gui.crazyae2addons.notification_terminal.unit_line";

    private static final String MAX_LONG_STR = Long.toString(Long.MAX_VALUE);
    private static final int MAX_LONG_DIGITS = MAX_LONG_STR.length();

    private Scrollbar scrollbar;
    private Button addRowButton;

    private final List<AETextField> thresholdFields = new ArrayList<>(VISIBLE_ROWS);
    private final int[] fieldRowIndex = new int[VISIBLE_ROWS];

    private int lastToastSeq = 0;

    public WirelessNotificationTerminalScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        if (this.getMenu().isWUT()) {
            this.addToLeftToolbar(new CycleTerminalButton((btn) -> this.cycleTerminal()));
        }
        this.widgets.add("singularityBackground", new BackgroundPanel(style.getImage("singularityBackground")));

        this.scrollbar = new Scrollbar();
        this.widgets.add("scrollbar", this.scrollbar);

        this.addRowButton = new IconButton(Icon.ENTER, (btn) -> getMenu().requestAddRow());
        this.addRowButton.setTooltip(Tooltip.create(
                Component.translatable("gui.crazyae2addons.notification_terminal_add_row")
        ));
        this.widgets.add("add_row", this.addRowButton);

        ensureThresholdFields();
        Arrays.fill(fieldRowIndex, -1);

        updateScrollbarRange();
        layoutSlotsAndSyncFields();
        consumeToastIfAny();
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

    private void ensureThresholdFields() {
        for (int visIndex = thresholdFields.size(); visIndex < VISIBLE_ROWS; visIndex++) {
            final int idx = visIndex;

            AETextField tf = new AETextField(style, font, 0, 0, 0, 0);
            tf.setMaxLength(MAX_LONG_DIGITS);
            tf.setBordered(false);

            tf.setFilter(WirelessNotificationTerminalScreen::isValidNonNegativeLongUpToMax);

            tf.setResponder(s -> {
                if (!tf.isFocused()) return;

                int row = fieldRowIndex[idx];
                if (row < 0) return;

                long units = 0;
                if (s != null && !s.isEmpty()) {
                    units = Long.parseLong(s);
                }

                long tech = toTechnicalAmount(row, units);
                this.getMenu().requestSetThreshold(row, tech);
            });

            thresholdFields.add(tf);
            this.widgets.add("threshold_" + idx, tf);
        }
    }

    private void updateScrollbarRange() {
        int rows = getMenu().getRows();
        int maxScroll = Math.max(0, rows - VISIBLE_ROWS);
        this.scrollbar.setRange(0, maxScroll, 1);
    }

    private void layoutSlotsAndSyncFields() {
        var menu = getMenu();

        int scrollOffset = scrollbar.getCurrentScroll();
        int rows = menu.getRows();

        long[] thresholds = parseThresholdsJson(menu.thresholdsJson);

        for (int vis = 0; vis < VISIBLE_ROWS; vis++) {
            int row = scrollOffset + vis;
            AETextField tf = thresholdFields.get(vis);

            if (row < rows) {
                fieldRowIndex[vis] = row;
                tf.setVisible(true);

                if (!tf.isFocused()) {
                    long tech = row < thresholds.length ? thresholds[row] : 0;
                    String target = formatForField(row, tech);
                    if (!tf.getValue().equals(target)) tf.setValue(target);
                }
            } else {
                fieldRowIndex[vis] = -1;
                if (tf.isFocused()) this.setFocused(null);
                tf.setVisible(false);
                if (!tf.getValue().isEmpty()) tf.setValue("");
            }
        }

        int baseSlotIndex = menu.getMonitorSlotStart();

        for (int row = 0; row < WirelessNotificationTerminalMenu.MAX_ROWS; row++) {
            Slot slot = menu.getSlot(baseSlotIndex + row);

            if (!(slot instanceof AppEngSlot appEngSlot)) continue;
            if (!(appEngSlot instanceof IMovableSlot movable)) continue;

            boolean inView = row >= scrollOffset && row < scrollOffset + VISIBLE_ROWS && row < rows;

            if (inView) {
                int vis = row - scrollOffset;

                AETextField anchor = thresholdFields.get(vis);

                int relX = anchor.getX() - this.leftPos - SLOT_X_FROM_THRESHOLD_LEFT;
                int relY = anchor.getY() - this.topPos + SLOT_Y_FROM_THRESHOLD_TOP;

                movable.setX(relX);
                movable.setY(relY);
                appEngSlot.setSlotEnabled(true);
            } else {
                appEngSlot.setSlotEnabled(false);
            }
        }
    }

    private long[] parseThresholdsJson(String json) {
        try {
            if (json == null || json.isBlank()) return new long[0];
            return GSON.fromJson(json, long[].class);
        } catch (Exception ignored) {
            return new long[0];
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();
        updateScrollbarRange();
        layoutSlotsAndSyncFields();
        consumeToastIfAny();
    }

    private void consumeToastIfAny() {
        var menu = getMenu();
        if (menu.toastSeq == lastToastSeq) return;
        lastToastSeq = menu.toastSeq;

        if (menu.toastJson == null || menu.toastJson.isBlank()) return;

        WirelessNotificationTerminalMenu.ToastPayload p;
        try {
            p = GSON.fromJson(menu.toastJson, WirelessNotificationTerminalMenu.ToastPayload.class);
        } catch (Exception ignored) {
            return;
        }

        if (p.row < 0 || p.row >= WirelessNotificationTerminalMenu.MAX_ROWS) return;

        int baseSlotIndex = menu.getMonitorSlotStart();
        ItemStack filter = menu.getSlot(baseSlotIndex + p.row).getItem();
        if (filter.isEmpty()) return;

        var mc = Minecraft.getInstance();
        if (mc.player == null) return;

        var gs = GenericStack.fromItemStack(filter);
        if (gs == null) return;

        AEKey key = gs.what();
        mc.getToasts().addToast(new StockThresholdToast(key, p.above, p.threshold, p.amount));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.render(graphics, mouseX, mouseY, partialTicks);

        for (int vis = 0; vis < VISIBLE_ROWS; vis++) {
            AETextField tf = thresholdFields.get(vis);
            int row = fieldRowIndex[vis];
            if (row < 0 || !tf.isVisible()) continue;

            if (tf.isFocused() || tf.isMouseOver(mouseX, mouseY)) {
                this.drawTooltip(graphics, mouseX, mouseY, List.of(thresholdTooltip(row)));
                break;
            }
        }
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

    private Component thresholdTooltip(int row) {
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

        return Component.translatable(TR_UNIT_LINE, unit);
    }

    @Override
    public void storeState() {}
}
