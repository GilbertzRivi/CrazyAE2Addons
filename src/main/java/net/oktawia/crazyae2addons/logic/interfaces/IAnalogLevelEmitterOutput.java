package net.oktawia.crazyae2addons.logic.interfaces;

public interface IAnalogLevelEmitterOutput {

    boolean crazyAE2Addons$usesAnalogOutput();

    int crazyAE2Addons$getAnalogOutputSignal();

    default boolean crazyAE2Addons$isAnalogLogarithmicMode() {
        return false;
    }

    default void crazyAE2Addons$setAnalogLogarithmicMode(boolean enabled) {
    }
}