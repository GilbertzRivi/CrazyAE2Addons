package net.oktawia.crazyae2addons.menus;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.RecipeFabricatorBE;
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;

public class RecipeFabricatorMenu extends UpgradeableMenu<RecipeFabricatorBE> {

    @GuiSync(801) public Integer progress = 0;
    @GuiSync(802) public Integer duration = 10;

    public RecipeFabricatorMenu(int id, Inventory ip, RecipeFabricatorBE host) {
        super(CrazyMenuRegistrar.RECIPE_FABRICATOR_MENU.get(), id, ip, host);
        this.addSlot(new AppEngSlot(host.input, 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngFilteredSlot(host.drive, 0, CrazyItemRegistrar.DATA_DRIVE.get()), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngFilteredSlot(host.output, 0, Items.AIR), SlotSemantics.MACHINE_OUTPUT);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (getHost() != null) {
            this.progress = getHost().getProgress();
            this.duration = getHost().getDuration(); // zawsze 10
        }
    }
}
