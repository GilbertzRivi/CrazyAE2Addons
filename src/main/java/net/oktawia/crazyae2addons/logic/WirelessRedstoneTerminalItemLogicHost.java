package net.oktawia.crazyae2addons.logic;

import appeng.api.upgrades.IUpgradeableObject;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.menus.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class WirelessRedstoneTerminalItemLogicHost extends WTMenuHost implements IUpgradeableObject {

    public WirelessRedstoneTerminalItemLogicHost(ItemWT item,
                                                 Player player,
                                                 ItemMenuHostLocator locator,
                                                 BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get());
    }

    public void toggle(String name) {
        if (!(getItemStack().getItem() instanceof ItemWT wt)) return;

        var grid = wt.getLinkedGrid(
                getItemStack(),
                getPlayer().level(),
                msg -> getPlayer().displayClientMessage(msg, true)
        );
        if (grid == null) return;

        grid.getActiveMachines(RedstoneEmitterPart.class)
                .stream()
                .filter(part -> Objects.equals(part.name, name))
                .findFirst()
                .ifPresent(emitter -> emitter.setState(!emitter.getState()));
    }

    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters(String filter) {
        if (!(getItemStack().getItem() instanceof ItemWT wt)) return List.of();

        var grid = wt.getLinkedGrid(
                getItemStack(),
                getPlayer().level(),
                msg -> getPlayer().displayClientMessage(msg, true)
        );
        if (grid == null) return List.of();

        final String f = filter.toLowerCase();
        return grid.getActiveMachines(RedstoneEmitterPart.class)
                .stream()
                .filter(emitter -> emitter.name.toLowerCase().contains(f))
                .sorted((a, b) -> a.name.compareToIgnoreCase(b.name))
                .map(part -> new RedstoneTerminalMenu.EmitterInfo(
                        part.getBlockEntity().getBlockPos(),
                        part.name,
                        part.getState()
                ))
                .toList();
    }



    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters() {
        return getEmitters("");
    }
}
