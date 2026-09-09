package net.oktawia.crazyae2addons.tracking;

import java.util.HashSet;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.networking.IGrid;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.MEStorage;
import appeng.helpers.InterfaceLogicHost;
import appeng.hooks.ticking.TickHandler;

public class TrackingMEStorage implements MEStorage {

    private final MEStorage delegate;
    private final IGrid grid;
    private final InterfaceLogicHost interfaceHost;
    private final UsageTarget target;
    private final String description;

    private IResourceTrackingService service;
    private boolean serviceResolved;

    private final Set<AEKey> replenishedKeys = new HashSet<>();
    private long replenishedKeysTick = -1L;

    public TrackingMEStorage(MEStorage delegate, IGrid grid, InterfaceLogicHost interfaceHost, Level level,
            BlockPos interfacePos) {
        this.delegate = delegate;
        this.grid = grid;
        this.interfaceHost = interfaceHost;
        this.target = UsageTarget.interfaceAt(GlobalPos.of(level.dimension(), interfacePos.immutable()));
        this.description = "interface at " + interfacePos.getX() + " " + interfacePos.getY() + " "
                + interfacePos.getZ();
    }

    @Override
    public long insert(AEKey what, long amount, Actionable mode, IActionSource source) {
        return delegate.insert(what, amount, mode, source);
    }

    @Override
    public long extract(AEKey what, long amount, Actionable mode, IActionSource source) {
        long extracted = delegate.extract(what, amount, mode, source);
        if (extracted <= 0 || mode != Actionable.MODULATE || !ResourceTrackingGate.isEnabled()) {
            return extracted;
        }

        var svc = service();
        if (svc != null && !isReplenished(what)) {
            svc.trackConsumption(what, extracted, target, description, null);
        }
        return extracted;
    }

    private IResourceTrackingService service() {
        if (!serviceResolved) {
            service = grid.getService(IResourceTrackingService.class);
            serviceResolved = true;
        }
        return service;
    }

    @Override
    public void getAvailableStacks(KeyCounter out) {
        delegate.getAvailableStacks(out);
    }

    @Override
    public boolean isPreferredStorageFor(AEKey what, IActionSource source) {
        return delegate.isPreferredStorageFor(what, source);
    }

    @Override
    public Component getDescription() {
        return delegate.getDescription();
    }

    private boolean isReplenished(AEKey what) {
        long tick = TickHandler.instance().getCurrentTick();
        if (tick != replenishedKeysTick) {
            replenishedKeysTick = tick;
            replenishedKeys.clear();

            var config = interfaceHost.getConfig();
            for (int i = 0; i < config.size(); i++) {
                AEKey key = config.getKey(i);
                if (key != null)
                    replenishedKeys.add(key);
            }
        }
        return replenishedKeys.contains(what);
    }
}
