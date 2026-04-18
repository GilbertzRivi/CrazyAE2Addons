package net.oktawia.crazyae2addons.parts;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.GenericStack;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.StorageLevelEmitterPart;
import appeng.parts.reporting.AbstractDisplayPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IEmitterTerminalMenuHost;
import net.oktawia.crazyae2addons.logic.interfaces.IStorageLevelEmitterUuid;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class EmitterTerminalPart extends AbstractDisplayPart implements IEmitterTerminalMenuHost {

    public static final ResourceLocation MODEL_OFF =
            CrazyAddons.makeId("part/emitter_terminal_off");
    public static final ResourceLocation MODEL_ON =
            CrazyAddons.makeId("part/emitter_terminal_on");

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(
            MODEL_BASE,
            MODEL_OFF,
            MODEL_STATUS_OFF
    );

    @PartModels
    public static final PartModel MODELS_ON = new PartModel(
            MODEL_BASE,
            MODEL_ON,
            MODEL_STATUS_ON
    );

    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(
            MODEL_BASE,
            MODEL_ON,
            MODEL_STATUS_HAS_CHANNEL
    );

    private static final Comparator<StorageLevelEmitterPart> EMITTER_COMPARATOR =
            Comparator
                    .comparing((StorageLevelEmitterPart part) -> {
                        String name = safeName(part);
                        return name.startsWith("ME Level Emitter");
                    })
                    .thenComparing(EmitterTerminalPart::safeName, String.CASE_INSENSITIVE_ORDER)
                    .thenComparing(
                            part -> ((IStorageLevelEmitterUuid) part).getPersistentUuid().toString(),
                            String.CASE_INSENSITIVE_ORDER
                    );

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

    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters() {
        return getEmitters("");
    }

    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters(String filter) {
        return getEmittersForNode(getActionableNode(), filter);
    }

    public void setEmitterValue(String uuid, long value) {
        setEmitterValueForNode(getActionableNode(), uuid, value);
    }

    public void setEmitterConfig(String uuid, @Nullable GenericStack stack) {
        setEmitterConfigForNode(getActionableNode(), uuid, stack);
    }

    @Override
    public IGridNode getActionableNode() {
        return getMainNode().getNode();
    }

    public static List<EmitterTerminalMenu.StorageEmitterInfo> getEmittersForHost(IEmitterTerminalMenuHost host) {
        return getEmittersForHost(host, "");
    }

    public static List<EmitterTerminalMenu.StorageEmitterInfo> getEmittersForHost(IEmitterTerminalMenuHost host, String filter) {
        return getEmittersForNode(host.getActionableNode(), filter);
    }

    public static void setEmitterConfigForHost(IEmitterTerminalMenuHost host, String uuid, @Nullable GenericStack stack) {
        setEmitterConfigForNode(host.getActionableNode(), uuid, stack);
    }

    public static void setEmitterValueForHost(IEmitterTerminalMenuHost host, String uuid, long value) {
        setEmitterValueForNode(host.getActionableNode(), uuid, value);
    }

    private static List<EmitterTerminalMenu.StorageEmitterInfo> getEmittersForNode(@Nullable IGridNode node, @Nullable String filter) {
        return emitterStream(node, filter)
                .sorted(EMITTER_COMPARATOR)
                .map(EmitterTerminalPart::toInfo)
                .toList();
    }

    private static void setEmitterConfigForNode(@Nullable IGridNode node, String uuid, @Nullable GenericStack stack) {
        StorageLevelEmitterPart emitter = findEmitter(node, uuid);
        if (emitter == null) {
            return;
        }

        emitter.getConfig().setStack(0, stack);
        markEmitterDirty(emitter);
    }

    private static void setEmitterValueForNode(@Nullable IGridNode node, String uuid, long value) {
        if (value < 0) {
            return;
        }

        StorageLevelEmitterPart emitter = findEmitter(node, uuid);
        if (emitter == null) {
            return;
        }

        emitter.setReportingValue(value);
        markEmitterDirty(emitter);
    }

    private static Stream<StorageLevelEmitterPart> emitterStream(@Nullable IGridNode node, @Nullable String filter) {
        IGrid grid = node == null ? null : node.getGrid();
        if (grid == null) {
            return Stream.empty();
        }

        String normalizedFilter = normalizeFilter(filter);

        return grid.getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(emitter -> matchesFilter(emitter, normalizedFilter));
    }

    @Nullable
    private static StorageLevelEmitterPart findEmitter(@Nullable IGridNode node, @Nullable String uuid) {
        if (uuid == null || uuid.isBlank()) {
            return null;
        }

        return emitterStream(node, "")
                .filter(e -> uuid.equals(((IStorageLevelEmitterUuid) e).getPersistentUuid().toString()))
                .findFirst()
                .orElse(null);
    }

    private static EmitterTerminalMenu.StorageEmitterInfo toInfo(StorageLevelEmitterPart part) {
        return new EmitterTerminalMenu.StorageEmitterInfo(
                ((IStorageLevelEmitterUuid) part).getPersistentUuid().toString(),
                part.getName(),
                GenericStack.writeTag(part.getLevel().registryAccess(), part.getConfig().getStack(0)).getAsString(),
                part.getReportingValue()
        );
    }

    private static void markEmitterDirty(StorageLevelEmitterPart emitter) {
        if (emitter.getHost() != null) {
            emitter.getHost().markForSave();
            emitter.getHost().markForUpdate();
        }
    }

    private static boolean matchesFilter(StorageLevelEmitterPart emitter, String filter) {
        if (filter.isEmpty()) {
            return true;
        }

        String name = safeName(emitter).trim();
        return !name.isEmpty() && name.toLowerCase().contains(filter);
    }

    private static String normalizeFilter(@Nullable String filter) {
        return filter == null ? "" : filter.toLowerCase().trim();
    }

    private static String safeName(StorageLevelEmitterPart part) {
        Component name = part.getName();
        return name.getString().trim();
    }
}