package net.oktawia.crazyae2addons.client.screens;

import appeng.client.gui.implementations.PatternProviderScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.UpgradesPanel;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.CrazyPatternProviderMenu;
import net.oktawia.crazyae2addons.mixins.accessors.WidgetContainerAccessor;

import java.util.List;

public class CrazyPatternProviderScreen<C extends CrazyPatternProviderMenu> extends PatternProviderScreen<C> {
    private static final int COLS = 9;
    private static final int VISIBLE_ROWS = 4;

    private final Scrollbar scrollbar = new Scrollbar();

    private int lastOffset = Integer.MIN_VALUE;
    private int lastLeftPos = Integer.MIN_VALUE;
    private int lastTopPos = Integer.MIN_VALUE;
    private int lastWidth = Integer.MIN_VALUE;
    private int lastHeight = Integer.MIN_VALUE;

    public CrazyPatternProviderScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        scrollbar.setRange(0, Math.max(0, (getMenu().slotNum / COLS) - VISIBLE_ROWS), 1);
        this.widgets.add("scrollbar", scrollbar);
        if (!((WidgetContainerAccessor) this.widgets).getCompositeWidgets().containsKey("upgrades")) {
            this.widgets.add("upgrades", new UpgradesPanel(getMenu().getSlots(SlotSemantics.UPGRADE)));
        }
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

    @Override
    public void updateBeforeRender() {
        super.updateBeforeRender();

        this.setTextContent(
                "patterninfo",
                Component.translatable(LangDefs.CRAZY_PROVIDER_CAPACITY_TOOLTIP.getTranslationKey())
                        .append(getMenu().slotNum + " ")
                        .append(Component.translatable(LangDefs.PATTERNS.getTranslationKey()))
        );

        scrollbar.setRange(0, Math.max(0, (getMenu().slotNum / COLS) - VISIBLE_ROWS), 1);

        int scrollOffset = this.scrollbar.getCurrentScroll();

        boolean relayoutNeeded =
                scrollOffset != lastOffset
                        || this.leftPos != lastLeftPos
                        || this.topPos != lastTopPos
                        || this.width != lastWidth
                        || this.height != lastHeight;

        if (relayoutNeeded) {
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

            lastOffset = scrollOffset;
            lastLeftPos = this.leftPos;
            lastTopPos = this.topPos;
            lastWidth = this.width;
            lastHeight = this.height;
        }
    }
}