package net.oktawia.crazyae2addons.logic;

import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.menu.ISubMenu;
import com.mojang.logging.LogUtils;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.menus.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.parts.RedstoneEmitterPart;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class WirelessRedstoneTerminalItemLogicHost extends WTMenuHost implements IUpgradeableObject {

    public IUpgradeInventory upgrades = UpgradeInventories.forItem(this.getItemStack(), 2);

    public WirelessRedstoneTerminalItemLogicHost(Player player, @Nullable Integer slot, ItemStack itemStack,
                                                 BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, slot, itemStack, returnToMainMenu);
        this.readFromNbt();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get());
    }

    public void toggle(String name) {
        if (!(this.getItemStack().getItem() instanceof ItemWT WRT)) return;
        var grid = WRT.getLinkedGrid(this.getItemStack(), this.getPlayer().level(), this.getPlayer());
        if (grid == null) return;
        grid.getActiveMachines(RedstoneEmitterPart.class)
                .stream().filter(part -> Objects.equals(part.name, name))
                .findFirst().ifPresent(emitter -> emitter.setState(!emitter.getState()));
    }

    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters(String filter) {
        if (!(this.getItemStack().getItem() instanceof ItemWT WRT)) return List.of();
        var grid = WRT.getLinkedGrid(this.getItemStack(), this.getPlayer().level(), this.getPlayer());
        if (grid == null) return List.of();
        return grid.getActiveMachines(RedstoneEmitterPart.class)
                .stream().filter(emitter -> emitter.name.toLowerCase().contains(filter.toLowerCase()))
                .sorted((a, b) -> a.name.compareToIgnoreCase(b.name)).map(
                        part -> new RedstoneTerminalMenu.EmitterInfo(
                                part.getBlockEntity().getBlockPos(), part.name, part.getState()
                        )
                ).toList();
    }

    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters() {
        return getEmitters("");
    }
}
