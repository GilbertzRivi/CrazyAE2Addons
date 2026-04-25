package net.oktawia.crazyae2addons.mixins.analogemitter;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.util.Platform;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = AbstractLevelEmitterPart.class, remap = false)
public abstract class MixinAbstractLevelEmitterAnalog {

    @Shadow
    protected long lastReportedValue;

    @Shadow
    private boolean prevState;

    @Shadow
    protected abstract boolean hasDirectOutput();

    @Unique
    private static final double crazyAE2Addons$LOG_2 = Math.log(2.0D);

    @Unique
    private int crazyAE2Addons$lastProvidedPower = Integer.MIN_VALUE;

    @Inject(method = "isProvidingStrongPower", at = @At("HEAD"), cancellable = true)
    private void crazyAE2Addons$useAnalogStrongPower(CallbackInfoReturnable<Integer> cir) {
        int analogPower = crazyAE2Addons$getAnalogPower();

        if (analogPower >= 0) {
            cir.setReturnValue(analogPower);
        }
    }

    @Inject(method = "isProvidingWeakPower", at = @At("HEAD"), cancellable = true)
    private void crazyAE2Addons$useAnalogWeakPower(CallbackInfoReturnable<Integer> cir) {
        int analogPower = crazyAE2Addons$getAnalogPower();

        if (analogPower >= 0) {
            cir.setReturnValue(analogPower);
        }
    }

    @Inject(method = "isLevelEmitterOn", at = @At("HEAD"), cancellable = true)
    private void crazyAE2Addons$useAnalogVisualState(CallbackInfoReturnable<Boolean> cir) {
        AbstractLevelEmitterPart self = (AbstractLevelEmitterPart) (Object) this;

        if (self.isClientSide()) {
            return;
        }

        int analogPower = crazyAE2Addons$getAnalogPower();

        if (analogPower >= 0) {
            cir.setReturnValue(analogPower > 0);
        }
    }

    @Inject(method = "updateState", at = @At("TAIL"))
    private void crazyAE2Addons$notifyAnalogPowerChange(CallbackInfo ci) {
        int currentPower = crazyAE2Addons$getEffectiveProvidedPower();

        if (crazyAE2Addons$lastProvidedPower == currentPower) {
            return;
        }

        crazyAE2Addons$lastProvidedPower = currentPower;
        crazyAE2Addons$notifyNeighbors();
    }

    @Unique
    private int crazyAE2Addons$getEffectiveProvidedPower() {
        int analogPower = crazyAE2Addons$getAnalogPower();

        if (analogPower >= 0) {
            return analogPower;
        }

        return this.prevState ? 15 : 0;
    }

    @Unique
    private int crazyAE2Addons$getAnalogPower() {
        Object selfObject = this;

        if (!(selfObject instanceof IAnalogLevelEmitterOutput analogOutput)) {
            return -1;
        }

        if (!analogOutput.crazyAE2Addons$usesAnalogOutput()) {
            return -1;
        }

        if (hasDirectOutput()) {
            return -1;
        }

        AbstractLevelEmitterPart self = (AbstractLevelEmitterPart) selfObject;

        if (!self.getMainNode().isActive()) {
            return 0;
        }

        long threshold = self.getReportingValue();
        if (threshold <= 0L) {
            return -1;
        }

        long amount = Math.max(0L, lastReportedValue);

        int power;

        if (amount <= 0L) {
            power = 0;
        } else if (amount >= threshold) {
            power = 15;
        } else if (analogOutput.crazyAE2Addons$isAnalogLogarithmicMode()) {
            double scaled = ((double) amount * (16384.0D / (double) threshold)) + 1.0D;
            power = Mth.clamp((int) Math.ceil(Math.log(scaled) / crazyAE2Addons$LOG_2), 0, 15);
        } else {
            power = Mth.clamp((int) Math.floor((double) amount * 15.0D / (double) threshold), 0, 15);
        }

        if (self.getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL) {
            power = 15 - power;
        }

        return Mth.clamp(power, 0, 15);
    }

    @Unique
    private void crazyAE2Addons$notifyNeighbors() {
        AbstractLevelEmitterPart self = (AbstractLevelEmitterPart) (Object) this;

        if (self.getHost() == null) {
            return;
        }

        BlockEntity be = self.getHost().getBlockEntity();
        if (be == null || be.getLevel() == null) {
            return;
        }

        Platform.notifyBlocksOfNeighbors(be.getLevel(), be.getBlockPos());
        Platform.notifyBlocksOfNeighbors(be.getLevel(), be.getBlockPos().relative(self.getSide()));
    }
}