package net.oktawia.crazyae2addons.mixins.analogemitter;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.StorageLevelEmitterScreen;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.menu.implementations.StorageLevelEmitterMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = StorageLevelEmitterScreen.class, remap = false)
public abstract class MixinStorageLevelEmitterScreenAnalog extends UpgradeableScreen<StorageLevelEmitterMenu> {

    @Unique
    private IconButton crazyAE2Addons$analogModeButton;

    protected MixinStorageLevelEmitterScreenAnalog(
            StorageLevelEmitterMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style
    ) {
        super(menu, playerInventory, title, style);
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void crazyAE2Addons$initAnalogModeButton(
            StorageLevelEmitterMenu menu,
            Inventory playerInventory,
            Component title,
            ScreenStyle style,
            CallbackInfo ci
    ) {
        if (!(this.menu instanceof IAnalogLevelEmitterMenu analogMenu)) {
            return;
        }

        this.crazyAE2Addons$analogModeButton = new IconButton(
                analogMenu.crazyAE2Addons$isAnalogLogarithmicMode()
                        ? Icon.REDSTONE_HIGH
                        : Icon.REDSTONE_LOW,
                button -> {
                    analogMenu.crazyAE2Addons$toggleAnalogLogarithmicMode();

                    boolean logarithmic = analogMenu.crazyAE2Addons$isAnalogLogarithmicMode();

                    this.crazyAE2Addons$analogModeButton.setIcon(
                            logarithmic ? Icon.REDSTONE_HIGH : Icon.REDSTONE_LOW
                    );

                    this.crazyAE2Addons$analogModeButton.setMessage(
                            Component.empty()
                                    .append(Component.translatable(LangDefs.ANALOG_OUTPUT_MODE.getTranslationKey()))
                                    .append("\n")
                                    .append(Component.translatable(
                                            analogMenu.crazyAE2Addons$isAnalogLogarithmicMode()
                                                    ? LangDefs.ANALOG_OUTPUT_LOGARITHMIC_DESC.getTranslationKey()
                                                    : LangDefs.ANALOG_OUTPUT_LINEAR_DESC.getTranslationKey()
                                    ).withStyle(ChatFormatting.GRAY))
                    );
                }
        );

        this.crazyAE2Addons$analogModeButton.setMessage(
                Component.empty()
                        .append(Component.translatable(LangDefs.ANALOG_OUTPUT_MODE.getTranslationKey()))
                        .append("\n")
                        .append(Component.translatable(
                                analogMenu.crazyAE2Addons$isAnalogLogarithmicMode()
                                        ? LangDefs.ANALOG_OUTPUT_LOGARITHMIC_DESC.getTranslationKey()
                                        : LangDefs.ANALOG_OUTPUT_LINEAR_DESC.getTranslationKey()
                        ).withStyle(ChatFormatting.GRAY))
        );

        this.crazyAE2Addons$analogModeButton.setVisibility(false);
        this.addToLeftToolbar(this.crazyAE2Addons$analogModeButton);
    }

    @Inject(method = "updateBeforeRender", at = @At("TAIL"))
    private void crazyAE2Addons$updateAnalogModeButton(CallbackInfo ci) {
        if (this.crazyAE2Addons$analogModeButton == null) {
            return;
        }

        if (!(this.menu instanceof IAnalogLevelEmitterMenu analogMenu)) {
            this.crazyAE2Addons$analogModeButton.setVisibility(false);
            return;
        }

        boolean visible = analogMenu.crazyAE2Addons$hasAnalogCard();
        this.crazyAE2Addons$analogModeButton.setVisibility(visible);

        if (!visible) {
            return;
        }

        boolean logarithmic = analogMenu.crazyAE2Addons$isAnalogLogarithmicMode();

        this.crazyAE2Addons$analogModeButton.setIcon(
                logarithmic ? Icon.REDSTONE_HIGH : Icon.REDSTONE_LOW
        );

        this.crazyAE2Addons$analogModeButton.setMessage(
                Component.empty()
                        .append(Component.translatable(LangDefs.ANALOG_OUTPUT_MODE.getTranslationKey()))
                        .append("\n")
                        .append(Component.translatable(
                                analogMenu.crazyAE2Addons$isAnalogLogarithmicMode()
                                        ? LangDefs.ANALOG_OUTPUT_LOGARITHMIC_DESC.getTranslationKey()
                                        : LangDefs.ANALOG_OUTPUT_LINEAR_DESC.getTranslationKey()
                        ).withStyle(ChatFormatting.GRAY))
        );
    }
}