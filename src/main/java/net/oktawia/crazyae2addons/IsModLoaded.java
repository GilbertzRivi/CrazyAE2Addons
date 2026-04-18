package net.oktawia.crazyae2addons;

import net.neoforged.fml.ModList;

public class IsModLoaded {
    public static final boolean APP_FLUX          = ModList.get().isLoaded("appflux");
    public static final boolean ARS_ENERGISTIQUE  = ModList.get().isLoaded("arseng");
    public static final boolean MEKANISM          = ModList.get().isLoaded("mekanism");
    public static final boolean APOTH_ENCHANTING  = ModList.get().isLoaded("apothic_enchanting");
    public static final boolean APOTH_SPAWNERS    = ModList.get().isLoaded("apothic_spawners");
}
