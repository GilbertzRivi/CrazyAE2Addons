package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.DataHost;

public class DataDriveMenu extends AEBaseMenu {
    public DataHost host;
    public DataDriveMenu(int id, Inventory playerInventory, DataHost host) {
        super(CrazyMenuRegistrar.DATA_DRIVE_MENU.get(), id, playerInventory, host);
        this.createPlayerInventorySlots(playerInventory);
        this.host = host;
    }
}
