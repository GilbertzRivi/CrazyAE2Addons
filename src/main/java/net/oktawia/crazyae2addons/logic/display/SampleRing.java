package net.oktawia.crazyae2addons.logic.display;

import org.jetbrains.annotations.Nullable;

public final class SampleRing {

    private static final int MAX_SAMPLES = 2048;

    private final long[] ticks  = new long[MAX_SAMPLES];
    private final long[] values = new long[MAX_SAMPLES];
    private int head = 0;
    private int size = 0;

    public record Sample(long tick, long value) {}

    private int idx(int i) {
        int x = head + i;
        return x >= MAX_SAMPLES ? x - MAX_SAMPLES : x;
    }

    public void add(long tick, long value) {
        if (size > 0 && tick <= ticks[idx(size - 1)]) return;
        if (size < MAX_SAMPLES) {
            int tail = idx(size);
            ticks[tail]  = tick;
            values[tail] = value;
            size++;
        } else {
            head = (head + 1) % MAX_SAMPLES;
            int tail = idx(size - 1);
            ticks[tail]  = tick;
            values[tail] = value;
        }
    }

    public void trimOlderThan(long minTick) {
        while (size > 0 && ticks[head] < minTick) {
            head = (head + 1) % MAX_SAMPLES;
            size--;
        }
    }

    @Nullable
    public Sample getAtOrBefore(long targetTick) {
        for (int i = size - 1; i >= 0; i--) {
            int p = idx(i);
            if (ticks[p] <= targetTick) return new Sample(ticks[p], values[p]);
        }
        return null;
    }

    @Nullable
    public Sample oldest() {
        if (size <= 0) return null;
        return new Sample(ticks[head], values[head]);
    }
}
