package net.oktawia.crazyae2addons.screens;

import appeng.api.config.*;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.ServerSettingToggleButton;
import appeng.client.gui.widgets.SettingToggleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.NBTStorageBusMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;

public class NBTStorageBusScreen<C extends NBTStorageBusMenu> extends UpgradeableScreen<C> {
    private static IconButton confirm;
    private static MultilineTextFieldWidget input;
    public static boolean initialized;
    private static Scrollbar scrollbar;
    public static IconButton load;
    private final SettingToggleButton<AccessRestriction> rwMode;
    private final SettingToggleButton<StorageFilter> storageFilter;
    private final SettingToggleButton<YesNo> filterOnExtract;
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

        this.storageFilter.set(this.menu.getStorageFilter());
        this.rwMode.set(this.menu.getReadWriteMode());
        this.filterOnExtract.set(this.menu.getFilterOnExtract());
    }

    public NBTStorageBusScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("confirm", confirm);
        this.widgets.add("data", input);
        this.widgets.add("scroll", scrollbar);
        this.widgets.add("load", load);
        initialized = false;
        widgets.addOpenPriorityButton();

        this.rwMode = new ServerSettingToggleButton<>(Settings.ACCESS, AccessRestriction.READ_WRITE);
        this.storageFilter = new ServerSettingToggleButton<>(Settings.STORAGE_FILTER, StorageFilter.EXTRACTABLE_ONLY);
        this.filterOnExtract = new ServerSettingToggleButton<>(Settings.FILTER_ON_EXTRACT, YesNo.YES);

        this.addToLeftToolbar(this.storageFilter);
        this.addToLeftToolbar(this.filterOnExtract);
        this.addToLeftToolbar(this.rwMode);
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
        confirm.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.nbt_storage_confirm")));

        input = new MultilineTextFieldWidget(
                font, 0, 0, 110, 100,
                Component.translatable("gui.crazyae2addons.nbt_storage_input"));

        load = new IconButton(Icon.ENTER, (x) -> getMenu().loadNBT());
        load.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.nbt_storage_load")));

        scrollbar = new Scrollbar();
        scrollbar.setSize(16, 64);
        scrollbar.setRange(0, 64, 4);
    }
}