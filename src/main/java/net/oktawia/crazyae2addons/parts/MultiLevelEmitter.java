package net.oktawia.crazyae2addons.parts;

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
import appeng.helpers.IConfigInvHost;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedAccessor;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class MultiLevelEmitter extends AbstractLevelEmitterPart implements IConfigInvHost, ICraftingProvider {

    public static final int FILTER_SLOTS = 16;

    private static final String NBT_STATE = "state";
    private static final String NBT_UPGRADES = "upgrades";
    private static final String NBT_THRESHOLDS = "thresholds";
    private static final String NBT_REDSTONE_MODE = "redstone_mode";
    private static final String NBT_FUZZY_MODE = "fuzzy_mode";
    private static final String NBT_CRAFT_VIA_REDSTONE = "craft_via_redstone";

    @PartModels
    public static final PartModel MODEL_OFF_OFF = new PartModel(
            CrazyAddons.makeId("part/multi_storage_level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_off")
    );

    @PartModels
    public static final PartModel MODEL_OFF_ON = new PartModel(
            CrazyAddons.makeId("part/multi_storage_level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_on")
    );

    @PartModels
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(
            CrazyAddons.makeId("part/multi_storage_level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_has_channel")
    );

    @PartModels
    public static final PartModel MODEL_ON_OFF = new PartModel(
            CrazyAddons.makeId("part/multi_storage_level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_off")
    );

    @PartModels
    public static final PartModel MODEL_ON_ON = new PartModel(
            CrazyAddons.makeId("part/multi_storage_level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_on")
    );

    @PartModels
    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(
            CrazyAddons.makeId("part/multi_storage_level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_has_channel")
    );

    @Getter
    private final PartState state = new PartState(this);

    private final long[] lastReportedValues = new long[FILTER_SLOTS];

    @Nullable
    private IStackWatcher storageWatcher;

    @Nullable
    private IStackWatcher craftingWatcher;

    private long lastUpdateTick = -1L;

    private final IStorageWatcherNode storageWatcherNode = new IStorageWatcherNode() {
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
                    IGrid grid = getGridNode().getGrid();
                    if (grid != null) {
                        updateReportingValues(grid);
                    }
                }
                return;
            }

            boolean touched = false;
            for (int i = 0; i < FILTER_SLOTS; i++) {
                AEKey configured = getConfig().getKey(i);
                if (configured != null && configured.equals(what)) {
                    lastReportedValues[i] = amount;
                    touched = true;
                }
            }

            if (touched) {
                lastReportedValue = pickLegacyDisplayValue();
                updateState();
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

    public MultiLevelEmitter(IPartItem<?> partItem) {
        super(partItem);

        getMainNode().addService(IStorageWatcherNode.class, storageWatcherNode);
        getMainNode().addService(ICraftingWatcherNode.class, craftingWatcherNode);
        getMainNode().addService(ICraftingProvider.class, this);

        getConfigManager().registerSetting(Settings.CRAFT_VIA_REDSTONE, YesNo.NO);
        getConfigManager().registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
    }

    public ConfigInventory getConfig() {
        return state.getConfig();
    }

    public boolean isLogicAnd() {
        return state.isLogicAnd();
    }

    public int getCompareMask() {
        return state.getCompareMask() & 0xFFFF;
    }

    public int getCraftMask() {
        return state.getCraftMask() & 0xFFFF;
    }

    public void setLogicAnd(boolean logicAnd) {
        if (state.logicAnd != logicAnd) {
            state.logicAnd = logicAnd;
            onStateChanged();
        }
    }

    public boolean isCompareGe(int slot) {
        return isValidSlot(slot) && isBitSet(state.compareMask, slot);
    }

    public void setCompareGe(int slot, boolean ge) {
        if (!isValidSlot(slot)) {
            return;
        }

        short newMask = setMaskBit(state.compareMask, slot, ge);
        if (newMask != state.compareMask) {
            state.compareMask = newMask;
            onStateChanged();
        }
    }

    public boolean isCraftEmitWhenCrafting(int slot) {
        return !isValidSlot(slot) || isBitSet(state.craftMask, slot);
    }

    public void setCraftEmitWhenCrafting(int slot, boolean whenCrafting) {
        if (!isValidSlot(slot)) {
            return;
        }

        short newMask = setMaskBit(state.craftMask, slot, whenCrafting);
        if (newMask != state.craftMask) {
            state.craftMask = newMask;
            onStateChanged();
        }
    }

    public long getThreshold(int slot) {
        return isValidSlot(slot) ? state.thresholds[slot] : 0L;
    }

    public void setThreshold(int slot, long value) {
        if (!isValidSlot(slot)) {
            return;
        }

        long clamped = Math.max(0L, value);
        if (state.thresholds[slot] != clamped) {
            state.thresholds[slot] = clamped;
            onStateChanged();

            if (isUpgradedWith(AEItems.FUZZY_CARD) || !hasAnyConfiguredKey()) {
                getMainNode().ifPresent(this::updateReportingValues);
            } else {
                updateState();
            }
        }
    }

    private void onStateChanged() {
        markForSave();
        updateState();
    }

    @Override
    protected int getUpgradeSlots() {
        return 1;
    }

    @Override
    public void upgradesChanged() {
        configureWatchers();
    }

    private boolean hasAnyConfiguredKey() {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (getConfig().getKey(i) != null) {
                return true;
            }
        }
        return false;
    }

    private long pickLegacyDisplayValue() {
        for (int i = 0; i < FILTER_SLOTS; i++) {
            if (getConfig().getKey(i) != null) {
                return lastReportedValues[i];
            }
        }
        return lastReportedValue;
    }

    private boolean evaluateStorageSlot(int slot, long amount) {
        long threshold = state.thresholds[slot];
        boolean compareGreaterOrEqual = isCompareGe(slot);
        return compareGreaterOrEqual == (amount >= threshold);
    }

    private boolean computeStorageOutput() {
        int active = 0;

        if (state.logicAnd) {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                AEKey key = getConfig().getKey(i);
                if (key == null) {
                    continue;
                }

                active++;
                if (!evaluateStorageSlot(i, lastReportedValues[i])) {
                    return false;
                }
            }

            return active != 0 || evaluateStorageSlot(0, lastReportedValue);
        }

        for (int i = 0; i < FILTER_SLOTS; i++) {
            AEKey key = getConfig().getKey(i);
            if (key == null) {
                continue;
            }

            active++;
            if (evaluateStorageSlot(i, lastReportedValues[i])) {
                return true;
            }
        }

        return active == 0 && evaluateStorageSlot(0, lastReportedValue);
    }

    private boolean evaluateCraftingSlot(int slot, boolean requesting) {
        return isCraftEmitWhenCrafting(slot) == requesting;
    }

    private boolean computeCraftingOutput(IGrid grid) {
        var crafting = grid.getCraftingService();
        int active = 0;

        if (state.logicAnd) {
            for (int i = 0; i < FILTER_SLOTS; i++) {
                AEKey key = getConfig().getKey(i);
                if (key == null) {
                    continue;
                }

                active++;
                if (!evaluateCraftingSlot(i, crafting.isRequesting(key))) {
                    return false;
                }
            }

            return active != 0 || evaluateCraftingSlot(0, crafting.isRequestingAny());
        }

        for (int i = 0; i < FILTER_SLOTS; i++) {
            AEKey key = getConfig().getKey(i);
            if (key == null) {
                continue;
            }

            active++;
            if (evaluateCraftingSlot(i, crafting.isRequesting(key))) {
                return true;
            }
        }

        return active == 0 && evaluateCraftingSlot(0, crafting.isRequestingAny());
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
        boolean inverted = getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL;
        return inverted != desired;
    }

    @Override
    protected boolean hasDirectOutput() {
        return isUpgradedWith(AEItems.CRAFTING_CARD);
    }

    @Override
    protected boolean getDirectOutput() {
        IGrid grid = getMainNode().getGrid();
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
        if (!isUpgradedWith(AEItems.CRAFTING_CARD)
                || getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE) != YesNo.YES) {
            return Set.of();
        }

        Set<AEKey> result = new HashSet<>();
        for (int i = 0; i < FILTER_SLOTS; i++) {
            AEKey key = getConfig().getKey(i);
            if (key != null) {
                result.add(key);
            }
        }
        return result;
    }

    @Override
    protected void configureWatchers() {
        if (storageWatcher != null) {
            storageWatcher.reset();
        }
        if (craftingWatcher != null) {
            craftingWatcher.reset();
        }

        ICraftingProvider.requestUpdate(getMainNode());

        if (isUpgradedWith(AEItems.CRAFTING_CARD)) {
            configureCraftingWatcher();
        } else {
            configureStorageWatcher();
            getMainNode().ifPresent(this::updateReportingValues);
        }

        updateState();
    }

    private void configureCraftingWatcher() {
        if (craftingWatcher == null) {
            return;
        }

        boolean anyConfigured = false;
        for (int i = 0; i < FILTER_SLOTS; i++) {
            AEKey key = getConfig().getKey(i);
            if (key != null) {
                anyConfigured = true;
                craftingWatcher.add(key);
            }
        }

        if (!anyConfigured) {
            craftingWatcher.setWatchAll(true);
        }
    }

    private void configureStorageWatcher() {
        if (storageWatcher == null) {
            return;
        }

        if (isUpgradedWith(AEItems.FUZZY_CARD) || !hasAnyConfiguredKey()) {
            storageWatcher.setWatchAll(true);
            return;
        }

        for (int i = 0; i < FILTER_SLOTS; i++) {
            AEKey key = getConfig().getKey(i);
            if (key != null) {
                storageWatcher.add(key);
            }
        }
    }

    private void updateReportingValues(IGrid grid) {
        var stacks = grid.getStorageService().getCachedInventory();
        Arrays.fill(lastReportedValues, 0L);

        if (!hasAnyConfiguredKey()) {
            long limit = state.thresholds[0];
            lastReportedValue = 0L;

            for (var stack : stacks) {
                lastReportedValue += stack.getLongValue();
                if (lastReportedValue >= limit) {
                    break;
                }
            }

            updateState();
            return;
        }

        boolean fuzzy = isUpgradedWith(AEItems.FUZZY_CARD);
        FuzzyMode fuzzyMode = getConfigManager().getSetting(Settings.FUZZY_MODE);

        for (int i = 0; i < FILTER_SLOTS; i++) {
            AEKey key = getConfig().getKey(i);
            if (key == null) {
                continue;
            }

            if (!fuzzy) {
                lastReportedValues[i] = stacks.get(key);
                continue;
            }

            long sum = 0L;
            long limit = state.thresholds[i];
            for (var stack : stacks.findFuzzy(key, fuzzyMode)) {
                sum += stack.getLongValue();
                if (sum >= limit) {
                    break;
                }
            }
            lastReportedValues[i] = sum;
        }

        lastReportedValue = pickLegacyDisplayValue();
        updateState();
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);

        if (data.contains(NBT_STATE, Tag.TAG_COMPOUND)) {
            state.loadPersisted(data.getCompound(NBT_STATE));
        }

        getUpgrades().readFromNBT(data, NBT_UPGRADES);
        readSettings(data);
        configureWatchers();
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);

        data.put(NBT_STATE, state.savePersisted());
        getUpgrades().writeToNBT(data, NBT_UPGRADES);
        writeSettings(data);
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);

        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }

        output.remove("reportingValue");
        output.put(NBT_STATE, state.savePersisted());
        writeSettings(output);
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode != SettingsFrom.MEMORY_CARD) {
            return;
        }

        if (input.contains(NBT_STATE, Tag.TAG_COMPOUND)) {
            state.loadPersisted(input.getCompound(NBT_STATE));
        }
        readSettings(input);
        configureWatchers();
        updateState();
        markForSave();
    }

    private void writeSettings(CompoundTag tag) {
        writeEnum(tag, NBT_REDSTONE_MODE, getConfigManager().getSetting(Settings.REDSTONE_EMITTER));
        writeEnum(tag, NBT_FUZZY_MODE, getConfigManager().getSetting(Settings.FUZZY_MODE));
        writeEnum(tag, NBT_CRAFT_VIA_REDSTONE, getConfigManager().getSetting(Settings.CRAFT_VIA_REDSTONE));
    }

    private void readSettings(CompoundTag tag) {
        readEnum(tag, NBT_REDSTONE_MODE, RedstoneMode.class,
                value -> getConfigManager().putSetting(Settings.REDSTONE_EMITTER, value));
        readEnum(tag, NBT_FUZZY_MODE, FuzzyMode.class,
                value -> getConfigManager().putSetting(Settings.FUZZY_MODE, value));
        readEnum(tag, NBT_CRAFT_VIA_REDSTONE, YesNo.class,
                value -> getConfigManager().putSetting(Settings.CRAFT_VIA_REDSTONE, value));
    }

    private static <E extends Enum<E>> void writeEnum(CompoundTag tag, String key, E value) {
        tag.putString(key, value.name());
    }

    private static <E extends Enum<E>> void readEnum(
            CompoundTag tag,
            String key,
            Class<E> enumClass,
            Consumer<E> consumer
    ) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            return;
        }

        try {
            consumer.accept(Enum.valueOf(enumClass, tag.getString(key)));
        } catch (IllegalArgumentException ignored) {
        }
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
        boolean emitterOn = isLevelEmitterOn();

        if (isActive() && isPowered()) {
            return emitterOn ? MODEL_ON_HAS_CHANNEL : MODEL_OFF_HAS_CHANNEL;
        }
        if (isPowered()) {
            return emitterOn ? MODEL_ON_ON : MODEL_OFF_ON;
        }
        return emitterOn ? MODEL_ON_OFF : MODEL_OFF_OFF;
    }

    private void markForSave() {
        if (getHost() != null) {
            getHost().markForSave();
        }
    }

    private static boolean isValidSlot(int slot) {
        return slot >= 0 && slot < FILTER_SLOTS;
    }

    private static boolean isBitSet(short mask, int slot) {
        return (mask & (1 << slot)) != 0;
    }

    private static short setMaskBit(short mask, int slot, boolean enabled) {
        return enabled
                ? (short) (mask | (1 << slot))
                : (short) (mask & ~(1 << slot));
    }

    private static final class PartState implements IManaged {
        private static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PartState.class);

        private final MultiLevelEmitter owner;
        private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

        @Persisted
        @Getter
        private boolean logicAnd = false;

        @Persisted
        @Getter
        private short compareMask = 0;

        @Persisted
        @Getter
        private short craftMask = (short) 0xFFFF;

        @Getter
        private final long[] thresholds = new long[FILTER_SLOTS];

        @Persisted
        @LazyManaged
        @Getter
        private final ConfigInventory config;

        private PartState(MultiLevelEmitter owner) {
            this.owner = owner;
            this.config = ConfigInventory.configTypes(FILTER_SLOTS, owner::onConfigChanged);
        }

        public CompoundTag savePersisted() {
            CompoundTag tag = IManagedAccessor.readManagedFields(this, new CompoundTag());
            tag.putLongArray(NBT_THRESHOLDS, thresholds);
            return tag;
        }

        public void loadPersisted(CompoundTag tag) {
            Arrays.fill(thresholds, 0L);
            IManagedAccessor.writePersistedFields(tag, getSyncStorage().getPersistedFields());

            if (tag.contains(NBT_THRESHOLDS, Tag.TAG_LONG_ARRAY)) {
                long[] saved = tag.getLongArray(NBT_THRESHOLDS);
                for (int i = 0; i < FILTER_SLOTS; i++) {
                    thresholds[i] = i < saved.length ? Math.max(0L, saved[i]) : 0L;
                }
            }
        }

        @Override
        public ManagedFieldHolder getFieldHolder() {
            return MANAGED_FIELD_HOLDER;
        }

        @Override
        public IManagedStorage getSyncStorage() {
            return syncStorage;
        }

        @Override
        public void onChanged() {
            owner.markForSave();
        }
    }

    private void onConfigChanged() {
        markForSave();
        configureWatchers();
    }
}