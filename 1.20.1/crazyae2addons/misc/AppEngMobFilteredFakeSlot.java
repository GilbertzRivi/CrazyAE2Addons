package net.oktawia.crazyae2addons.misc;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.FakeSlot;
import net.minecraft.world.item.ItemStack;

public class AppEngMobFilteredFakeSlot extends FakeSlot {

    public AppEngMobFilteredFakeSlot(InternalInventory inv, int invSlot) {
        super(inv, invSlot);
    }

    @Override
    public boolean canSetFilterTo(ItemStack stack) {
        return true;
    }
}
