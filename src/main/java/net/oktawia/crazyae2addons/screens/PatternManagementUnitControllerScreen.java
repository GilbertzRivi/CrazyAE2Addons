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
import net.oktawia.crazyae2addons.interfaces.IMovableSlot;
import net.oktawia.crazyae2addons.menus.PatternManagementUnitControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.mixins.SlotAccessor;

public class PatternManagementUnitControllerScreen<C extends PatternManagementUnitControllerMenu> extends AEBaseScreen<C> implements CrazyScreen {

    private static final String NAME = "pattern_management_unit_controller";
    private Scrollbar scrollbar;
    private int lastOffset = -1;

    static {
        CrazyScreen.i18n(NAME, "preview_toggle", "Enable/Disable preview");
        CrazyScreen.i18n(NAME, "preview_status", "Preview: %s");
    }

    public PatternManagementUnitControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        int visibleRows = 4;
        int totalRows = PatternManagementUnitControllerMenu.ROWS;

        scrollbar = new Scrollbar();
        scrollbar.setHeight(visibleRows * 18);
        scrollbar.setRange(0, totalRows - visibleRows, 1);

        this.widgets.add("scrollbar", scrollbar);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(l10n(NAME, "preview_toggle")));
        this.widgets.add("prevbtn", prevBtn);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        setTextContent("prev", l10n(NAME, "preview_status", getMenu().preview));
        int scrollOffset = scrollbar.getCurrentScroll();
        if (scrollOffset != lastOffset) {
            var slots = getMenu().getSlots(SlotSemantics.ENCODED_PATTERN);
            for (int i = 0; i < slots.size(); i++) {
                int row = i / 9;
                int col = i % 9;

                int x = 8 + col * 18;
                int y = 42 + (row - scrollOffset) * 18;

                Slot slot = slots.get(i);
                if (slot instanceof AppEngSlot aeSlot) {
                    if (aeSlot instanceof IMovableSlot accessor) {
                        if (row >= scrollOffset && row < scrollOffset + 4) {
                            accessor.setX(x);
                            accessor.setY(y);
                            aeSlot.setSlotEnabled(true);
                        } else {
                            aeSlot.setSlotEnabled(false);
                        }
                    }
                }
            }
            lastOffset = scrollOffset;
        }
    }
}
