package net.oktawia.crazyae2addons.menus.block.penrose;

import appeng.menu.AEBaseMenu;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.penrose.PenroseMassEmitterBE;

public class PenroseMassEmitterMenu extends AEBaseMenu {

    public static final String ACTION_SET_MASS_ON = "actionSetMassOnPercent";
    public static final String ACTION_SET_MASS_OFF = "actionSetMassOffPercent";

    @Getter
    private final PenroseMassEmitterBE host;

    public PenroseMassEmitterMenu(int id, Inventory playerInventory, PenroseMassEmitterBE host) {
        super(CrazyMenuRegistrar.PENROSE_MASS_EMITTER_MENU.get(), id, playerInventory, host);
        this.host = host;
        registerClientAction(ACTION_SET_MASS_ON, Double.class, this::setMassOnPercent);
        registerClientAction(ACTION_SET_MASS_OFF, Double.class, this::setMassOffPercent);
        createPlayerInventorySlots(playerInventory);
    }

    public void setMassOnPercent(double percent) {
        host.setMassOnPercent(percent);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_MASS_ON, host.getMassOnPercent());
        }
    }

    public void setMassOffPercent(double percent) {
        host.setMassOffPercent(percent);

        if (isClientSide()) {
            sendClientAction(ACTION_SET_MASS_OFF, host.getMassOffPercent());
        }
    }
}