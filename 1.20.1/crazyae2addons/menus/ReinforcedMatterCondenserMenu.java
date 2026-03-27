package net.oktawia.crazyae2addons.menus;

import appeng.api.config.Settings;
import appeng.client.gui.Icon;
import appeng.core.definitions.AEItems;
import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.interfaces.IProgressProvider;
import appeng.menu.slot.AppEngSlot;
import appeng.menu.slot.FakeSlot;
import appeng.menu.slot.OutputSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.EjectorBE;
import net.oktawia.crazyae2addons.entities.ReinforcedMatterCondenserBE;
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;

public class ReinforcedMatterCondenserMenu extends AEBaseMenu implements IProgressProvider {

    @GuiSync(39)
    public Long storedPower = 0L;
    @GuiSync(94)
    public Integer storedCells = 0;

    public ReinforcedMatterCondenserBE host;
    public CellProgressProvider CellProvider = new CellProgressProvider();

    public ReinforcedMatterCondenserMenu(int id, Inventory ip, ReinforcedMatterCondenserBE host) {
        super(CrazyMenuRegistrar.REINFORCED_MATTER_CONDENSER_MENU.get(), id, ip, host);
        this.storedPower = host.storedPower;
        this.storedCells = host.componentInv.getStackInSlot(0).getCount();
        this.host = host;
        this.addSlot(new AppEngSlot(host.inputInv, 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngFilteredSlot(host.componentInv, 0, AEItems.CELL_COMPONENT_256K.asItem()), SlotSemantics.STORAGE_CELL);
        this.addSlot(new OutputSlot(host.outputInv, 0, Icon.CONDENSER_OUTPUT_SINGULARITY), SlotSemantics.MACHINE_OUTPUT);
        this.createPlayerInventorySlots(ip);
    }

    @Override
    public void broadcastChanges() {
        if (isServerSide()) {
            this.storedPower = this.host.storedPower;
            this.storedCells = this.host.componentInv.getStackInSlot(0).getCount();
        }

        super.broadcastChanges();
    }

    @Override
    public int getCurrentProgress() {
        return Math.toIntExact(this.storedPower);
    }

    @Override
    public int getMaxProgress() {
        return 8192;
    }

    public class CellProgressProvider implements IProgressProvider {

        @Override
        public int getCurrentProgress() {
            return ReinforcedMatterCondenserMenu.this.storedCells;
        }

        @Override
        public int getMaxProgress() {
            return 64;
        }
    }
}
