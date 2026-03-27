package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.CrazyCalculatorHost;

public class CrazyCalculatorMenu extends AEBaseMenu {
    public CrazyCalculatorHost host;

    public CrazyCalculatorMenu(int id, Inventory ip, CrazyCalculatorHost host) {
        super(CrazyMenuRegistrar.CRAZY_CALCULATOR_MENU.get(), id, ip, host);
        this.createPlayerInventorySlots(ip);
        this.host = host;
    }
}
