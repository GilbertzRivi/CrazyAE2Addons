package net.oktawia.crazyae2addons.compat.Jade;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

public final class PortalJadeContext {
    private static ResourceKey<Level> dim;
    private static HitResult hit;
    private static long validUntilGameTime;

    private PortalJadeContext() {}

    public static void set(ResourceKey<Level> d, HitResult h, long nowGameTime, long ttlTicks) {
        dim = d;
        hit = h;
        validUntilGameTime = nowGameTime + ttlTicks;
    }

    public static void clear() {
        dim = null;
        hit = null;
        validUntilGameTime = 0;
    }

    public static boolean isActive(long nowGameTime) {
        return dim != null && hit != null && nowGameTime <= validUntilGameTime;
    }

    public static ResourceKey<Level> dim() { return dim; }
    public static HitResult hit() { return hit; }
}
