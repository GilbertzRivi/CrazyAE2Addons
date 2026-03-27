package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PenroseHawkingVentBE;

public class PenroseHawkingVentMenu extends AEBaseMenu {

    private final PenroseHawkingVentBE host;

    @GuiSync(887)
    public int desiredEvap;

    @GuiSync(888)
    public long costPerTick;

    public String SET_EVAP = "actionSetEvap";

    public PenroseHawkingVentMenu(int id, Inventory playerInventory, PenroseHawkingVentBE host) {
        super(CrazyMenuRegistrar.PENROSE_HAWKING_VENT_MENU.get(), id, playerInventory, host);
        this.host = host;
        this.desiredEvap = (int) Math.round(host.desiredEvap);
        this.costPerTick = PenroseHawkingVentBE.computeCostForEvap(host.desiredEvap);
        registerClientAction(SET_EVAP, Integer.class, this::setDesiredEvap);
        this.createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!isClientSide()) {
            this.desiredEvap = (int) Math.round(host.desiredEvap);
            this.costPerTick = PenroseHawkingVentBE.computeCostForEvap(host.desiredEvap);
        }
    }

    public void setDesiredEvap(int evap) {
        if (evap < 0) evap = 0;
        host.desiredEvap = evap;
        this.desiredEvap = evap;
        this.costPerTick = PenroseHawkingVentBE.computeCostForEvap(host.desiredEvap);

        if (isClientSide()) {
            sendClientAction(SET_EVAP, evap);
        }
    }
}
