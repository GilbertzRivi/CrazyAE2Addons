package net.oktawia.crazyae2addons.parts;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import appeng.api.config.RedstoneMode;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import appeng.api.config.FuzzyMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingWatcherNode;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.stacks.KeyCounter;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import net.oktawia.crazyae2addons.menus.MultiLevelEmitterMenu;

public class MultiStorageLevelEmitterPart extends AbstractLevelEmitterPart implements ICraftingProvider {

    public static final int MAX_RULES = 63;

    public enum LogicMode { OR, AND }
    public enum CompareMode { ABOVE_OR_EQUAL, BELOW }

    // --- Models (jak AE2 emitter)
    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_status_has_channel");

    public static final PartModel MODEL_OFF_OFF = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    public static final PartModel MODEL_OFF_ON = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    public static final PartModel MODEL_ON_OFF = new PartModel(MODEL_BASE_ON, MODEL_STATUS_OFF);
    public static final PartModel MODEL_ON_ON = new PartModel(MODEL_BASE_ON, MODEL_STATUS_ON);
    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(MODEL_BASE_ON, MODEL_STATUS_HAS_CHANNEL);

    // --- NBT keys
    private static final String NBT_ROWS = "ml_rows";
    private static final String NBT_LOGIC = "ml_logic";
    private static final String NBT_LIMITS = "ml_limits";
    private static final String NBT_MODES = "ml_modes";
    private static final String NBT_FILTERS = "ml_filters"; // CompoundTag { Items: [...] }
    public static final int MIN_ROWS = 6;
    // --- Inventory na filtry (ghost stacks) - InternalInventory (jak w terminalu)
    private final FilterInventory monitorInv = new FilterInventory();

    // Per row config
    private int rows = MIN_ROWS;
    private LogicMode logicMode = LogicMode.OR;
    private final long[] limits = new long[MAX_RULES];
    private final CompareMode[] modes = new CompareMode[MAX_RULES];

    // Watchers + cache
    private IStackWatcher storageWatcher;
    private IStackWatcher craftingWatcher;
    private long lastUpdateTick = -1;

    private final Map<AEKey, Long> cachedAmounts = new HashMap<>();
    private final Set<AEKey> watchedKeys = new HashSet<>();

    private final IStorageWatcherNode stackWatcherNode = new IStorageWatcherNode() {
        @Override
        public void updateWatcher(IStackWatcher newWatcher) {
            storageWatcher = newWatcher;
            configureWatchers();
        }

        @Override
        public void onStackChange(AEKey what, long amount) {
            if (isUpgradedWith(AEItems.FUZZY_CARD)) {
                long currentTick = TickHandler.instance().getCurrentTick();
                if (currentTick != lastUpdateTick) {
                    lastUpdateTick = currentTick;
                    var grid = getGridNode().getGrid();
                    if (grid != null) updateAllFromGrid(grid);
                    else updateStateFromCache();
                }
                return;
            }

            if (watchedKeys.contains(what)) {
                cachedAmounts.put(what, amount);
                updateStateFromCache();
            }
        }
    };

    private final ICraftingWatcherNode craftingWatcherNode = new ICraftingWatcherNode() {
        @Override
        public void updateWatcher(IStackWatcher newWatcher) {
            craftingWatcher = newWatcher;
            configureWatchers();
        }

        @Override
        public void onRequestChange(AEKey what) {
            updateState();
        }

        @Override
        public void onCraftableChange(AEKey what) {
        }
    };

    public MultiStorageLevelEmitterPart(IPartItem<?> partItem) {
        super(partItem);

        getMainNode().addService(IStorageWatcherNode.class, stackWatcherNode);
        getMainNode().addService(ICraftingWatcherNode.class, craftingWatcherNode);
        getMainNode().addService(ICraftingProvider.class, this);

        getConfigManager().registerSetting(Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        getConfigManager().registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);

        if (getReportingValue() != 0) setReportingValue(0);

        for (int i = 0; i < MAX_RULES; i++) {
            limits[i] = 0;
            modes[i] = CompareMode.ABOVE_OR_EQUAL;
        }
    }

    // ---------- API dla Menu ----------
    public InternalInventory getMonitorInventory() {
        return monitorInv;
    }

    public int getRows() {
        return Math.max(MIN_ROWS, Math.min(rows, MAX_RULES));
    }

    public void setRows(int r) {
        rows = Math.max(MIN_ROWS, Math.min(r, MAX_RULES));
        getHost().markForUpdate();
        configureWatchers();
    }

    public LogicMode getLogicMode() {
        return logicMode;
    }

    public void setLogicMode(LogicMode mode) {
        logicMode = (mode == null) ? LogicMode.OR : mode;
        getHost().markForUpdate();
        updateStateFromCache();
    }

    public long getLimit(int row) {
        if (row < 0 || row >= MAX_RULES) return 0;
        return limits[row];
    }

    public void setLimit(int row, long value) {
        if (row < 0 || row >= MAX_RULES) return;
        limits[row] = Math.max(0, value);
        getHost().markForUpdate();
        updateStateFromCache();
    }

    public CompareMode getCompareMode(int row) {
        if (row < 0 || row >= MAX_RULES) return CompareMode.ABOVE_OR_EQUAL;
        return modes[row];
    }

    public void setCompareMode(int row, CompareMode mode) {
        if (row < 0 || row >= MAX_RULES) return;
        modes[row] = (mode == null) ? CompareMode.ABOVE_OR_EQUAL : mode;
        getHost().markForUpdate();
        updateStateFromCache();
    }

    // ---------- Upgrade slots ----------
    @Override
    protected int getUpgradeSlots() {
        return 1;
    }

    @Override
    public void upgradesChanged() {
        configureWatchers();
    }

    // ---------- Direct output (crafting card) ----------
    @Override
    protected boolean hasDirectOutput() {
        return isUpgradedWith(AEItems.CRAFTING_CARD);
    }

    @Override
    protected boolean getDirectOutput() {
        var grid = getMainNode().getGrid();
        if (grid == null) return false;

        var crafting = grid.getCraftingService();
        var keys = getConfiguredKeysUnique();

        if (keys.isEmpty()) {
            return crafting.isRequestingAny();
        }

        if (logicMode == LogicMode.OR) {
            for (var k : keys) if (crafting.isRequesting(k)) return true;
            return false;
        } else {
            for (var k : keys) if (!crafting.isRequesting(k)) return false;
            return true;
        }
    }

    @Override
    public Set<AEKey> getEmitableItems() {
        if (isUpgradedWith(AEItems.CRAFTING_CARD)
                && getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE) == YesNo.YES) {
            return getConfiguredKeysUnique();
        }
        return Set.of();
    }

    @Override
    public java.util.List<IPatternDetails> getAvailablePatterns() {
        return java.util.List.of();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return false;
    }

    @Override
    public boolean isBusy() {
        return true;
    }

    // ---------- Watchers ----------
    @Override
    protected void configureWatchers() {
        if (storageWatcher != null) storageWatcher.reset();
        if (craftingWatcher != null) craftingWatcher.reset();

        watchedKeys.clear();
        cachedAmounts.clear();

        ICraftingProvider.requestUpdate(getMainNode());

        var keys = getConfiguredKeysUnique();

        if (isUpgradedWith(AEItems.CRAFTING_CARD)) {
            if (craftingWatcher != null) {
                if (keys.isEmpty()) craftingWatcher.setWatchAll(true);
                else for (var k : keys) craftingWatcher.add(k);
            }
            updateState();
            return;
        }

        if (storageWatcher != null) {
            if (isUpgradedWith(AEItems.FUZZY_CARD)) {
                storageWatcher.setWatchAll(true);
            } else {
                for (var k : keys) {
                    storageWatcher.add(k);
                    watchedKeys.add(k);
                }
            }
        }

        getMainNode().ifPresent(n -> {
            var grid = n.getPivot().getGrid();
            if (grid != null) updateAllFromGrid(grid);
            else updateStateFromCache();
        });
    }

    private Set<AEKey> getConfiguredKeysUnique() {
        Set<AEKey> out = new HashSet<>();
        int r = getRows();
        for (int i = 0; i < r; i++) {
            var k = keyFromFilter(monitorInv.getStackInSlot(i));
            if (k != null) out.add(k);
        }
        return out;
    }

    private void updateAllFromGrid(IGrid grid) {
        var inv = grid.getStorageService().getCachedInventory();

        cachedAmounts.clear();

        int r = getRows();
        var keys = getConfiguredKeysUnique();

        if (keys.isEmpty()) {
            updateStateFromCache();
            return;
        }

        if (!isUpgradedWith(AEItems.FUZZY_CARD)) {
            for (var k : keys) {
                cachedAmounts.put(k, inv.get(k));
            }
        } else {
            var fzMode = getConfigManager().getSetting(Settings.FUZZY_MODE);

            // cap per key = max(limit used with this key)
            Map<AEKey, Long> cap = new HashMap<>();
            for (int i = 0; i < r; i++) {
                var k = keyFromFilter(monitorInv.getStackInSlot(i));
                if (k == null) continue;
                cap.merge(k, limits[i], Math::max);
            }

            for (var k : keys) {
                long max = cap.getOrDefault(k, 0L);
                long sum = 0;

                var fuzzyList = inv.findFuzzy(k, fzMode);
                for (var st : fuzzyList) {
                    sum += st.getLongValue();
                    if (sum > max) break;
                }
                cachedAmounts.put(k, sum);
            }
        }

        updateStateFromCache();
    }

    private void updateStateFromCache() {
        if (getReportingValue() != 0) setReportingValue(0);

        boolean baseOn = evaluateRulesWithCache();

        // encode base boolean into lastReportedValue (HIGH_SIGNAL -> ON if 0, OFF if -1)
        this.lastReportedValue = baseOn ? 0 : -1;
        updateState();
    }

    private boolean evaluateRulesWithCache() {
        int r = getRows();
        int configured = 0;

        if (logicMode == LogicMode.OR) {
            for (int i = 0; i < r; i++) {
                var key = keyFromFilter(monitorInv.getStackInSlot(i));
                if (key == null) continue;
                configured++;

                long amount = cachedAmounts.getOrDefault(key, 0L);
                long lim = limits[i];

                boolean ok = (modes[i] == CompareMode.ABOVE_OR_EQUAL) ? (amount >= lim) : (amount < lim);
                if (ok) return true;
            }
            return false;
        } else {
            for (int i = 0; i < r; i++) {
                var key = keyFromFilter(monitorInv.getStackInSlot(i));
                if (key == null) continue;
                configured++;

                long amount = cachedAmounts.getOrDefault(key, 0L);
                long lim = limits[i];

                boolean ok = (modes[i] == CompareMode.ABOVE_OR_EQUAL) ? (amount >= lim) : (amount < lim);
                if (!ok) return false;
            }
            return configured > 0;
        }
    }

    private static @Nullable AEKey keyFromFilter(ItemStack filter) {
        var gs = GenericStack.fromItemStack(filter);
        return gs != null ? gs.what() : null;
    }

    // ---------- NBT ----------
    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);

        if (getReportingValue() != 0) setReportingValue(0);

        rows = Math.max(MIN_ROWS, Math.min(data.getInt(NBT_ROWS), MAX_RULES));
        logicMode = data.getByte(NBT_LOGIC) == 1 ? LogicMode.AND : LogicMode.OR;

        long[] l = data.getLongArray(NBT_LIMITS);
        for (int i = 0; i < MAX_RULES; i++) limits[i] = (i < l.length && l[i] >= 0) ? l[i] : 0;

        byte[] m = data.getByteArray(NBT_MODES);
        for (int i = 0; i < MAX_RULES; i++) {
            byte v = (i < m.length) ? m[i] : 0;
            modes[i] = (v == 1) ? CompareMode.BELOW : CompareMode.ABOVE_OR_EQUAL;
        }

        // filters
        monitorInv.beginBulkLoad();
        try {
            monitorInv.clearDirect();

            if (data.contains(NBT_FILTERS)) {
                CompoundTag invTag = data.getCompound(NBT_FILTERS);
                ListTag items = invTag.getList("Items", 10); // CompoundTag list
                for (int i = 0; i < items.size(); i++) {
                    CompoundTag it = items.getCompound(i);
                    int slot = it.getByte("Slot") & 0xFF;
                    if (slot >= 0 && slot < MAX_RULES) {
                        ItemStack s = ItemStack.of(it);
                        monitorInv.setItemDirect(slot, s);
                    }
                }
            }
        } finally {
            monitorInv.endBulkLoad();
        }

        configureWatchers();
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);

        data.putInt(NBT_ROWS, getRows());
        data.putByte(NBT_LOGIC, (byte) (logicMode == LogicMode.AND ? 1 : 0));

        data.putLongArray(NBT_LIMITS, limits);

        byte[] m = new byte[MAX_RULES];
        for (int i = 0; i < MAX_RULES; i++) m[i] = (byte) (modes[i] == CompareMode.BELOW ? 1 : 0);
        data.putByteArray(NBT_MODES, m);

        // filters -> CompoundTag z listą {Slot, ...stack...}
        CompoundTag invTag = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < MAX_RULES; i++) {
            ItemStack s = monitorInv.getStackInSlot(i);
            if (s.isEmpty()) continue;

            CompoundTag it = new CompoundTag();
            it.putByte("Slot", (byte) i);
            s.save(it);
            list.add(it);
        }
        invTag.put("Items", list);
        data.put(NBT_FILTERS, invTag);
    }

    // ---------- GUI open ----------
    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.MULTI_LEVEL_EMITTER_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    // ---------- Models ----------
    @Override
    public IPartModel getStaticModels() {
        if (this.isActive() && this.isPowered()) {
            return this.isLevelEmitterOn() ? MODEL_ON_HAS_CHANNEL : MODEL_OFF_HAS_CHANNEL;
        } else if (this.isPowered()) {
            return this.isLevelEmitterOn() ? MODEL_ON_ON : MODEL_OFF_ON;
        } else {
            return this.isLevelEmitterOn() ? MODEL_ON_OFF : MODEL_OFF_OFF;
        }
    }

    // ---------- InternalInventory impl ----------
    private final class FilterInventory extends BaseInternalInventory {
        private final ItemStack[] stacks = new ItemStack[MAX_RULES];
        private boolean bulkLoading = false;

        private FilterInventory() {
            for (int i = 0; i < MAX_RULES; i++) stacks[i] = ItemStack.EMPTY;
        }

        @Override
        public int size() {
            return MAX_RULES;
        }

        @Override
        public ItemStack getStackInSlot(int slotIndex) {
            if (slotIndex < 0 || slotIndex >= MAX_RULES) return ItemStack.EMPTY;
            ItemStack s = stacks[slotIndex];
            return s == null ? ItemStack.EMPTY : s;
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            if (slotIndex < 0 || slotIndex >= MAX_RULES) return;

            ItemStack normalized;
            if (stack == null || stack.isEmpty()) {
                normalized = ItemStack.EMPTY;
            } else {
                normalized = stack.copy();
                normalized.setCount(1); // ghost
            }

            stacks[slotIndex] = normalized;

            if (!bulkLoading && !isClientSide()) {
                // Reconfigure watchers when filter changes (jak w emitterach)
                configureWatchers();
                getHost().markForUpdate();
            }
        }

        private void clearDirect() {
            for (int i = 0; i < MAX_RULES; i++) stacks[i] = ItemStack.EMPTY;
        }

        private void beginBulkLoad() {
            bulkLoading = true;
        }

        private void endBulkLoad() {
            bulkLoading = false;
        }
    }
}
