package net.oktawia.crazyae2addons.menus.block;

import appeng.menu.AEBaseMenu;
import appeng.menu.SlotSemantics;
import lombok.Getter;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.AutoEnchanterBE;
import net.oktawia.crazyae2addons.misc.AppEngEnchantableSlot;
import net.oktawia.crazyae2addons.misc.AppEngFilteredSlot;

public class AutoEnchanterMenu extends AEBaseMenu {

    private static final String ACTION_SYNC_OPTION = "sync_option";
    private static final String ACTION_CHANGE_AUTO_SUPPLY_LAPIS = "change_auto_supply_lapis";
    private static final String ACTION_CHANGE_AUTO_SUPPLY_BOOKS = "change_auto_supply_books";

    @Getter
    private final AutoEnchanterBE host;

    public AutoEnchanterMenu(int id, Inventory playerInventory, AutoEnchanterBE host) {
        super(CrazyMenuRegistrar.AUTO_ENCHANTER_MENU.get(), id, playerInventory, host);
        this.host = host;

        this.addSlot(new AppEngEnchantableSlot(this.host.getInputInv(), 0), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngFilteredSlot(this.host.getLapisInv(), 0, Items.LAPIS_LAZULI), SlotSemantics.MACHINE_INPUT);
        this.addSlot(new AppEngFilteredSlot(this.host.getOutputInv(), 0, Items.AIR), SlotSemantics.MACHINE_OUTPUT);

        this.registerClientAction(ACTION_SYNC_OPTION, Integer.class, this::syncOption);
        this.registerClientAction(ACTION_CHANGE_AUTO_SUPPLY_LAPIS, Boolean.class, this::changeAutoSupplyLapis);
        this.registerClientAction(ACTION_CHANGE_AUTO_SUPPLY_BOOKS, Boolean.class, this::changeAutoSupplyBooks);

        createPlayerInventorySlots(playerInventory);
    }

    public void syncOption(Integer option) {
        if (option == null) {
            return;
        }

        if (isClientSide()) {
            sendClientAction(ACTION_SYNC_OPTION, option);
            return;
        }

        host.setOption(option);
    }

    public void changeAutoSupplyLapis(Boolean value) {
        if (value == null) {
            return;
        }

        if (isClientSide()) {
            sendClientAction(ACTION_CHANGE_AUTO_SUPPLY_LAPIS, value);
            return;
        }

        host.setAutoSupplyLapis(value);
    }

    public void changeAutoSupplyBooks(Boolean value) {
        if (value == null) {
            return;
        }

        if (isClientSide()) {
            sendClientAction(ACTION_CHANGE_AUTO_SUPPLY_BOOKS, value);
            return;
        }

        host.setAutoSupplyBooks(value);
    }
}