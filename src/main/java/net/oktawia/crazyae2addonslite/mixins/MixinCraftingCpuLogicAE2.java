package net.oktawia.crazyae2addonslite.mixins;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.crafting.execution.CraftingCpuLogic;
import appeng.me.service.CraftingService;
import net.oktawia.crazyae2addonslite.interfaces.ICrazyProviderSourceFilter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Iterator;
import java.util.NoSuchElementException;

@Mixin(value = CraftingCpuLogic.class)
public abstract class MixinCraftingCpuLogicAE2 {

    @Unique private IActionSource crazyAE2Addons$src = null;

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
    private void beforeTrySubmitJob(IGrid grid, ICraftingPlan plan, IActionSource src, ICraftingRequester requester, CallbackInfoReturnable<ICraftingSubmitResult> cir) {
        this.crazyAE2Addons$src = src;
    }
}
