package net.oktawia.insaneae2addons.parts.nbt;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;

import appeng.api.behaviors.StackExportStrategy;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.core.AppEng;
import appeng.core.settings.TickRates;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.IOBusPart;
import appeng.parts.automation.StackWorldBehaviors;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;
import appeng.util.prioritylist.DefaultPriorityList;

import net.oktawia.crazyae2addons.tracking.IResourceTrackingService;
import net.oktawia.crazyae2addons.tracking.ResourceTrackingGate;
import net.oktawia.crazyae2addons.tracking.UsageTarget;
import net.oktawia.insaneae2addons.InsaneConfig;
import net.oktawia.insaneae2addons.defs.regs.InsaneMenuRegistrar;
import net.oktawia.insaneae2addons.logic.nbt.NBTMatcher;

public class NbtExportBusPart extends IOBusPart {

    public static final ResourceLocation MODEL_BASE = new ResourceLocation(AppEng.MOD_ID, "part/export_bus_base");

    @PartModels
    public static final IPartModel MODELS_OFF = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_off"));
    @PartModels
    public static final IPartModel MODELS_ON = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_on"));
    @PartModels
    public static final IPartModel MODELS_HAS_CHANNEL = new PartModel(MODEL_BASE,
            new ResourceLocation(AppEng.MOD_ID, "part/export_bus_has_channel"));

    private static final String NBT_STATE = "state";
    private static final String NBT_FILTER = "nbt_filter";

    public final ConfigInventory inv = ConfigInventory.configTypes(1, () -> {
    });

    private final NbtFilterState state = new NbtFilterState(this::markForSaveSafe);

    private NBTMatcher.Compiled matcher = NBTMatcher.compile("");
    private String matcherSource = "";

    @Nullable
    private StackExportStrategy exportStrategy;

    private UsageTarget trackTarget;
    private String trackDesc;
    private AEKey trackIcon;

    public NbtExportBusPart(IPartItem<?> partItem) {
        super(TickRates.ExportBus, StackWorldBehaviors.hasExportStrategyFilter(), partItem);
    }

    public String getData() {
        return state.getData();
    }

    public void setFilter(String expression) {
        state.setData(expression);
        if (getHost() != null) {
            getHost().markForSave();
            getHost().markForUpdate();
        }
    }

    private void markForSaveSafe() {
        if (getHost() != null) {
            getHost().markForSave();
        }
    }

    private NBTMatcher.Compiled matcher() {
        String data = state.getData();
        if (!matcherSource.equals(data)) {
            matcher = NBTMatcher.compile(data);
            matcherSource = data;
        }
        return matcher;
    }

    @Override
    protected MenuType<?> getMenuType() {
        return InsaneMenuRegistrar.NBT_EXPORT_BUS_MENU.get();
    }

    @Override
    protected boolean doBusWork(IGrid grid) {
        if (!InsaneConfig.COMMON.NBT_EXPORT_BUS_ENABLED.get()) {
            return false;
        }
        var storageService = grid.getStorageService();
        var context = new InsaneStackTransferContext(
                storageService, grid.getEnergyService(), this.source,
                getOperationsPerTick(), DefaultPriorityList.INSTANCE);

        var stacks = storageService.getCachedInventory();
        if (stacks.isEmpty()) {
            return false;
        }

        var compiled = matcher();
        List<AEItemKey> matching = new ArrayList<>();
        for (var entry : stacks) {
            if (entry.getKey() instanceof AEItemKey itemKey && NBTMatcher.doesItemMatch(itemKey, compiled)) {
                matching.add(itemKey);
            }
        }
        if (matching.isEmpty()) {
            return false;
        }

        boolean didWork = false;
        for (AEItemKey itemKey : matching) {
            int transferFactor = InsaneConfig.COMMON.NBT_EXPORT_BUS_TRANSFER_FACTOR.get();
            long amount = (long) context.getOperationsRemaining() * transferFactor;
            long transferred = getExportStrategy().transfer(context, itemKey, amount);
            if (transferred > 0) {
                context.reduceOperationsRemaining(Math.max(1, transferred / transferFactor));
                trackConsumed(grid, itemKey, transferred);
                didWork = true;
            }
            if (!context.hasOperationsLeft()) {
                break;
            }
        }
        return didWork;
    }

    private void trackConsumed(IGrid grid, AEKey what, long amount) {
        if (!ResourceTrackingGate.isEnabled()) {
            return;
        }
        var svc = grid.getService(IResourceTrackingService.class);
        if (svc == null) {
            return;
        }
        if (trackTarget == null) {
            var pos = getBlockEntity().getBlockPos().immutable();
            trackTarget = UsageTarget.machine(GlobalPos.of(getLevel().dimension(), pos));
            trackDesc = "at " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
            trackIcon = AEItemKey.of(getPartItem().asItem());
        }
        svc.trackConsumption(what, amount, trackTarget, trackDesc, trackIcon);
    }

    private StackExportStrategy getExportStrategy() {
        if (exportStrategy == null) {
            var self = getHost().getBlockEntity();
            var fromPos = self.getBlockPos().relative(getSide());
            var fromSide = getSide().getOpposite();
            exportStrategy = StackWorldBehaviors.createExportFacade((ServerLevel) getLevel(), fromPos, fromSide);
        }
        return exportStrategy;
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(4, 4, 12, 12, 12, 14);
        bch.addBox(5, 5, 14, 11, 11, 15);
        bch.addBox(6, 6, 15, 10, 10, 16);
        bch.addBox(6, 6, 11, 10, 10, 12);
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);

        if (mode == SettingsFrom.MEMORY_CARD) {
            output.putString(NBT_FILTER, state.getData());
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.MEMORY_CARD && input.contains(NBT_FILTER, Tag.TAG_STRING)) {
            setFilter(input.getString(NBT_FILTER));
        }
    }

    @Override
    public void readFromNBT(CompoundTag extra) {
        super.readFromNBT(extra);
        if (extra.contains(NBT_STATE, Tag.TAG_COMPOUND)) {
            state.loadPersisted(extra.getCompound(NBT_STATE));
        }
    }

    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        extra.put(NBT_STATE, state.savePersisted());
    }

    @Override
    public void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeNbt(state.saveSync(true));
    }

    @Override
    public boolean readFromStream(FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);
        CompoundTag tag = data.readNbt();
        if (tag != null) {
            state.loadSync(tag);
        }
        return changed;
    }
}
