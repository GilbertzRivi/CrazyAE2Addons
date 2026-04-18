package net.oktawia.crazyae2addons.logic.interfaces;

import appeng.menu.locator.MenuHostLocator;
import net.minecraft.world.entity.player.Player;

public interface IMenuOpeningBlockEntity {
    void openMenu(Player player, MenuHostLocator locator);
}