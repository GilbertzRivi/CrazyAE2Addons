package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PenroseInjectionPortBE;

public class PenroseInjectionPortMenu extends AEBaseMenu {

    private final PenroseInjectionPortBE host;

    @GuiSync(884)
    public int desiredRate;

    public String SET_RATE = "actionSetRate";

    public PenroseInjectionPortMenu(int id, Inventory playerInventory, PenroseInjectionPortBE host) {
        super(CrazyMenuRegistrar.PENROSE_INJECTION_PORT_MENU.get(), id, playerInventory, host);
        this.host = host;
        this.desiredRate = host.desiredRate;
        registerClientAction(SET_RATE, Integer.class, this::setDesiredRate);
        this.createPlayerInventorySlots(playerInventory);
    }

    public void setDesiredRate(int rate) {
        if (rate < 0) rate = 0;
        if (rate > PenroseInjectionPortBE.MAX_RATE) rate = PenroseInjectionPortBE.MAX_RATE;

        host.desiredRate = rate;
        this.desiredRate = rate;

        if (isClientSide()) {
            sendClientAction(SET_RATE, rate);
        }
    }
}
