package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.interfaces.IModifierMenu;
import net.oktawia.crazyae2addons.menus.CrazyPatternModifierMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;

public class CrazyPatternModifierHost extends ItemMenuHost implements InternalInventoryHost {
    public final AppEngInternalInventory inv = new AppEngInternalInventory(this, 1);
    private IModifierMenu menu;

    public CrazyPatternModifierHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
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
            if (this.getMenu() != null){
                this.getMenu().ping();
            }
        }
    }

    public void setMenu(IModifierMenu menu) {
        this.menu = menu;
    }

    public IModifierMenu getMenu(){
        return this.menu;
    }
}
