package net.oktawia.crazyae2addons;

import net.minecraftforge.fml.ModList;

public class IsModLoaded {
    public static boolean isGTCEuLoaded() {
        return ModList.get().isLoaded("gtceu");
    }
    public static boolean isApothLoaded() {
        return ModList.get().isLoaded("apotheosis");
    }
    public static boolean isAppFluxLoaded() { return ModList.get().isLoaded("appflux"); }
    public static boolean isIPLoaded() { return ModList.get().isLoaded("imm_ptl_core"); }
    public static boolean isMekanismLoaded() { return ModList.get().isLoaded("mekanism"); }
    public static boolean isAppliedMekanisticsLoaded() { return ModList.get().isLoaded("appmek"); }
    public static boolean isArsEnergistiqueLoaded() { return ModList.get().isLoaded("arseng"); }
    public static boolean isAppliedBotanicsLoaded() { return ModList.get().isLoaded("appbot"); }
}

