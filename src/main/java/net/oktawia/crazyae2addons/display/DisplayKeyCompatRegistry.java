package net.oktawia.crazyae2addons.display;

import appeng.api.stacks.AEKey;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.display.compat.*;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Set;

public class DisplayKeyCompatRegistry {

    private static final LinkedHashMap<String, IDisplayKeyResolver> RESOLVERS = new LinkedHashMap<>();
    private static boolean initialized = false;

    private static void ensureInit() {
        if (initialized) return;
        initialized = true;
        tryRegister(new MobKeyResolver());
        if (IsModLoaded.isAppFluxLoaded())            tryRegister(new AppFluxResolver());
        if (IsModLoaded.isArsEnergistiqueLoaded())    tryRegister(new ArsEnergistiqueResolver());
        if (IsModLoaded.isAppliedBotanicsLoaded())    tryRegister(new AppliedBotanicsResolver());
        if (IsModLoaded.isMekanismLoaded() && IsModLoaded.isAppliedMekanisticsLoaded()) {
            tryRegister(new MekanismGasResolver());
            tryRegister(new MekanismInfusionResolver());
            tryRegister(new MekanismPigmentResolver());
            tryRegister(new MekanismSlurryResolver());
        }
    }

    private static void tryRegister(IDisplayKeyResolver resolver) {
        try {
            RESOLVERS.put(resolver.getTypePrefix(), resolver);
        } catch (Throwable ignored) {}
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
        var resolver = RESOLVERS.get(prefix);
        return resolver != null ? resolver.resolve(id) : null;
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public static ItemStack getIcon(String prefix, String id) {
        ensureInit();
        var resolver = RESOLVERS.get(prefix);
        return resolver != null ? resolver.getIcon(id) : null;
    }
}
