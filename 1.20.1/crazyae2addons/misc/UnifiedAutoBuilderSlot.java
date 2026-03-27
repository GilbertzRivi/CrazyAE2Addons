package net.oktawia.crazyae2addons.misc;

import appeng.api.inventories.InternalInventory;
import appeng.menu.slot.AppEngSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import org.jetbrains.annotations.NotNull;

public class UnifiedAutoBuilderSlot extends AppEngSlot {
    private final InternalInventory inv;

    public UnifiedAutoBuilderSlot(InternalInventory inv, int index) {
        super(inv, index);
        this.inv = inv;
    }

    @Override
    public boolean mayPlace(@NotNull ItemStack stack) {
        return inv.getStackInSlot(1).isEmpty() &&
               stack.getItem() == CrazyItemRegistrar.BUILDER_PATTERN.get() &&
               inv.getStackInSlot(0).isEmpty();
    }

    @Override
    public @NotNull ItemStack getItem() {
        ItemStack in = inv.getStackInSlot(0);
        return in.isEmpty() ? inv.getStackInSlot(1) : in;
    }

    @Override
    public void set(@NotNull ItemStack stack) {
        if (stack.isEmpty()) {
            if (!inv.getStackInSlot(0).isEmpty()) {
                inv.setItemDirect(0, ItemStack.EMPTY);
            } else if (!inv.getStackInSlot(1).isEmpty()) {
                inv.setItemDirect(1, ItemStack.EMPTY);
            }
        } else if (inv.getStackInSlot(1).isEmpty()) {
            inv.setItemDirect(0, stack);
        }
    }

    @Override
    public boolean mayPickup(@NotNull Player player) {
        return !inv.getStackInSlot(0).isEmpty() || !inv.getStackInSlot(1).isEmpty();
    }

    @Override
    public @NotNull ItemStack remove(int amount) {
        if (!inv.getStackInSlot(0).isEmpty()) {
            return inv.extractItem(0, amount, false);
        } else {
            return inv.extractItem(1, amount, false);
        }
    }
}
