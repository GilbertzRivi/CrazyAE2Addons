package net.oktawia.crazyae2addonslite.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addonslite.menus.CrazyPatternMultiplierMenu;
import org.jetbrains.annotations.Nullable;

public class CrazyPatternMultiplierHost extends ItemMenuHost implements InternalInventoryHost {

    public final AppEngInternalInventory inv = new AppEngInternalInventory(this, 36);
    private CrazyPatternMultiplierMenu menu;

    public CrazyPatternMultiplierHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);

        CompoundTag itemTag = this.getItemStack().getTag();
        if (itemTag != null) {
            this.inv.readFromNBT(itemTag, "inv");
        }
    }

    @Override
    public void saveChanges() {
        CompoundTag itemTag = this.getItemStack().getOrCreateTag();
        this.inv.writeToNBT(itemTag, "inv");
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        CompoundTag itemTag = this.getItemStack().getOrCreateTag();
        if (inv == this.inv) {
            this.inv.writeToNBT(itemTag, "inv");
        }
    }

    public void setMenu(CrazyPatternMultiplierMenu menu) {
        this.menu = menu;
    }

    public CrazyPatternMultiplierMenu getMenu(){
        return this.menu;
    }
}
