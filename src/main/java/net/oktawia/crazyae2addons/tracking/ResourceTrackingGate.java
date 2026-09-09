package net.oktawia.crazyae2addons.tracking;

import net.oktawia.crazyae2addons.CrazyConfig;

public final class ResourceTrackingGate {

    private ResourceTrackingGate() {
    }

    public static boolean isEnabled() {
        return CrazyConfig.COMMON_SPEC.isLoaded() && CrazyConfig.COMMON.RESOURCE_TRACKING_TERMINAL_ENABLED.get();
    }
}
