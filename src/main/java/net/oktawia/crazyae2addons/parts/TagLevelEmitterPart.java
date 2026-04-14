package net.oktawia.crazyae2addons.parts;

import appeng.api.networking.IGrid;
import appeng.api.networking.IStackWatcher;
import appeng.api.networking.storage.IStorageWatcherNode;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.AppEng;
import appeng.hooks.ticking.TickHandler;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.automation.AbstractLevelEmitterPart;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.misc.TagMatcher;

public class TagLevelEmitterPart extends AbstractLevelEmitterPart {

    @PartModels
    public static final ResourceLocation MODEL_BASE_OFF = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_base_off");
    @PartModels
    public static final ResourceLocation MODEL_BASE_ON  = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_base_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_OFF         = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_status_off");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_ON          = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_status_on");
    @PartModels
    public static final ResourceLocation MODEL_STATUS_HAS_CHANNEL = new ResourceLocation(AppEng.MOD_ID, "part/level_emitter_status_has_channel");

    public static final PartModel MODEL_OFF_OFF         = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_OFF);
    public static final PartModel MODEL_OFF_ON          = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_ON);
    public static final PartModel MODEL_OFF_HAS_CHANNEL = new PartModel(MODEL_BASE_OFF, MODEL_STATUS_HAS_CHANNEL);
    public static final PartModel MODEL_ON_OFF          = new PartModel(MODEL_BASE_ON,  MODEL_STATUS_OFF);
    public static final PartModel MODEL_ON_ON           = new PartModel(MODEL_BASE_ON,  MODEL_STATUS_ON);
    public static final PartModel MODEL_ON_HAS_CHANNEL  = new PartModel(MODEL_BASE_ON,  MODEL_STATUS_HAS_CHANNEL);

    private static final String NBT_EXPRESSION = "tagExpr";

    private String expression = "";
    private TagMatcher.Compiled compiledExpr = TagMatcher.Compiled.EMPTY;
    private IStackWatcher storageWatcher;
    private long lastUpdateTick = -1;

    private final IStorageWatcherNode stackWatcherNode = new IStorageWatcherNode() {
        @Override
        public void updateWatcher(IStackWatcher newWatcher) {
            storageWatcher = newWatcher;
            configureWatchers();
        }

        @Override
        public void onStackChange(AEKey what, long amount) {
            long currentTick = TickHandler.instance().getCurrentTick();
            if (currentTick != lastUpdateTick) {
                lastUpdateTick = currentTick;
                var grid = getGridNode() != null ? getGridNode().getGrid() : null;
                if (grid != null) updateMatchingCount(grid);
            }
        }
    };

    public TagLevelEmitterPart(IPartItem<?> partItem) {
        super(partItem);
        getMainNode().addService(IStorageWatcherNode.class, stackWatcherNode);
    }

    public String getExpression() {
        return expression;
    }

    public void setExpression(String expr) {
        this.expression = expr == null ? "" : expr;
        this.compiledExpr = TagMatcher.compile(this.expression);
        configureWatchers();
    }

    @Override
    protected void configureWatchers() {
        if (storageWatcher != null) {
            storageWatcher.reset();
            storageWatcher.setWatchAll(true);
        }
        getMainNode().ifPresent(this::updateMatchingCount);
        updateState();
    }

    private void updateMatchingCount(IGrid grid) {
        if (!compiledExpr.isValid() || !compiledExpr.needsTags()) {
            this.lastReportedValue = 0;
            updateState();
            return;
        }

        var stacks = grid.getStorageService().getCachedInventory();
        long total = 0;

        try {
            for (var entry : stacks) {
                AEKey key = entry.getKey();
                long amount = entry.getLongValue();
                boolean matches = false;

                if (key instanceof AEItemKey itemKey) {
                    matches = TagMatcher.doesItemMatch(itemKey, compiledExpr);
                } else if (key instanceof AEFluidKey fluidKey) {
                    matches = TagMatcher.doesFluidMatch(fluidKey, compiledExpr);
                }

                if (matches) total += amount;
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
        throw new UnsupportedOperationException();
    }

    @Override
    protected int getUpgradeSlots() {
        return 0;
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        if (!isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.TAG_LEVEL_EMITTER_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        if (data.contains(NBT_EXPRESSION)) {
            this.expression = data.getString(NBT_EXPRESSION);
            this.compiledExpr = TagMatcher.compile(this.expression);
        }
        configureWatchers();
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putString(NBT_EXPRESSION, this.expression);
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
}
