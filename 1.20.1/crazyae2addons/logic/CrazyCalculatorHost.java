package net.oktawia.crazyae2addons.logic;

import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.menus.CrazyCalculatorMenu;
import org.jetbrains.annotations.Nullable;

public class CrazyCalculatorHost extends ItemMenuHost {
    private CrazyCalculatorMenu menu;

    public CrazyCalculatorHost(Player player, @Nullable Integer slot, ItemStack itemStack) {
        super(player, slot, itemStack);
    }

    public void setMenu(CrazyCalculatorMenu menu) {
        this.menu = menu;
    }

    public CrazyCalculatorMenu getMenu(){
        return this.menu;
    }
}
