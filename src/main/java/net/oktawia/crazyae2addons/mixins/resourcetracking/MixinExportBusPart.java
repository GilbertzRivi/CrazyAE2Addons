package net.oktawia.crazyae2addons.mixins.resourcetracking;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.GlobalPos;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.behaviors.StackTransferContext;
import appeng.api.config.Actionable;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.parts.automation.ExportBusPart;

import net.oktawia.crazyae2addons.tracking.IResourceTrackingService;
import net.oktawia.crazyae2addons.tracking.ResourceTrackingGate;
import net.oktawia.crazyae2addons.tracking.UsageTarget;

@Mixin(value = ExportBusPart.class, remap = false)
public class MixinExportBusPart {

    @Redirect(method = "doBusWork", at = @At(value = "INVOKE", target = "Lappeng/api/behaviors/StackExportStrategy;transfer(Lappeng/api/behaviors/StackTransferContext;Lappeng/api/stacks/AEKey;J)J"))
    private long crazyAe2$trackTransfer(StackExportStrategy strategy, StackTransferContext context, AEKey what,
            long maxAmount) {
        long transferred = strategy.transfer(context, what, maxAmount);
        crazyAe2$track(what, transferred);
        return transferred;
    }

    @Redirect(method = "insertCraftedItems", at = @At(value = "INVOKE", target = "Lappeng/api/behaviors/StackExportStrategy;push(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J"))
    private long crazyAe2$trackCraftedPush(StackExportStrategy strategy, AEKey what, long maxAmount, Actionable mode) {
        long pushed = strategy.push(what, maxAmount, mode);
        if (mode == Actionable.MODULATE) {
            crazyAe2$track(what, pushed);
        }
        return pushed;
    }

    @Unique
    private UsageTarget crazyAe2$cachedTarget;

    @Unique
    private String crazyAe2$cachedDesc;

    @Unique
    private AEKey crazyAe2$cachedIcon;

    @Unique
    private void crazyAe2$track(AEKey what, long amount) {
        if (amount <= 0 || !ResourceTrackingGate.isEnabled()) {
            return;
        }

        var self = (ExportBusPart) (Object) this;
        var grid = self.getMainNode().getGrid();
        if (grid == null) {
            return;
        }
        var svc = grid.getService(IResourceTrackingService.class);
        if (svc == null) {
            return;
        }

        if (crazyAe2$cachedTarget == null) {
            var pos = self.getBlockEntity().getBlockPos().immutable();
            crazyAe2$cachedTarget = UsageTarget.machine(GlobalPos.of(self.getLevel().dimension(), pos));
            crazyAe2$cachedDesc = "at " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
            crazyAe2$cachedIcon = AEItemKey.of(self.getPartItem().asItem());
        }

        svc.trackConsumption(what, amount, crazyAe2$cachedTarget, crazyAe2$cachedDesc, crazyAe2$cachedIcon);
    }
}
