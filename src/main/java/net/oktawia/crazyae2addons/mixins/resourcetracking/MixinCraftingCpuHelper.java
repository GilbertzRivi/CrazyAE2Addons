package net.oktawia.crazyae2addons.mixins.resourcetracking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.nbt.CompoundTag;

import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.crafting.execution.CraftingCpuHelper;
import appeng.crafting.inv.ListCraftingInventory;

import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;
import net.oktawia.crazyae2addons.tracking.IResourceTrackingService;
import net.oktawia.crazyae2addons.tracking.ResourceTrackingGate;
import net.oktawia.crazyae2addons.tracking.UsageTarget;

@Mixin(value = CraftingCpuHelper.class, remap = false)
public class MixinCraftingCpuHelper {

    @Inject(method = "tryExtractInitialItems", at = @At("RETURN"))
    private static void trackCraftingExtraction(
            ICraftingPlan plan,
            IGrid grid,
            ListCraftingInventory cpuInventory,
            IActionSource src,
            CallbackInfoReturnable<GenericStack> cir) {
        if (cir.getReturnValue() != null)
            return;

        if (!ResourceTrackingGate.isEnabled())
            return;

        var svc = grid.getService(IResourceTrackingService.class);
        if (svc == null)
            return;

        AEKey outputKey = plan.finalOutput().what();
        if (outputKey instanceof AEItemKey itemKey) {
            CompoundTag tag = itemKey.getTag();
            if (tag != null && tag.getBoolean(ManagedBuffer.DUMMY_MARKER)) {
                return;
            }
        }
        UsageTarget target = UsageTarget.crafting(outputKey);

        for (var entry : plan.usedItems()) {
            svc.trackConsumption(entry.getKey(), entry.getLongValue(), target, "crafting", outputKey);
        }
    }
}
