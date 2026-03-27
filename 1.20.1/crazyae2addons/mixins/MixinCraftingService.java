package net.oktawia.crazyae2addons.mixins;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.AEKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.google.common.collect.ImmutableSet;
import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

@Mixin(value = CraftingService.class, remap = false)
public abstract class MixinCraftingService {

    @Shadow private Set<CraftingCPUCluster> craftingCPUClusters;

    @Inject(method = "insertIntoCpus", at = @At("HEAD"), cancellable = true)
    private void prioritizedInsert(AEKey what, long amount, Actionable type,
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
            inserted += cpu.craftingLogic.insert(what, amount - inserted, type);
        }

        cir.setReturnValue(inserted);
    }

    @Redirect(
            method = "onServerEndTick",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<CraftingCPUCluster> sortedCpuIterator(Set<CraftingCPUCluster> self) {
        var byPrioDesc = Comparator
                .comparingInt((CraftingCPUCluster c) -> ((ICraftingClusterPrio) (Object) c).getPrio())
                .reversed()
                .thenComparingInt(System::identityHashCode);

        return self.stream().sorted(byPrioDesc).iterator();
    }

    @Inject(method = "getCpus", at = @At("HEAD"), cancellable = true)
    private void getCpusSortedByPriority(CallbackInfoReturnable<ImmutableSet<ICraftingCPU>> cir) {
        var byPrioDesc = Comparator
                .comparingInt((CraftingCPUCluster c) -> {
                    if ((Object) c instanceof ICraftingClusterPrio prio) {
                        return prio.getPrio();
                    }
                    return 0;
                })
                .reversed()
                .thenComparingInt(System::identityHashCode);

        var builder = ImmutableSet.<ICraftingCPU>builder();

        this.craftingCPUClusters.stream()
                .filter(cpu -> cpu.isActive() && !cpu.isDestroyed())
                .sorted(byPrioDesc)
                .forEach(builder::add);

        cir.setReturnValue(builder.build());
    }
}
