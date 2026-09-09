package net.oktawia.crazyae2addons.logic.display;

public final class DisplayImageTransfer {

    public static final int SIGNAL_BEGIN = 0;
    public static final int SIGNAL_DATA = 1;
    public static final int SIGNAL_END = 2;

    public static final int CHUNK_BYTES = 24 * 1024;
    public static final int MAX_ID_LEN = 128;
    public static final int MAX_NAME_LEN = 256;

    private DisplayImageTransfer() {
    }
}
