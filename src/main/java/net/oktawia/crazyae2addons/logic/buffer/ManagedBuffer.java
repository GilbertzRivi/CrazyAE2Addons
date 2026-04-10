package net.oktawia.crazyae2addons.logic.buffer;

import appeng.api.config.Actionable;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.networking.IManagedGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.IActionSource;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.api.storage.StorageHelper;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.helpers.MachineSource;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.defs.components.AEItemBufferData;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Future;
import java.util.function.Supplier;

public class ManagedBuffer {

    private final Object2LongOpenHashMap<AEKey> items = new Object2LongOpenHashMap<>();
    private final ManagedBufferLogic logic;
    private final IManagedGridNode mainNode;
    private final PatternProviderLogicHost logicHost;
    private final IActionHost actionHost;
    private final Runnable onDirty;
    private final Runnable onReady;
    private final Supplier<Boolean> isActive;

    private final List<Future<ICraftingPlan>> pendingPlans = new ArrayList<>();
    private final List<ICraftingLink> activeLinks = new ArrayList<>();

    private boolean flushPending = false;
    private int flushTickAcc = 0;
    private long readyAtTick = 0;
    private boolean canCraft = true;

    public ManagedBuffer(IManagedGridNode mainNode, PatternProviderLogicHost logicHost,
                         IActionHost actionHost, Runnable onDirty, Runnable onReady,
                         Supplier<Boolean> isActive) {
        this.mainNode = mainNode;
        this.logicHost = logicHost;
        this.actionHost = actionHost;
        this.onDirty = onDirty;
        this.onReady = onReady;
        this.isActive = isActive;
        this.items.defaultReturnValue(0L);
        this.logic = new ManagedBufferLogic(mainNode, logicHost, this);
    }

    public PatternProviderLogic getLogic() {
        return logic;
    }

    public long get(AEKey key) {
        return items.getLong(key);
    }

    public void add(AEKey key, long amount) {
        if (amount <= 0) return;
        items.put(key, get(key) + amount);
        onDirty.run();
    }

    public long extract(AEKey key, long amount) {
        if (amount <= 0) return 0;
        long have = get(key);
        long take = Math.min(have, amount);
        if (take <= 0) return 0;
        long left = have - take;
        if (left <= 0) items.removeLong(key);
        else items.put(key, left);
        onDirty.run();
        return take;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    public void clear() {
        items.clear();
        onDirty.run();
    }

    public void collectFromNetwork(List<GenericStack> required, Supplier<Boolean> hasCreative) {
        if (hasCreative.get()) return;
        var grid = grid();
        if (grid == null) return;
        var storage = grid.getStorageService().getInventory();
        var es = grid.getEnergyService();
        for (var stack : required) {
            var key = stack.what();
            if (key == null) continue;
            long need = stack.amount() - get(key);
            if (need <= 0) continue;
            long pulled = StorageHelper.poweredExtraction(es, storage, key, need, src(), Actionable.MODULATE);
            if (pulled > 0) add(key, pulled);
        }
    }

    public List<GenericStack> computeMissing(List<GenericStack> required, Supplier<Boolean> hasCreative) {
        if (hasCreative.get()) return new ArrayList<>();
        List<GenericStack> missing = new ArrayList<>();
        for (var stack : required) {
            var key = stack.what();
            if (key == null) continue;
            long need = stack.amount() - get(key);
            if (need > 0) missing.add(new GenericStack(key, need));
        }
        return missing;
    }

    public boolean request(List<GenericStack> required) {
        if (flushPending) return false;
        if (!flushUnneeded(required)) return false;

        collectFromNetwork(required, () -> false);
        var missing = computeMissing(required, () -> false);
        if (missing.isEmpty()) {
            fireReady();
            return true;
        }

        requestCrafting(missing);
        return hasActiveCrafting();
    }

    private boolean flushUnneeded(List<GenericStack> required) {
        if (items.isEmpty()) {
            flushPending = false;
            return true;
        }

        var needed = new Object2LongOpenHashMap<AEKey>();
        needed.defaultReturnValue(0L);
        for (var s : required) {
            if (s.what() != null && s.amount() > 0) {
                needed.put(s.what(), needed.getLong(s.what()) + s.amount());
            }
        }

        var grid = grid();
        if (grid != null) {
            var inv = grid.getStorageService().getInventory();
            var es = grid.getEnergyService();
            var it = items.object2LongEntrySet().iterator();
            while (it.hasNext()) {
                var e = it.next();
                long have = e.getLongValue();
                long excess = have - needed.getLong(e.getKey());
                if (excess <= 0) continue;

                long inserted = StorageHelper.poweredInsert(es, inv, e.getKey(), excess, src(), Actionable.MODULATE);
                long remaining = have - inserted;
                if (remaining <= 0) it.remove();
                else e.setValue(remaining);
            }
        }

        boolean stillHasExcess = false;
        for (var e : items.object2LongEntrySet()) {
            if (e.getLongValue() > needed.getLong(e.getKey())) {
                stillHasExcess = true;
                break;
            }
        }

        flushPending = stillHasExcess;
        onDirty.run();
        return !stillHasExcess;
    }

    public void setCanCraft(boolean canCraft) { this.canCraft = canCraft; }

    public boolean requestCrafting(List<GenericStack> inputs) {
        if (!canCraft || hasActiveCrafting()) return false;
        var grid = grid();
        if (grid == null) return false;

        var tag = new CompoundTag();
        tag.putUUID("s", UUID.randomUUID());
        var dummy = logicHost.getBlockEntity().getBlockState().getBlock().asItem().getDefaultInstance();
        dummy.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        var dummyOutput = new GenericStack(AEItemKey.of(dummy), 1);

        var patternStack = PatternDetailsHelper.encodeProcessingPattern(inputs, List.of(dummyOutput));
        logic.getPatternInv().setItemDirect(0, patternStack);
        logic.updatePatterns();

        var plan = grid.getCraftingService().beginCraftingCalculation(
                level(), () -> new MachineSource(actionHost),
                dummyOutput.what(), dummyOutput.amount(), CalculationStrategy.REPORT_MISSING_ITEMS
        );
        pendingPlans.add(plan);
        onDirty.run();
        return true;
    }

    public void onPushPatternComplete() {
        clearPattern();
        readyAtTick = level().getGameTime() + 1;
        cancelAllLinks();
    }

    public @Nullable GenericStack tick(int ticksSinceLastCall) {
        if (!isActive.get() && !hasActiveCrafting() && !items.isEmpty() && !flushPending) beginFlush();
        var missing = tickCrafting();
        tickFlush(ticksSinceLastCall);
        return missing;
    }

    @Nullable
    private GenericStack tickCrafting() {
        if (readyAtTick > 0) {
            if (level().getGameTime() >= readyAtTick) {
                readyAtTick = 0;
                fireReady();
            }
            return null;
        }
        if (pendingPlans.isEmpty() && activeLinks.isEmpty()) return null;

        GenericStack firstMissing = null;
        var it = pendingPlans.iterator();
        while (it.hasNext()) {
            var future = it.next();
            if (!future.isDone()) continue;
            it.remove();
            try {
                var plan = future.get();
                var grid = grid();
                if (grid == null) {
                    clearPattern();
                    beginFlush();
                    continue;
                }

                var result = grid.getCraftingService().submitJob(
                        plan, actionHost instanceof ICraftingRequester r ? r : null,
                        null, true, src());
                if (result.successful() && result.link() != null) {
                    activeLinks.add(result.link());
                    onDirty.run();
                } else {
                    if (firstMissing == null) {
                        try {
                            KeyCounter mc = plan.missingItems();
                            if (mc != null && !mc.isEmpty()) {
                                var e = mc.iterator().next();
                                firstMissing = new GenericStack(e.getKey(), e.getLongValue());
                            }
                        } catch (Throwable ignored) {
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (firstMissing != null) {
            clearPattern();
            cancelAllLinks();
            beginFlush();
            return firstMissing;
        }
        return null;
    }

    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.copyOf(activeLinks);
    }

    public long insertCraftedItems(AEKey what, long amount, Actionable mode) {
        if (mode == Actionable.MODULATE && what != null) {
            add(what, amount);
        }
        return 0;
    }

    public void jobStateChange(ICraftingLink link) {
        activeLinks.remove(link);
        onDirty.run();

        if (link.isDone()) {
            if (pendingPlans.isEmpty() && activeLinks.isEmpty() && readyAtTick == 0) {
                readyAtTick = level().getGameTime() + 1;
            }
        } else if (link.isCanceled()) {
            if (readyAtTick == 0 && pendingPlans.isEmpty() && activeLinks.isEmpty()) {
                beginFlush();
            }
        }
    }

    public void beginFlush() {
        if (items.isEmpty()) {
            flushPending = false;
            return;
        }
        flushPending = true;
        flushTickAcc = 0;
        onDirty.run();
    }

    private void tickFlush(int ticksSinceLastCall) {
        if (!flushPending) return;
        flushTickAcc += ticksSinceLastCall;
        if (flushTickAcc >= 20) {
            flushTickAcc = 0;
            flushOnce();
        }
    }

    private void flushOnce() {
        var grid = grid();
        if (grid == null) return;
        var inv = grid.getStorageService().getInventory();
        var es = grid.getEnergyService();
        var it = items.object2LongEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            long amt = e.getLongValue();
            if (amt <= 0) {
                it.remove();
                continue;
            }
            long inserted = StorageHelper.poweredInsert(es, inv, e.getKey(), amt, src(), Actionable.MODULATE);
            if (inserted >= amt) it.remove();
            else e.setValue(amt - inserted);
        }
        if (items.isEmpty()) flushPending = false;
        onDirty.run();
    }

    public boolean isFlushPending() {
        return flushPending;
    }

    public AEItemBufferData toData() {
        List<GenericStack> entries = new ArrayList<>(items.size());
        for (Object2LongMap.Entry<AEKey> e : items.object2LongEntrySet()) {
            if (e.getLongValue() > 0) entries.add(new GenericStack(e.getKey(), e.getLongValue()));
        }
        List<CompoundTag> linkTags = new ArrayList<>();
        for (var link : activeLinks) {
            if (link.isCanceled() || link.isDone()) continue;
            var lt = new CompoundTag();
            link.writeToNBT(lt);
            linkTags.add(lt);
        }
        return new AEItemBufferData(entries, flushPending, flushTickAcc, linkTags);
    }

    public void fromData(AEItemBufferData data) {
        items.clear();
        for (var s : data.entries()) {
            if (s == null || s.amount() <= 0 || s.what() == null) continue;
            items.put(s.what(), s.amount());
        }
        flushPending = data.flushPending() && !items.isEmpty();
        flushTickAcc = data.flushTickAcc();
        readyAtTick = 0;
        pendingPlans.clear();
        activeLinks.clear();
        if (actionHost instanceof ICraftingRequester requester) {
            for (var lt : data.links()) {
                var link = StorageHelper.loadCraftingLink(lt, requester);
                if (link != null) activeLinks.add(link);
            }
        }
    }

    public void cancelCrafting() {
        readyAtTick = 0;
        clearPattern();
        cancelAllLinks();
        if (!items.isEmpty()) beginFlush();
    }

    public boolean hasActiveCrafting() {
        return !pendingPlans.isEmpty() || !activeLinks.isEmpty() || readyAtTick > 0;
    }

    private void clearPattern() {
        logic.getPatternInv().setItemDirect(0, net.minecraft.world.item.ItemStack.EMPTY);
        logic.updatePatterns();
    }

    private void cancelAllLinks() {
        var toCancel = new ArrayList<>(activeLinks);
        activeLinks.clear();
        pendingPlans.clear();
        for (var link : toCancel) {
            try {
                link.cancel();
            } catch (Throwable ignored) {
            }
        }
    }

    private void fireReady() {
        onReady.run();
    }

    private IActionSource src() {
        return IActionSource.ofMachine(actionHost);
    }

    private Level level() {
        return logicHost.getBlockEntity().getLevel();
    }

    private @Nullable appeng.api.networking.IGrid grid() {
        return mainNode.getGrid();
    }
}