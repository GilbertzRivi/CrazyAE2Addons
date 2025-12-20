package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.CrazyPatternModifierMenuPP;
import net.oktawia.crazyae2addons.misc.IconButton;

import java.util.ArrayList;
import java.util.List;

public class CrazyPatternModifierScreenPP<C extends CrazyPatternModifierMenuPP> extends AEBaseScreen<C> {

    public IconButton nbt;
    public IconButton circConfirm;
    public AETextField circ;
    public Button mult2;
    public Button div2;

    private final List<Button> circuitButtons = new ArrayList<>(33);

    private Scrollbar configScrollbar;
    private int lastOffset = -1;

    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 4;
    private static final int CELL = 18;

    private static final int GRID_START_X = 8;
    private static final int GRID_START_Y = 175;

    public CrazyPatternModifierScreenPP(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
    }

    private void setupGui() {
        this.nbt = new IconButton(Icon.ENTER, this::changeNbt);
        this.widgets.add("nbt", this.nbt);

        // NEW: /2 i x2
        this.div2 = Button.builder(
                        Component.translatable("gui.crazyae2addons.modifier_mult_div2"),
                        btn -> this.getMenu().multDiv2()
                )
                .bounds(0, 0, 24, 16)
                .build();
        this.div2.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.modifier_mult_div2_tooltip")));
        this.widgets.add("div2", this.div2);

        this.mult2 = Button.builder(
                        Component.translatable("gui.crazyae2addons.modifier_mult_x2"),
                        btn -> this.getMenu().multX2()
                )
                .bounds(0, 0, 24, 16)
                .build();
        this.mult2.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.modifier_mult_x2_tooltip")));
        this.widgets.add("mult2", this.mult2);

        this.configScrollbar = new Scrollbar();
        this.widgets.add("scrollbar", this.configScrollbar);
        this.nbt.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.modifier_ignore_nbt_tooltip")));

        setupCircuitUI();
        setupCircuitButtons();
    }

    private int getConfigRows() {
        var list = getMenu().getSlots(SlotSemantics.CONFIG);
        int size = (list != null) ? list.size() : 0;
        return (int) Math.ceil(size / (double) COLS);
    }

    private void setupCircuitUI() {
        this.circ = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.circ.setBordered(false);
        this.circ.setMaxLength(2);
        this.circ.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.modifier_circuit_desc")));
        this.widgets.add("circ", this.circ);

        this.circConfirm = new IconButton(Icon.ENTER, this::onCircuitConfirm);
        this.circConfirm.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.modifier_tooltip")));
        this.widgets.add("confirmcirc", this.circConfirm);
    }

    private void setupCircuitButtons() {
        for (int i = 0; i <= 32; i++) {
            final int value = i;

            Button b = Button.builder(
                            Component.translatable("gui.crazyae2addons.modifier_circuit_button", value),
                            btn -> onCircuitPick(value)
                    )
                    .bounds(0, 0, 16, 16)
                    .build();

            b.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.modifier_circuit_tooltip", value)));
            this.circuitButtons.add(b);
            this.widgets.add("circ_btn_" + value, b);
        }
    }

    private void renderCircuitUI() {
        var menu = getMenu();

        setTextContent(
                "info2",
                menu.circuit == -1
                        ? Component.translatable("gui.crazyae2addons.modifier_circuit_none")
                        : Component.translatable("gui.crazyae2addons.modifier_circuit_selected", menu.circuit)
        );

        this.circ.setEditable(true);
        this.circConfirm.active = true;
    }

    private void layoutConfigSlots(int scrollOffset) {
        var slots = getMenu().getSlots(SlotSemantics.CONFIG);
        if (slots == null || slots.isEmpty()) return;

        for (int i = 0; i < slots.size(); i++) {
            int row = i / COLS;
            int col = i % COLS;

            var s = slots.get(i);
            if (!(s instanceof AppEngSlot appEngSlot)) continue;

            if (appEngSlot instanceof IMovableSlot movable) {
                if (row >= scrollOffset && row < scrollOffset + VISIBLE_ROWS) {
                    int x = GRID_START_X + col * CELL;
                    int y = GRID_START_Y + (row - scrollOffset) * CELL;
                    movable.setX(x);
                    movable.setY(y);
                    appEngSlot.setSlotEnabled(true);
                } else {
                    appEngSlot.setSlotEnabled(false);
                }
            } else {
                boolean visibleRow = row >= scrollOffset && row < scrollOffset + VISIBLE_ROWS;
                appEngSlot.setSlotEnabled(visibleRow);
            }
        }
    }

    private void onCircuitPick(int value) {
        if (value >= 0 && value <= 32) {
            this.getMenu().changeCircuit(value);
            if (this.circ != null) {
                this.circ.setValue(Integer.toString(value));
            }
        }
    }

    private void onCircuitConfirm(Button btn) {
        final String value = this.circ.getValue();
        if (value == null || value.isEmpty()) {
            this.getMenu().changeCircuit(-1);
            return;
        }

        if (value.chars().allMatch(Character::isDigit)) {
            try {
                int v = Integer.parseInt(value);
                if (v >= 0 && v <= 32) {
                    this.getMenu().changeCircuit(v);
                }
            } catch (NumberFormatException ignored) {
            }
        }
    }

    private boolean handleCircuitRightClick(double mouseX, double mouseY, int button) {
        if (button == 1 && circ != null && circ.isMouseOver(mouseX, mouseY)) {
            circ.setValue("");
            return true;
        }
        return false;
    }

    public void changeNbt(Button btn) {
        this.getMenu().changeNBT();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        setTextContent(
                "info1",
                getMenu().ignoreNbt
                        ? Component.translatable("gui.crazyae2addons.modifier_info_ignore_nbt")
                        : Component.translatable("gui.crazyae2addons.modifier_info_do_not_ignore_nbt")
        );

        renderCircuitUI();

        int rows = getConfigRows();
        int maxOffset = Math.max(0, rows - VISIBLE_ROWS);
        this.configScrollbar.setRange(0, maxOffset, 1);

        int scrollOffset = this.configScrollbar.getCurrentScroll();
        if (scrollOffset != lastOffset) {
            layoutConfigSlots(scrollOffset);
            lastOffset = scrollOffset;
            menu.requestUpdate();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (handleCircuitRightClick(mouseX, mouseY, button)) {
            return true;
        }
        return handled;
    }

    public void updatePatternsFromServer(int startIndex, List<ItemStack> stacks) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.CONFIG);
        for (int i = 0; i < stacks.size(); i++) {
            int global = startIndex + i;
            if (global >= 0 && global < slots.size()) {
                Slot s = slots.get(global);
                if (s instanceof AppEngSlot slot) {
                    slot.set(stacks.get(i));
                }
            }
        }
    }
}
