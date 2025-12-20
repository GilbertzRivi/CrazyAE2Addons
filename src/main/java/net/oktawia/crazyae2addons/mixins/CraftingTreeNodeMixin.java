package net.oktawia.crazyae2addons.mixins;

import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSimulationRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.crafting.CraftingCalculation;
import appeng.crafting.CraftingTreeNode;
import appeng.me.service.CraftingService;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mixin(CraftingTreeNode.class)
public abstract class CraftingTreeNodeMixin {

    @Shadow
    private CraftingCalculation job;

    @Redirect(
            method = "buildChildPatterns",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingService;getCraftingFor(Lappeng/api/stacks/AEKey;)Ljava/util/Collection;"
            ),
            remap = false
    )
    private Collection<IPatternDetails> filterPatternsForRequester(
            ICraftingService craftingService, AEKey what
    ) {
        Collection<IPatternDetails> original = craftingService.getCraftingFor(what);
        if (original.isEmpty()) {
            return original;
        }

        ICraftingSimulationRequester simRequester =
                ((CraftingCalculationAccessor) this.job).getSimRequester();
        if (simRequester == null) {
            return original;
        }

        IActionSource actionSource = simRequester.getActionSource();
        if (actionSource == null) {
            return original;
        }

        var gridNode = simRequester.getGridNode();
        if (gridNode == null) {
            return original;
        }

        IGrid grid = gridNode.getGrid();
        if (grid == null) {
            return original;
        }

        boolean isPlayer = actionSource.player().isPresent();
        boolean isMachine = actionSource.machine().isPresent();

        List<IPatternDetails> filtered = new ArrayList<>();
        for (IPatternDetails details : original) {
            if (isPatternAllowedForRequester(details, craftingService, grid, isPlayer, isMachine)) {
                filtered.add(details);
            }
        }

        return filtered;
    }


    private boolean isPatternAllowedForRequester(IPatternDetails details,
                                                 ICraftingService craftingService,
                                                 IGrid grid,
                                                 boolean isPlayer,
                                                 boolean isMachine) {
        if (!(craftingService instanceof CraftingService ae2Service)) {
            return true;
        }

        Iterable<ICraftingProvider> iterable = ae2Service.getProviders(details);
        List<ICraftingProvider> providers = new ArrayList<>();
        iterable.forEach(providers::add);

        if (providers.isEmpty()) {
            return false;
        }

        for (IGridNode node : grid.getNodes()) {
            ICraftingProvider providerOnNode = node.getService(ICraftingProvider.class);
            if (providerOnNode == null) {
                continue;
            }
            if (!providers.contains(providerOnNode)) {
                continue;
            }

            if (node.getOwner() instanceof CrazyPatternProviderBE cpp) {
                boolean hasAutomationCard =
                        cpp.getUpgrades().isInstalled(CrazyItemRegistrar.AUTOMATION_UPGRADE_CARD.get());
                boolean hasPlayerCard =
                        cpp.getUpgrades().isInstalled(CrazyItemRegistrar.PLAYER_UPGRADE_CARD.get());

                if (hasAutomationCard && !hasPlayerCard) {
                    if (isMachine && !isPlayer) {
                        return true;
                    }
                } else if (hasPlayerCard && !hasAutomationCard) {
                    if (isPlayer) {
                        return true;
                    }
                } else {
                    return true;
                }
            } else {
                return true;
            }
        }

        return false;
    }
}
