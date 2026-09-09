package net.oktawia.crazyae2addons.mixins.resourcetracking;

import com.gregtechceu.gtceu.integration.ae2.machine.MEInputBusPartMachine;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;

import appeng.api.config.Actionable;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.MEStorage;

import net.oktawia.crazyae2addons.tracking.IResourceTrackingService;
import net.oktawia.crazyae2addons.tracking.ResourceTrackingGate;
import net.oktawia.crazyae2addons.tracking.UsageTarget;

@Mixin(value = MEInputBusPartMachine.class, remap = false)
public class MixinMEInputBusPartMachine {

    @Redirect(method = "syncME", at = @At(value = "INVOKE", target = "Lappeng/api/storage/MEStorage;extract(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;Lappeng/api/networking/security/IActionSource;)J"))
    private long crazyAe2$trackExtract(MEStorage storage, AEKey what, long amount, Actionable mode,
            IActionSource source) {
        long extracted = storage.extract(what, amount, mode, source);
        if (extracted > 0 && ResourceTrackingGate.isEnabled()) {
            var self = (MEInputBusPartMachine) (Object) this;
            var grid = self.getMainNode().getGrid();
            if (grid != null) {
                var svc = grid.getService(IResourceTrackingService.class);
                if (svc != null) {
                    BlockPos pos = self.getPos();
                    AEKey icon = AEItemKey.of(self.getDefinition().asStack());
                    UsageTarget target = UsageTarget
                            .machine(GlobalPos.of(self.getLevel().dimension(), pos.immutable()));
                    svc.trackConsumption(what, extracted, target,
                            "at " + pos.getX() + " " + pos.getY() + " " + pos.getZ(), icon);
                }
            }
        }
        return extracted;
    }
}
