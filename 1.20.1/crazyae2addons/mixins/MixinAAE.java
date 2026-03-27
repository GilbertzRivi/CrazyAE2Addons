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
import appeng.crafting.inv.ListCraftingInventory;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.interfaces.IAdvPatternProviderCpu;
import net.oktawia.crazyae2addons.interfaces.IIgnoreNBT;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderCpu;
import net.pedroksl.advanced_ae.common.cluster.AdvCraftingCPU;
import net.pedroksl.advanced_ae.common.logic.AdvCraftingCPULogic;
import net.pedroksl.advanced_ae.common.logic.ExecutingCraftingJob;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(AdvCraftingCPULogic.class)
public abstract class MixinAAE {

    @Unique
    private boolean ignoreNBT = false;

    @Shadow
    private ExecutingCraftingJob job;

    @Shadow @Final private AdvCraftingCPU cpu;

    @Inject(
            method = "trySubmitJob(Lappeng/api/networking/IGrid;Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/api/networking/security/IActionSource;Lappeng/api/networking/crafting/ICraftingRequester;)Lappeng/api/networking/crafting/ICraftingSubmitResult;",
            at = @At("RETURN"),
            remap = false
    )
    private void afterTrySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src, ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir){
        plan.patternTimes().forEach((pattern, ignored) -> {
            if (pattern.getPrimaryOutput().what().matches(plan.finalOutput())) {
                if (pattern.getDefinition().getTag() != null && pattern.getDefinition().getTag().contains("ignorenbt")){
                    this.ignoreNBT = pattern.getDefinition().getTag().getBoolean("ignorenbt");
                    ListCraftingInventory waiting = ((AAEExecutingCraftingJobAccessor) job).getWaitingFor();
                    LogUtils.getLogger().info(String.valueOf(waiting));
                    ((IIgnoreNBT) waiting).setIgnoreNBT(this.ignoreNBT);
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
        return (what.getId() == ((AAEExecutingCraftingJobAccessor) job).getFinalOutput().what().getId() && this.ignoreNBT || originalCheck);
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
            if (instance instanceof IAdvPatternProviderCpu provider) {
                provider.setCpuLogic(this.cpu);
            }
            return true;
        }
        return false;
    }
}
