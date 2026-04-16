package net.oktawia.crazyae2addons;

import net.neoforged.fml.ModList;

public class IsModLoaded {
    public static boolean isAppFluxLoaded() { return ModList.get().isLoaded("appflux"); }
    public static boolean isArsEnergistiqueLoaded() { return ModList.get().isLoaded("arseng"); }
    public static boolean isMekanismLoaded() { return ModList.get().isLoaded("mekanism"); }
    public static boolean isApothEnchLoaded() {
        return ModList.get().isLoaded("apothic_enchanting");
    }
    public static boolean isApothSpawnersLoaded() {
        return ModList.get().isLoaded("apothic_spawners");
    }
}
