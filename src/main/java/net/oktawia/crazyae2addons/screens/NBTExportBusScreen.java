package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.NBTExportBusMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;

public class NBTExportBusScreen<C extends NBTExportBusMenu> extends AEBaseScreen<C> implements CrazyScreen {
    private static final String NAME = "nbt_export_bus";
    private static IconButton confirm;
    private static MultilineTextFieldWidget input;
    private static Scrollbar scrollbar;
    public static boolean initialized;
    public static IconButton load;
    private int lastScroll = -1;

    static {
        CrazyScreen.i18n(NAME, "confirm", "Confirm changes");
        CrazyScreen.i18n(NAME, "load", "Load NBT data");
        CrazyScreen.i18n(NAME, "input_filter", "Input filter:");
    }

    public NBTExportBusScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("confirm", confirm);
        this.widgets.add("data", input);
        this.widgets.add("scroll", scrollbar);
        this.widgets.add("load", load);
        initialized = false;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!initialized) {
            input.setValue(getMenu().data);
            initialized = true;
        }
        if (getMenu().newData) {
            input.setValue(getMenu().data);
            getMenu().newData = false;
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();

        int cur = scrollbar.getCurrentScroll();
        if (cur != lastScroll) {
            lastScroll = cur;
            input.setScrollAmount(cur);
        }
    }

    private void setupGui() {
        confirm = new IconButton(Icon.ENTER, (btn) -> getMenu().updateData(input.getValue()));
        confirm.setTooltip(Tooltip.create(l10n(NAME, "confirm")));
        input = new MultilineTextFieldWidget(
                font, 0, 0, 110, 100,
                l10n(NAME, "input_filter"));
        load = new IconButton(Icon.ENTER, (x) -> getMenu().loadNBT());
        load.setTooltip(Tooltip.create(l10n(NAME, "load")));
        scrollbar = new Scrollbar();
        scrollbar.setSize(16, 64);
        scrollbar.setRange(0, 64, 4);
    }
}
