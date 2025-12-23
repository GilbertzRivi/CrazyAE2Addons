package net.oktawia.crazyae2addons.mixins;

import appeng.api.config.LockCraftingMode;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.GenericStack;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.helpers.patternprovider.UnlockCraftingEvent;
import appeng.me.cluster.implementations.CraftingCPUCluster;
import appeng.me.helpers.MachineSource;
import appeng.me.helpers.PlayerSource;
import appeng.util.ConfigManager;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.logging.LogUtils;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.interfaces.IAdvPatternProviderCpu;
import net.oktawia.crazyae2addons.interfaces.ICrazyProviderSourceFilter;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderCpu;
import net.oktawia.crazyae2addons.interfaces.IPatternProviderTargetCacheExt;
import net.oktawia.crazyae2addons.logic.ImpulsedPatternProviderLogic;
import net.oktawia.crazyae2addons.misc.PatternDetailsSerializer;
import org.jetbrains.annotations.Nullable;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Field;

@Mixin(value = PatternProviderLogic.class, priority = 1100)
public abstract class MixinPatternProviderLogic implements IPatternProviderCpu, ICrazyProviderSourceFilter {

    @Shadow @Final private PatternProviderLogicHost host;
    @Shadow @Nullable private UnlockCraftingEvent unlockEvent;
    @Shadow @Nullable private GenericStack unlockStack;
    @Unique @Nullable private IPatternDetails ca_patternDetails;
    @Unique @Nullable private CraftingCPUCluster ca_cpuCluster;

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

    @Unique
    @Override
    public void setPatternDetails(IPatternDetails details) {
        this.ca_patternDetails = details;
    }

    @Unique
    @Override
    public IPatternDetails getPatternDetails() {
        return this.ca_patternDetails;
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

    @Inject(
            method = "findAdapter",
            at = @At("RETURN"),
            cancellable = true,
            remap = false
    )
    private void redirectFind(Direction side, CallbackInfoReturnable<PatternProviderTarget> cir) {
        IPatternDetails pattern = this.getPatternDetails();
        if (pattern == null) return;

        Object rawCache = getTargetCache(this, side.get3DDataValue());
        if (rawCache instanceof IPatternProviderTargetCacheExt ext) {
            cir.setReturnValue(ext.find(pattern));
        }
    }

    @Unique
    private static Object getTargetCache(Object logicInstance, int index) {
        try {
            Class<?> c = logicInstance.getClass();
            while (c != null) {
                try {
                    Field f = c.getDeclaredField("targetCaches");
                    f.setAccessible(true);
                    Object[] caches = (Object[]) f.get(logicInstance);
                    return caches[index];
                } catch (NoSuchFieldException ignored) {
                    c = c.getSuperclass();
                }
            }
            return null;
        } catch (Exception e) {
            LogUtils.getLogger().info(e.toString());
            return null;
        }
    }

    @Unique
    private boolean isImpulsed() {
        var be = host != null ? host.getBlockEntity() : null;
        return be != null && be.getType() == CrazyBlockEntityRegistrar.IMPULSED_PATTERN_PROVIDER_BE.get();
    }

    @ModifyExpressionValue(
            method = "pushPattern(Lappeng/api/crafting/IPatternDetails;[Lappeng/api/stacks/KeyCounter;)Z",
            at = @At(
                    value = "INVOKE",
                    target = "Lappeng/helpers/patternprovider/PatternProviderLogic;getCraftingLockedReason()Lappeng/api/config/LockCraftingMode;"
            ),
            remap = false
    )
    private LockCraftingMode bypassLockForImpulsed(LockCraftingMode original, IPatternDetails details) {
        if (!isImpulsed()) return original;

        if ((Object) this instanceof ImpulsedPatternProviderLogic logic && logic.bypassLock()) {
            return LockCraftingMode.NONE;
        }
        return original;
    }

    @ModifyExpressionValue(
            method = "onStackReturnedToNetwork(Lappeng/api/stacks/GenericStack;)V",
            at = @At(value = "INVOKE", target = "Ljava/lang/Object;equals(Ljava/lang/Object;)Z"),
            remap = false
    )
    private boolean ignoreNbtUnlockMatch(boolean originalCheck, GenericStack returned) {
        if (originalCheck) return true;
        if (!isImpulsed()) return false;
        if (this.unlockStack == null) return false;

        if ((Object) this instanceof ImpulsedPatternProviderLogic logic && logic.ignoreNbtUnlock()) {
            return returned.what().getId() == this.unlockStack.what().getId();
        }
        return false;
    }

    @Inject(
            method = "onStackReturnedToNetwork(Lappeng/api/stacks/GenericStack;)V",
            at = @At(
                    value  = "FIELD",
                    target = "Lappeng/helpers/patternprovider/PatternProviderLogic;unlockEvent:Lappeng/helpers/patternprovider/UnlockCraftingEvent;",
                    opcode = Opcodes.PUTFIELD,
                    shift  = At.Shift.AFTER
            ),
            remap = false
    )
    private void afterUnlockCleared(GenericStack genericStack, CallbackInfo ci) {
        if (!isImpulsed()) return;

        if (this.unlockEvent == null && (Object) this instanceof ImpulsedPatternProviderLogic logic) {
            logic.onUnlockCleared();
        }
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
