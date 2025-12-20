package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.PenroseMassEmitterBE;

public class PenroseMassEmitterMenu extends AEBaseMenu {

    private final PenroseMassEmitterBE host;

    @GuiSync(891) public double mass_on_percent;
    @GuiSync(892) public double mass_off_percent;
    @GuiSync(893) public boolean emitting;

    public static final String SET_MASS_ON  = "actionSetMassOnPercent";
    public static final String SET_MASS_OFF = "actionSetMassOffPercent";

    public PenroseMassEmitterMenu(int id, Inventory playerInventory, PenroseMassEmitterBE host) {
        super(CrazyMenuRegistrar.PENROSE_MASS_EMITTER_MENU.get(), id, playerInventory, host);
        this.host = host;

        this.mass_on_percent = host.massOnPercent;
        this.mass_off_percent = host.massOffPercent;
        this.emitting = host.shouldEmit();

        registerClientAction(SET_MASS_ON, Double.class, this::setMassOnPercent);
        registerClientAction(SET_MASS_OFF, Double.class, this::setMassOffPercent);

        this.createPlayerInventorySlots(playerInventory);
    }

    public void setMassOnPercent(double pct) {
        host.massOnPercent = pct;
        host.setChanged();
        if (isClientSide()) {
            sendClientAction(SET_MASS_ON, pct);
        }
    }

    public void setMassOffPercent(double pct) {
        host.massOffPercent = pct;
        host.setChanged();
        if (isClientSide()) {
            sendClientAction(SET_MASS_OFF, pct);
        }
    }
}
