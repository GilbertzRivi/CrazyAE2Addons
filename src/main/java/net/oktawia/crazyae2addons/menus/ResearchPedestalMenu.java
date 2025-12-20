package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.guisync.GuiSync;
import appeng.menu.interfaces.IProgressProvider;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.ResearchPedestalBottomBE;
import net.oktawia.crazyae2addons.entities.ResearchUnitBE;

public class ResearchPedestalMenu extends AEBaseMenu {

    @GuiSync(881) public int computation;
    @GuiSync(882) public boolean validConnection;

    public ResearchPedestalMenu(int id, Inventory playerInventory, ResearchPedestalBottomBE host) {
        super(CrazyMenuRegistrar.RESEARCH_PEDESTAL_MENU.get(), id, playerInventory, host);
        this.computation = host.getConnectedComputation();
        this.validConnection = host.isValidConnection();

        this.createPlayerInventorySlots(playerInventory);
    }
}
