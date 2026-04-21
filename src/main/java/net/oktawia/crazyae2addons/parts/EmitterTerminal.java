package net.oktawia.crazyae2addons.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.items.parts.PartModels;
import net.oktawia.crazyae2addons.CrazyAddons;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.StorageLevelEmitterPart;
import appeng.parts.reporting.AbstractDisplayPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.StorageLevelEmitterUuid;
import net.oktawia.crazyae2addons.logic.interfaces.IEmitterTerminalHost;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class EmitterTerminal extends AbstractDisplayPart implements IUpgradeableObject, IEmitterTerminalHost {

    @PartModels
    public static final ResourceLocation MODEL_OFF = new ResourceLocation(CrazyAddons.MODID, "part/emitter_terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = new ResourceLocation(CrazyAddons.MODID, "part/emitter_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public EmitterTerminal(IPartItem<?> partItem) {
        super(partItem, false);
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!super.onPartActivate(player, hand, pos) && !this.isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.EMITTER_TERMINAL_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public void markDirty() {
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }

    @Override
    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters(String filter) {
        var grid = getMainNode().getGrid();
        if (grid == null) return List.of();

        String f = filter == null ? "" : filter.toLowerCase(Locale.ROOT).trim();

        return grid.getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(emitter -> matchesFilter(emitter, f))
                .sorted(
                        Comparator
                                .comparing((StorageLevelEmitterPart part) ->
                                        part.getName().getString().trim().startsWith("ME Level Emitter"))
                                .thenComparing(part -> part.getName().getString(), String.CASE_INSENSITIVE_ORDER)
                                .thenComparing(
                                        part -> ((StorageLevelEmitterUuid) part).getPersistentUuid().toString(),
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

    @Override
    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters() {
        return getEmitters("");
    }

    private boolean matchesFilter(StorageLevelEmitterPart emitter, String filter) {
        if (filter == null || filter.isBlank()) return true;

        String emitterName = emitter.getName() != null
                ? emitter.getName().getString().trim().toLowerCase(Locale.ROOT)
                : "";

        if (!emitterName.isEmpty() && emitterName.contains(filter)) return true;

        GenericStack config = emitter.getConfig().getStack(0);
        if (config == null || config.what() == null) return false;

        String configName = config.what().getDisplayName().getString().trim().toLowerCase(Locale.ROOT);
        return !configName.isEmpty() && configName.contains(filter);
    }

    private StorageLevelEmitterPart findEmitterByUuid(String uuid) {
        if (uuid == null || uuid.isBlank()) return null;

        var grid = getMainNode().getGrid();
        if (grid == null) return null;

        return grid.getActiveMachines(StorageLevelEmitterPart.class)
                .stream()
                .filter(emitter -> uuid.equals(((StorageLevelEmitterUuid) emitter).getPersistentUuid().toString()))
                .findFirst()
                .orElse(null);
    }

    @Override
    public boolean setEmitterValue(String uuid, long value) {
        if (value < 0) return false;

        var emitter = findEmitterByUuid(uuid);
        if (emitter == null) return false;

        emitter.setReportingValue(value);
        if (emitter.getHost() != null) {
            emitter.getHost().markForSave();
            emitter.getHost().markForUpdate();
        }
        return true;
    }

    @Override
    public boolean setEmitterConfig(String uuid, GenericStack stack) {
        var emitter = findEmitterByUuid(uuid);
        if (emitter == null) return false;

        emitter.getConfig().setStack(0, stack);
        if (emitter.getHost() != null) {
            emitter.getHost().markForSave();
            emitter.getHost().markForUpdate();
        }
        return true;
    }
}
