package net.oktawia.crazyae2addonslite.misc;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public class AppEngEnchantableSlot extends AppEngSlot {
    public AppEngEnchantableSlot(InternalInventory inv, int invSlot) {
        super(inv, invSlot);
    }
    @Override
    public boolean mayPlace(ItemStack stack) {
        return this.isSlotEnabled() && stack.isEnchantable() || stack.getItem() == Items.BOOK;
    }
}
