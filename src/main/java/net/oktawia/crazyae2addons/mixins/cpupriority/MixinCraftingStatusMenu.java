package net.oktawia.crazyae2addons.mixins.cpupriority;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.menu.me.crafting.CraftingStatusMenu;
import appeng.menu.me.crafting.CraftingStatusMenu.CraftingCpuList;
import com.google.common.collect.ImmutableSet;
import net.oktawia.crazyae2addons.logic.interfaces.ICpuPrio;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.HashMap;
import java.util.WeakHashMap;

@Mixin(value = CraftingStatusMenu.class, remap = false)
public abstract class MixinCraftingStatusMenu {

    @Shadow
    @Final
    private WeakHashMap<ICraftingCPU, Integer> cpuSerialMap;

    @Shadow
    private ImmutableSet<ICraftingCPU> lastCpuSet;

    @Inject(method = "createCpuList", at = @At("RETURN"))
    private void crazyae2addons$fillPrio(CallbackInfoReturnable<CraftingCpuList> cir) {
        var list = cir.getReturnValue();
        if (list == null || list.cpus().isEmpty() || this.lastCpuSet == null) {
            return;
        }

        var serialToCpu = new HashMap<Integer, ICraftingCPU>(cpuSerialMap.size());
        for (var cpu : this.lastCpuSet) {
            var serial = cpuSerialMap.get(cpu);
            if (serial != null) {
                serialToCpu.put(serial, cpu);
            }
        }

        for (var entry : list.cpus()) {
            var cpu = serialToCpu.get(entry.serial());
            int prio = 0;

            if (cpu instanceof ICpuPrio clusterPrio) {
                prio = clusterPrio.getPrio();
            }

            ((ICpuPrio) (Object) entry).setPrio(prio);
        }
    }
}