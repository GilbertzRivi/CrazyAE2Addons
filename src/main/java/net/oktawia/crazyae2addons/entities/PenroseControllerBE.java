package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.BlackHoleManager;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.PenroseControllerMenu;
import net.oktawia.crazyae2addons.misc.PenroseValidator;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.renderer.preview.Previewable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PenroseControllerBE extends AENetworkInvBlockEntity
        implements Previewable, MenuProvider, IUpgradeableObject, IGridTickable {

    public static final Set<PenroseControllerBE> CLIENT_INSTANCES = new HashSet<>();

    /*
     * ===== Units glossary =====
     * - MU: internal "mass unit" used by controller accounting (BH mass + disk mass)
     * - singu: count of Super Singularity items fed into the system (integer)
     * - MK: internal heat unit used by controller ("heat" variable)
     */

    private static final double DEF_MASS_FACTOR_MAX         = 2.0;

    private static final double DEF_DUTY_COMPENSATION       = 4.0 / 3.0;

    private static final double DEF_FE_BASE_PER_SINGU_FLOW  = (double) (1L << 27) * 0.5;

    private static final double DEF_HEAT_PER_SINGU_FLOW     = 0.5;

    private static final double DEF_HEAT_PEAK_GK            = 50_000.0;
    private static final double DEF_MAX_HEAT_GK             = 100_000.0;


    /**
     * MU gained by disk injection per 1 singularity item.
     * (Units: MU / singu)
     */
    public static final long MU_PER_SINGU = 1L;

    // ===== DISK WINDOW =====
    /** Disk history length (ticks). Default: 2400 ticks = 120s */
    private static final int DISK_WINDOW_TICKS = 20 * 120;

    /** Mean "orbit" delay used as smoothing window (ticks). Default: 1200 ticks = 60s */
    private static final int DISK_MEAN_TICKS   = DISK_WINDOW_TICKS / 2;

    /**
     * Ring-buffer pointer to the newest slot (age=0).
     * Range: [0..DISK_WINDOW_TICKS-1]
     */
    private int diskAge0Ptr = 0;

    /**
     * diskBatchMuByAge[i] = MU injected at that age slot (double for sub-MU precision)
     */
    private final double[] diskBatchMuByAge = new double[DISK_WINDOW_TICKS];

    /**
     * diskMassMu = total MU currently stored in the disk ring-buffer (double for sub-MU precision)
     */
    private double diskMassMu = 0.0;

    /**
     * DISK_DELTA_FRAC[age] = fraction of a given age bucket that leaves disk this tick.
     * Derived from a normalized Gaussian CDF window.
     */
    private static final double[] DISK_DELTA_FRAC = new double[DISK_WINDOW_TICKS];

    /** Gaussian mean for disk leaving distribution (ticks) */
    private static final double GAUSS_MU_TICKS    = DISK_WINDOW_TICKS / 2.0;

    /** Gaussian sigma for disk leaving distribution (ticks) */
    private static final double GAUSS_SIGMA_TICKS = 20.0 * 20.0;

    static {
        final double sqrt2 = Math.sqrt(2.0);
        final double mu = GAUSS_MU_TICKS;
        final double sigma = GAUSS_SIGMA_TICKS;

        java.util.function.DoubleUnaryOperator cdf = (x) -> {
            // z = normalized distance from mean (dimensionless)
            double z = (x - mu) / (sigma * sqrt2);
            return 0.5 * (1.0 + Utils.erf(z));
        };

        double c0 = cdf.applyAsDouble(0.0);
        double cW = cdf.applyAsDouble(DISK_WINDOW_TICKS);
        double denom = cW - c0;
        if (denom <= 0.0) denom = 1.0;

        double sum = 0.0;
        for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
            double a = cdf.applyAsDouble(i);
            double b = cdf.applyAsDouble(i + 1);

            double d = (b - a) / denom;
            if (d < 0.0) d = 0.0;
            DISK_DELTA_FRAC[i] = d;
            sum += d;
        }

        if (sum > 0.0) {
            for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
                DISK_DELTA_FRAC[i] /= sum;
            }
        }
    }

    /**
     * Fractional BH mass remainder (sub-MU precision).
     * bhMass + bhMassRemainder == exact mass.
     */
    private double bhMassRemainder = 0.0;

    // ===== ENERGY =====
    private static final long MAX_STORED_ENERGY = Long.MAX_VALUE;
    private long storedEnergy = 0L;

    // >>> UI telemetry
    private long storedEnergyInDisk = 0L;
    private long lastGeneratedFePerTickGross = 0L; // FE/t (gross)
    private long lastConsumedFePerTick = 0L;       // FE/t (cost)

    public boolean preview = false;

    private PreviewInfo previewInfo = null;
    private boolean formed = false;

    private boolean blackHoleActive = false;
    private long   bhMass = 0L;   // MU
    private double heat   = 0.0;  // GK

    /**
     * pendingFeedMu = pending singularity items for injection.
     * Units: items (singu)
     */
    private int    pendingFeedMu      = 0;

    /**
     * pendingCoolingHeat = cooling that should be applied (accumulated between ticks).
     * Units: GK
     */
    private double pendingCoolingHeat = 0.0;

    /**
     * pendingEvaporation = hawking evaporation that should be applied (accumulated between ticks).
     * Units: MU
     */
    private long   pendingEvaporation = 0L;

    private long lastSecondMassDelta = 0L;
    private int  secTickCounter = 0;
    private long secMassDeltaAcc = 0L;

    private int structureTickCounter = 0;

    // FE output ports
    private final Set<PenrosePortBE> ports = new HashSet<>();

    // children ticked by controller
    private final Set<PenroseInjectionPortBE> injectionPorts = new HashSet<>();
    private final Set<PenroseHeatVentBE> heatVents = new HashSet<>();
    private final Set<PenroseHawkingVentBE> hawkingVents = new HashSet<>();

    private int lastFeedMu = 0;
    private int lastAccretionSingu = 0;

    private int ventingLockTicks = 0;

    public boolean isVentingLocked() {
        return ventingLockTicks > 0 || pendingEvaporation > 0;
    }

    public void requestVentingLock(int ticks) {
        bumpVentingLock(Math.max(1, ticks));
    }

    private void bumpVentingLock(int ticks) {
        if (ticks > ventingLockTicks) ventingLockTicks = ticks;
    }

    private final LazyOptional<IEnergyStorage> energyCap =
            LazyOptional.of(() -> new IEnergyStorage() {
                @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
                @Override public boolean canReceive() { return false; }

                @Override
                public int extractEnergy(int maxExtract, boolean simulate) {
                    if (maxExtract <= 0 || storedEnergy <= 0) return 0;
                    long available = Math.min(storedEnergy, Integer.MAX_VALUE);
                    int toExtract = (int) Math.min((long) maxExtract, available);
                    if (!simulate) {
                        storedEnergy -= toExtract;
                        if (storedEnergy < 0) storedEnergy = 0;
                    }
                    return toExtract;
                }

                @Override public boolean canExtract() { return true; }
                @Override public int getEnergyStored() { return (int) Math.min(storedEnergy, Integer.MAX_VALUE); }
                @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
            });

    public PenroseValidator validator;

    public AppEngInternalInventory diskInv = new AppEngInternalInventory(this, 1, 1, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (stack.getItem() == AEItems.ITEM_CELL_1K.asItem() ||
                stack.getItem() == AEItems.ITEM_CELL_4K.asItem() ||
                stack.getItem() == AEItems.ITEM_CELL_16K.asItem() ||
                stack.getItem() == AEItems.ITEM_CELL_64K.asItem() ||
                stack.getItem() == AEItems.ITEM_CELL_256K.asItem() ){
                var cellInv = StorageCells.getCellInventory(stack, null);
                if (cellInv == null) return false;
                var stacks = cellInv.getAvailableStacks();
                if (stacks.isEmpty()) return true;
                return stacks.size() == 1
                        && Objects.equals(stacks.getFirstKey(),
                        AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
            }
            return false;
        }
    });

    public ConfigInventory config = ConfigInventory.configTypes(
            key -> key instanceof AEItemKey itemkey
                    && itemkey.toStack().getItem() != CrazyItemRegistrar.SUPER_SINGULARITY.get(),
            1, () -> {});

    public PenroseControllerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.PENROSE_CONTROLLER_BE.get(), pos, blockState);
        validator = new PenroseValidator();
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.PENROSE_CONTROLLER.get().asItem()));
    }

    // === PREVIEW ===
    @Override @OnlyIn(Dist.CLIENT)
    public PreviewInfo getPreviewInfo() { return previewInfo; }

    @Override @OnlyIn(Dist.CLIENT)
    public void setPreviewInfo(PreviewInfo info) { this.previewInfo = info; }

    // === Config helpers (clamped) ===
    private static long cfgStartupCostSingu() {
        long v = CrazyConfig.COMMON.PenroseStartupCostSingu.get();
        return Math.max(0L, v);
    }

    public static long cfgInitialMassMu() {
        long v = CrazyConfig.COMMON.PenroseInitialMassMu.get();
        return Math.max(0L, v);
    }

    private static long cfgMassWindowMu() {
        long v = CrazyConfig.COMMON.PenroseMassWindowMu.get();
        return Math.max(0L, v);
    }

    public static long cfgMaxMassMu() {
        long a = cfgInitialMassMu();
        long w = cfgMassWindowMu();
        if (a > Long.MAX_VALUE - w) return Long.MAX_VALUE;
        return a + w;
    }

    private static long cfgSweetSpotMassMu() {
        return cfgInitialMassMu() + (cfgMassWindowMu() / 2L);
    }

    private static double cfgMassFactorMax() {
        double v = CrazyConfig.COMMON.PenroseMassFactorMax.get();
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 1.0) return DEF_MASS_FACTOR_MAX;
        return v;
    }

    private static double cfgDutyComp() {
        double v = CrazyConfig.COMMON.PenroseDutyCompensation.get();
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) return DEF_DUTY_COMPENSATION;
        return v;
    }

    private static double cfgFeBasePerSinguFlow() {
        double v = CrazyConfig.COMMON.PenroseFeBasePerSinguFlow.get();
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) return DEF_FE_BASE_PER_SINGU_FLOW;
        return v;
    }

    private static double cfgHeatPerSinguFlow() {
        double v = CrazyConfig.COMMON.PenroseHeatPerSinguFlow.get();
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) return DEF_HEAT_PER_SINGU_FLOW;
        return v;
    }

    private static double cfgHeatPeakGK() {
        double v = CrazyConfig.COMMON.PenroseHeatPeakMK.get();
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) return DEF_HEAT_PEAK_GK;
        return v;
    }

    private static double cfgMaxHeatGK() {
        double v = CrazyConfig.COMMON.PenroseMaxHeatMK.get();
        if (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) return DEF_MAX_HEAT_GK;
        return v;
    }

    private static int cfgMaxFeedPerTick() {
        int v = CrazyConfig.COMMON.PenroseMaxFeedPerTick.get();
        return Math.max(0, v);
    }

    // === NBT ===
    private static final double NBT_SCALE = 1_000_000.0;

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.config.readFromChildTag(data, "config");
        this.diskInv.readFromNBT(data, "diskinv");

        if (data.contains("energy")) this.storedEnergy = data.getLong("energy");
        if (data.contains("energyDisk")) this.storedEnergyInDisk = data.getLong("energyDisk");

        // backward compat:
        if (data.contains("lastGenFePerTickGross")) this.lastGeneratedFePerTickGross = data.getLong("lastGenFePerTickGross");
        else if (data.contains("lastGenFePerTick")) this.lastGeneratedFePerTickGross = data.getLong("lastGenFePerTick");
        if (data.contains("lastConsumedFePerTick")) this.lastConsumedFePerTick = data.getLong("lastConsumedFePerTick");

        if (data.contains("bhActive")) this.blackHoleActive = data.getBoolean("bhActive");
        if (data.contains("bhMass")) this.bhMass = data.getLong("bhMass");
        if (data.contains("heat")) this.heat = data.getDouble("heat");

        if (data.contains("lastSecondMassDelta")) this.lastSecondMassDelta = data.getLong("lastSecondMassDelta");
        if (data.contains("lastAccretionSingu")) this.lastAccretionSingu = data.getInt("lastAccretionSingu");
        if (data.contains("lastFeedMu")) this.lastFeedMu = data.getInt("lastFeedMu");
        if (data.contains("ventingLockTicks")) this.ventingLockTicks = data.getInt("ventingLockTicks");

        clearDisk();

        if (data.contains("diskAge0Ptr")) {
            this.diskAge0Ptr = data.getInt("diskAge0Ptr");
            if (diskAge0Ptr < 0 || diskAge0Ptr >= DISK_WINDOW_TICKS) diskAge0Ptr = 0;
        }
        if (data.contains("diskMassMicro")) {
            long m = data.getLong("diskMassMicro");
            this.diskMassMu = (double) m / NBT_SCALE;
            if (diskMassMu < 0) diskMassMu = 0.0;
        }
        if (data.contains("bhRemMicro")) {
            long rr = data.getLong("bhRemMicro");
            this.bhMassRemainder = (double) rr / NBT_SCALE;
            if (bhMassRemainder < 0) bhMassRemainder = 0.0;
        }
        if (data.contains("diskBatchesMicro")) {
            long[] arr = data.getLongArray("diskBatchesMicro");
            if (arr.length == DISK_WINDOW_TICKS) {
                for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
                    diskBatchMuByAge[i] = (double) arr[i] / NBT_SCALE;
                }
            }
        }

        secTickCounter = 0;
        secMassDeltaAcc = 0L;
        structureTickCounter = 0;

        recomputeUiEnergy();
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.config.writeToChildTag(data, "config");
        this.diskInv.writeToNBT(data, "diskinv");

        data.putLong("energy", this.storedEnergy);
        data.putLong("energyDisk", this.storedEnergyInDisk);

        data.putLong("lastGenFePerTickGross", this.lastGeneratedFePerTickGross);
        data.putLong("lastConsumedFePerTick", this.lastConsumedFePerTick);
        // backward compat:
        data.putLong("lastGenFePerTick", this.lastGeneratedFePerTickGross);

        data.putBoolean("bhActive", this.blackHoleActive);
        data.putLong("bhMass", this.bhMass);
        data.putDouble("heat", this.heat);

        data.putLong("lastSecondMassDelta", this.lastSecondMassDelta);
        data.putInt("lastAccretionSingu", this.lastAccretionSingu);
        data.putInt("lastFeedMu", this.lastFeedMu);
        data.putInt("ventingLockTicks", this.ventingLockTicks);

        data.putInt("diskAge0Ptr", this.diskAge0Ptr);
        data.putLong("diskMassMicro", (long) Math.round(this.diskMassMu * NBT_SCALE));
        data.putLong("bhRemMicro", (long) Math.round(this.bhMassRemainder * NBT_SCALE));

        long[] arr = new long[DISK_WINDOW_TICKS];
        for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
            arr[i] = (long) Math.round(diskBatchMuByAge[i] * NBT_SCALE);
        }
        data.putLongArray("diskBatchesMicro", arr);
    }

    public CompoundTag makePortableTag() {
        CompoundTag tag = new CompoundTag();
        // zapis tylko naszych danych (bez danych AE2 noda)
        this.config.writeToChildTag(tag, "config");
        this.diskInv.writeToNBT(tag, "diskinv");

        tag.putLong("energy", this.storedEnergy);
        tag.putLong("energyDisk", this.storedEnergyInDisk);

        tag.putLong("lastGenFePerTickGross", this.lastGeneratedFePerTickGross);
        tag.putLong("lastConsumedFePerTick", this.lastConsumedFePerTick);
        tag.putLong("lastGenFePerTick", this.lastGeneratedFePerTickGross);

        tag.putBoolean("bhActive", this.blackHoleActive);
        tag.putLong("bhMass", this.bhMass);
        tag.putDouble("heat", this.heat);

        tag.putLong("lastSecondMassDelta", this.lastSecondMassDelta);
        tag.putInt("lastAccretionSingu", this.lastAccretionSingu);
        tag.putInt("lastFeedMu", this.lastFeedMu);
        tag.putInt("ventingLockTicks", this.ventingLockTicks);

        tag.putInt("diskAge0Ptr", this.diskAge0Ptr);
        tag.putLong("diskMassMicro", (long) Math.round(this.diskMassMu * NBT_SCALE));
        tag.putLong("bhRemMicro", (long) Math.round(this.bhMassRemainder * NBT_SCALE));

        long[] arr = new long[DISK_WINDOW_TICKS];
        for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
            arr[i] = (long) Math.round(diskBatchMuByAge[i] * NBT_SCALE);
        }
        tag.putLongArray("diskBatchesMicro", arr);

        return tag;
    }

    public void applyPortableTag(@Nullable CompoundTag data) {
        if (data == null) return;

        this.config.readFromChildTag(data, "config");
        this.diskInv.readFromNBT(data, "diskinv");

        if (data.contains("energy")) this.storedEnergy = data.getLong("energy");
        if (data.contains("energyDisk")) this.storedEnergyInDisk = data.getLong("energyDisk");

        if (data.contains("lastGenFePerTickGross")) this.lastGeneratedFePerTickGross = data.getLong("lastGenFePerTickGross");
        else if (data.contains("lastGenFePerTick")) this.lastGeneratedFePerTickGross = data.getLong("lastGenFePerTick");
        if (data.contains("lastConsumedFePerTick")) this.lastConsumedFePerTick = data.getLong("lastConsumedFePerTick");

        if (data.contains("bhActive")) this.blackHoleActive = data.getBoolean("bhActive");
        if (data.contains("bhMass")) this.bhMass = data.getLong("bhMass");
        if (data.contains("heat")) this.heat = data.getDouble("heat");

        if (data.contains("lastSecondMassDelta")) this.lastSecondMassDelta = data.getLong("lastSecondMassDelta");
        if (data.contains("lastAccretionSingu")) this.lastAccretionSingu = data.getInt("lastAccretionSingu");
        if (data.contains("lastFeedMu")) this.lastFeedMu = data.getInt("lastFeedMu");
        if (data.contains("ventingLockTicks")) this.ventingLockTicks = data.getInt("ventingLockTicks");

        clearDisk();

        if (data.contains("diskAge0Ptr")) {
            this.diskAge0Ptr = data.getInt("diskAge0Ptr");
            if (diskAge0Ptr < 0 || diskAge0Ptr >= DISK_WINDOW_TICKS) diskAge0Ptr = 0;
        }
        if (data.contains("diskMassMicro")) {
            long m = data.getLong("diskMassMicro");
            this.diskMassMu = (double) m / NBT_SCALE;
            if (diskMassMu < 0) diskMassMu = 0.0;
        }
        if (data.contains("bhRemMicro")) {
            long rr = data.getLong("bhRemMicro");
            this.bhMassRemainder = (double) rr / NBT_SCALE;
            if (bhMassRemainder < 0) bhMassRemainder = 0.0;
        }
        if (data.contains("diskBatchesMicro")) {
            long[] arr = data.getLongArray("diskBatchesMicro");
            if (arr.length == DISK_WINDOW_TICKS) {
                for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
                    diskBatchMuByAge[i] = (double) arr[i] / NBT_SCALE;
                }
            }
        }

        secTickCounter = 0;
        secMassDeltaAcc = 0L;
        structureTickCounter = 0;

        recomputeUiEnergy();
        setChanged();
    }

    private void clearDisk() {
        diskAge0Ptr = 0;
        diskMassMu = 0.0;
        bhMassRemainder = 0.0;
        for (int i = 0; i < DISK_WINDOW_TICKS; i++) diskBatchMuByAge[i] = 0.0;
        lastAccretionSingu = 0;
        lastFeedMu = 0;

        storedEnergyInDisk = 0L;
        lastGeneratedFePerTickGross = 0L;
        lastConsumedFePerTick = 0L;
    }

    // === LIFECYCLE ===
    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) CLIENT_INSTANCES.add(this);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_INSTANCES.remove(this);
        ports.clear();
        injectionPorts.clear();
        heatVents.clear();
        hawkingVents.clear();
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, java.util.List<ItemStack> drops) {
        ItemStack stack = CrazyBlockRegistrar.PENROSE_CONTROLLER.get().asItem().getDefaultInstance();
        stack.addTagElement("BlockEntityTag", makePortableTag());
        drops = List.of(stack);
        super.addAdditionalDrops(level, pos, drops);
    }

    // === MENU ===
    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PenroseControllerMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.penrose_controller");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(), player, locator);
    }

    // === GUI helpers ===
    public long getStoredEnergy() { return storedEnergy; }
    public long getStoredEnergyInDisk() { return storedEnergyInDisk; }
    public long getLastGeneratedFePerTickGross() { return lastGeneratedFePerTickGross; }
    public long getLastConsumedFePerTick() { return lastConsumedFePerTick; }

    public long getLastSecondMassDelta() { return lastSecondMassDelta; }

    public long getDiskMassSingu() { return (long) Math.max(0.0, Math.round(diskMassMu)); }
    public int getLastAccretionSinguPerTick() { return lastAccretionSingu; }
    public int getLastFeedMu() { return lastFeedMu; }
    public int getOrbitDelaySeconds() { return DISK_MEAN_TICKS / 20; }

    /**
     * Disk flow in "singu per tick" equivalent:
     * diskFlow = diskMassMu / DISK_MEAN_TICKS
     */
    public int getDiskFlowSinguPerTick() {
        double flow = diskMassMu / (double) DISK_MEAN_TICKS;
        if (flow < 0) flow = 0;
        if (flow > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.round(flow);
    }

    public long getInitialBhMass() { return cfgInitialMassMu(); }
    public long getMaxBhMass() { return cfgMaxMassMu(); }
    public long getSweetSpotBhMass() { return cfgSweetSpotMassMu(); }
    public double getMaxHeatGK() { return cfgMaxHeatGK(); }

    // === FE output ports ===
    public void registerPort(PenrosePortBE port) { if (port != null) ports.add(port); }
    public void unregisterPort(PenrosePortBE port) { ports.remove(port); }

    // === children registration ===
    public void registerInjectionPort(PenroseInjectionPortBE be) { if (be != null) injectionPorts.add(be); }
    public void unregisterInjectionPort(PenroseInjectionPortBE be) { injectionPorts.remove(be); }

    public void registerHeatVent(PenroseHeatVentBE be) { if (be != null) heatVents.add(be); }
    public void unregisterHeatVent(PenroseHeatVentBE be) { heatVents.remove(be); }

    public void registerHawkingVent(PenroseHawkingVentBE be) { if (be != null) hawkingVents.add(be); }
    public void unregisterHawkingVent(PenroseHawkingVentBE be) { hawkingVents.remove(be); }

    private <T extends BlockEntity> ArrayList<T> snapshotValid(Set<T> set) {
        if (set.isEmpty() || level == null) return new ArrayList<>();
        var out = new ArrayList<T>(set.size());
        for (T be : set) {
            if (be == null) continue;
            if (be.isRemoved()) continue;
            if (be.getLevel() != this.level) continue;
            out.add(be);
        }
        set.retainAll(out);
        return out;
    }

    private void tickPorts(int ticks) {
        if (!formed) return;
        if (ports.isEmpty()) return;
        if (level == null || level.isClientSide) return;

        var snapshot = new ArrayList<>(ports);
        snapshot.removeIf(p -> p == null || p.isRemoved() || p.getLevel() != this.level);
        ports.retainAll(snapshot);

        for (int i = 0; i < ticks; i++) {
            if (storedEnergy <= 0) break;
            for (PenrosePortBE port : snapshot) {
                port.tickPort();
                if (storedEnergy <= 0) break;
            }
        }
    }

    // === START BH ===
    public boolean canStartBlackHole() {
        if (!formed || blackHoleActive) return false;

        ItemStack cellStack = diskInv.getStackInSlot(0);
        if (cellStack.isEmpty() || cellStack.getItem() != AEItems.ITEM_CELL_4K.asItem()) return false;

        var cellInv = StorageCells.getCellInventory(cellStack, null);
        if (cellInv == null) return false;

        var stacks = cellInv.getAvailableStacks();
        if (stacks.isEmpty()) return false;

        var key = AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get());
        long available = stacks.get(key);

        long needed = cfgStartupCostSingu();
        if (available >= needed) {
            ventingLockTicks = 0;
            return true;
        }
        return false;
    }

    public boolean startBlackHole() {
        if (!canStartBlackHole()) return false;

        ItemStack cellStack = diskInv.getStackInSlot(0);
        var cellInv = StorageCells.getCellInventory(cellStack, null);
        if (cellInv == null) return false;

        var key = AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get());
        long needed = cfgStartupCostSingu();
        long extracted = cellInv.extract(key, needed, Actionable.MODULATE, IActionSource.ofMachine(this));
        if (extracted < needed) return false;

        this.bhMass = cfgInitialMassMu();
        this.bhMassRemainder = 0.0;
        this.heat = 0.0;
        this.blackHoleActive = true;

        pendingFeedMu = 0;
        pendingCoolingHeat = 0.0;
        pendingEvaporation = 0L;

        lastSecondMassDelta = 0L;
        secTickCounter = 0;
        secMassDeltaAcc = 0L;
        structureTickCounter = 0;

        ventingLockTicks = 0;

        clearDisk();

        this.setChanged();
        return true;
    }

    // === API ===
    public void addFeedMu(int singus) {
        if (singus <= 0) return;
        if (isVentingLocked()) return;
        long sum = (long) pendingFeedMu + (long) singus;
        pendingFeedMu = (sum > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) sum;
    }

    public void addCooling(double heatGK) {
        if (heatGK <= 0) return;
        pendingCoolingHeat += heatGK;
        if (Double.isNaN(pendingCoolingHeat) || Double.isInfinite(pendingCoolingHeat)) pendingCoolingHeat = 0.0;
    }

    public void addEvaporation(long massMu) {
        if (massMu <= 0) return;
        if (pendingEvaporation > Long.MAX_VALUE - massMu) pendingEvaporation = Long.MAX_VALUE;
        else pendingEvaporation += massMu;
        bumpVentingLock(2);
    }

    public long getBlackHoleMass() { return bhMass; }
    public double getHeat() { return heat; }
    public boolean isBlackHoleActive() { return blackHoleActive; }

    private static double clamp(double v, double lo, double hi) {
        if (v < lo) return lo;
        if (v > hi) return hi;
        return v;
    }

    /**
     * Mass factor based on distance to sweet spot.
     * Range: [1.0 .. massFactorMax]
     */
    private double computeMassFactorSweet() {
        // half = half-width of allowed mass window around sweet spot
        double half = (double) cfgMassWindowMu() / 2.0;
        if (half <= 0.0) return 1.0;

        // mExact = BH mass including fractional remainder (sub-MU)
        double mExact = (double) bhMass + bhMassRemainder;

        // d = absolute distance from sweet spot
        double d = Math.abs(mExact - (double) cfgSweetSpotMassMu());

        // k = normalized closeness to sweet spot:
        // k=1 at sweet spot, k=0 at edges (sweetSpot +/- half)
        double k = 1.0 - (d / half);
        k = clamp(k, 0.0, 1.0);

        // massFactor = 1 + (max-1)*k  => [1..max]
        double max = cfgMassFactorMax();
        return 1.0 + (max - 1.0) * k;
    }

    /**
     * Heat efficiency curve.
     * x = heat/peak; eff = 2x - x^2; clamped to [0..1]
     */
    private double computeHeatEff() {
        final double PEAK = cfgHeatPeakGK();
        if (PEAK <= 0.0) return 0.0;

        double x = heat / PEAK;
        double eff = 2.0 * x - x * x;
        return clamp(eff, 0.0, 1.0);
    }

    private long computeStoredEnergyInDiskNow(double massFactor, double heatEff) {
        if (!blackHoleActive || diskMassMu <= 0.0 || heatEff <= 0.0) return 0L;

        // e = dutyComp * baseFEPerFlow * diskMassMu * heatEff * massFactor
        double e = cfgDutyComp() * cfgFeBasePerSinguFlow() * diskMassMu * heatEff * massFactor;
        if (Double.isNaN(e) || Double.isInfinite(e) || e <= 0.0) return 0L;
        if (e >= (double) Long.MAX_VALUE) return Long.MAX_VALUE;
        return (long) Math.round(e);
    }

    private void recomputeUiEnergy() {
        if (!blackHoleActive) {
            storedEnergyInDisk = 0L;
            lastGeneratedFePerTickGross = 0L;
            lastConsumedFePerTick = 0L;
            return;
        }
        double mf = computeMassFactorSweet();
        double eff = computeHeatEff();
        storedEnergyInDisk = computeStoredEnergyInDiskNow(mf, eff);
    }

    /**
     * Advance disk state by one tick:
     * - push new injection to age=0 bucket
     * - compute leavingMu from all buckets using DISK_DELTA_FRAC
     * - remove leavingMu from diskMassMu and add it to BH mass (with fractional remainder)
     */
    private void diskAdvanceOneTick(double injectedMuThisTick) {
        // Move "age=0" pointer backwards (ring buffer)
        diskAge0Ptr--;
        if (diskAge0Ptr < 0) diskAge0Ptr += DISK_WINDOW_TICKS;

        // Reset new age=0 bucket
        diskBatchMuByAge[diskAge0Ptr] = 0.0;

        // Add new injection to age=0
        if (injectedMuThisTick > 0.0) {
            diskBatchMuByAge[diskAge0Ptr] += injectedMuThisTick;
            diskMassMu += injectedMuThisTick;
        }

        // Compute leavingMu this tick (MU leaving disk -> BH)
        double leavingMu = 0.0;
        int idx = diskAge0Ptr;
        for (int age = 0; age < DISK_WINDOW_TICKS; age++) {
            double bucketMu = diskBatchMuByAge[idx];
            if (bucketMu != 0.0) leavingMu += bucketMu * DISK_DELTA_FRAC[age];

            idx++;
            if (idx >= DISK_WINDOW_TICKS) idx = 0;
        }

        if (leavingMu < 0.0) leavingMu = 0.0;
        if (leavingMu > diskMassMu) leavingMu = diskMassMu;

        diskMassMu -= leavingMu;
        if (diskMassMu < 0.0) diskMassMu = 0.0;

        // Add leavingMu into BH mass with fractional precision
        bhMassRemainder += leavingMu;
        if (bhMassRemainder >= 1.0) {
            long add = (long) Math.floor(bhMassRemainder);
            bhMass += add;
            bhMassRemainder -= (double) add;
        }

        // Telemetry: leavingMu in "singu/t"
        lastAccretionSingu = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Math.round(leavingMu)));
    }

    private void addStoredEnergyCapped(long add) {
        if (add <= 0) return;
        if (storedEnergy >= MAX_STORED_ENERGY - add) storedEnergy = MAX_STORED_ENERGY;
        else storedEnergy += add;
    }

    private long takeStoredEnergy(long want) {
        if (want <= 0 || storedEnergy <= 0) return 0L;
        long got = Math.min(storedEnergy, want);
        storedEnergy -= got;
        if (storedEnergy < 0) storedEnergy = 0;
        return got;
    }

    /**
     * Apply hawking evaporation to BH mass (cannot go below initial mass).
     */
    private void applyEvaporationInternal(long evapMu) {
        if (evapMu <= 0L) return;

        double total = (double) bhMass + bhMassRemainder;
        total -= (double) evapMu;

        double minMass = (double) cfgInitialMassMu();
        if (total < minMass) total = minMass;

        long newMass = (long) Math.floor(total);
        double rem = total - (double) newMass;

        bhMass = newMass;
        bhMassRemainder = rem;

        bumpVentingLock(2);
    }

    // === TICKING ===
    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {

        int t = Math.max(1, ticksSinceLastCall);

        lastGeneratedFePerTickGross = 0L;
        lastConsumedFePerTick = 0L;

        if (ventingLockTicks > 0) ventingLockTicks = Math.max(0, ventingLockTicks - t);

        final int    maxFeedPerTick = cfgMaxFeedPerTick();
        final double maxHeatGK      = cfgMaxHeatGK();
        final double heatPerFlow    = cfgHeatPerSinguFlow();
        final double feBasePerFlow  = cfgFeBasePerSinguFlow();
        final double dutyComp       = cfgDutyComp();
        final long   maxMassMu      = cfgMaxMassMu();

        structureTickCounter += t;
        while (structureTickCounter >= 20) {
            this.formed = validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this);

            if (!formed) {
                ports.clear();
                injectionPorts.clear();
                heatVents.clear();
                hawkingVents.clear();

                pendingFeedMu = 0;
                pendingCoolingHeat = 0.0;
                pendingEvaporation = 0L;
            }

            structureTickCounter -= 20;
        }

        if (!blackHoleActive) {
            pendingFeedMu = 0;
            pendingCoolingHeat = 0.0;
            pendingEvaporation = 0L;
            clearDisk();
            tickPorts(t);
            return TickRateModulation.IDLE;
        }

        var grid = getMainNode().getGrid();
        boolean ioEnabled = formed && grid != null && level != null && !level.isClientSide;

        // snapshots dzieci (server only, tylko jeśli IO działa)
        ArrayList<PenroseInjectionPortBE> injSnapshot = new ArrayList<>();
        ArrayList<PenroseHeatVentBE> heatSnapshot = new ArrayList<>();
        ArrayList<PenroseHawkingVentBE> hawkSnapshot = new ArrayList<>();

        if (ioEnabled) {
            injSnapshot = snapshotValid(injectionPorts);
            heatSnapshot = snapshotValid(heatVents);
            hawkSnapshot = snapshotValid(hawkingVents);

            // jeśli jakikolwiek hawking vent jest "armed" => blokuj injection w tej paczce ticków
            if (!hawkSnapshot.isEmpty()) {
                boolean anyArmed = false;
                for (PenroseHawkingVentBE v : hawkSnapshot) {
                    var lv = v.getLevel();
                    if (lv == null) continue;
                    if (v.desiredEvap <= 0.0) continue;
                    if (!lv.hasNeighborSignal(v.getBlockPos())) continue;
                    anyArmed = true;
                    break;
                }
                if (anyArmed) requestVentingLock(Math.min(40, t + 1));
            }

            // tick injection portów (raz na paczkę ticków)
            if (!injSnapshot.isEmpty() && !isVentingLocked()) {
                for (PenroseInjectionPortBE p : injSnapshot) {
                    p.tickFromController(t);
                }
            } else {
                if (isVentingLocked()) pendingFeedMu = 0;
            }
        } else {
            pendingFeedMu = 0;
            pendingCoolingHeat = 0.0;
            pendingEvaporation = 0L;
        }

        // external cooling/evap (inne źródła) – tylko gdy IO działa
        double coolTotal = ioEnabled ? Math.max(0.0, pendingCoolingHeat) : 0.0;
        long evapTotal = ioEnabled ? Math.max(0L, pendingEvaporation) : 0L;
        pendingCoolingHeat = 0.0;
        pendingEvaporation = 0L;

        double coolPerTick = coolTotal / (double) t;
        long evapBase = (t > 0) ? (evapTotal / (long) t) : 0L;
        long evapRem  = (t > 0) ? (evapTotal % (long) t) : 0L;

        long genGrossAcc = 0L;
        long consumedAcc = 0L;

        for (int step = 0; step < t; step++) {
            long massBefore = bhMass;

            // feed tylko jeśli IO działa i nie vent locked
            int feedThisTick;
            if (!ioEnabled || isVentingLocked()) {
                pendingFeedMu = 0;
                feedThisTick = 0;
            } else {
                int avail = Math.max(0, pendingFeedMu);
                feedThisTick = Math.min(maxFeedPerTick, avail);
                pendingFeedMu = Math.max(0, avail - feedThisTick);
            }
            lastFeedMu = feedThisTick;

            // injectMu = MU injected into disk this tick
            double injectMu = (feedThisTick > 0) ? ((double) feedThisTick * (double) MU_PER_SINGU) : 0.0;
            diskAdvanceOneTick(injectMu);

            // diskFlow = "singu/t" equivalent
            double diskFlow = diskMassMu / (double) DISK_MEAN_TICKS;
            if (diskFlow < 0.0) diskFlow = 0.0;

            double mFactor = computeMassFactorSweet();

            // accretion heat (GK/t)
            heat += heatPerFlow * diskFlow * mFactor;

            // external cooling (średnia)
            heat -= coolPerTick;
            if (heat < 0) heat = 0.0;

            if (heat >= maxHeatGK) {
                triggerMeltdown("Accretion disk overheated");
                tickPorts(1);
                return TickRateModulation.IDLE;
            }

            // external hawking evap (średnia)
            long evapExt = evapBase + (step < evapRem ? 1L : 0L);
            if (evapExt > 0L) {
                applyEvaporationInternal(evapExt);
            }

            // ===== GROSS GENERATION (budget na ten tick) =====
            long generatedGross = 0L;
            if (diskMassMu > 0.0) {
                double heatEff = computeHeatEff();
                double pGross = dutyComp * feBasePerFlow * diskFlow * heatEff * mFactor;
                generatedGross = (long) Math.max(0.0, Math.round(pGross));
            }
            genGrossAcc += generatedGross;

            long genBudget = generatedGross;
            long consumedThisTick = 0L;

            // ===== HEAT VENTS =====
            if (ioEnabled && !heatSnapshot.isEmpty()) {
                for (PenroseHeatVentBE v : heatSnapshot) {
                    var lv = v.getLevel();
                    if (lv == null) continue;
                    if (v.desiredCooling <= 0.0) continue;
                    if (!lv.hasNeighborSignal(v.getBlockPos())) continue;

                    long cost = PenroseHeatVentBE.computeCostForCooling(v.desiredCooling);
                    if (cost <= 0L) continue;

                    long need = cost;

                    long fromGen = Math.min(genBudget, need);
                    genBudget -= fromGen;
                    need -= fromGen;

                    long fromCtrl = (need > 0) ? takeStoredEnergy(need) : 0L;
                    need -= fromCtrl;

                    int fromBuf = 0;
                    if (need > 0) {
                        EnergyStorage buf = v.getInternalEnergyStorage();
                        int wantInt = (int) Math.min((long) Integer.MAX_VALUE, need);
                        if (wantInt > 0) fromBuf = buf.extractEnergy(wantInt, false);
                    }

                    long paid = fromGen + fromCtrl + (long) fromBuf;
                    if (paid <= 0L) continue;

                    consumedThisTick += paid;

                    double frac = (double) paid / (double) cost;
                    if (frac > 1.0) frac = 1.0;

                    heat -= v.desiredCooling * frac;
                    if (heat < 0) heat = 0.0;
                }
            }

            // ===== HAWKING VENTS =====
            if (ioEnabled && !hawkSnapshot.isEmpty()) {
                for (PenroseHawkingVentBE v : hawkSnapshot) {
                    var lv = v.getLevel();
                    if (lv == null) continue;
                    if (v.desiredEvap <= 0.0) continue;
                    if (!lv.hasNeighborSignal(v.getBlockPos())) continue;

                    // vent aktywny -> blokuj injection niezależnie od FE
                    bumpVentingLock(2);

                    long cost = PenroseHawkingVentBE.computeCostForEvap(v.desiredEvap);
                    if (cost <= 0L) continue;

                    long need = cost;

                    long fromGen = Math.min(genBudget, need);
                    genBudget -= fromGen;
                    need -= fromGen;

                    long fromCtrl = (need > 0) ? takeStoredEnergy(need) : 0L;
                    need -= fromCtrl;

                    int fromBuf = 0;
                    if (need > 0) {
                        EnergyStorage buf = v.getInternalEnergyStorage();
                        int wantInt = (int) Math.min((long) Integer.MAX_VALUE, need);
                        if (wantInt > 0) fromBuf = buf.extractEnergy(wantInt, false);
                    }

                    long paid = fromGen + fromCtrl + (long) fromBuf;
                    if (paid <= 0L) continue;

                    consumedThisTick += paid;

                    double frac = (double) paid / (double) cost;
                    if (frac > 1.0) frac = 1.0;

                    long evapMu = Math.round(v.desiredEvap * frac);
                    if (evapMu > 0L) applyEvaporationInternal(evapMu);
                }
            }

            consumedAcc += consumedThisTick;

            if (heat >= maxHeatGK) {
                triggerMeltdown("Accretion disk overheated");
                tickPorts(1);
                return TickRateModulation.IDLE;
            }

            if (bhMass >= maxMassMu) {
                triggerMeltdown("Black hole mass limit exceeded");
                tickPorts(1);
                return TickRateModulation.IDLE;
            }

            // ===== store leftover generation after costs =====
            if (genBudget > 0) addStoredEnergyCapped(genBudget);

            // telemetry ΔM/s
            long dM = bhMass - massBefore;
            secMassDeltaAcc += dM;
            secTickCounter++;
            while (secTickCounter >= 20) {
                lastSecondMassDelta = secMassDeltaAcc;
                secMassDeltaAcc = 0L;
                secTickCounter -= 20;
            }
        }

        lastGeneratedFePerTickGross = genGrossAcc / (long) t;
        lastConsumedFePerTick = consumedAcc / (long) t;

        recomputeUiEnergy();

        tickPorts(t);
        setChanged();
        return TickRateModulation.IDLE;
    }

    private void triggerMeltdown(String reason) {
        this.blackHoleActive = false;
        this.bhMass = 0L;
        this.bhMassRemainder = 0.0;
        this.heat = 0.0;
        this.storedEnergy = 0L;
        this.storedEnergyInDisk = 0L;
        this.lastGeneratedFePerTickGross = 0L;
        this.lastConsumedFePerTick = 0L;
        this.ventingLockTicks = 0;

        this.pendingFeedMu = 0;
        this.pendingCoolingHeat = 0.0;
        this.pendingEvaporation = 0L;

        this.lastSecondMassDelta = 0L;
        this.secTickCounter = 0;
        this.secMassDeltaAcc = 0L;

        clearDisk();
        setChanged();

        CrazyAddons.LOGGER.info("Triggering black hole escape, reason: {}", reason);

        if (level == null || level.isClientSide) return;
        if (!CrazyConfig.COMMON.PenroseMeltdownExplosionsEnabled.get()) return;

        ServerLevel lvl = (ServerLevel) level;
        BlockPos center = getBlockPos();

        int fieldR = Math.max(0, CrazyConfig.COMMON.PenroseMeltdownFieldRadius.get());

        float explosionRadius = 32.0f;
        boolean causesFire = true;
        var interaction = net.minecraft.world.level.Level.ExplosionInteraction.TNT;

        lvl.explode(
                null,
                center.getX() + 0.5,
                center.getY() + 0.5,
                center.getZ() + 0.5,
                explosionRadius,
                causesFire,
                interaction
        );

        if (fieldR > 0) {
            BlackHoleManager.start(lvl, center, fieldR);
        }
    }

    // === INVENTORY / CAP ===
    @Override
    public InternalInventory getInternalInventory() { return diskInv; }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {}

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.empty();
        return super.getCapability(cap, side);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.ENERGY) return energyCap.cast();
        if (cap == ForgeCapabilities.ITEM_HANDLER) return LazyOptional.empty();
        return super.getCapability(cap);
    }
}
