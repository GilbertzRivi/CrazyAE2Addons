package net.oktawia.crazyae2addons.logic.display.keytypes;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.IsModLoaded;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Set;

public final class DisplayKeyCompatRegistry {

    private static final LinkedHashMap<String, IDisplayKeyResolver> RESOLVERS = new LinkedHashMap<>();
    private static boolean initialized = false;

    private DisplayKeyCompatRegistry() {
    }

    private static void ensureInit() {
        if (initialized) {
            return;
        }

        initialized = true;

        if (IsModLoaded.APP_FLUX) {
            register(new AppFluxResolver());
        }

        if (IsModLoaded.APP_BOT) {
            register(new AppliedBotanicsResolver());
        }

        if (IsModLoaded.ARS_ENG) {
            register(new ArsEnergistiqueResolver());
        }

        if (IsModLoaded.APP_MEK) {
            register(new MekanismGasResolver());
            register(new MekanismInfusionResolver());
            register(new MekanismPigmentResolver());
            register(new MekanismSlurryResolver());
        }
    }

    private static void register(IDisplayKeyResolver resolver) {
        RESOLVERS.put(resolver.getTypePrefix(), resolver);
    }

    public static boolean hasPrefix(String prefix) {
        ensureInit();
        return RESOLVERS.containsKey(prefix);
    }

    public static Set<String> getPrefixes() {
        ensureInit();
        return RESOLVERS.keySet();
    }

    @Nullable
    public static AEKey resolve(String prefix, String id) {
        ensureInit();
        IDisplayKeyResolver resolver = RESOLVERS.get(prefix);
        return resolver != null ? resolver.resolve(id) : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static ItemStack getIcon(String prefix, String id) {
        ensureInit();
        IDisplayKeyResolver resolver = RESOLVERS.get(prefix);
        return resolver != null ? resolver.getIcon(id) : null;
    }
}