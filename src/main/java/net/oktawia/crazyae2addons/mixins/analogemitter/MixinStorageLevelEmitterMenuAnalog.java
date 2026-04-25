package net.oktawia.crazyae2addons.mixins.analogemitter;

import appeng.menu.implementations.StorageLevelEmitterMenu;
import appeng.menu.implementations.UpgradeableMenu;
import appeng.parts.automation.StorageLevelEmitterPart;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterMenu;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageLevelEmitterMenu.class, remap = false)
public abstract class MixinStorageLevelEmitterMenuAnalog extends UpgradeableMenu<StorageLevelEmitterPart> implements IAnalogLevelEmitterMenu {

    @Unique
    private static final String CRAZY_AE2_ADDONS_ACTION_SET_ANALOG_LOG_MODE =
            "crazySetAnalogLogMode";

    @Unique
    private boolean crazyAE2Addons$analogLogarithmicMode = false;

    protected MixinStorageLevelEmitterMenuAnalog(
            MenuType<?> menuType,
            int id,
            Inventory ip,
            StorageLevelEmitterPart host
    ) {
        super(menuType, id, ip, host);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void crazyAE2Addons$initAnalogMode(
            MenuType<StorageLevelEmitterMenu> menuType,
            int id,
            Inventory ip,
            StorageLevelEmitterPart te,
            CallbackInfo ci
    ) {
        this.registerClientAction(
                CRAZY_AE2_ADDONS_ACTION_SET_ANALOG_LOG_MODE,
                Boolean.class,
                this::crazyAE2Addons$handleSetAnalogLogarithmicMode
        );

        this.addDataSlot(new DataSlot() {
            @Override
            public int get() {
                return crazyAE2Addons$getHostAnalogLogarithmicMode() ? 1 : 0;
            }

            @Override
            public void set(int value) {
                crazyAE2Addons$analogLogarithmicMode = value != 0;
            }
        });
    }

    @Override
    public boolean crazyAE2Addons$hasAnalogCard() {
        return this.hasUpgrade(CrazyItemRegistrar.ANALOG_CARD.get());
    }

    @Override
    public boolean crazyAE2Addons$isAnalogLogarithmicMode() {
        if (this.isClientSide()) {
            return crazyAE2Addons$analogLogarithmicMode;
        }

        return crazyAE2Addons$getHostAnalogLogarithmicMode();
    }

    @Override
    public void crazyAE2Addons$setAnalogLogarithmicMode(boolean enabled) {
        if (this.isClientSide()) {
            crazyAE2Addons$analogLogarithmicMode = enabled;
            this.sendClientAction(CRAZY_AE2_ADDONS_ACTION_SET_ANALOG_LOG_MODE, enabled);
            return;
        }

        crazyAE2Addons$setHostAnalogLogarithmicMode(enabled);
    }

    @Unique
    private void crazyAE2Addons$handleSetAnalogLogarithmicMode(Boolean enabled) {
        if (enabled == null) {
            return;
        }

        if (this.isClientSide()) {
            crazyAE2Addons$analogLogarithmicMode = enabled;
            return;
        }

        crazyAE2Addons$setHostAnalogLogarithmicMode(enabled);
    }

    @Unique
    private boolean crazyAE2Addons$getHostAnalogLogarithmicMode() {
        StorageLevelEmitterPart host = this.getHost();

        if (host instanceof IAnalogLevelEmitterOutput analogOutput) {
            return analogOutput.crazyAE2Addons$isAnalogLogarithmicMode();
        }

        return false;
    }

    @Unique
    private void crazyAE2Addons$setHostAnalogLogarithmicMode(boolean enabled) {
        StorageLevelEmitterPart host = this.getHost();

        if (host instanceof IAnalogLevelEmitterOutput analogOutput) {
            analogOutput.crazyAE2Addons$setAnalogLogarithmicMode(enabled);
        }
    }
}