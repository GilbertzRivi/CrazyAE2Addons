package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingStatusMenu;
import appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuList;
import com.google.common.collect.ImmutableSet;
import net.oktawia.crazyae2addons.interfaces.ICpuPrio;
import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

@Mixin(value = CraftingStatusMenu.class, remap = false)
public abstract class MixinCraftingStatusMenu {

    @Shadow @Final
    private WeakHashMap<ICraftingCPU, Integer> cpuSerialMap;

    @Shadow
    private ImmutableSet<ICraftingCPU> lastCpuSet;

    @Inject(method = "createCpuList", at = @At("RETURN"))
    private void crazyae2addons$fillPrio(CallbackInfoReturnable<CraftingCpuList> cir) {
        var list = cir.getReturnValue();
        if (list == null || list.cpus().isEmpty()) return;

        var serialToCpu = new HashMap<Integer, ICraftingCPU>(cpuSerialMap.size());
        for (var cpu : lastCpuSet) {
            var s = cpuSerialMap.get(cpu);
            if (s != null) serialToCpu.put(s, cpu);
        }

        for (var e : list.cpus()) {
            var cpu = serialToCpu.get(e.serial());
            int prio = 0;
            if (cpu instanceof ICraftingClusterPrio p) prio = p.getPrio();
            ((ICpuPrio) (Object) e).setPrio(prio);
        }
    }
}
