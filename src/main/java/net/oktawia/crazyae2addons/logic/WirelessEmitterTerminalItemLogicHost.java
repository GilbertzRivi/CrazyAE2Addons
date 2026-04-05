package net.oktawia.crazyae2addons.logic;

import appeng.api.networking.IGrid;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.menu.ISubMenu;
import appeng.parts.automation.StorageLevelEmitterPart;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import de.mari_023.ae2wtlib.terminal.WTMenuHost;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.interfaces.StorageLevelEmitterUuid;
import net.oktawia.crazyae2addons.menus.EmitterTerminalMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;

public class WirelessEmitterTerminalItemLogicHost extends WTMenuHost implements IUpgradeableObject {

    public IUpgradeInventory upgrades = UpgradeInventories.forItem(this.getItemStack(), 2);

    public WirelessEmitterTerminalItemLogicHost(Player player, @Nullable Integer slot, ItemStack itemStack,
                                                BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(player, slot, itemStack, returnToMainMenu);
        this.readFromNbt();
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return new ItemStack(CrazyItemRegistrar.WIRELESS_EMITTER_TERMINAL.get());
    }

    private IGrid getGrid() {
        if (!(this.getItemStack().getItem() instanceof ItemWT wt)) return null;
        return wt.getLinkedGrid(this.getItemStack(), this.getPlayer().level(), this.getPlayer());
    }

    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters(String filter) {
        IGrid grid = getGrid();
        if (grid == null) return List.of();

        String f = filter == null ? "" : filter.toLowerCase().trim();

        return grid.getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(emitter -> {
                    String name = emitter.getName().getString().trim();
                    if (f.isEmpty()) return true;
                    if (name.isEmpty()) return false;
                    return name.toLowerCase().contains(f);
                })
                .sorted(
                        Comparator
                                .comparing((StorageLevelEmitterPart part) -> {
                                    String name = part.getName().getString().trim();
                                    return name.startsWith("ME Level Emitter");
                                })
                                .thenComparing(
                                        part -> part.getName().getString(),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                                .thenComparing(
                                        part -> ((StorageLevelEmitterUuid) part)
                                                .getPersistentUuid()
                                                .toString(),
                                        String.CASE_INSENSITIVE_ORDER
                                )
                )
                .map(part -> new EmitterTerminalMenu.StorageEmitterInfo(
                        ((StorageLevelEmitterUuid) part).getPersistentUuid().toString(),
                        part.getName(),
                        part.getConfig().getStack(0),
                        part.getReportingValue()
                ))
                .toList();
    }

    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters() {
        return getEmitters("");
    }

    private StorageLevelEmitterPart findEmitterByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        IGrid grid = getGrid();
        if (grid == null) return null;

        return grid.getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(emitter -> uuid.equals(((StorageLevelEmitterUuid) emitter)
                        .getPersistentUuid().toString()))
                .findFirst()
                .orElse(null);
    }

    public boolean setEmitterValue(String uuid, long value) {
        if (value < 0) return false;
        var emitter = findEmitterByUuid(uuid);
        if (emitter == null) return false;
        emitter.setReportingValue(value);
        return true;
    }

    public boolean setEmitterConfig(String uuid, GenericStack stack) {
        var emitter = findEmitterByUuid(uuid);
        if (emitter == null) return false;
        emitter.getConfig().setStack(0, stack);
        return true;
    }
}
