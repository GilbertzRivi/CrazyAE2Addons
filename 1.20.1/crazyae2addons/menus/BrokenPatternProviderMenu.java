package net.oktawia.crazyae2addons.menus;

import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.implementations.PatternProviderMenu;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.UpdatePatternsPacket;

import java.util.ArrayList;
import java.util.List;

public class BrokenPatternProviderMenu extends PatternProviderMenu {
    public BrokenPatternProviderMenu(int id, Inventory ip, PatternProviderLogicHost host) {
        super(CrazyMenuRegistrar.BROKEN_PATTERN_PROVIDER_MENU.get(), id, ip, host);
    }
}