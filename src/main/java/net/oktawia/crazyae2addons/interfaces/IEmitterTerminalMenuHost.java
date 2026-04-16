package net.oktawia.crazyae2addons.interfaces;

import appeng.api.networking.IGridNode;
import appeng.api.stacks.GenericStack;
import net.oktawia.crazyae2addons.menus.part.EmitterTerminalMenu;

import java.util.List;

public interface IEmitterTerminalMenuHost {
    List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters();
    List<EmitterTerminalMenu.StorageEmitterInfo> getEmitters(String search);
    void setEmitterConfig(String uuid, GenericStack config);
    void setEmitterValue(String uuid, long value);
    IGridNode getActionableNode();
}
