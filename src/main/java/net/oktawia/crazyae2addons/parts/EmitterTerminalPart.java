package net.oktawia.crazyae2addons.parts;

import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.GenericStack;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.StorageLevelEmitterPart;
import appeng.parts.reporting.AbstractDisplayPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.interfaces.IEmitterTerminalMenuHost;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.StorageLevelEmitterUuid;
import net.oktawia.crazyae2addons.menus.EmitterTerminalMenu;

import java.util.Comparator;
import java.util.List;

public class EmitterTerminalPart extends AbstractDisplayPart implements IEmitterTerminalMenuHost {
    @PartModels
    public static final ResourceLocation MODEL_OFF = AppEng.makeId("part/emitter_terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = AppEng.makeId("part/emitter_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public EmitterTerminalPart(IPartItem<?> partItem) {
        super(partItem, false);
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!super.onUseWithoutItem(player, pos) && !this.isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.EMITTER_TERMINAL_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters(String filter) {
        var grid = getMainNode().getGrid();
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
                .sorted(Comparator
                        .comparing((StorageLevelEmitterPart part) -> {
                            String name = part.getName().getString().trim();
                            return name.startsWith("ME Level Emitter");
                        })
                        .thenComparing(part -> part.getName().getString(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(part -> ((StorageLevelEmitterUuid) part).getPersistentUuid().toString(), String.CASE_INSENSITIVE_ORDER)
                )
                .map(part -> new EmitterTerminalMenu.StorageEmitterInfo(
                        ((StorageLevelEmitterUuid) part).getPersistentUuid().toString(),
                        part.getName(),
                        GenericStack.writeTag(getLevel().registryAccess(), part.getConfig().getStack(0)).getAsString(),
                        part.getReportingValue()
                ))
                .toList();
    }

    public boolean isGridAvailable() {
        return getMainNode() != null && getMainNode().getGrid() != null;
    }

    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters() {
        return getEmitters("");
    }

    private StorageLevelEmitterPart findEmitterByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;
        var grid = getMainNode().getGrid();
        if (grid == null) return null;
        return grid.getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(e -> uuid.equals(((StorageLevelEmitterUuid) e).getPersistentUuid().toString()))
                .findFirst()
                .orElse(null);
    }

    public void setEmitterValue(String uuid, long value) {
        if (value < 0) return;
        var emitter = findEmitterByUuid(uuid);
        if (emitter == null) return;
        emitter.setReportingValue(value);
        if (emitter.getHost() != null) {
            emitter.getHost().markForSave();
            emitter.getHost().markForUpdate();
        }
    }

    public void setEmitterConfig(String uuid, GenericStack stack) {
        var emitter = findEmitterByUuid(uuid);
        if (emitter == null) return;
        emitter.getConfig().setStack(0, stack);
        if (emitter.getHost() != null) {
            emitter.getHost().markForSave();
            emitter.getHost().markForUpdate();
        }
    }

    public static List<EmitterTerminalMenu.StorageEmitterInfo> getEmittersForHost(IEmitterTerminalMenuHost host, String filter) {
        IGridNode node = host.getActionableNode();
        if (node == null || node.getGrid() == null) return List.of();
        String f = filter == null ? "" : filter.toLowerCase().trim();
        return node.getGrid().getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(emitter -> {
                    String name = emitter.getName().getString().trim();
                    if (f.isEmpty()) return true;
                    if (name.isEmpty()) return false;
                    return name.toLowerCase().contains(f);
                })
                .sorted(Comparator
                        .comparing((StorageLevelEmitterPart part) -> {
                            String name = part.getName().getString().trim();
                            return name.startsWith("ME Level Emitter");
                        })
                        .thenComparing(part -> part.getName().getString(), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(part -> ((StorageLevelEmitterUuid) part).getPersistentUuid().toString(), String.CASE_INSENSITIVE_ORDER)
                )
                .map(part -> new EmitterTerminalMenu.StorageEmitterInfo(
                        ((StorageLevelEmitterUuid) part).getPersistentUuid().toString(),
                        part.getName(),
                        GenericStack.writeTag(part.getLevel().registryAccess(), part.getConfig().getStack(0)).getAsString(),
                        part.getReportingValue()
                ))
                .toList();
    }

    public static List<EmitterTerminalMenu.StorageEmitterInfo> getEmittersForHost(IEmitterTerminalMenuHost host) {
        return getEmittersForHost(host, "");
    }

    public static boolean setEmitterConfigForHost(IEmitterTerminalMenuHost host, String uuid, GenericStack stack) {
        IGridNode node = host.getActionableNode();
        if (node == null || node.getGrid() == null) return false;
        StorageLevelEmitterPart emitter = node.getGrid().getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(e -> uuid.equals(((StorageLevelEmitterUuid) e).getPersistentUuid().toString()))
                .findFirst()
                .orElse(null);
        if (emitter == null) return false;
        emitter.getConfig().setStack(0, stack);
        if (emitter.getHost() != null) {
            emitter.getHost().markForSave();
            emitter.getHost().markForUpdate();
        }
        return true;
    }

    public static boolean setEmitterValueForHost(IEmitterTerminalMenuHost host, String uuid, long value) {
        if (value < 0) return false;
        IGridNode node = host.getActionableNode();
        if (node == null || node.getGrid() == null) return false;
        StorageLevelEmitterPart emitter = node.getGrid().getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(e -> uuid.equals(((StorageLevelEmitterUuid) e).getPersistentUuid().toString()))
                .findFirst()
                .orElse(null);
        if (emitter == null) return false;
        emitter.setReportingValue(value);
        if (emitter.getHost() != null) {
            emitter.getHost().markForSave();
            emitter.getHost().markForUpdate();
        }
        return true;
    }

    @Override
    public IGridNode getActionableNode() {
        return getMainNode().getNode();
    }
}
