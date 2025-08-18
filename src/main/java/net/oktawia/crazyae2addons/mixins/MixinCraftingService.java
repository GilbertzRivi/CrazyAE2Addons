package net.oktawia.crazyae2addons.mixins;

import appeng.api.config.Actionable;
import appeng.api.stacks.AEKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Set;

@Mixin(value = CraftingService.class, remap = false)
public abstract class MixinCraftingService {

    @Shadow private Set<CraftingCPUCluster> craftingCPUClusters;

    @Inject(method = "insertIntoCpus", at = @At("HEAD"), cancellable = true)
    private void crazy$prioritizedInsert(AEKey what, long amount, Actionable type,
                                         CallbackInfoReturnable<Long> cir) {
        long inserted = 0L;

        var sorted = this.craftingCPUClusters.stream()
                .sorted(Comparator
                        .comparingInt((CraftingCPUCluster c) ->
                                ((ICraftingClusterPrio) (Object) c).getPrio())
                        .reversed()
                        .thenComparingInt(System::identityHashCode))
                .toList();

        for (var cpu : sorted) {
            if (inserted >= amount) break;
            inserted += cpu.craftingLogic.insert(what, amount - inserted, type);
        }

        cir.setReturnValue(inserted);
    }
}
