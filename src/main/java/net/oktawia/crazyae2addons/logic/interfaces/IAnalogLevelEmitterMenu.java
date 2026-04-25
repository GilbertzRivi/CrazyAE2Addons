package net.oktawia.crazyae2addons.logic.interfaces;

public interface IAnalogLevelEmitterMenu {

    boolean crazyAE2Addons$hasAnalogCard();

    boolean crazyAE2Addons$isAnalogLogarithmicMode();

    void crazyAE2Addons$setAnalogLogarithmicMode(boolean enabled);

    default void crazyAE2Addons$toggleAnalogLogarithmicMode() {
        crazyAE2Addons$setAnalogLogarithmicMode(!crazyAE2Addons$isAnalogLogarithmicMode());
    }
}