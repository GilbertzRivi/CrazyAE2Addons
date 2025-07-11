package net.oktawia.crazyae2addons.mixins;

import appeng.api.config.Actionable;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.crafting.execution.*;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.oktawia.crazyae2addons.interfaces.IIgnoreNBT;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderCpu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = CraftingCpuLogic.class)
public abstract class MixinCraftingCpuLogic {

    @Shadow
    private ExecutingCraftingJob job;

    @Shadow @Final private CraftingCPUCluster cluster;

    @Unique private boolean ignoreNBT = false;


    @Inject(
            method = "trySubmitJob",
            at = @At("RETURN"),
            remap = false
    )
    private void trySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src, ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        plan.patternTimes().forEach((pattern, ignored) -> {
            if (pattern.getPrimaryOutput().what().matches(plan.finalOutput())) {
                if (pattern.getDefinition().getTag() != null && pattern.getDefinition().getTag().contains("ignorenbt")){
                    this.ignoreNBT = pattern.getDefinition().getTag().getBoolean("ignorenbt");
                    ((IIgnoreNBT) ((ExecutingCraftingJobAccessor) job).getWaitingFor()).setIgnoreNBT(ignoreNBT);
                }
            }
        });
    }

    @ModifyExpressionValue(
            method = "insert(Lappeng/api/stacks/AEKey;JLappeng/api/config/Actionable;)J",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/stacks/AEKey;matches(Lappeng/api/stacks/GenericStack;)Z"
            ),
            remap = false
    )
    private boolean modifyFinalOutputCheck(boolean originalCheck, AEKey what, long amount, Actionable type) {
        return (what.getId() == ((ExecutingCraftingJobAccessor) job).getFinalOutput().what().getId() && this.ignoreNBT || originalCheck);
    }

    @Redirect(
            method = "executeCrafting(ILappeng/me/service/CraftingService;Lappeng/api/networking/energy/IEnergyService;Lnet/minecraft/world/level/Level;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"
            ),
            remap = false
    )
    private boolean redirectPushPattern(
            ICraftingProvider instance, IPatternDetails iPatternDetails, KeyCounter[] keyCounters
    ) {
        if (instance instanceof IPatternProviderCpu provider){
            provider.setPatternDetails(iPatternDetails);
        }
        boolean result = instance.pushPattern(iPatternDetails, keyCounters);
        if (result) {
            if (instance instanceof IPatternProviderCpu provider) {
                provider.setCpuCluster(this.cluster);
            }
            return true;
        }
        return false;
    }
}