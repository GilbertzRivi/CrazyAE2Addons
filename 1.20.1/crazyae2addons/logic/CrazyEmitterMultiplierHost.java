package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.inventories.InternalInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.menus.CrazyEmitterMultiplierMenu;
import net.oktawia.crazyae2addons.menus.CrazyPatternMultiplierMenu;
import org.jetbrains.annotations.Nullable;

public class CrazyEmitterMultiplierHost extends ItemMenuHost {

    private CrazyEmitterMultiplierMenu menu;

    public CrazyEmitterMultiplierHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);
    }

    public void setMenu(CrazyEmitterMultiplierMenu menu) {
        this.menu = menu;
    }

    public CrazyEmitterMultiplierMenu getMenu(){
        return this.menu;
    }
}
