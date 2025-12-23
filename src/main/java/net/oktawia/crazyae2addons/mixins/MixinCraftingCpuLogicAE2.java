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
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.crafting.execution.ExecutingCraftingJob;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PlayerSource;
import appeng.me.service.CraftingService;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.interfaces.ICrazyProviderSourceFilter;
import net.oktawia.crazyae2addons.interfaces.IIgnoreNBT;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderCpu;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

@Mixin(value = CraftingCpuLogic.class)
public abstract class MixinCraftingCpuLogicAE2 {

    @Shadow private ExecutingCraftingJob job;
    @Shadow @Final
    CraftingCPUCluster cluster;

    @Unique private boolean crazyAE2Addons$ignoreNBT = false;
    @Unique private IActionSource crazyAE2Addons$src = null;

    @Unique
    private void crazyAE2Addons$captureSrc(IActionSource src) {
        this.crazyAE2Addons$src = src;
        this.crazyAE2Addons$ignoreNBT = false; // reset per job
    }

    @Unique
    private void crazyAE2Addons$afterSubmitJob(ICraftingPlan plan) {
        if (this.job == null) return;

        this.crazyAE2Addons$ignoreNBT = false;

        for (var entry : plan.patternTimes().entrySet()) {
            var pattern = entry.getKey();
            if (pattern == null) continue;
            if (pattern.getPrimaryOutput().what().matches(plan.finalOutput())) {
                var def = pattern.getDefinition();
                if (def != null) {
                    var tag = def.getTag();
                    if (tag != null && tag.contains("ignorenbt")) {
                        this.crazyAE2Addons$ignoreNBT = tag.getBoolean("ignorenbt");
                    }
                }
                break;
            }
        }

        ((IIgnoreNBT) ((ExecutingCraftingJobAccessor) job).getWaitingFor())
                .setIgnoreNBT(this.crazyAE2Addons$ignoreNBT);
    }

    @Redirect(
            method = "executeCrafting",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/me/service/CraftingService;getProviders(Lappeng/api/crafting/IPatternDetails;)Ljava/lang/Iterable;"
            ),
            remap = false
    )
    private Iterable<ICraftingProvider> redirectGetProviders(CraftingService instance, IPatternDetails key) {
        final IActionSource src = this.crazyAE2Addons$src;
        final Iterable<ICraftingProvider> original = instance.getProviders(key);

        return () -> new Iterator<>() {
            private final Iterator<ICraftingProvider> it = original.iterator();
            private ICraftingProvider next;

            private void advance() {
                while (next == null && it.hasNext()) {
                    ICraftingProvider p = it.next();

                    if (p instanceof ICrazyProviderSourceFilter filter) {
                        if (!filter.allowSource(src)) {
                            continue;
                        }
                    }

                    next = p;
                    return;
                }
            }

            @Override
            public boolean hasNext() {
                advance();
                return next != null;
            }

            @Override
            public ICraftingProvider next() {
                advance();
                if (next == null) throw new NoSuchElementException();
                ICraftingProvider r = next;
                next = null;
                return r;
            }
        };
    }

    @Inject(
            method = "trySubmitJob(Lappeng/api/networking/IGrid;Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/api/networking/security/IActionSource;Lappeng/api/networking/crafting/ICraftingRequester;)Lappeng/api/networking/crafting/ICraftingSubmitResult;",
            at = @At("HEAD"),
            remap = false
    )
    private void beforeTrySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src,
                                    ICraftingRequester requester,
                                    CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        crazyAE2Addons$captureSrc(src);
    }

    @Inject(
            method = "trySubmitJob(Lappeng/api/networking/IGrid;Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/api/networking/security/IActionSource;Lappeng/api/networking/crafting/ICraftingRequester;)Lappeng/api/networking/crafting/ICraftingSubmitResult;",
            at = @At("RETURN"),
            remap = false
    )
    private void afterTrySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src,
                                   ICraftingRequester requester,
                                   CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        crazyAE2Addons$afterSubmitJob(plan);
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
        return (what.getId() == ((ExecutingCraftingJobAccessor) job).getFinalOutput().what().getId() && this.crazyAE2Addons$ignoreNBT)
                || originalCheck;
    }

    @Redirect(
            method = "executeCrafting(ILappeng/me/service/CraftingService;Lappeng/api/networking/energy/IEnergyService;Lnet/minecraft/world/level/Level;)I",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingProvider;pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z"
            ),
            remap = false
    )
    private boolean redirectPushPattern(ICraftingProvider instance, IPatternDetails details, KeyCounter[] inputs) {
        if (instance instanceof IPatternProviderCpu provider) {
            provider.setPatternDetails(details);
        }

        boolean result = instance.pushPattern(details, inputs);

        if (result) {
            if (instance instanceof IPatternProviderCpu provider) {
                provider.setCpuCluster(this.cluster);
            }
            return true;
        }

        return false;
    }
}
