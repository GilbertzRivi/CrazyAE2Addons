package net.oktawia.crazyae2addons.mixins;

import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.service.CraftingService;
import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(CraftingService.class)
public class MixinCraftingServiceCL {
    @Shadow @Final
    @Mutable
    private static Comparator<CraftingCPUCluster> FAST_FIRST_COMPARATOR;

    @Shadow
    @Final @Mutable
    private static Comparator<CraftingCPUCluster> FAST_LAST_COMPARATOR;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void crazyae2addons$extendCpuComparators(CallbackInfo ci) {
        final var baseFastFirst = FAST_FIRST_COMPARATOR;
        final var baseFastLast = FAST_LAST_COMPARATOR;

        final Comparator<CraftingCPUCluster> prioDesc =
                Comparator.comparingInt(MixinCraftingServiceCL::prioOf).reversed();

        final Comparator<CraftingCPUCluster> prioAsc =
                Comparator.comparingInt(MixinCraftingServiceCL::prioOf);

        FAST_FIRST_COMPARATOR = prioDesc
                .thenComparing(baseFastFirst)
                .thenComparingInt(System::identityHashCode);

        FAST_LAST_COMPARATOR = prioAsc
                .thenComparing(baseFastLast)
                .thenComparingInt(System::identityHashCode);
    }

    private static int prioOf(CraftingCPUCluster c) {
        return ((Object) c instanceof ICraftingClusterPrio prio) ? prio.getPrio() : 0;
    }
}
