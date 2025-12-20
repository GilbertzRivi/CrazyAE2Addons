package net.oktawia.crazyae2addons.mixins;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class Plugin implements IMixinConfigPlugin {

    private boolean hasTrySubmitJobBoolean = false;

    private static boolean isModLoaded(String modId) {
        try {
            if (ModList.get() == null) {
                return LoadingModList.get().getMods().stream()
                        .map(ModInfo::getModId)
                        .anyMatch(modId::equals);
            } else {
                return ModList.get().isLoaded(modId);
            }
        } catch (Throwable t) {
            return false;
        }
    }

    private static boolean detectTrySubmitJobBooleanByBytecode() {
        final String classRes = "appeng/crafting/execution/CraftingCpuLogic.class";

        try (InputStream in = Plugin.class.getClassLoader().getResourceAsStream(classRes)) {
            if (in == null) return false;

            ClassReader cr = new ClassReader(in);
            ClassNode cn = new ClassNode();
            cr.accept(cn, 0);

            List<MethodNode> methods = cn.methods;

            for (MethodNode m : methods) {
                if (!"trySubmitJob".equals(m.name)) continue;
                if (m.desc != null && m.desc.contains(";Z)")) {
                    return true;
                }
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.hasTrySubmitJobBoolean = detectTrySubmitJobBooleanByBytecode() || isModLoaded("ae2cl");
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return switch (mixinClassName) {
            case "net.oktawia.crazyae2addons.mixins.MixinGT" -> isModLoaded("gtceu");
            case "net.oktawia.crazyae2addons.mixins.MixinPatternProviderTargetCache" -> !isModLoaded("mae2") && !isModLoaded("gtceu");
            case "net.oktawia.crazyae2addons.mixins.MixinAAE",
                 "net.oktawia.crazyae2addons.mixins.MixinAAE2",
                 "net.oktawia.crazyae2addons.mixins.AAEExecutingCraftingJobAccessor",
                 "net.oktawia.crazyae2addons.mixins.AAECraftingServiceMixin" -> isModLoaded("advanced_ae");
            case "net.oktawia.crazyae2addons.mixins.MixinCraftingService" -> !isModLoaded("advanced_ae");
            case "net.oktawia.crazyae2addons.mixins.MixinPatternP2PTunnelLogic" -> isModLoaded("mae2") && isModLoaded("gtceu");
            case "net.oktawia.crazyae2addons.mixins.MixinCraftingCpuLogicAE2" -> !hasTrySubmitJobBoolean;
            case "net.oktawia.crazyae2addons.mixins.MixinCraftingCpuLogicAE2CL" -> hasTrySubmitJobBoolean;
            default -> true;
        };
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() { return null; }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}

    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}