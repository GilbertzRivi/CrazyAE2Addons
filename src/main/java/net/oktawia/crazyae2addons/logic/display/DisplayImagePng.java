package net.oktawia.crazyae2addons.logic.display;

import javax.annotation.Nullable;

public final class DisplayImagePng {

    public record Size(int width, int height) {
    }

    private DisplayImagePng() {
    }

    @Nullable
    public static Size readSize(byte[] bytes) {
        if (bytes == null || bytes.length < 24) {
            return null;
        }

        boolean signature = (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G'
                && (bytes[4] & 0xFF) == 0x0D && (bytes[5] & 0xFF) == 0x0A
                && (bytes[6] & 0xFF) == 0x1A && (bytes[7] & 0xFF) == 0x0A;

        if (!signature) {
            return null;
        }

        if (bytes[12] != 'I' || bytes[13] != 'H' || bytes[14] != 'D' || bytes[15] != 'R') {
            return null;
        }

        int width = readBigEndianInt(bytes, 16);
        int height = readBigEndianInt(bytes, 20);

        if (width <= 0 || height <= 0) {
            return null;
        }

        return new Size(width, height);
    }

    private static int readBigEndianInt(byte[] bytes, int offset) {
        return ((bytes[offset] & 0xFF) << 24)
                | ((bytes[offset + 1] & 0xFF) << 16)
                | ((bytes[offset + 2] & 0xFF) << 8)
                | (bytes[offset + 3] & 0xFF);
    }
}
