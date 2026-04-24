package net.oktawia.crazyae2addons.menus.item;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.viewcell.TagViewCellHost;

public class TagViewCellMenu extends AEBaseMenu {

    public static final String SEND_DATA = "SendData";
    public static final String FILTER_TAG = "filter";

    private final TagViewCellHost host;

    @GuiSync(31)
    public String data = "";

    public TagViewCellMenu(int id, Inventory playerInventory, TagViewCellHost host) {
        super(CrazyMenuRegistrar.TAG_VIEW_CELL_MENU.get(), id, playerInventory, host);
        this.host = host;

        registerClientAction(SEND_DATA, String.class, this::updateData);
        createPlayerInventorySlots(playerInventory);

        var tag = host.getItemStack().getOrCreateTag();
        if (tag.contains(FILTER_TAG)) {
            this.data = tag.getString(FILTER_TAG);
        }
    }

    public void updateData(String data) {
        this.data = data == null ? "" : data;
        this.host.getItemStack().getOrCreateTag().putString(FILTER_TAG, this.data);

        if (isClientSide()) {
            sendClientAction(SEND_DATA, this.data);
        }
    }
}