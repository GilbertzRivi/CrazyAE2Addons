package net.oktawia.crazyae2addons.menus.block.penrose;

import appeng.menu.AEBaseMenu;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHeatVentBE;

public class PenroseHeatVentMenu extends AEBaseMenu {

    public static final String ACTION_SET_COOLING = "actionSetCooling";

    @Getter
    private final PenroseHeatVentBE host;

    public PenroseHeatVentMenu(int id, Inventory playerInventory, PenroseHeatVentBE host) {
        super(CrazyMenuRegistrar.PENROSE_HEAT_VENT_MENU.get(), id, playerInventory, host);
        this.host = host;
        registerClientAction(ACTION_SET_COOLING, Integer.class, this::setDesiredCooling);
        createPlayerInventorySlots(playerInventory);
    }

    public void setDesiredCooling(int cooling) {
        int clamped = Math.max(0, cooling);
        host.setDesiredCooling(clamped);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_COOLING, clamped);
        }
    }
}