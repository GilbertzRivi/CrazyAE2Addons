package net.oktawia.crazyae2addons.mixins.compat;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPriorityHelper;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCluster;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@Mixin(value = CraftingService.class, priority = 1200, remap = false)
public abstract class MixinAdvancedAECraftingServiceCompat {

    @Shadow
    @Final
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Shadow
    @Final
    private IGrid grid;

    /**
     * @author Oktawia
     * @reason Makes CPU priorities work with Advanced AE.
     */
    @Overwrite
    public long insertIntoCpus(AEKey what, long amount, Actionable type) {
        long inserted = 0L;

        var sortedVanilla = this.craftingCPUClusters.stream()
                .sorted(CpuPriorityHelper.clusterComparator())
                .toList();

        for (var cpu : sortedVanilla) {
            if (inserted >= amount) {
                break;
            }

            inserted += cpu.craftingLogic.insert(what, amount - inserted, type);
        }

        Set<AdvCraftingCPUCluster> seen = new HashSet<>();
        Comparator<AdvCraftingCPU> advCpuComparator = Comparator
                .comparingInt((AdvCraftingCPU cpu) -> CpuPriorityHelper.getCpuPriority(cpu))
                .reversed()
                .thenComparingInt(System::identityHashCode);

        for (AdvCraftingBlockEntity be : this.grid.getMachines(AdvCraftingBlockEntity.class)) {
            AdvCraftingCPUCluster cluster = be.getCluster();
            if (cluster == null || !seen.add(cluster)) {
                continue;
            }

            var sortedAdvanced = cluster.getActiveCPUs().stream()
                    .sorted(advCpuComparator)
                    .toList();

            for (AdvCraftingCPU cpu : sortedAdvanced) {
                if (inserted >= amount) {
                    break;
                }

                inserted += cpu.craftingLogic.insert(what, amount - inserted, type);
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
    private Iterator<CraftingCPUCluster> crazyae2addons$sortedIteratorOnTick(Set<CraftingCPUCluster> self) {
        return self.stream()
                .sorted(CpuPriorityHelper.clusterComparator())
                .iterator();
    }

    @Redirect(
            method = "getCpus",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Set;iterator()Ljava/util/Iterator;"
            )
    )
    private Iterator<CraftingCPUCluster> crazyae2addons$sortedIteratorGetCpus(Set<CraftingCPUCluster> self) {
        return self.stream()
                .sorted(CpuPriorityHelper.clusterComparator())
                .iterator();
    }
}