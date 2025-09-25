package net.oktawia.crazyae2addons.items;

import appeng.menu.locator.ItemMenuHostLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import org.jetbrains.annotations.NotNull;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;

public class WirelessRedstoneTerminal extends ItemWT {

    public WirelessRedstoneTerminal() {
        super();
    }

    @Override
    public @NotNull MenuType<?> getMenuType(@NotNull ItemMenuHostLocator locator, @NotNull Player player) {
        return CrazyMenuRegistrar.WIRELESS_REDSTONE_TERMINAL_MENU.get();
    }
}
