package net.oktawia.crazyae2addons.mixins;

import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.util.Ae2clOpenCraftingMenu;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public class Plugin implements IMixinConfigPlugin {

    private boolean hasTrySubmitJobBoolean;
    private boolean advancedAeLoaded;

    private static boolean isModLoaded(String modId) {
        try {
            var modList = ModList.get();
            if (modList != null) {
                return modList.isLoaded(modId);
            }

            return LoadingModList.get().getMods().stream()
                    .map(ModInfo::getModId)
                    .anyMatch(modId::equals);
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean detectTrySubmitJobBooleanByBytecode() {
        final String classRes = "appeng/crafting/execution/CraftingCpuLogic.class";

        try (InputStream in = Plugin.class.getClassLoader().getResourceAsStream(classRes)) {
            if (in == null) {
                return false;
            }

            ClassReader reader = new ClassReader(in);
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            for (MethodNode method : node.methods) {
                if (!"trySubmitJob".equals(method.name)) {
                    continue;
                }

                if (method.desc != null && method.desc.contains(";Z)")) {
                    return true;
                }
            }

            return false;
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public void onLoad(String mixinPackage) {
        this.advancedAeLoaded = isModLoaded("advanced_ae");
        this.hasTrySubmitJobBoolean = detectTrySubmitJobBooleanByBytecode() || isModLoaded("ae2cl");
        Ae2clOpenCraftingMenu.IS_AE2_CL = this.hasTrySubmitJobBoolean;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return switch (mixinClassName) {
            case "net.oktawia.crazyae2addons.mixins.cpupriority.MixinCraftingService" ->
                    !advancedAeLoaded;

            case "net.oktawia.crazyae2addons.mixins.compat.MixinAdvancedAECraftingServiceCompat" ->
                    advancedAeLoaded;

            case "net.oktawia.crazyae2addons.mixins.compat.MixinCraftingServiceCLCompat" ->
                    hasTrySubmitJobBoolean;

            default -> true;
        };
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }
}