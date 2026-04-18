package net.oktawia.crazyae2addons.menus.block.penrose;

import appeng.menu.AEBaseMenu;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHawkingVentBE;

public class PenroseHawkingVentMenu extends AEBaseMenu {

    public static final String ACTION_SET_EVAP = "actionSetEvap";

    @Getter
    private final PenroseHawkingVentBE host;

    public PenroseHawkingVentMenu(int id, Inventory playerInventory, PenroseHawkingVentBE host) {
        super(CrazyMenuRegistrar.PENROSE_HAWKING_VENT_MENU.get(), id, playerInventory, host);
        this.host = host;
        registerClientAction(ACTION_SET_EVAP, Integer.class, this::setDesiredEvap);
        createPlayerInventorySlots(playerInventory);
    }

    public void setDesiredEvap(int evap) {
        int clamped = Math.max(0, evap);
        host.setDesiredEvap(clamped);
        host.setChanged();

        if (isClientSide()) {
            sendClientAction(ACTION_SET_EVAP, clamped);
        }
    }
}