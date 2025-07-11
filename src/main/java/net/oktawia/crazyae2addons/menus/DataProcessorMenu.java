package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.DataProcessorBE;
import net.oktawia.crazyae2addons.parts.DisplayPart;


public class DataProcessorMenu extends AEBaseMenu {
    public DataProcessorMenu(int id, Inventory ip, DataProcessorBE host) {
        super(CrazyMenuRegistrar.DATA_PROCESSOR_MENU.get(), id, ip, host);
        this.addSlot(new Slot(host.inv.toContainer(), 0, 16, 16));
        this.createPlayerInventorySlots(ip);

    }
}
