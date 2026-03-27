package net.oktawia.crazyae2addons.misc;

public final class ClientWormholeState {
    private ClientWormholeState() {}
    private static volatile boolean active;

    public static boolean isActive() { return active; }
    public static void setActive(boolean v) { active = v; }
}
