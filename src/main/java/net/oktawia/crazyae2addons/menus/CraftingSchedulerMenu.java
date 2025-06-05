package net.oktawia.crazyae2addons.menus;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantic;
import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.menu.slot.FakeSlot;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;
import net.oktawia.crazyae2addons.entities.CraftingSchedulerBE;


public class CraftingSchedulerMenu extends AEBaseMenu {
    public CraftingSchedulerBE host;
    public String SAVE = "actionSave";

    @GuiSync(493)
    public Integer amount;

    public CraftingSchedulerMenu(int id, Inventory ip, CraftingSchedulerBE host) {
        super(CrazyMenuRegistrar.CRAFTING_SCHEDULER_MENU.get(), id, ip, host);
        this.host = host;
        this.amount = host.amount;
        addSlot(new FakeSlot(host.inv.createMenuWrapper(), 0), SlotSemantics.CONFIG);
        registerClientAction(SAVE, Integer.class, this::save);
    }

    public void save(Integer amount) {
        this.host.amount = amount;
        if (isClientSide()){
            sendClientAction(SAVE, amount);
        }
    }
}
