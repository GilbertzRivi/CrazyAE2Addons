package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PenroseHeatEmitterBE;

public class PenroseHeatEmitterMenu extends AEBaseMenu {

    private final PenroseHeatEmitterBE host;

    @GuiSync(882)
    public double heat_on_gk;

    @GuiSync(883)
    public double heat_off_gk;

    @GuiSync(884)
    public boolean emitting;

    public static final String SET_HEAT_ON  = "actionSetHeatOn";
    public static final String SET_HEAT_OFF = "actionSetHeatOff";

    public PenroseHeatEmitterMenu(int id, Inventory playerInventory, PenroseHeatEmitterBE host) {
        super(CrazyMenuRegistrar.PENROSE_HEAT_EMITTER_MENU.get(), id, playerInventory, host);
        this.host = host;

        this.heat_on_gk = host.heatOnGK;
        this.heat_off_gk = host.heatOffGK;
        this.emitting = host.shouldEmit();

        registerClientAction(SET_HEAT_ON, Double.class, this::setHeatOn);
        registerClientAction(SET_HEAT_OFF, Double.class, this::setHeatOff);

        this.createPlayerInventorySlots(playerInventory);
    }

    public void setHeatOn(double heat) {
        host.heatOnGK = heat;
        host.setChanged();

        if (isClientSide()) {
            sendClientAction(SET_HEAT_ON, heat);
        }
    }

    public void setHeatOff(double heat) {
        host.heatOffGK = heat;
        host.setChanged();

        if (isClientSide()) {
            sendClientAction(SET_HEAT_OFF, heat);
        }
    }
}
