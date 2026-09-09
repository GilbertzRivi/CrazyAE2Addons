package net.oktawia.crazyae2addons.logic.display;

public enum DisplayImageUploadStatus {
    OK,
    TOO_LARGE,
    INVALID_IMAGE,
    TOO_MANY_IMAGES,
    STORAGE_FULL,
    DISABLED,
    FAILED;

    public static DisplayImageUploadStatus byId(int id) {
        DisplayImageUploadStatus[] values = values();
        return id < 0 || id >= values.length ? FAILED : values[id];
    }
}
