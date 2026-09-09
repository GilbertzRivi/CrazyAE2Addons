package net.oktawia.crazyae2addons.logic.display;

public record DisplayImageLimits(int maxDimension, int maxBytes) {

    public static DisplayImageLimits fromConfig() {
        return new DisplayImageLimits(
                DisplayImageStore.maxImageDimension(),
                DisplayImageStore.maxImageBytes());
    }

    public DisplayImageLimits sanitized() {
        return new DisplayImageLimits(
                maxDimension > 0 ? maxDimension : DisplayImageStore.maxImageDimension(),
                maxBytes > 0 ? maxBytes : DisplayImageStore.maxImageBytes());
    }
}
