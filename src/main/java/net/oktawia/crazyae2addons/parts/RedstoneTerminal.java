package net.oktawia.crazyae2addons.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.logic.interfaces.IRedstoneTerminalHost;
import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class RedstoneTerminal extends AbstractDisplayPart implements IUpgradeableObject, IRedstoneTerminalHost {

    @PartModels
    public static final ResourceLocation MODEL_OFF = CrazyAddons.makeId("part/redstone_terminal_off");

    @PartModels
    public static final ResourceLocation MODEL_ON = CrazyAddons.makeId("part/redstone_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public RedstoneTerminal(IPartItem<?> partItem) {
        super(partItem, false);
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!super.onPartActivate(player, hand, pos) && !isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.REDSTONE_TERMINAL_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    @Override
    public void toggle(String name) {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return;
        }

        var emitters = grid.getActiveMachines(RedstoneEmitter.class)
                .stream()
                .filter(part -> Objects.equals(part.getNameId(), name))
                .toList();

        if (emitters.isEmpty()) {
            return;
        }

        boolean newState = !emitters.get(0).getState();
        for (RedstoneEmitter emitter : emitters) {
            emitter.setState(newState);
        }
    }

    @Override
    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters(String filter) {
        var grid = getMainNode().getGrid();
        if (grid == null) {
            return List.of();
        }

        String normalizedFilter = filter == null ? "" : filter.toLowerCase(Locale.ROOT).trim();

        return grid.getActiveMachines(RedstoneEmitter.class)
                .stream()
                .filter(emitter -> normalizedFilter.isBlank()
                        || emitter.getNameId().toLowerCase(Locale.ROOT).contains(normalizedFilter))
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
}