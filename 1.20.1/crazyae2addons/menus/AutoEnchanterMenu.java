package net.oktawia.crazyae2addons.menus;

import appeng.menu.SlotSemantics;
import appeng.menu.guisync.GuiSync;
import appeng.menu.implementations.UpgradeableMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;
import net.oktawia.crazyae2addons.misc.AppEngEnchantableSlot;
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;


public class AutoEnchanterMenu extends UpgradeableMenu<AutoEnchanterBE> {
    public AutoEnchanterBE host;
    public String ACTION_SYNC_OPTION = "actionSyncOption";
    public String CHANGE_AUTO_SUPPLY_LAPIS = "actionChangeAutoSupplyLapis";
    public String CHANGE_AUTO_SUPPLY_BOOKS = "actionChangeAutoSupplyBooks";

    @GuiSync(394)
    public Integer xp = 0;
    @GuiSync(324)
    public Integer option;
    @GuiSync(322)
    public Boolean autoSupplyLapis;
    @GuiSync(329)
    public Boolean autoSupplyBooks;
    @GuiSync(320)
    public String levelCost;

    public AutoEnchanterMenu(int id, Inventory ip, AutoEnchanterBE host) {
        super(CrazyMenuRegistrar.AUTO_ENCHANTER_MENU.get(), id, ip, host);
        this.host = host;
        this.host.setMenu(this);
        this.xp = this.host.xp;
        this.option = this.host.option;
        this.autoSupplyLapis = this.host.autoSupplyLapis;
        this.autoSupplyBooks = this.host.autoSupplyBooks;
        this.levelCost = this.host.levelCost;
        this.addSlot(new AppEngEnchantableSlot(this.host.inputInv, 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngFilteredSlot(this.host.lapisInv, 0, Items.LAPIS_LAZULI), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngFilteredSlot(this.host.outputInv, 0, Items.AIR), SlotSemantics.MACHINE_OUTPUT);
        this.registerClientAction(ACTION_SYNC_OPTION, Integer.class, this::syncOption);
        this.registerClientAction(CHANGE_AUTO_SUPPLY_LAPIS, Boolean.class, this::changeAutoSupplyLapis);
        this.registerClientAction(CHANGE_AUTO_SUPPLY_BOOKS, Boolean.class, this::changeAutoSupplyBooks);
    }

    public void changeAutoSupplyLapis(Boolean val) {
        this.host.autoSupplyLapis = val;
        this.autoSupplyLapis = val;
        this.host.setChanged();
        this.host.markForUpdate();
        if (isClientSide()){
            sendClientAction(CHANGE_AUTO_SUPPLY_LAPIS, val);
        }
    }

    public void changeAutoSupplyBooks(Boolean val) {
        this.host.autoSupplyBooks = val;
        this.autoSupplyBooks = val;
        this.host.setChanged();
        this.host.markForUpdate();
        if (isClientSide()){
            sendClientAction(CHANGE_AUTO_SUPPLY_BOOKS, val);
        }
    }

    public void syncOption(Integer option) {
        this.option = option;
        this.host.option = option;
        this.host.setChanged();
        this.host.markForUpdate();
        if (isClientSide()){
            sendClientAction(ACTION_SYNC_OPTION, option);
        }
    }
}
