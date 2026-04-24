package net.oktawia.crazyae2addons.mixins.cpupriority;

import appeng.api.networking.crafting.ICraftingCPU;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPriorityHelper;
import net.oktawia.crazyae2addons.mixins.accessors.CraftingCPURecordAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;
import java.util.List;

@Mixin(targets = "appeng.menu.me.crafting.CraftingCPUCycler", remap = false)
public abstract class MixinCraftingCPUCycler {

    @Redirect(
            method = "detectAndSendChanges",
            at = @At(
                    value = "INVOKE",
                    target = "Ljava/util/Collections;sort(Ljava/util/List;)V"
            )
    )
    private void crazyae2addons$sortCpuRecordsByPrio(List<?> list) {
        Comparator<Object> comparator = (a, b) -> {
            int pa = getPriorityFromRecord(a);
            int pb = getPriorityFromRecord(b);

            if (pa != pb) {
                return Integer.compare(pb, pa);
            }

            if (a instanceof Comparable<?> comparable) {
                try {
                    Comparable<Object> raw = (Comparable<Object>) comparable;
                    return raw.compareTo(b);
                } catch (ClassCastException ignored) {
                }
            }

            return Integer.compare(System.identityHashCode(a), System.identityHashCode(b));
        };

        List<Object> raw = (List<Object>) list;
        raw.sort(comparator);
    }

    private static int getPriorityFromRecord(Object record) {
        ICraftingCPU cpu = ((CraftingCPURecordAccessor) record).getTheCpu();
        return CpuPriorityHelper.getCpuPriority(cpu);
    }
}