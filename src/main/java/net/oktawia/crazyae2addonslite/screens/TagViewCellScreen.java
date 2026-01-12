package net.oktawia.crazyae2addonslite.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addonslite.menus.TagViewCellMenu;
import net.oktawia.crazyae2addonslite.misc.IconButton;
import net.oktawia.crazyae2addonslite.misc.MultilineTextFieldWidget;

public class TagViewCellScreen<C extends TagViewCellMenu> extends AEBaseScreen<C> {
    private static IconButton confirm;
    private static MultilineTextFieldWidget input;
    private static Scrollbar scrollbar;
    public static boolean initialized;
    private int lastScrollFromBar = -1;
    private int lastScrollFromInput = -1;

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            input.setValue(getMenu().data);
            initialized = true;
            updateScrollbarRangeFromInput();
            syncBarToInput();
        }

        if (getMenu().newData) {
            input.setValue(getMenu().data);
            getMenu().newData = false;
            updateScrollbarRangeFromInput();
            syncBarToInput();
        }
    }

    public TagViewCellScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        setTextContent("dialog_title", Component.translatable("gui.crazyae2addons.tag_view_cell_title"));
        this.widgets.add("confirm", confirm);
        this.widgets.add("data", input);
        this.widgets.add("scroll", scrollbar);
        initialized = false;
    }

    @Override
    public void containerTick() {
        super.containerTick();

        int curBar = scrollbar.getCurrentScroll();
        if (curBar != lastScrollFromBar) {
            lastScrollFromBar = curBar;
            input.setScrollAmount(curBar);
            lastScrollFromInput = (int) input.getScrollAmount();
        }

        int curInput = (int) input.getScrollAmount();
        if (curInput != lastScrollFromInput) {
            lastScrollFromInput = curInput;

            updateScrollbarRangeFromInput();

            int max = getInputMaxScroll();
            int clamped = Math.max(0, Math.min(max, curInput));
            if (clamped != scrollbar.getCurrentScroll()) {
                scrollbar.setCurrentScroll(clamped);
            }
            lastScrollFromBar = scrollbar.getCurrentScroll();
        }

        updateScrollbarRangeFromInput();
    }

    private void setupGui() {
        confirm = new IconButton(Icon.ENTER, (btn) -> getMenu().updateData(input.getValue()));
        confirm.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.tag_view_cell_confirm")));

        input = new MultilineTextFieldWidget(
                font, 0, 0, 145, 80,
                Component.translatable("gui.crazyae2addons.tag_view_cell_input"));

        scrollbar = new Scrollbar();
        updateScrollbarRangeFromInput();
        syncBarToInput();
    }

    private void updateScrollbarRangeFromInput() {
        int max = getInputMaxScroll();
        int step = getInputScrollStep();
        int cur = Math.min(scrollbar.getCurrentScroll(), max);

        int safeStep = Math.max(1, step);

        scrollbar.setRange(0, max, safeStep);
        scrollbar.setCurrentScroll(cur);

        lastScrollFromBar = cur;
    }

    private void syncBarToInput() {
        int curInput = (int) input.getScrollAmount();
        int max = getInputMaxScroll();
        int clamped = Math.max(0, Math.min(max, curInput));
        scrollbar.setCurrentScroll(clamped);
        lastScrollFromBar = clamped;
        lastScrollFromInput = curInput;
    }


    private int getInputMaxScroll() {
        return (int) Math.max(0, input.getMaxScroll());
    }

    private int getInputScrollStep() {
        return Math.max(1, input.getScrollStep());
    }
}