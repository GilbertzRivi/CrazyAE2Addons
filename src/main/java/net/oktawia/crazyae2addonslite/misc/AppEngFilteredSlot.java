package net.oktawia.crazyae2addonslite.misc;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class AppEngFilteredSlot extends AppEngSlot {

    public Item filter;
    public AppEngFilteredSlot(InternalInventory inv, int invSlot, Item filter) {
        super(inv, invSlot);
        this.filter = filter;
    }
    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isSlotEnabled() && filter == stack.getItem();
    }
}
