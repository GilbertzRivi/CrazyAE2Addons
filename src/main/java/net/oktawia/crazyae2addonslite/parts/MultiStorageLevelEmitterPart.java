package net.oktawia.crazyae2addonslite.parts;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import appeng.helpers.IConfigInvHost;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.util.SettingsFrom;
import net.minecraft.nbt.Tag;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.config.YesNo;
import appeng.api.crafting.IPatternDetails;
import appeng.api.networking.IGrid;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.crafting.ICraftingWatcherNode;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.KeyCounter;
import appeng.core.AppEng;
import appeng.core.definitions.AEItems;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.util.ConfigInventory;
import org.jetbrains.annotations.Nullable;

public class MultiStorageLevelEmitterPart extends AbstractLevelEmitterPart implements IConfigInvHost, ICraftingProvider {

    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = new ResourceLocation(AppEng.MOD_ID,
            "part/level_emitter_status_has_channel");

    public static final PartModel MODEL_OFF_OFF = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    public static final PartModel MODEL_OFF_ON = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    public static final PartModel MODEL_ON_OFF = new PartModel(MODEL_BASE_ON, MODEL_STATUS_OFF);
    public static final PartModel MODEL_ON_ON = new PartModel(MODEL_BASE_ON, MODEL_STATUS_ON);
    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(MODEL_BASE_ON, MODEL_STATUS_HAS_CHANNEL);

    private static final int FILTER_SLOTS = 16;
    private static final String NBT_CONFIG = "config";
    private static final String NBT_COMPARE_MASK = "cmpMask";
    private static final String NBT_CRAFT_MASK = "craftMask";
    private static final String NBT_LOGIC_AND = "logicAnd";
    private static final String NBT_THRESHOLDS = "thresholds";
    private static final String NBT_RS_MODE = "rsMode";
    private static final String NBT_FUZZY_MODE = "fzMode";
    private static final String NBT_CRAFT_VIA = "craftVia";
    private short compareMask = 0;
    private short craftMask = (short) 0xFFFF;
    private boolean logicAnd = false;
    private final long[] thresholds = new long[FILTER_SLOTS];
    private final ConfigInventory config = ConfigInventory.configTypes(FILTER_SLOTS, this::configureWatchers);
    private final long[] lastReportedValues = new long[FILTER_SLOTS];

    private IStackWatcher storageWatcher;
    private IStackWatcher craftingWatcher;
    private long lastUpdateTick = -1;

    private final IStorageWatcherNode stackWatcherNode = new IStorageWatcherNode() {
        @Override
        public void updateWatcher(IStackWatcher newWatcher) {
            storageWatcher = newWatcher;
            configureWatchers();
        }

        @Override
        public void onStackChange(AEKey what, long amount) {
            if (isUpgradedWith(AEItems.CRAFTING_CARD)) {
                return;
            }

            if (isUpgradedWith(AEItems.FUZZY_CARD) || !hasAnyConfiguredKey()) {
                long currentTick = TickHandler.instance().getCurrentTick();
                if (currentTick != lastUpdateTick) {
                    lastUpdateTick = currentTick;
                    var grid = getGridNode().getGrid();
                    if (grid != null) {
                        updateReportingValues(grid);
                    }
                }
                return;
            }

            boolean touched = false;
            for (int i = 0; i < FILTER_SLOTS; i++) {
                var key = config.getKey(i);
                if (key != null && key.equals(what)) {
                    lastReportedValues[i] = amount;
                    touched = true;
                }
            }

            if (touched) {
                MultiStorageLevelEmitterPart.this.lastReportedValue = pickLegacyDisplayValue();
                MultiStorageLevelEmitterPart.this.updateState();
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
    }

    public ConfigInventory getConfig() {
        return config;
    }

    public boolean isLogicAnd() {
        return logicAnd;
    }

    public void setLogicAnd(boolean logicAnd) {
        if (this.logicAnd != logicAnd) {
            this.logicAnd = logicAnd;
            updateState();
        }
    }

    public boolean isCompareGe(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return false;
        return (compareMask & (1 << slot)) != 0;
    }

    public void setCompareGe(int slot, boolean ge) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;

        short old = compareMask;
        if (ge) compareMask = (short) (compareMask | (1 << slot));
        else compareMask = (short) (compareMask & ~(1 << slot));

        if (old != compareMask) updateState();
    }

    public boolean isCraftEmitWhenCrafting(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return true;
        return (craftMask & (1 << slot)) != 0;
    }

    public void setCraftEmitWhenCrafting(int slot, boolean whenCrafting) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;

        short old = craftMask;
        if (whenCrafting) craftMask = (short) (craftMask | (1 << slot));
        else craftMask = (short) (craftMask & ~(1 << slot));

        if (old != craftMask) updateState();
    }

    public long getThreshold(int slot) {
        if (slot < 0 || slot >= FILTER_SLOTS) return 0;
        return thresholds[slot];
    }

    public void setThreshold(int slot, long value) {
        if (slot < 0 || slot >= FILTER_SLOTS) return;
        if (value < 0) value = 0;

        thresholds[slot] = value;

        if (isUpgradedWith(AEItems.FUZZY_CARD) || !hasAnyConfiguredKey()) {
            getMainNode().ifPresent(this::updateReportingValues);
        } else {
            updateState();
        }
    }

    private boolean hasAnyConfiguredKey() {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (config.getKey(i) != null) return true;
        }
        return false;
    }

    private long pickLegacyDisplayValue() {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (config.getKey(i) != null) return lastReportedValues[i];
        }
        return this.lastReportedValue;
    }

    private boolean evaluateStorageSlot(int slot, long amount) {
        long threshold = thresholds[slot];
        boolean ge = isCompareGe(slot);
        return ge == (amount >= threshold);
    }

    private boolean computeStorageOutput() {
        int active = 0;

        if (logicAnd) {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                var key = config.getKey(i);
                if (key == null) continue;
                active++;
                if (!evaluateStorageSlot(i, lastReportedValues[i])) return false;
            }
            return active != 0 || evaluateStorageSlot(0, this.lastReportedValue);
        } else {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                var key = config.getKey(i);
                if (key == null) continue;
                active++;
                if (evaluateStorageSlot(i, lastReportedValues[i])) return true;
            }
            return active == 0 && evaluateStorageSlot(0, this.lastReportedValue);
        }
    }

    private boolean evaluateCraftingSlot(int slot, boolean requesting) {
        boolean wantWhenCrafting = isCraftEmitWhenCrafting(slot);
        return wantWhenCrafting == requesting;
    }

    private boolean computeCraftingOutput(IGrid grid) {
        var crafting = grid.getCraftingService();
        int active = 0;

        if (logicAnd) {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                var key = config.getKey(i);
                if (key == null) continue;
                active++;

                boolean requesting = crafting.isRequesting(key);
                if (!evaluateCraftingSlot(i, requesting)) return false;
            }

            if (active == 0) {
                boolean any = crafting.isRequestingAny();
                return evaluateCraftingSlot(0, any);
            }
            return true;
        } else {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                var key = config.getKey(i);
                if (key == null) continue;
                active++;

                boolean requesting = crafting.isRequesting(key);
                if (evaluateCraftingSlot(i, requesting)) return true;
            }

            if (active == 0) {
                boolean any = crafting.isRequestingAny();
                return evaluateCraftingSlot(0, any);
            }
            return false;
        }
    }

    @Override
    protected boolean isLevelEmitterOn() {
        if (isClientSide()) {
            return super.isLevelEmitterOn();
        }

        if (!getMainNode().isActive()) {
            return false;
        }

        if (hasDirectOutput()) {
            return getDirectOutput();
        }

        boolean desired = computeStorageOutput();

        boolean invert = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        return invert != desired;
    }

    @Override
    protected final int getUpgradeSlots() {
        return 1;
    }

    @Override
    public final void upgradesChanged() {
        this.configureWatchers();
    }

    @Override
    protected boolean hasDirectOutput() {
        return isUpgradedWith(AEItems.CRAFTING_CARD);
    }

    @Override
    protected boolean getDirectOutput() {
        var grid = getMainNode().getGrid();
        return grid != null && computeCraftingOutput(grid);
    }

    @Override
    public List<IPatternDetails> getAvailablePatterns() {
        return List.of();
    }

    @Override
    public boolean pushPattern(IPatternDetails patternDetails, KeyCounter[] inputHolder) {
        return false;
    }

    @Override
    public boolean isBusy() {
        return true;
    }

    @Override
    public Set<AEKey> getEmitableItems() {
        if (isUpgradedWith(AEItems.CRAFTING_CARD)
                && getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE) == YesNo.YES) {
            var out = new HashSet<AEKey>();
            for (int i = 0; i < FILTER_SLOTS; i++) {
                var k = config.getKey(i);
                if (k != null) out.add(k);
            }
            return out;
        }
        return Set.of();
    }

    @Override
    protected void configureWatchers() {
        if (this.storageWatcher != null) this.storageWatcher.reset();
        if (this.craftingWatcher != null) this.craftingWatcher.reset();

        ICraftingProvider.requestUpdate(getMainNode());

        if (isUpgradedWith(AEItems.CRAFTING_CARD)) {
            if (this.craftingWatcher != null) {
                boolean any = false;
                for (int i = 0; i < FILTER_SLOTS; i++) {
                    var k = config.getKey(i);
                    if (k != null) {
                        any = true;
                        this.craftingWatcher.add(k);
                    }
                }
                if (!any) {
                    this.craftingWatcher.setWatchAll(true);
                }
            }
        } else {
            if (this.storageWatcher != null) {
                if (isUpgradedWith(AEItems.FUZZY_CARD) || !hasAnyConfiguredKey()) {
                    this.storageWatcher.setWatchAll(true);
                } else {
                    for (int i = 0; i < FILTER_SLOTS; i++) {
                        var k = config.getKey(i);
                        if (k != null) this.storageWatcher.add(k);
                    }
                }
            }
            getMainNode().ifPresent(this::updateReportingValues);
        }

        updateState();
    }

    private void updateReportingValues(IGrid grid) {
        var stacks = grid.getStorageService().getCachedInventory();
        Arrays.fill(this.lastReportedValues, 0);

        if (!hasAnyConfiguredKey()) {
            long limit = thresholds[0];
            this.lastReportedValue = 0;
            for (var st : stacks) {
                this.lastReportedValue += st.getLongValue();
                if (this.lastReportedValue >= limit) break;
            }
            updateState();
            return;
        }

        boolean fuzzy = isUpgradedWith(AEItems.FUZZY_CARD);
        var fzMode = getConfigManager().getSetting(Settings.FUZZY_MODE);

        for (int i = 0; i < FILTER_SLOTS; i++) {
            var key = config.getKey(i);
            if (key == null) continue;

            if (!fuzzy) {
                lastReportedValues[i] = stacks.get(key);
            } else {
                long limit = thresholds[i];
                long sum = 0;
                var fuzzyList = stacks.findFuzzy(key, fzMode);
                for (var st : fuzzyList) {
                    sum += st.getLongValue();
                    if (sum >= limit) break;
                }
                lastReportedValues[i] = sum;
            }
        }

        this.lastReportedValue = pickLegacyDisplayValue();
        updateState();
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        config.readFromChildTag(data, NBT_CONFIG);
        importSettings(SettingsFrom.MEMORY_CARD, data, null);
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        config.writeToChildTag(data, NBT_CONFIG);
        exportSettings(SettingsFrom.MEMORY_CARD, data);
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.MULTI_LEVEL_EMITTER_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

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

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);
        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }
        output.remove("reportingValue");
        output.putBoolean(NBT_LOGIC_AND, this.logicAnd);
        output.putShort(NBT_COMPARE_MASK, this.compareMask);
        output.putShort(NBT_CRAFT_MASK, this.craftMask);
        output.putLongArray(NBT_THRESHOLDS, this.thresholds);
        this.config.writeToChildTag(output, NBT_CONFIG);
        output.putString(NBT_RS_MODE, getConfigManager().getSetting(Settings.REDSTONE_EMITTER).name());
        output.putString(NBT_FUZZY_MODE, getConfigManager().getSetting(Settings.FUZZY_MODE).name());
        output.putString(NBT_CRAFT_VIA, getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE).name());
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }
        if (input.contains(NBT_LOGIC_AND, Tag.TAG_BYTE)) {
            this.logicAnd = input.getBoolean(NBT_LOGIC_AND);
        }
        if (input.contains(NBT_COMPARE_MASK, Tag.TAG_SHORT)) {
            this.compareMask = input.getShort(NBT_COMPARE_MASK);
        }
        if (input.contains(NBT_CRAFT_MASK, Tag.TAG_SHORT)) {
            this.craftMask = input.getShort(NBT_CRAFT_MASK);
        }
        if (input.contains(NBT_THRESHOLDS, Tag.TAG_LONG_ARRAY)) {
            long[] arr = input.getLongArray(NBT_THRESHOLDS);
            for (int i = 0; i < FILTER_SLOTS; i++) {
                this.thresholds[i] = (i < arr.length) ? Math.max(0, arr[i]) : 0L;
            }
        } else {
            Arrays.fill(this.thresholds, 0L);
        }
        if (input.contains(NBT_CONFIG)) {
            this.config.readFromChildTag(input, NBT_CONFIG);
        } else {
            this.config.clear();
        }
        try {
            if (input.contains(NBT_RS_MODE, Tag.TAG_STRING)) {
                var v = RedstoneMode.valueOf(input.getString(NBT_RS_MODE));
                getConfigManager().putSetting(Settings.REDSTONE_EMITTER, v);
            }
        } catch (Exception ignored) { }
        try {
            if (input.contains(NBT_FUZZY_MODE, Tag.TAG_STRING)) {
                var v = FuzzyMode.valueOf(input.getString(NBT_FUZZY_MODE));
                getConfigManager().putSetting(Settings.FUZZY_MODE, v);
            }
        } catch (Exception ignored) { }
        try {
            if (input.contains(NBT_CRAFT_VIA, Tag.TAG_STRING)) {
                var v = YesNo.valueOf(input.getString(NBT_CRAFT_VIA));
                getConfigManager().putSetting(Settings.CRAFT_VIA_REDSTONE, v);
            }
        } catch (Exception ignored) { }
        this.configureWatchers();
        this.updateState();
        this.getHost().markForSave();
    }

}
