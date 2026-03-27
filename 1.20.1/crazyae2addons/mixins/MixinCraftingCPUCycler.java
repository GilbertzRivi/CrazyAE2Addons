package net.oktawia.crazyae2addons.mixins;

import appeng.api.networking.crafting.ICraftingCPU;
import net.oktawia.crazyae2addons.interfaces.ICraftingClusterPrio;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
import java.util.List;

@Mixin(targets = "appeng.menu.me.crafting.CraftingCPUCycler", remap = false)
public abstract class MixinCraftingCPUCycler {

    @Redirect(
            method = "detectAndSendChanges",
            at = @At(value = "INVOKE", target = "Ljava/util/Collections;sort(Ljava/util/List;)V")
    )
    private void sortCpuRecordsByPrio(List list) {
        Comparator<Object> byPrioDescThenOriginal = (a, b) -> {
            int pa = getPrio(a);
            int pb = getPrio(b);
            if (pa != pb) {
                return Integer.compare(pb, pa); // DESC
            }

            if (a instanceof Comparable) {
                try {
                    return ((Comparable) a).compareTo(b);
                } catch (ClassCastException ignored) {
                }
            }

            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        };

        list.sort(byPrioDescThenOriginal);
    }

    private static int getPrio(Object record) {
        ICraftingCPU cpu = ((CraftingCPURecordAccessor) (Object) record).getTheCpu();
        if ((Object) cpu instanceof ICraftingClusterPrio prio) {
            return prio.getPrio();
        }
        return 0;
    }
}
