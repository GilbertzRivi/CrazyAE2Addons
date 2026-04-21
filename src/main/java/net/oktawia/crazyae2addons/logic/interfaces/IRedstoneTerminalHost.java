package net.oktawia.crazyae2addons.logic.interfaces;

import net.oktawia.crazyae2addons.menus.part.RedstoneTerminalMenu;

import java.util.List;

public interface IRedstoneTerminalHost {
    List<RedstoneTerminalMenu.EmitterInfo> getEmitters();
    List<RedstoneTerminalMenu.EmitterInfo> getEmitters(String filter);
    void toggle(String name);
}