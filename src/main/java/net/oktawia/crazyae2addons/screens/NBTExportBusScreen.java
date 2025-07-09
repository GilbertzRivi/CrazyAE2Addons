package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import com.lowdragmc.lowdraglib.gui.editor.Icons;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.NBTExportBusMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;

public class NBTExportBusScreen<C extends NBTExportBusMenu> extends AEBaseScreen<C> {
    private static IconButton confirm;
    private static MultilineTextFieldWidget input;
    private static Scrollbar scrollbar;
    public static boolean initialized;
    public static IconButton load;
    private int lastScroll = -1;

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        if (!initialized){
            input.setValue(getMenu().data);
            initialized = true;
        }
        if (getMenu().newData){
            input.setValue(getMenu().data);
            getMenu().newData = false;
        }
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
    public void containerTick() {
        super.containerTick();

        int cur = scrollbar.getCurrentScroll();
        if (cur != lastScroll) {
            lastScroll = cur;
            input.setScrollAmount(cur);
        }
    }

    private void setupGui(){
        confirm = new IconButton(Icon.ENTER, (btn) -> getMenu().updateData(input.getValue()));
        confirm.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.nbt_export_bus.confirm")));
        input = new MultilineTextFieldWidget(
                font, 0, 0, 110, 100,
                Component.translatable("gui.crazyae2addons.nbt_export_bus.input_filter"));
        load = new IconButton(Icon.ENTER, (x) -> getMenu().loadNBT());
        load.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.nbt_export_bus.load")));
        scrollbar = new Scrollbar();
        scrollbar.setSize(16, 64);
        scrollbar.setRange(0, 64, 4);
    }
}
