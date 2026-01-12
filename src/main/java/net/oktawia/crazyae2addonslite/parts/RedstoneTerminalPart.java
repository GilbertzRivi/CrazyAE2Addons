package net.oktawia.crazyae2addonslite.parts;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.reporting.AbstractDisplayPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.menus.RedstoneTerminalMenu;

import java.util.List;
import java.util.Objects;

public class RedstoneTerminalPart extends AbstractDisplayPart implements IUpgradeableObject {
    @PartModels
    public static final ResourceLocation MODEL_OFF = new ResourceLocation(AppEng.MOD_ID, "part/redstone_terminal_off");
    @PartModels
    public static final ResourceLocation MODEL_ON = new ResourceLocation(AppEng.MOD_ID, "part/redstone_terminal_on");

    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE, MODEL_OFF, MODEL_STATUS_OFF);
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_ON);
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE, MODEL_ON, MODEL_STATUS_HAS_CHANNEL);

    public RedstoneTerminalPart(IPartItem<?> partItem) {
        super(partItem, false);
    }

    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!super.onPartActivate(player, hand, pos) && !this.isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.REDSTONE_TERMINAL_MENU.get(), player, MenuLocators.forPart(this));
        }

        return true;
    }

    @Override
    public IPartModel getStaticModels() {
        return this.selectModel(MODELS_OFF, MODELS_ON, MODELS_HAS_CHANNEL);
    }

    public void toggle(String name) {
        var grid = getMainNode().getGrid();
        if (grid == null) return;
        var emitters = grid.getActiveMachines(RedstoneEmitterPart.class)
                .stream()
                .filter(part -> Objects.equals(part.name, name))
                .toList();
        if (emitters.isEmpty()) return;
        boolean newState = !emitters.get(0).getState();
        for (var emitter : emitters) {
            emitter.setState(newState);
        }
    }


    public List<RedstoneTerminalMenu.EmitterInfo> getEmitters(String filter) {
        var grid = getMainNode().getGrid();
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
