package net.oktawia.crazyae2addons.tracking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import appeng.api.networking.IGrid;
import appeng.api.networking.IGridServiceProvider;
import appeng.api.stacks.AEKey;

public class ResourceTrackingService implements IResourceTrackingService, IGridServiceProvider {

    private static final int MAX_KEYS = 10_000;
    private static final int MAX_TARGETS_PER_KEY = 200;
    private static final int CLEANUP_INTERVAL = 20;

    private final HashMap<AEKey, PerKeyData> data = new HashMap<>();

    private int cleanupCountdown = CLEANUP_INTERVAL;

    public ResourceTrackingService(IGrid grid) {
    }

    @Override
    public void trackConsumption(AEKey key, long amount, UsageTarget target, String description, @Nullable AEKey icon) {
        if (amount <= 0)
            return;
        PerKeyData d = data.get(key);
        if (d == null) {
            if (data.size() >= MAX_KEYS)
                return;
            d = new PerKeyData();
            data.put(key, d);
        }
        if (d.targetAmounts.size() >= MAX_TARGETS_PER_KEY && !d.targetAmounts.containsKey(target))
            return;
        d.record(amount, target, description, icon, System.currentTimeMillis());
    }

    @Override
    public List<ResourceSummary> getSummaries() {
        long now = System.currentTimeMillis();
        List<ResourceSummary> result = new ArrayList<>(data.size());
        for (Map.Entry<AEKey, PerKeyData> entry : data.entrySet()) {
            long perMin = entry.getValue().perMinute(now);
            if (perMin > 0) {
                result.add(new ResourceSummary(entry.getKey(), perMin, perMin));
            }
        }
        result.sort(Comparator.comparingLong(ResourceSummary::perMinute).reversed());
        return result;
    }

    @Override
    public List<UsageEntry> getDetails(AEKey key) {
        PerKeyData d = data.get(key);
        if (d == null)
            return List.of();
        long now = System.currentTimeMillis();
        List<UsageEntry> entries = new ArrayList<>(d.targetAmounts.size());
        for (UsageTarget target : d.targetAmounts.keySet()) {
            long perMin = d.perMinuteTarget(target, now);
            if (perMin > 0) {
                entries.add(new UsageEntry(d.targetDescriptions.get(target), perMin, d.targetIcons.get(target),
                        target.pos()));
            }
        }
        entries.sort(Comparator.comparingLong(UsageEntry::totalAmount).reversed());
        return entries;
    }

    @Override
    public void onServerEndTick() {
        if (--cleanupCountdown > 0) {
            return;
        }
        cleanupCountdown = CLEANUP_INTERVAL;

        if (!ResourceTrackingGate.isEnabled()) {
            data.clear();
            return;
        }

        long now = System.currentTimeMillis();
        data.entrySet().removeIf(e -> e.getValue().perMinute(now) == 0);
    }
}
