package net.oktawia.crazyae2addons.logic.wireless;

import appeng.api.networking.IGrid;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.menu.ISubMenu;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IRedstoneTerminalHost;
import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.parts.RedstoneEmitter;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;

public class WirelessRedstoneTerminalItemLogicHost extends WTMenuHost
        implements IUpgradeableObject, IRedstoneTerminalHost {

    public final IUpgradeInventory upgrades = UpgradeInventories.forItem(this.getItemStack(), 2);

    public WirelessRedstoneTerminalItemLogicHost(
            Player player,
            @Nullable Integer slot,
            ItemStack itemStack,
            BiConsumer<Player, ISubMenu> returnToMainMenu
    ) {
        super(player, slot, itemStack, returnToMainMenu);
        this.readFromNbt();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(CrazyItemRegistrar.WIRELESS_REDSTONE_TERMINAL.get());
    }

    private IGrid getGrid() {
        if (!(this.getItemStack().getItem() instanceof ItemWT wt)) {
            return null;
        }

        return wt.getLinkedGrid(this.getItemStack(), this.getPlayer().level(), this.getPlayer());
    }

    @Override
    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters(String filter) {
        IGrid grid = getGrid();
        if (grid == null) {
            return List.of();
        }

        String normalizedFilter = filter == null ? "" : filter.toLowerCase(Locale.ROOT).trim();

        return grid.getActiveMachines(RedstoneEmitter.class)
                .stream()
                .filter(emitter -> matchesFilter(emitter, normalizedFilter))
                .sorted((a, b) -> a.getNameId().compareToIgnoreCase(b.getNameId()))
                .map(emitter -> new RedstoneTerminalMenu.EmitterInfo(
                        emitter.getBlockEntity().getBlockPos(),
                        emitter.getNameId(),
                        emitter.getState()
                ))
                .toList();
    }

    @Override
    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters() {
        return getEmitters("");
    }

    @Override
    public void toggle(String name) {
        IGrid grid = getGrid();
        if (grid == null) {
            return;
        }

        var emitters = grid.getActiveMachines(RedstoneEmitter.class)
                .stream()
                .filter(emitter -> Objects.equals(emitter.getNameId(), name))
                .toList();

        if (emitters.isEmpty()) {
            return;
        }

        boolean newState = !emitters.get(0).getState();
        for (RedstoneEmitter emitter : emitters) {
            emitter.setState(newState);
        }
    }

    private boolean matchesFilter(RedstoneEmitter emitter, String filter) {
        if (filter == null || filter.isBlank()) {
            return true;
        }

        String emitterName = emitter.getNameId() == null
                ? ""
                : emitter.getNameId().trim().toLowerCase(Locale.ROOT);

        return !emitterName.isEmpty() && emitterName.contains(filter);
    }
}