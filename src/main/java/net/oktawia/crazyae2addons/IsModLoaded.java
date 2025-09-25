package net.oktawia.crazyae2addons;

import net.neoforged.fml.loading.LoadingModList;

public class IsModLoaded {

    public static boolean isGTCEuLoaded() {
        return LoadingModList.get().getModFileById("gtceu") != null;
    }

    public static boolean isApothLoaded() {
        return LoadingModList.get().getModFileById("apotheosis") != null;
    }

    public static boolean isCCLoaded() {
        return LoadingModList.get().getModFileById("computercraft") != null;
    }

    public static boolean isAppFluxLoaded() {
        return LoadingModList.get().getModFileById("appflux") != null;
    }
}
