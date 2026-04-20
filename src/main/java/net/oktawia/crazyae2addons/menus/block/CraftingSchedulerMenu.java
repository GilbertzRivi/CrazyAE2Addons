package net.oktawia.crazyae2addons.menus.block;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import appeng.menu.slot.FakeSlot;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.CraftingSchedulerBE;

public class CraftingSchedulerMenu extends AEBaseMenu {

    @Getter
    private final CraftingSchedulerBE host;

    public static final String SAVE = "actionSave";

    public CraftingSchedulerMenu(int id, Inventory ip, CraftingSchedulerBE host) {
        super(CrazyMenuRegistrar.CRAFTING_SCHEDULER_MENU.get(), id, ip, host);
        this.host = host;

        addSlot(new FakeSlot(host.inv.createMenuWrapper(), 0), SlotSemantics.CONFIG);
        registerClientAction(SAVE, Integer.class, this::save);
        createPlayerInventorySlots(ip);
    }

    public void save(Integer amount) {
        this.host.setAmount(amount);
        this.host.setChanged();

        if (isClientSide()) {
            sendClientAction(SAVE, amount);
        }
    }
}