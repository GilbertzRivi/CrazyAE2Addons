package net.oktawia.crazyae2addons.mixins;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PlayerSource;
import appeng.util.inv.AppEngInternalInventory;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.interfaces.*;
import net.oktawia.crazyae2addons.misc.PatternDetailsSerializer;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = PatternProviderLogic.class, priority = 1100)
public class MixinPatternProviderLogic implements IPatternProviderCpu, ICrazyProviderSourceFilter, IProviderLogicResizable {

    @Shadow @Final private PatternProviderLogicHost host;
    @Unique @Nullable private CraftingCPUCluster ca_cpuCluster;

    @Shadow @Mutable @Final private AppEngInternalInventory patternInventory;

    public void setSize(int size) {
        var tag = new CompoundTag();
        patternInventory.writeToNBT(tag, "dainv");
        this.patternInventory = new AppEngInternalInventory(patternInventory.getHost(), size);
        this.patternInventory.readFromNBT(tag, "dainv");
    }

    @Unique
    @Override
    public void setCpuCluster(CraftingCPUCluster cpu) {
        this.ca_cpuCluster = cpu;
    }

    @Unique
    @Override
    public CraftingCPUCluster getCpuCluster() {
        return this.ca_cpuCluster;
    }

    @Inject(
            method = "writeToNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void afterWriteToNBT(CompoundTag tag, CallbackInfo ci) {
        if ((Object) this instanceof IAdvPatternProviderCpu advCpu) {
            advCpu.advSaveNbt(tag);
        }
    }

    @Inject(
            method = "readFromNBT(Lnet/minecraft/nbt/CompoundTag;)V",
            at = @At("TAIL"),
            remap = false
    )
    private void afterReadFromNBT(CompoundTag tag, CallbackInfo ci) {
        if ((Object) this instanceof IAdvPatternProviderCpu advCpu) {
            advCpu.advReadNbt(tag);
        }
    }

    @ModifyExpressionValue(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
            at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"),
            remap = false
    )
    private boolean onPatternsContains(boolean originalResult, IPatternDetails pd) {
        return pd.getClass() == PatternDetailsSerializer.PatternDetails.class || originalResult;
    }

    @ModifyExpressionValue(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/helpers/patternprovider/PatternProviderLogic;getCraftingLockedReason()Lappeng/api/config/LockCraftingMode;"
            ),
            remap = false
    )
    private LockCraftingMode ca_onLockReason(LockCraftingMode original, IPatternDetails patternDetails) {
        if (patternDetails instanceof PatternDetailsSerializer.PatternDetails) {
            return LockCraftingMode.NONE;
        }
        return original;
    }

    @Unique
    @Override
    public boolean allowSource(@Nullable IActionSource src) {
        if (src == null) return true;
        var be = host != null ? host.getBlockEntity() : null;
        if (!(be instanceof CrazyPatternProviderBE cpp)) return true;
        var upgrades = cpp.getUpgrades();
        if (upgrades == null) return true;
        if (upgrades.isInstalled(CrazyItemRegistrar.AUTOMATION_UPGRADE_CARD.get())) {
            return src instanceof MachineSource;
        }
        if (upgrades.isInstalled(CrazyItemRegistrar.PLAYER_UPGRADE_CARD.get())) {
            return src instanceof PlayerSource;
        }
        return true;
    }

}
