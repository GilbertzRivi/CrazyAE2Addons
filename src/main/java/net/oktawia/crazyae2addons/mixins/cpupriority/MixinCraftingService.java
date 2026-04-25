package net.oktawia.crazyae2addons.mixins.cpupriority;

import appeng.api.config.Actionable;
import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.stacks.AEKey;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import com.google.common.collect.ImmutableSet;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPriorityHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.Set;

@Mixin(value = CraftingService.class, remap = false)
public abstract class MixinCraftingService {

    @Final
    @Shadow
    private Set<CraftingCPUCluster> craftingCPUClusters;

    @Inject(method = "insertIntoCpus", at = @At("HEAD"), cancellable = true)
    private void crazyae2addons$prioritizedInsert(
            AEKey what,
            long amount,
            Actionable type,
            CallbackInfoReturnable<Long> cir
    ) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return;
        }

        if (amount <= 0) {
            cir.setReturnValue(0L);
            return;
        }

        long inserted = 0L;

        for (var cpu : this.craftingCPUClusters.stream().sorted(CpuPriorityHelper.clusterComparator()).toList()) {
            long remaining = amount - inserted;
            if (remaining <= 0) {
                break;
            }

            inserted += cpu.craftingLogic.insert(what, remaining, type);
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
    private Iterator<CraftingCPUCluster> crazyae2addons$sortedCpuIterator(Set<CraftingCPUCluster> self) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return self.iterator();
        }
        return self.stream().sorted(CpuPriorityHelper.clusterComparator()).iterator();
    }

    @Inject(method = "getCpus", at = @At("HEAD"), cancellable = true)
    private void crazyae2addons$getCpusSortedByPriority(CallbackInfoReturnable<ImmutableSet<ICraftingCPU>> cir) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return;
        }

        var builder = ImmutableSet.<ICraftingCPU>builder();

        this.craftingCPUClusters.stream()
                .filter(cpu -> cpu.isActive() && !cpu.isDestroyed())
                .sorted(CpuPriorityHelper.clusterComparator())
                .forEach(builder::add);

        cir.setReturnValue(builder.build());
    }
}