package net.oktawia.crazyae2addons.menus.block.penrose;

import appeng.menu.AEBaseMenu;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHeatEmitterBE;

public class PenroseHeatEmitterMenu extends AEBaseMenu {

    public static final String ACTION_SET_HEAT_ON = "actionSetHeatOn";
    public static final String ACTION_SET_HEAT_OFF = "actionSetHeatOff";

    @Getter
    private final PenroseHeatEmitterBE host;

    public PenroseHeatEmitterMenu(int id, Inventory playerInventory, PenroseHeatEmitterBE host) {
        super(CrazyMenuRegistrar.PENROSE_HEAT_EMITTER_MENU.get(), id, playerInventory, host);
        this.host = host;

        registerClientAction(ACTION_SET_HEAT_ON, Double.class, this::setHeatOn);
        registerClientAction(ACTION_SET_HEAT_OFF, Double.class, this::setHeatOff);

        createPlayerInventorySlots(playerInventory);
    }

    public void setHeatOn(double heat) {
        host.setHeatOnGK(heat);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_HEAT_ON, host.getHeatOnGK());
        }
    }

    public void setHeatOff(double heat) {
        host.setHeatOffGK(heat);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_HEAT_OFF, host.getHeatOffGK());
        }
    }
}