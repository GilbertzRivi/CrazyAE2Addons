package net.oktawia.crazyae2addons.logic.patternmultiplier;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import lombok.Getter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.items.PatternMultiplierItem;
import org.jetbrains.annotations.Nullable;

public class PatternMultiplierHost extends ItemMenuHost implements InternalInventoryHost {

    @Getter
    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 36) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return PatternMultiplierItem.isAllowedInMultiplier(stack);
        }
    };

    public PatternMultiplierHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);

        var tag = getItemStack().getTag();
        if (tag != null) {
            inventory.readFromNBT(tag, PatternMultiplierItem.INV_TAG);
        }
    }

    @Override
    public void saveChanges() {
        inventory.writeToNBT(getItemStack().getOrCreateTag(), PatternMultiplierItem.INV_TAG);
    }

    @Override
    public void onChangeInventory(InternalInventory inventory, int slot) {
        if (inventory == this.inventory) {
            saveChanges();
        }
    }
}