package net.oktawia.crazyae2addons.parts;

import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.AppEng;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import appeng.util.SettingsFrom;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IAnalogLevelEmitterOutput;
import net.oktawia.crazyae2addons.util.TagMatcher;
import org.jetbrains.annotations.Nullable;

public class TagLevelEmitter extends AbstractLevelEmitterPart implements IAnalogLevelEmitterOutput {

    private static final String NBT_EXPRESSION = "expression";
    private static final String NBT_ANALOG_LOGARITHMIC_MODE = "analog_logarithmic_mode";

    @PartModels
    public static final PartModel MODEL_OFF_OFF = new PartModel(
            CrazyAddons.makeId("part/tag_level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_off")
    );

    @PartModels
    public static final PartModel MODEL_OFF_ON = new PartModel(
            CrazyAddons.makeId("part/tag_level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_on")
    );

    @PartModels
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(
            CrazyAddons.makeId("part/tag_level_emitter_base_off"),
            AppEng.makeId("part/level_emitter_status_has_channel")
    );

    @PartModels
    public static final PartModel MODEL_ON_OFF = new PartModel(
            CrazyAddons.makeId("part/tag_level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_off")
    );

    @PartModels
    public static final PartModel MODEL_ON_ON = new PartModel(
            CrazyAddons.makeId("part/tag_level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_on")
    );

    @PartModels
    public static final PartModel MODEL_ON_HAS_CHANNEL = new PartModel(
            CrazyAddons.makeId("part/tag_level_emitter_base_on"),
            AppEng.makeId("part/level_emitter_status_has_channel")
    );

    @Getter
    private String expression = "";

    private TagMatcher.Compiled compiledExpression = TagMatcher.Compiled.EMPTY;

    private boolean analogLogarithmicMode = false;

    @Nullable
    private IStackWatcher storageWatcher;

    private long lastUpdateTick = -1L;

    private final IStorageWatcherNode storageWatcherNode = new IStorageWatcherNode() {
        @Override
        public void updateWatcher(IStackWatcher newWatcher) {
            storageWatcher = newWatcher;
            configureWatchers();
        }

        @Override
        public void onStackChange(AEKey what, long amount) {
            if (!CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()) {
                return;
            }

            long currentTick = appeng.hooks.ticking.TickHandler.instance().getCurrentTick();
            if (currentTick == lastUpdateTick) {
                return;
            }

            lastUpdateTick = currentTick;

            var gridNode = getGridNode();
            IGrid grid = gridNode != null ? gridNode.getGrid() : null;
            if (grid != null) {
                updateMatchingCount(grid);
            }
        }
    };

    public TagLevelEmitter(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IStorageWatcherNode.class, storageWatcherNode);
        refreshCompiledExpression();
    }

    public void setExpression(String expression) {
        if (!CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()) {
            return;
        }

        String normalized = expression == null ? "" : expression.strip();
        if (this.expression.equals(normalized)) {
            return;
        }

        this.expression = normalized;
        refreshCompiledExpression();
        configureWatchers();
        markForSave();
    }

    private void refreshCompiledExpression() {
        this.compiledExpression = TagMatcher.compile(this.expression);
    }

    @Override
    protected void configureWatchers() {
        if (!CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()) {
            if (storageWatcher != null) {
                storageWatcher.reset();
            }

            this.lastReportedValue = 0L;
            updateState();
            return;
        }

        if (storageWatcher != null) {
            storageWatcher.reset();
            storageWatcher.setWatchAll(true);
        }

        getMainNode().ifPresent(this::updateMatchingCount);
        updateState();
    }

    private void updateMatchingCount(IGrid grid) {
        if (!CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()) {
            this.lastReportedValue = 0L;
            updateState();
            return;
        }

        var stacks = grid.getStorageService().getCachedInventory();
        long total = 0L;

        if (this.expression == null || this.expression.isBlank()) {
            for (var entry : stacks) {
                AEKey key = entry.getKey();

                if (key instanceof AEItemKey) {
                    total += entry.getLongValue();
                }
            }

            this.lastReportedValue = total;
            updateState();
            return;
        }

        if (!compiledExpression.isValid() || !compiledExpression.isNeedsTags()) {
            this.lastReportedValue = 0L;
            updateState();
            return;
        }

        try {
            for (var entry : stacks) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();

                boolean matches = false;
                if (key instanceof AEItemKey itemKey) {
                    matches = TagMatcher.doesItemMatch(itemKey, compiledExpression);
                } else if (key instanceof AEFluidKey fluidKey) {
                    matches = TagMatcher.doesFluidMatch(fluidKey, compiledExpression);
                }

                if (matches) {
                    total += amount;
                }
            }
        } finally {
            TagMatcher.ItemTagCache.clearThreadLocal();
            TagMatcher.FluidTagCache.clearThreadLocal();
        }

        this.lastReportedValue = total;
        updateState();
    }

    @Override
    protected boolean hasDirectOutput() {
        return false;
    }

    @Override
    protected boolean getDirectOutput() {
        return false;
    }

    @Override
    protected int getUpgradeSlots() {
        return 1;
    }

    @Override
    public void upgradesChanged() {
        configureWatchers();
        updateState();
        markForSave();
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()) {
            return true;
        }

        if (!isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.TAG_LEVEL_EMITTER_MENU.get(), player, MenuLocators.forPart(this));
        }

        return true;
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);

        if (data.contains(NBT_EXPRESSION, Tag.TAG_STRING)) {
            this.expression = data.getString(NBT_EXPRESSION);
        } else {
            this.expression = "";
        }

        this.analogLogarithmicMode = data.getBoolean(NBT_ANALOG_LOGARITHMIC_MODE);

        refreshCompiledExpression();
        configureWatchers();
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putString(NBT_EXPRESSION, this.expression);
        data.putBoolean(NBT_ANALOG_LOGARITHMIC_MODE, this.analogLogarithmicMode);
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);

        if (mode == SettingsFrom.MEMORY_CARD) {
            output.putString(NBT_EXPRESSION, this.expression);
            output.putBoolean(NBT_ANALOG_LOGARITHMIC_MODE, this.analogLogarithmicMode);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.MEMORY_CARD) {
            this.expression = input.contains(NBT_EXPRESSION, Tag.TAG_STRING)
                    ? input.getString(NBT_EXPRESSION)
                    : "";

            this.analogLogarithmicMode = input.getBoolean(NBT_ANALOG_LOGARITHMIC_MODE);

            refreshCompiledExpression();
            configureWatchers();
            markForSave();
        }
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

    @Override
    protected boolean isLevelEmitterOn() {
        return CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get() && super.isLevelEmitterOn();
    }

    @Override
    public boolean crazyAE2Addons$usesAnalogOutput() {
        return CrazyConfig.COMMON.TAG_LEVEL_EMITTER_ENABLED.get()
                && crazyAE2Addons$hasAnalogCard();
    }

    @Override
    public int crazyAE2Addons$getAnalogOutputSignal() {
        if (!crazyAE2Addons$usesAnalogOutput()) {
            return isLevelEmitterOn() ? 15 : 0;
        }

        long threshold = getReportingValue();
        long amount = this.lastReportedValue;

        int signal;

        if (threshold <= 0L) {
            signal = 15;
        } else if (amount <= 0L) {
            signal = 0;
        } else if (this.analogLogarithmicMode) {
            double scaled = ((double) amount * (16384.0D / (double) threshold)) + 1.0D;
            signal = (int) Math.ceil(Math.log(scaled) / Math.log(2.0D));
        } else {
            signal = (int) Math.floor((double) amount * 15.0D / (double) threshold);
        }

        signal = Math.max(0, Math.min(15, signal));

        if (getConfigManager().getSetting(Settings.REDSTONE_EMITTER) == RedstoneMode.LOW_SIGNAL) {
            signal = 15 - signal;
        }

        return signal;
    }

    public boolean crazyAE2Addons$hasAnalogCard() {
        return isUpgradedWith(CrazyItemRegistrar.ANALOG_CARD.get());
    }

    @Override
    public boolean crazyAE2Addons$isAnalogLogarithmicMode() {
        return this.analogLogarithmicMode;
    }

    @Override
    public void crazyAE2Addons$setAnalogLogarithmicMode(boolean value) {
        if (this.analogLogarithmicMode == value) {
            return;
        }

        this.analogLogarithmicMode = value;
        updateState();
        markForSave();
    }

    private void markForSave() {
        if (getHost() != null) {
            getHost().markForSave();
        }
    }
}