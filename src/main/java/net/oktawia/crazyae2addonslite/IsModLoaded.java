package net.oktawia.crazyae2addonslite;

import net.minecraftforge.fml.ModList;

public class IsModLoaded {
    public static boolean isGTCEuLoaded() {
        return ModList.get().isLoaded("gtceu");
    }
    public static boolean isLDLibLoaded() {
        return ModList.get().isLoaded("ldlib");
    }
    public static boolean isAppFluxLoaded() { return ModList.get().isLoaded("appflux"); }
    public static boolean isIPLoaded() { return ModList.get().isLoaded("imm_ptl_core");  }
}

