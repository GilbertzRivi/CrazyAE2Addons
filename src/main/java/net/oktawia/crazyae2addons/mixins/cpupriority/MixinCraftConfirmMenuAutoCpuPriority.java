package net.oktawia.crazyae2addons.mixins.cpupriority;

import appeng.api.networking.crafting.ICraftingCPU;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.crafting.ICraftingService;
import appeng.api.networking.crafting.ICraftingSubmitResult;
import appeng.api.networking.security.IActionSource;
import appeng.menu.me.crafting.CraftConfirmMenu;
import net.oktawia.crazyae2addons.logic.cpupriority.CpuPriorityHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Comparator;

@Mixin(value = CraftConfirmMenu.class, remap = false)
public abstract class MixinCraftConfirmMenuAutoCpuPriority {

    @Redirect(
            method = "startJob",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/api/networking/crafting/ICraftingService;submitJob(Lappeng/api/networking/crafting/ICraftingPlan;Lappeng/api/networking/crafting/ICraftingRequester;Lappeng/api/networking/crafting/ICraftingCPU;ZLappeng/api/networking/security/IActionSource;)Lappeng/api/networking/crafting/ICraftingSubmitResult;"
            )
    )
    private ICraftingSubmitResult crazyae2addons$pickHighestPriorityCpuForAuto(
            ICraftingService craftingService,
            ICraftingPlan plan,
            @Nullable ICraftingRequester requester,
            @Nullable ICraftingCPU selectedCpu,
            boolean prioritizePower,
            IActionSource actionSource) {

        ICraftingCPU cpuToUse = selectedCpu;

        if (cpuToUse == null) {
            long requiredBytes = plan.bytes();

            cpuToUse = craftingService.getCpus().stream()
                    .filter(cpu -> !cpu.isBusy())
                    .filter(cpu -> cpu.getAvailableStorage() >= requiredBytes)
                    .sorted(
                            Comparator
                                    .comparingInt(CpuPriorityHelper::getCpuPriority)
                                    .reversed()
                                    .thenComparingInt(System::identityHashCode)
                    )
                    .findFirst()
                    .orElse(null);
        }

        return craftingService.submitJob(
                plan,
                requester,
                cpuToUse,
                prioritizePower,
                actionSource
        );
    }
}