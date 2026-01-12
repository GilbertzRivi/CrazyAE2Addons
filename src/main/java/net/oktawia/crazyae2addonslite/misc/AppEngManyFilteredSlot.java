package net.oktawia.crazyae2addonslite.misc;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class AppEngManyFilteredSlot extends AppEngSlot {

    public List<ItemStack> filter;
    public AppEngManyFilteredSlot(InternalInventory inv, int invSlot, List<ItemStack> filter) {
        super(inv, invSlot);
        this.filter = filter;
    }
    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isSlotEnabled() && filter.stream().anyMatch(entry -> entry.getItem().equals(stack.getItem()));
    }
}
