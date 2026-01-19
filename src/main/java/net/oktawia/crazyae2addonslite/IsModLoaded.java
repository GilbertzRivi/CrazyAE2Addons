package net.oktawia.crazyae2addonslite;

import net.neoforged.fml.ModList;

public class IsModLoaded {
    public static boolean isAppFluxLoaded() { return ModList.get().isLoaded("appflux"); }
}
