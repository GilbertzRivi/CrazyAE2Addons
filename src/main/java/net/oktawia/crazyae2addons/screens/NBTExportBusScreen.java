package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.NBTExportBusMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;

public class NBTExportBusScreen<C extends NBTExportBusMenu> extends AEBaseScreen<C> {
    private static IconButton confirm;
    private static MultilineTextFieldWidget input;
    public static boolean initialized;
    public static IconButton load;

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
        this.widgets.add("load", load);
        initialized = false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (input.isMouseOver(x, y)) return input.mouseScrolled(x, y, delta);
        return super.mouseScrolled(x, y, delta);
    }

    private void setupGui(){
        confirm = new IconButton(Icon.ENTER, (btn) -> getMenu().updateData(input.getValue()));
        confirm.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.nbt_export_confirm")));

        input = new MultilineTextFieldWidget(
                font, 0, 0, 205, 135,
                Component.translatable("gui.crazyae2addons.nbt_export_input"));

        load = new IconButton(Icon.ENTER, (x) -> getMenu().loadNBT());
        load.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.nbt_export_load")));
    }
}
