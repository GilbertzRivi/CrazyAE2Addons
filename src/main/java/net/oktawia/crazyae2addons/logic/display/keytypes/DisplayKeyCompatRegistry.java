package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.IsModLoaded;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Set;

public class DisplayKeyCompatRegistry {

    private static final LinkedHashMap<String, IDisplayKeyResolver> RESOLVERS = new LinkedHashMap<>();
    private static boolean initialized = false;

    private static void ensureInit() {
        if (initialized) return;
        initialized = true;
        if (IsModLoaded.APP_FLUX) safeRegister(AppFluxResolver::new);
        if (IsModLoaded.ARS_ENERGISTIQUE) safeRegister(ArsEnergistiqueResolver::new);
        if (IsModLoaded.MEKANISM) safeRegister(MekanismChemicalResolver::new);
    }

    private interface ResolverFactory { IDisplayKeyResolver create(); }

    private static void safeRegister(ResolverFactory factory) {
        try {
            IDisplayKeyResolver r = factory.create();
            RESOLVERS.put(r.getTypePrefix(), r);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to initialize display key resolver", e);
        }
    }

    public static boolean hasPrefix(String prefix) { ensureInit(); return RESOLVERS.containsKey(prefix); }

    public static Set<String> getPrefixes() { ensureInit(); return RESOLVERS.keySet(); }

    @Nullable
    public static AEKey resolve(String prefix, String id) {
        ensureInit();
        var r = RESOLVERS.get(prefix);
        return r != null ? r.resolve(id) : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static ItemStack getIcon(String prefix, String id) {
        ensureInit();
        var r = RESOLVERS.get(prefix);
        return r != null ? r.getIcon(id) : null;
    }
}
