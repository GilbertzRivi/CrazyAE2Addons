package net.oktawia.crazyae2addonslite;

import net.minecraftforge.fml.ModList;

public class IsModLoaded {
    public static boolean isGTCEuLoaded() {
        return ModList.get().isLoaded("gtceu");
    }
    public static boolean isApothLoaded() {
        return ModList.get().isLoaded("apotheosis");
    }
    public static boolean isAppFluxLoaded() { return ModList.get().isLoaded("appflux"); }
}

