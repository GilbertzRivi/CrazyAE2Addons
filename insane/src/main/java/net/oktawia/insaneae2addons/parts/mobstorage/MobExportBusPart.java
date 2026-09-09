package net.oktawia.insaneae2addons.parts.mobstorage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.Level;

import appeng.api.config.Actionable;
import appeng.api.config.SchedulingMode;
import appeng.api.config.Settings;
import appeng.api.networking.IGrid;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.core.AppEng;
import appeng.core.settings.TickRates;
import appeng.items.parts.PartModels;
import appeng.parts.PartModel;
import appeng.parts.automation.IOBusPart;

import net.oktawia.crazyae2addons.tracking.IResourceTrackingService;
import net.oktawia.crazyae2addons.tracking.ResourceTrackingGate;
import net.oktawia.crazyae2addons.tracking.UsageTarget;
import net.oktawia.insaneae2addons.defs.regs.InsaneMenuRegistrar;
import net.oktawia.insaneae2addons.mobstorage.MobKey;

public class MobExportBusPart extends IOBusPart {

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

    private static final int MAX_SPAWNS_PER_TICK = 1;

    private UsageTarget trackTarget;
    private String trackDesc;
    private AEKey trackIcon;

    private int nextSlot = 0;

    private long lastWorkGameTime = Long.MIN_VALUE;

    public MobExportBusPart(IPartItem<?> partItem) {
        super(TickRates.ExportBus, what -> what instanceof MobKey, partItem);
        getConfigManager().registerSetting(Settings.SCHEDULING_MODE, SchedulingMode.DEFAULT);
    }

    @Override
    protected MenuType<?> getMenuType() {
        return InsaneMenuRegistrar.MOB_EXPORT_BUS_MENU.get();
    }

    public static boolean canSpawn(Level level, BlockPos pos) {
        return level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir();
    }

    @Override
    protected boolean doBusWork(IGrid grid) {
        if (!(getLevel() instanceof ServerLevel level)) {
            return false;
        }
        int elapsedTicks = takeElapsedTicks(level);

        var spot = getBlockEntity().getBlockPos().relative(getSide());
        if (!canSpawn(level, spot)) {
            return false;
        }

        var schedulingMode = getConfigManager().getSetting(Settings.SCHEDULING_MODE);
        int budget = Math.min(getOperationsPerTick(), elapsedTicks * MAX_SPAWNS_PER_TICK);
        int spawned = 0;
        int x = 0;
        for (; x < availableSlots() && spawned < budget; x++) {
            int slot = getStartingSlot(schedulingMode, x);
            if (!(getConfig().getKey(slot) instanceof MobKey mobKey)) {
                continue;
            }
            while (spawned < budget && trySpawn(grid, level, spot, mobKey)) {
                spawned++;
            }
        }

        if (spawned > 0) {
            updateSchedulingMode(schedulingMode, x);
        }
        return spawned > 0;
    }

    private int takeElapsedTicks(ServerLevel level) {
        long now = level.getGameTime();
        long elapsed = this.lastWorkGameTime == Long.MIN_VALUE ? 1L : now - this.lastWorkGameTime;
        this.lastWorkGameTime = now;

        return (int) Math.max(1L, Math.min(TickRates.ExportBus.getMin(), elapsed));
    }

    private boolean trySpawn(IGrid grid, ServerLevel level, BlockPos spot, MobKey mobKey) {
        var inventory = grid.getStorageService().getInventory();
        var energy = grid.getEnergyService();
        if (StorageHelper.poweredExtraction(energy, inventory, mobKey, 1, source) <= 0) {
            return false;
        }

        var mob = mobKey.getEntityType().spawn(level, spot, MobSpawnType.COMMAND);
        if (mob == null) {
            StorageHelper.poweredInsert(energy, inventory, mobKey, 1, source, Actionable.MODULATE);
            return false;
        }

        level.sendParticles(ParticleTypes.FIREWORK, mob.getX(), mob.getY() + 1, mob.getZ(),
                20, 0.5, 0.5, 0.5, 0.01);
        trackConsumed(grid, level, mobKey);
        return true;
    }

    private void trackConsumed(IGrid grid, ServerLevel level, MobKey mobKey) {
        if (!ResourceTrackingGate.isEnabled()) {
            return;
        }
        var svc = grid.getService(IResourceTrackingService.class);
        if (svc == null) {
            return;
        }
        if (trackTarget == null) {
            var pos = getBlockEntity().getBlockPos().immutable();
            trackTarget = UsageTarget.machine(GlobalPos.of(level.dimension(), pos));
            trackDesc = "at " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
            trackIcon = AEItemKey.of(getPartItem().asItem());
        }
        svc.trackConsumption(mobKey, 1, trackTarget, trackDesc, trackIcon);
    }

    private int getStartingSlot(SchedulingMode schedulingMode, int x) {
        if (schedulingMode == SchedulingMode.RANDOM) {
            return getLevel().getRandom().nextInt(availableSlots());
        }
        if (schedulingMode == SchedulingMode.ROUNDROBIN) {
            return (nextSlot + x) % availableSlots();
        }
        return x;
    }

    private void updateSchedulingMode(SchedulingMode schedulingMode, int x) {
        if (schedulingMode == SchedulingMode.ROUNDROBIN) {
            nextSlot = (nextSlot + x) % availableSlots();
        }
    }

    @Override
    public void readFromNBT(CompoundTag extra) {
        super.readFromNBT(extra);
        nextSlot = extra.getInt("nextSlot");
    }

    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        extra.putInt("nextSlot", nextSlot);
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
}
