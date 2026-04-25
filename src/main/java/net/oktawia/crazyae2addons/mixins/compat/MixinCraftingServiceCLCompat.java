package net.oktawia.crazyae2addons.mixins.compat;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPriorityHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(value = CraftingService.class, remap = false)
public abstract class MixinCraftingServiceCLCompat {

    @Shadow
    @Final
    @Mutable
    private static Comparator<CraftingCPUCluster> FAST_FIRST_COMPARATOR;

    @Shadow
    @Final
    @Mutable
    private static Comparator<CraftingCPUCluster> FAST_LAST_COMPARATOR;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void crazyae2addons$extendCpuComparators(CallbackInfo ci) {
        if (!CrazyConfig.COMMON.CPU_PRIORITIES_ENABLED.get()) {
            return;
        }

        final Comparator<CraftingCPUCluster> baseFastFirst = FAST_FIRST_COMPARATOR;
        final Comparator<CraftingCPUCluster> baseFastLast = FAST_LAST_COMPARATOR;

        FAST_FIRST_COMPARATOR = Comparator
                .comparingInt(CpuPriorityHelper::getClusterPriority)
                .reversed()
                .thenComparing(baseFastFirst)
                .thenComparingInt(System::identityHashCode);

        FAST_LAST_COMPARATOR = Comparator
                .comparingInt(CpuPriorityHelper::getClusterPriority)
                .thenComparing(baseFastLast)
                .thenComparingInt(System::identityHashCode);
    }
}