package net.oktawia.crazyae2addons.items.wireless;

import appeng.api.networking.IGridNode;
import appeng.api.stacks.GenericStack;
import appeng.menu.ISubMenu;
import appeng.menu.locator.ItemMenuHostLocator;
import de.mari_023.ae2wtlib.api.terminal.ItemWT;
import de.mari_023.ae2wtlib.api.terminal.WTMenuHost;
import net.minecraft.world.entity.player.Player;
import net.oktawia.crazyae2addons.logic.interfaces.IEmitterTerminalMenuHost;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;
import net.oktawia.crazyae2addons.parts.EmitterTerminalPart;

import java.util.List;
import java.util.function.BiConsumer;

public class EmitterTerminalMenuHost extends WTMenuHost implements IEmitterTerminalMenuHost {

    public EmitterTerminalMenuHost(ItemWT item, Player player, ItemMenuHostLocator locator,
                                   BiConsumer<Player, ISubMenu> returnToMainMenu) {
        super(item, player, locator, returnToMainMenu);
    }

    @Override
    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters() {
        return EmitterTerminalPart.getEmittersForHost(this);
    }

    @Override
    public List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters(String search) {
        return EmitterTerminalPart.getEmittersForHost(this, search);
    }

    @Override
    public void setEmitterConfig(String uuid, GenericStack config) {
        EmitterTerminalPart.setEmitterConfigForHost(this, uuid, config);
    }

    @Override
    public void setEmitterValue(String uuid, long value) {
        EmitterTerminalPart.setEmitterValueForHost(this, uuid, value);
    }

    @Override
    public IGridNode getActionableNode() {
        return super.getActionableNode();
    }
}
