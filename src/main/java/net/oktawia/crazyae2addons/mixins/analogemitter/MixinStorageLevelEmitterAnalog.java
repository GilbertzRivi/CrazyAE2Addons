package net.oktawia.crazyae2addons.mixins.analogemitter;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.parts.IPartItem;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.parts.automation.StorageLevelEmitterPart;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageLevelEmitterPart.class, remap = false)
public abstract class MixinStorageLevelEmitterAnalog extends AbstractLevelEmitterPart implements IAnalogLevelEmitterOutput {

    @Unique
    private static final String CRAZY_AE2_ADDONS_ANALOG_LOG_MODE = "crazyAnalogLogarithmicMode";

    @Unique
    private boolean crazyAE2Addons$analogLogarithmicMode = false;

    protected MixinStorageLevelEmitterAnalog(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    public boolean crazyAE2Addons$usesAnalogOutput() {
        for (ItemStack stack : getUpgrades()) {
            if (stack.is(CrazyItemRegistrar.ANALOG_CARD.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int crazyAE2Addons$getAnalogOutputSignal() {
        if (!crazyAE2Addons$usesAnalogOutput()) {
            return isLevelEmitterOn() ? 15 : 0;
        }

        if (hasDirectOutput()) {
            return getDirectOutput() ? 15 : 0;
        }

        long threshold = getReportingValue();
        long amount = this.lastReportedValue;

        int signal;

        if (threshold <= 0L) {
            signal = 15;
        } else if (amount <= 0L) {
            signal = 0;
        } else if (crazyAE2Addons$analogLogarithmicMode) {
            double scaled = ((double) amount * (16384.0D / (double) threshold)) + 1.0D;
            signal = (int) Math.ceil(Math.log(scaled) / Math.log(2.0D));
        } else {
            signal = (int) Math.floor((double) amount * 15.0D / (double) threshold);
        }

        signal = Math.max(0, Math.min(15, signal));

        if (getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL) {
            signal = 15 - signal;
        }

        return signal;
    }

    @Override
    public boolean crazyAE2Addons$isAnalogLogarithmicMode() {
        return crazyAE2Addons$analogLogarithmicMode;
    }

    @Override
    public void crazyAE2Addons$setAnalogLogarithmicMode(boolean enabled) {
        if (crazyAE2Addons$analogLogarithmicMode == enabled) {
            return;
        }

        crazyAE2Addons$analogLogarithmicMode = enabled;

        if (getHost() != null) {
            getHost().markForSave();
        }

        updateState();
    }

    @Inject(method = "readFromNBT", at = @At("TAIL"))
    private void crazyAE2Addons$readAnalogMode(CompoundTag data, CallbackInfo ci) {
        crazyAE2Addons$analogLogarithmicMode = data.getBoolean(CRAZY_AE2_ADDONS_ANALOG_LOG_MODE);
    }

    @Inject(method = "writeToNBT", at = @At("TAIL"))
    private void crazyAE2Addons$writeAnalogMode(CompoundTag data, CallbackInfo ci) {
        data.putBoolean(CRAZY_AE2_ADDONS_ANALOG_LOG_MODE, crazyAE2Addons$analogLogarithmicMode);
    }

    @Unique
    private String crazyAE2Addons$getDebugPos() {
        if (getHost() == null || getHost().getBlockEntity() == null) {
            return "unknown";
        }

        BlockPos pos = getHost().getBlockEntity().getBlockPos();
        return pos.toShortString();
    }
}