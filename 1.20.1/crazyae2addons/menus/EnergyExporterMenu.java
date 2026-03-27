package net.oktawia.crazyae2addons.menus;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.parts.EnergyExporterPart;


public class EnergyExporterMenu extends UpgradeableMenu<EnergyExporterPart> {

    @GuiSync(353)
    public int maxAmps;
    @GuiSync(313)
    public int voltage;
    @GuiSync(113)
    public String transfered;
    @GuiSync(319)
    public boolean greg;

    public EnergyExporterMenu(int id, Inventory ip, EnergyExporterPart host) {
        super(CrazyMenuRegistrar.ENERGY_EXPORTER_MENU.get(), id, ip, host);
        this.getHost().setMenu(this);
        this.addSlot(new AppEngSlot(host.inv, 0), SlotSemantics.STORAGE);
        this.maxAmps = host.maxAmps;
        this.voltage = host.voltage;
        this.transfered = host.transfered;
        this.greg = host.greg;
    }
}
