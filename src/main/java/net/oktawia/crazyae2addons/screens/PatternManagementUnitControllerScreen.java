package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.PatternManagementUnitControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

import java.util.List;

public class PatternManagementUnitControllerScreen<C extends PatternManagementUnitControllerMenu> extends AEBaseScreen<C> {

    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 4;

    private final Scrollbar scrollbar = new Scrollbar();
    private int lastOffset = -1;

    public PatternManagementUnitControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        scrollbar.setRange(0, Math.max(0, (getMenu().slotNum / COLS) - VISIBLE_ROWS), 1);
        this.widgets.add("scrollbar", scrollbar);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(Component.literal("Enable/Disable preview")));
        this.widgets.add("prevbtn", prevBtn);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        setTextContent("prev", Component.literal("Preview: " + getMenu().preview));

        scrollbar.setRange(0, Math.max(0, (getMenu().slotNum / COLS) - VISIBLE_ROWS), 1);

        int scrollOffset = this.scrollbar.getCurrentScroll();
        if (scrollOffset != lastOffset) {
            List<Slot> slots = getMenu().getSlots(SlotSemantics.ENCODED_PATTERN);

            for (int i = 0; i < getMenu().slotNum && i < slots.size(); i++) {
                int row = i / COLS;
                int col = i % COLS;

                int x = 8 + col * 18;
                int y = 42 + (row - scrollOffset) * 18;

                Slot s = slots.get(i);
                if (!(s instanceof AppEngSlot slot)) {
                    continue;
                }

                if (slot instanceof IMovableSlot movable) {
                    boolean inView = row >= scrollOffset && row < scrollOffset + VISIBLE_ROWS;
                    if (inView) {
                        movable.setX(x);
                        movable.setY(y);
                        slot.setSlotEnabled(true);
                    } else {
                        slot.setSlotEnabled(false);
                    }
                }
            }

            getMenu().requestUpdate(scrollOffset);
            lastOffset = scrollOffset;
        }
    }

    public void updatePatternsFromServer(int startIndex, List<ItemStack> stacks) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.ENCODED_PATTERN);
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
