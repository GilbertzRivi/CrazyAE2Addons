package net.oktawia.crazyae2addons.menus.block.penrose;

import appeng.menu.AEBaseMenu;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.penrose.PenroseInjectionPortBE;

public class PenroseInjectionPortMenu extends AEBaseMenu {

    public static final String ACTION_SET_RATE = "actionSetRate";

    @Getter
    private final PenroseInjectionPortBE host;

    public PenroseInjectionPortMenu(int id, Inventory playerInventory, PenroseInjectionPortBE host) {
        super(CrazyMenuRegistrar.PENROSE_INJECTION_PORT_MENU.get(), id, playerInventory, host);
        this.host = host;
        registerClientAction(ACTION_SET_RATE, Integer.class, this::setDesiredRate);
        createPlayerInventorySlots(playerInventory);
    }

    public void setDesiredRate(int rate) {
        host.setDesiredRate(rate);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_RATE, host.getDesiredRate());
        }
    }
}