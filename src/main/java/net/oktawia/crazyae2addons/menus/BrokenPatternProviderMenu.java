package net.oktawia.crazyae2addons.menus;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.world.entity.player.Inventory;

public class BrokenPatternProviderMenu extends PatternProviderMenu {
    public BrokenPatternProviderMenu(int id, Inventory ip, PatternProviderLogicHost host) {
        super(net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar.BROKEN_PATTERN_PROVIDER_MENU.get(), id, ip, host);
    }
}
