package net.oktawia.crazyae2addons.misc;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.FakeSlot;
import net.minecraft.world.item.ItemStack;

public class DisplayFakeSlot extends FakeSlot {
    public DisplayFakeSlot(InternalInventory inv, int invSlot) {
        super(inv, invSlot);
    }
    @Override
    public boolean canSetFilterTo(ItemStack stack) { return false; }
    @Override
    public void increase(ItemStack is){}
    @Override
    public void decrease(ItemStack is){}
}
