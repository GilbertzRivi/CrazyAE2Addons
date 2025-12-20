package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PenroseHeatVentBE;

public class PenroseHeatVentMenu extends AEBaseMenu {

    private final PenroseHeatVentBE host;

    @GuiSync(885)
    public int desiredCooling;

    @GuiSync(886)
    public long costPerTick;

    public String SET_COOLING = "actionSetCooling";

    public PenroseHeatVentMenu(int id, Inventory playerInventory, PenroseHeatVentBE host) {
        super(CrazyMenuRegistrar.PENROSE_HEAT_VENT_MENU.get(), id, playerInventory, host);
        this.host = host;
        this.desiredCooling = (int) Math.round(host.desiredCooling);
        this.costPerTick = PenroseHeatVentBE.computeCostForCooling(host.desiredCooling);
        registerClientAction(SET_COOLING, Integer.class, this::setDesiredCooling);
        this.createPlayerInventorySlots(playerInventory);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (!isClientSide()) {
            this.desiredCooling = (int) Math.round(host.desiredCooling);
            this.costPerTick = PenroseHeatVentBE.computeCostForCooling(host.desiredCooling);
        }
    }

    public void setDesiredCooling(int cooling) {
        if (cooling < 0) cooling = 0;
        // jak chcesz, możesz tu dać też górny limit
        host.desiredCooling = cooling;
        this.desiredCooling = cooling;
        this.costPerTick = PenroseHeatVentBE.computeCostForCooling(host.desiredCooling);

        if (isClientSide()) {
            sendClientAction(SET_COOLING, cooling);
        }
    }
}
