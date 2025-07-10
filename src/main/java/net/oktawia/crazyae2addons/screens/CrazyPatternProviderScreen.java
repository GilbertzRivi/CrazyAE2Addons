package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.mixins.SlotAccessor;

import java.util.List;

public class CrazyPatternProviderScreen<C extends CrazyPatternProviderMenu> extends PatternProviderScreen<C> implements CrazyScreen {
    private static final String NAME = "crazy_pattern_provider";
    private Scrollbar scrollbar;
    private int lastOffset = -1;

    static {
        CrazyScreen.i18n(NAME, "capacity", "Capacity: %s patterns");
    }

    public CrazyPatternProviderScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        scrollbar = new Scrollbar();
        scrollbar.setRange(0, (getMenu().slotNum / 9) - 4, 1);
        this.widgets.add("scrollbar", scrollbar);
    }

    public void updateBeforeRender() {
        super.updateBeforeRender();

        this.setTextContent("patterninfo", l10n(NAME, "capacity", getMenu().slotNum));

        int scrollOffset = this.scrollbar.getCurrentScroll();
        if (scrollOffset != lastOffset) {
            for (int i = 0; i < getMenu().slotNum; i++) {
                int row = i / 9;
                int col = i % 9;

                int x = 8 + col * 18;
                int y = 42 + (row - scrollOffset) * 18;

                Slot s = getMenu().getSlots(SlotSemantics.ENCODED_PATTERN).get(i);
                if (!(s instanceof AppEngSlot slot)) return;
                if (slot instanceof IMovableSlot accessor) {
                    if (row >= scrollOffset && row < scrollOffset + 4) {
                        accessor.setX(x);
                        accessor.setY(y);
                        slot.setSlotEnabled(true);
                        getMenu().requestUpdate();
                    } else {
                        slot.setSlotEnabled(false);
                    }
                }
            }
        }
        lastOffset = scrollOffset;
    }

    public void updatePatternsFromServer(List<ItemStack> stacks) {
        List<Slot> slots = getMenu().getSlots(SlotSemantics.ENCODED_PATTERN);

        for (int i = 0; i < stacks.size(); i++) {
            Slot s = slots.get(i);
            if (s instanceof AppEngSlot slot) {
                slot.set(stacks.get(i));
            }
        }
    }
}
