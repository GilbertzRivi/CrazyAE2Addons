package net.oktawia.crazyae2addonslite.screens;

import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addonslite.interfaces.IMovableSlot;
import net.oktawia.crazyae2addonslite.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addonslite.mixins.WidgetContainerAccessor;

import java.util.List;

public class CrazyPatternProviderScreen<C extends CrazyPatternProviderMenu> extends PatternProviderScreen<C> {
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 4;

    private final Scrollbar scrollbar = new Scrollbar();
    private int lastOffset = -1;

    public CrazyPatternProviderScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        scrollbar.setRange(0, Math.max(0, (getMenu().slotNum / COLS) - VISIBLE_ROWS), 1);
        this.widgets.add("scrollbar", scrollbar);
        if (!((WidgetContainerAccessor)this.widgets).getCompositeWidgets().containsKey("upgrades")) {
            this.widgets.add("upgrades", new UpgradesPanel(getMenu().getSlots(SlotSemantics.UPGRADE)));
        }
    }

    public void updateBeforeRender() {
        super.updateBeforeRender();

        this.setTextContent("patterninfo",
                Component.literal("Capacity: " + getMenu().slotNum + " patterns"));

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
