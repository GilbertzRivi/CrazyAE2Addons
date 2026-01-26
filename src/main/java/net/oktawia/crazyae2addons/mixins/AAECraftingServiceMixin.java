package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.AEKey;
import appeng.api.config.Actionable;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.google.common.collect.ImmutableSet;
import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCluster;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;

@Mixin(value = CraftingService.class, priority = 1200, remap = false)
public abstract class AAECraftingServiceMixin {

    @Shadow @Final private Set<CraftingCPUCluster> craftingCPUClusters;
    @Shadow @Final private IGrid grid;

    /**
     * @author Oktawia
     * @reason Makes Crazy priorities work with Advanced AE,
     * required because Advanced AE already made it an overwrite
     */
    @Overwrite
    public long insertIntoCpus(AEKey what, long amount, Actionable type) {
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

        for (AdvCraftingBlockEntity be : this.grid.getMachines(AdvCraftingBlockEntity.class)) {
            AdvCraftingCPUCluster cluster = be.getCluster();
            if (cluster != null) {
                for (AdvCraftingCPU cpu : cluster.getActiveCPUs()) {
                    if (inserted >= amount) return inserted;
                    inserted += cpu.craftingLogic.insert(what, amount - inserted, type);
                }
            }
        }

        return inserted;
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
