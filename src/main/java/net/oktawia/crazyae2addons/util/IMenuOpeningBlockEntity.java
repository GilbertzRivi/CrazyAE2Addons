package net.oktawia.crazyae2addons.util;

import appeng.menu.locator.MenuLocator;
import net.minecraft.world.entity.player.Player;

public interface IMenuOpeningBlockEntity {
    void openMenu(Player player, MenuLocator locator);
}