package net.oktawia.crazyae2addonslite.mixins;

import appeng.api.networking.IGrid;
import appeng.api.stacks.AEKey;
import appeng.api.config.Actionable;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import net.oktawia.crazyae2addonslite.interfaces.ICraftingClusterPrio;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPUCluster;
import net.pedroksl.advanced_ae.common.entities.AdvCraftingBlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Comparator;
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
}
