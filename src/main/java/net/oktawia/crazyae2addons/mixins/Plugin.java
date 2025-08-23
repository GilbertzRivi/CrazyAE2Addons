package net.oktawia.crazyae2addons.mixins;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class Plugin implements IMixinConfigPlugin {

    private static boolean isModLoaded(String modId) {
        if (ModList.get() == null) {
            return LoadingModList.get().getMods().stream()
                    .map(ModInfo::getModId)
                    .anyMatch(modId::equals);
        } else {
            return ModList.get().isLoaded(modId);
        }
    }


    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return switch (mixinClassName) {
            case "net.oktawia.crazyae2addons.mixins.MixinGT" -> isModLoaded("gtceu");
            case "net.oktawia.crazyae2addons.mixins.MixinMAE2" -> isModLoaded("mae2");
            case "net.oktawia.crazyae2addons.mixins.MixinMAE22" -> isModLoaded("mae2") && !isModLoaded("gtceu");
            case "net.oktawia.crazyae2addons.mixins.MixinMAE23" -> isModLoaded("mae2") && isModLoaded("gtceu");
            case "net.oktawia.crazyae2addons.mixins.MixinPatternProviderTargetCache" -> !isModLoaded("mae2") && !isModLoaded("gtceu");
            case "net.oktawia.crazyae2addons.mixins.MixinAAE",
                 "net.oktawia.crazyae2addons.mixins.MixinAAE2",
                 "net.oktawia.crazyae2addons.mixins.AAEExecutingCraftingJobAccessor",
                 "net.oktawia.crazyae2addons.mixins.AAECraftingServiceMixin" -> isModLoaded("advanced_ae");
            case "net.oktawia.crazyae2addons.mixins.MixinCraftingService" -> !isModLoaded("advanced_ae");
            default -> true;
        };
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}