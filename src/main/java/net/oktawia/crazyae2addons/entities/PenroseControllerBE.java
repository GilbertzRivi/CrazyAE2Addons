package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.blocks.PenroseControllerBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockControllerBE;
import net.oktawia.crazyae2addons.multiblock.CrazyMultiblocks;
import net.oktawia.crazyae2addons.multiblock.MultiblockDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class PenroseControllerBE extends AbstractMultiblockControllerBE
        implements ISyncPersistRPCBlockEntity, InternalInventoryHost, MenuProvider {

    @Getter
    private final FieldManagedStorage syncStorage;

    /*
     * ===== Units =====
     * MU  = internal mass unit
     * singu = Super Singularity item count
     * MK  = internal heat unit
     */

    private static final double DEF_MASS_FACTOR_MAX        = 2.0;
    private static final double DEF_DUTY_COMPENSATION      = 4.0 / 3.0;
    private static final double DEF_FE_BASE_PER_SINGU_FLOW = (double) (1L << 27) * 0.5;
    private static final double DEF_HEAT_PER_SINGU_FLOW    = 0.5;
    private static final double DEF_HEAT_PEAK_GK           = 50_000.0;
    private static final double DEF_MAX_HEAT_GK            = 100_000.0;

    public static final long MU_PER_SINGU = 1L;

    // ===== DISK WINDOW =====
    private static final int DISK_WINDOW_TICKS = 20 * 120;
    private static final int DISK_MEAN_TICKS   = DISK_WINDOW_TICKS / 2;

    private int diskAge0Ptr = 0;
    private final double[] diskBatchMuByAge = new double[DISK_WINDOW_TICKS];
    private double diskMassMu = 0.0;

    private static final double[] DISK_DELTA_FRAC = new double[DISK_WINDOW_TICKS];
    private static final double GAUSS_MU_TICKS    = DISK_WINDOW_TICKS / 2.0;
    private static final double GAUSS_SIGMA_TICKS = 20.0 * 20.0;

    static {
        final double sqrt2 = Math.sqrt(2.0);
        final double mu = GAUSS_MU_TICKS;
        final double sigma = GAUSS_SIGMA_TICKS;

        java.util.function.DoubleUnaryOperator cdf = x -> {
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

    private double bhMassRemainder = 0.0;

    // ===== ENERGY / STATE =====

    private static final long MAX_STORED_ENERGY = Long.MAX_VALUE;

    @Getter @Persisted
    private long storedEnergy = 0L;

    @Getter @Persisted
    private long storedEnergyInDisk = 0L;

    @Getter @Persisted
    private long lastGeneratedFePerTickGross = 0L;

    @Getter @Persisted
    private long lastConsumedFePerTick = 0L;

    @Getter @Persisted
    private boolean blackHoleActive = false;

    @Persisted
    private long bhMass = 0L;

    @Getter @Persisted
    private double heat = 0.0;

    @Persisted
    private int pendingFeedMu = 0;

    @Persisted
    private double pendingCoolingHeat = 0.0;

    @Persisted
    private long pendingEvaporation = 0L;

    @Getter @Persisted
    private long lastSecondMassDelta = 0L;

    @Persisted
    private int ventingLockTicks = 0;

    @Getter @Persisted
    private int lastFeedMu = 0;

    @Getter @Persisted
    private int lastAccretionSingu = 0;

    @Getter
    private boolean formed = false;

    private int secTickCounter = 0;
    private long secMassDeltaAcc = 0L;

    @Setter
    private ServerPlayer placer;

    @Getter
    @Persisted
    private final AppEngInternalInventory diskInv = new AppEngInternalInventory(this, 1, 1,
            new IAEItemFilter() {
                @Override
                public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                    if (stack.getItem() == AEItems.ITEM_CELL_1K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_4K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_16K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_64K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_256K.asItem()) {
                        var cellInv = StorageCells.getCellInventory(stack, null);
                        if (cellInv == null) return false;
                        var stacks = cellInv.getAvailableStacks();
                        if (stacks.isEmpty()) return true;
                        return stacks.size() == 1
                                && Objects.equals(
                                stacks.getFirstKey(),
                                AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get())
                        );
                    }
                    return false;
                }
            });

    @Getter
    @Persisted
    private final ConfigInventory config = ConfigInventory.configTypes(1)
            .slotFilter(key -> key instanceof AEItemKey itemKey
                    && itemKey.toStack().getItem() != CrazyItemRegistrar.SUPER_SINGULARITY.get())
            .build();

    public PenroseControllerBE(BlockPos pos, BlockState state) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_CONTROLLER_BE.get(),
                pos,
                state,
                new ItemStack(CrazyBlockRegistrar.PENROSE_CONTROLLER.get().asItem()),
                2.0F
        );
        this.syncStorage = new FieldManagedStorage(this);
    }

    @Override
    protected MultiblockDefinition getMultiblockDefinition() {
        return CrazyMultiblocks.PENROSE_SPHERE;
    }

    @Override
    protected char frameSymbol() {
        return 'A';
    }

    @Override
    protected void setOwnFormedState(boolean formed) {
        this.formed = formed;
        Level level = getLevel();
        if (level == null) return;

        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(PenroseControllerBlock.FORMED)
                && state.getValue(PenroseControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), state.setValue(PenroseControllerBlock.FORMED, formed), 3);
        }
    }

    @Override
    protected void setMemberFormedState(BlockPos pos, boolean formed) {
        // TODO: set FORMED on frame BEs when they are ported
    }

    @Override
    protected void afterDisformed() {
        pendingFeedMu = 0;
        pendingCoolingHeat = 0.0;
        pendingEvaporation = 0L;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        super.tickingRequest(node, ticksSinceLastCall);

        Level level = getLevel();
        if (level == null || level.isClientSide()) {
            return TickRateModulation.IDLE;
        }

        int t = Math.max(1, ticksSinceLastCall);

        lastGeneratedFePerTickGross = 0L;
        lastConsumedFePerTick = 0L;

        if (ventingLockTicks > 0) {
            ventingLockTicks = Math.max(0, ventingLockTicks - t);
        }

        if (!blackHoleActive) {
            pendingFeedMu = 0;
            pendingCoolingHeat = 0.0;
            pendingEvaporation = 0L;
            clearDisk();
            return TickRateModulation.IDLE;
        }

        var grid = getMainNode().getGrid();
        boolean ioEnabled = formed && grid != null;

        if (!ioEnabled) {
            pendingFeedMu = 0;
            pendingCoolingHeat = 0.0;
            pendingEvaporation = 0L;
        }

        final int maxFeedPerTick = cfgMaxFeedPerTick();
        final double maxHeatGK = cfgMaxHeatGK();
        final double heatPerFlow = cfgHeatPerSinguFlow();
        final double feBasePerFlow = cfgFeBasePerSinguFlow();
        final double dutyComp = cfgDutyComp();
        final long maxMassMu = cfgMaxMassMu();

        double coolTotal = ioEnabled ? Math.max(0.0, pendingCoolingHeat) : 0.0;
        long evapTotal = ioEnabled ? Math.max(0L, pendingEvaporation) : 0L;
        pendingCoolingHeat = 0.0;
        pendingEvaporation = 0L;

        double coolPerTick = coolTotal / (double) t;
        long evapBase = evapTotal / (long) t;
        long evapRem = evapTotal % (long) t;

        long genGrossAcc = 0L;
        long consumedAcc = 0L;

        for (int step = 0; step < t; step++) {
            long massBefore = bhMass;

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

            double injectMu = feedThisTick > 0 ? ((double) feedThisTick * (double) MU_PER_SINGU) : 0.0;
            diskAdvanceOneTick(injectMu);

            double diskFlow = diskMassMu / (double) DISK_MEAN_TICKS;
            if (diskFlow < 0.0) diskFlow = 0.0;

            double mFactor = computeMassFactorSweet();

            heat += heatPerFlow * diskFlow * mFactor;
            heat -= coolPerTick;
            if (heat < 0.0) heat = 0.0;

            if (heat >= maxHeatGK) {
                triggerMeltdown("Accretion disk overheated");
                return TickRateModulation.IDLE;
            }

            long evapExt = evapBase + (step < evapRem ? 1L : 0L);
            if (evapExt > 0L) {
                applyEvaporationInternal(evapExt);
            }

            long generatedGross = 0L;
            if (diskMassMu > 0.0) {
                double heatEff = computeHeatEff();
                double pGross = dutyComp * feBasePerFlow * diskFlow * heatEff * mFactor;
                generatedGross = (long) Math.max(0.0, Math.round(pGross));
            }
            genGrossAcc += generatedGross;

            long genBudget = generatedGross;
            long consumedThisTick = 0L;

            // TODO: heat vent and hawking vent ticking (child BEs not yet ported)

            consumedAcc += consumedThisTick;

            if (heat >= maxHeatGK) {
                triggerMeltdown("Accretion disk overheated");
                return TickRateModulation.IDLE;
            }
            if (bhMass >= maxMassMu) {
                triggerMeltdown("Black hole mass limit exceeded");
                return TickRateModulation.IDLE;
            }

            if (genBudget > 0) {
                addStoredEnergyCapped(genBudget);
            }

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

        // TODO: tickPorts(t) when PenrosePortBE is ported

        recomputeUiEnergy();
        setChanged();
        return TickRateModulation.IDLE;
    }

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

        bhMass = cfgInitialMassMu();
        bhMassRemainder = 0.0;
        heat = 0.0;
        blackHoleActive = true;

        pendingFeedMu = 0;
        pendingCoolingHeat = 0.0;
        pendingEvaporation = 0L;

        lastSecondMassDelta = 0L;
        secTickCounter = 0;
        secMassDeltaAcc = 0L;
        ventingLockTicks = 0;

        clearDisk();
        setChanged();
        return true;
    }

    public void addFeedMu(int singus) {
        if (singus <= 0 || isVentingLocked()) return;

        long sum = (long) pendingFeedMu + (long) singus;
        pendingFeedMu = sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum;
    }

    public void addCooling(double heatGK) {
        if (heatGK <= 0) return;

        pendingCoolingHeat += heatGK;
        if (Double.isNaN(pendingCoolingHeat) || Double.isInfinite(pendingCoolingHeat)) {
            pendingCoolingHeat = 0.0;
        }
    }

    public void addEvaporation(long massMu) {
        if (massMu <= 0) return;

        if (pendingEvaporation > Long.MAX_VALUE - massMu) {
            pendingEvaporation = Long.MAX_VALUE;
        } else {
            pendingEvaporation += massMu;
        }
        bumpVentingLock(2);
    }

    public boolean isVentingLocked() {
        return ventingLockTicks > 0 || pendingEvaporation > 0;
    }

    public long getBlackHoleMass() {
        return bhMass;
    }

    public int getDiskFlowSinguPerTick() {
        double flow = diskMassMu / (double) DISK_MEAN_TICKS;
        if (flow < 0) flow = 0;
        if (flow > Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.round(flow);
    }

    public long getDiskMassSingu() {
        return (long) Math.max(0.0, Math.round(diskMassMu));
    }

    public int getOrbitDelaySeconds() {
        return DISK_MEAN_TICKS / 20;
    }

    public long getInitialBhMass() {
        return cfgInitialMassMu();
    }

    public long getMaxBhMass() {
        return cfgMaxMassMu();
    }

    public double getMaxHeatGK() {
        return cfgMaxHeatGK();
    }

    public IEnergyStorage getEnergyStorage(@Nullable Direction dir) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                return 0;
            }

            @Override
            public boolean canReceive() {
                return false;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) {
                if (maxExtract <= 0 || storedEnergy <= 0) return 0;

                int toExtract = (int) Math.min(maxExtract, Math.min(storedEnergy, Integer.MAX_VALUE));
                if (!simulate) {
                    storedEnergy -= toExtract;
                    if (storedEnergy < 0) storedEnergy = 0;
                }
                return toExtract;
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public int getEnergyStored() {
                return (int) Math.min(storedEnergy, Integer.MAX_VALUE);
            }

            @Override
            public int getMaxEnergyStored() {
                return Integer.MAX_VALUE;
            }
        };
    }

    public long takeStoredEnergy(long want) {
        if (want <= 0 || storedEnergy <= 0) return 0L;

        long got = Math.min(storedEnergy, want);
        storedEnergy -= got;
        if (storedEnergy < 0) storedEnergy = 0;
        return got;
    }

    private static final double NBT_SCALE = 1_000_000.0;

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);

        data.putInt("diskAge0Ptr", diskAge0Ptr);
        data.putLong("diskMassMicro", (long) Math.round(diskMassMu * NBT_SCALE));
        data.putLong("bhRemMicro", (long) Math.round(bhMassRemainder * NBT_SCALE));

        long[] arr = new long[DISK_WINDOW_TICKS];
        for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
            arr[i] = (long) Math.round(diskBatchMuByAge[i] * NBT_SCALE);
        }
        data.putLongArray("diskBatchesMicro", arr);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);

        clearDisk();

        if (data.contains("diskAge0Ptr")) {
            diskAge0Ptr = data.getInt("diskAge0Ptr");
            if (diskAge0Ptr < 0 || diskAge0Ptr >= DISK_WINDOW_TICKS) diskAge0Ptr = 0;
        }
        if (data.contains("diskMassMicro")) {
            long m = data.getLong("diskMassMicro");
            diskMassMu = (double) m / NBT_SCALE;
            if (diskMassMu < 0) diskMassMu = 0.0;
        }
        if (data.contains("bhRemMicro")) {
            long rr = data.getLong("bhRemMicro");
            bhMassRemainder = (double) rr / NBT_SCALE;
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
        recomputeUiEnergy();
    }

    public CompoundTag makePortableTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("energy", storedEnergy);
        tag.putLong("energyDisk", storedEnergyInDisk);
        tag.putBoolean("bhActive", blackHoleActive);
        tag.putLong("bhMass", bhMass);
        tag.putDouble("heat", heat);
        tag.putLong("lastSecDelta", lastSecondMassDelta);
        tag.putInt("lastAccretion", lastAccretionSingu);
        tag.putInt("lastFeed", lastFeedMu);
        tag.putInt("ventingLock", ventingLockTicks);

        tag.putInt("diskAge0Ptr", diskAge0Ptr);
        tag.putLong("diskMassMicro", Math.round(diskMassMu * NBT_SCALE));
        tag.putLong("bhRemMicro", Math.round(bhMassRemainder * NBT_SCALE));

        long[] arr = new long[DISK_WINDOW_TICKS];
        for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
            arr[i] = Math.round(diskBatchMuByAge[i] * NBT_SCALE);
        }
        tag.putLongArray("diskBatchesMicro", arr);
        return tag;
    }

    public void applyPortableTag(@Nullable CompoundTag data) {
        if (data == null) return;

        if (data.contains("energy")) storedEnergy = data.getLong("energy");
        if (data.contains("energyDisk")) storedEnergyInDisk = data.getLong("energyDisk");
        if (data.contains("bhActive")) blackHoleActive = data.getBoolean("bhActive");
        if (data.contains("bhMass")) bhMass = data.getLong("bhMass");
        if (data.contains("heat")) heat = data.getDouble("heat");
        if (data.contains("lastSecDelta")) lastSecondMassDelta = data.getLong("lastSecDelta");
        if (data.contains("lastAccretion")) lastAccretionSingu = data.getInt("lastAccretion");
        if (data.contains("lastFeed")) lastFeedMu = data.getInt("lastFeed");
        if (data.contains("ventingLock")) ventingLockTicks = data.getInt("ventingLock");

        clearDisk();
        if (data.contains("diskAge0Ptr")) {
            diskAge0Ptr = data.getInt("diskAge0Ptr");
            if (diskAge0Ptr < 0 || diskAge0Ptr >= DISK_WINDOW_TICKS) diskAge0Ptr = 0;
        }
        if (data.contains("diskMassMicro")) {
            long m = data.getLong("diskMassMicro");
            diskMassMu = (double) m / NBT_SCALE;
            if (diskMassMu < 0) diskMassMu = 0.0;
        }
        if (data.contains("bhRemMicro")) {
            long rr = data.getLong("bhRemMicro");
            bhMassRemainder = (double) rr / NBT_SCALE;
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
        recomputeUiEnergy();
        setChanged();
    }

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        setChanged();
    }

    @Override
    public boolean isClientSide() {
        Level level = getLevel();
        return level != null && level.isClientSide();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public void openMenu(Player player, MenuHostLocator menuHostLocator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(), player, menuHostLocator);
    }

    private static long cfgStartupCostSingu() {
        return Math.max(0L, CrazyConfig.COMMON.PenroseStartupCostSingu.get());
    }

    public static long cfgInitialMassMu() {
        return Math.max(0L, CrazyConfig.COMMON.PenroseInitialMassMu.get());
    }

    private static long cfgMassWindowMu() {
        return Math.max(0L, CrazyConfig.COMMON.PenroseMassWindowMu.get());
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
        return (Double.isNaN(v) || Double.isInfinite(v) || v < 1.0) ? DEF_MASS_FACTOR_MAX : v;
    }

    private static double cfgDutyComp() {
        double v = CrazyConfig.COMMON.PenroseDutyCompensation.get();
        return (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) ? DEF_DUTY_COMPENSATION : v;
    }

    private static double cfgFeBasePerSinguFlow() {
        double v = CrazyConfig.COMMON.PenroseFeBasePerSinguFlow.get();
        return (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) ? DEF_FE_BASE_PER_SINGU_FLOW : v;
    }

    private static double cfgHeatPerSinguFlow() {
        double v = CrazyConfig.COMMON.PenroseHeatPerSinguFlow.get();
        return (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) ? DEF_HEAT_PER_SINGU_FLOW : v;
    }

    private static double cfgHeatPeakGK() {
        double v = CrazyConfig.COMMON.PenroseHeatPeakMK.get();
        return (Double.isNaN(v) || Double.isInfinite(v) || v <= 0.0) ? DEF_HEAT_PEAK_GK : v;
    }

    private static double cfgMaxHeatGK() {
        double v = CrazyConfig.COMMON.PenroseMaxHeatMK.get();
        return (Double.isNaN(v) || Double.isInfinite(v) || v < 0.0) ? DEF_MAX_HEAT_GK : v;
    }

    private static int cfgMaxFeedPerTick() {
        return Math.max(0, CrazyConfig.COMMON.PenroseMaxFeedPerTick.get());
    }

    private double computeMassFactorSweet() {
        double half = (double) cfgMassWindowMu() / 2.0;
        if (half <= 0.0) return 1.0;

        double mExact = (double) bhMass + bhMassRemainder;
        double d = Math.abs(mExact - (double) cfgSweetSpotMassMu());
        double k = Math.clamp(1.0 - (d / half), 0.0, 1.0);
        return 1.0 + (cfgMassFactorMax() - 1.0) * k;
    }

    private double computeHeatEff() {
        final double peak = cfgHeatPeakGK();
        if (peak <= 0.0) return 0.0;

        double x = heat / peak;
        return Math.clamp(2.0 * x - x * x, 0.0, 1.0);
    }

    public void recomputeUiEnergy() {
        if (!blackHoleActive) {
            storedEnergyInDisk = 0L;
            lastGeneratedFePerTickGross = 0L;
            lastConsumedFePerTick = 0L;
            return;
        }

        double mf = computeMassFactorSweet();
        double eff = computeHeatEff();
        if (diskMassMu <= 0.0 || eff <= 0.0) {
            storedEnergyInDisk = 0L;
            return;
        }

        double e = cfgDutyComp() * cfgFeBasePerSinguFlow() * diskMassMu * eff * mf;
        storedEnergyInDisk = (Double.isNaN(e) || Double.isInfinite(e) || e <= 0.0)
                ? 0L
                : (e >= (double) Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.round(e));
    }

    private void diskAdvanceOneTick(double injectedMuThisTick) {
        diskAge0Ptr--;
        if (diskAge0Ptr < 0) diskAge0Ptr += DISK_WINDOW_TICKS;

        diskBatchMuByAge[diskAge0Ptr] = 0.0;
        if (injectedMuThisTick > 0.0) {
            diskBatchMuByAge[diskAge0Ptr] += injectedMuThisTick;
            diskMassMu += injectedMuThisTick;
        }

        double leavingMu = 0.0;
        int idx = diskAge0Ptr;
        for (int age = 0; age < DISK_WINDOW_TICKS; age++) {
            double bucketMu = diskBatchMuByAge[idx];
            if (bucketMu != 0.0) {
                leavingMu += bucketMu * DISK_DELTA_FRAC[age];
            }
            idx++;
            if (idx >= DISK_WINDOW_TICKS) idx = 0;
        }

        if (leavingMu < 0.0) leavingMu = 0.0;
        if (leavingMu > diskMassMu) leavingMu = diskMassMu;

        diskMassMu -= leavingMu;
        if (diskMassMu < 0.0) diskMassMu = 0.0;

        bhMassRemainder += leavingMu;
        if (bhMassRemainder >= 1.0) {
            long add = (long) Math.floor(bhMassRemainder);
            bhMass += add;
            bhMassRemainder -= (double) add;
        }

        lastAccretionSingu = (int) Math.max(0, Math.min(Integer.MAX_VALUE, Math.round(leavingMu)));
    }

    private void addStoredEnergyCapped(long add) {
        if (add <= 0) return;
        if (storedEnergy >= MAX_STORED_ENERGY - add) {
            storedEnergy = MAX_STORED_ENERGY;
        } else {
            storedEnergy += add;
        }
    }

    private void applyEvaporationInternal(long evapMu) {
        if (evapMu <= 0L) return;

        double total = (double) bhMass + bhMassRemainder - (double) evapMu;
        double minMass = (double) cfgInitialMassMu();
        if (total < minMass) total = minMass;

        long newMass = (long) Math.floor(total);
        bhMass = newMass;
        bhMassRemainder = total - (double) newMass;
        bumpVentingLock(2);
    }

    private void clearDisk() {
        diskAge0Ptr = 0;
        diskMassMu = 0.0;
        bhMassRemainder = 0.0;

        for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
            diskBatchMuByAge[i] = 0.0;
        }

        lastAccretionSingu = 0;
        lastFeedMu = 0;
        storedEnergyInDisk = 0L;
        lastGeneratedFePerTickGross = 0L;
        lastConsumedFePerTick = 0L;
    }

    private void triggerMeltdown(String reason) {
        blackHoleActive = false;
        bhMass = 0L;
        bhMassRemainder = 0.0;
        heat = 0.0;
        storedEnergy = 0L;
        storedEnergyInDisk = 0L;
        lastGeneratedFePerTickGross = 0L;
        lastConsumedFePerTick = 0L;
        ventingLockTicks = 0;
        pendingFeedMu = 0;
        pendingCoolingHeat = 0.0;
        pendingEvaporation = 0L;
        lastSecondMassDelta = 0L;
        secTickCounter = 0;
        secMassDeltaAcc = 0L;
        clearDisk();
        setChanged();

        CrazyAddons.LOGGER.info("Penrose meltdown triggered: {}", reason);

        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        if (!CrazyConfig.COMMON.PenroseMeltdownExplosionsEnabled.get()) return;

        ServerLevel lvl = (ServerLevel) level;
        BlockPos center = getBlockPos();
        int fieldR = Math.max(0, CrazyConfig.COMMON.PenroseMeltdownFieldRadius.get());

        lvl.explode(
                null,
                center.getX() + 0.5,
                center.getY() + 0.5,
                center.getZ() + 0.5,
                32.0f,
                true,
                Level.ExplosionInteraction.TNT
        );

        if (fieldR > 0) {
            // TODO: BlackHoleManager.start(lvl, center, fieldR);
        }
    }

    private void bumpVentingLock(int ticks) {
        if (ticks > ventingLockTicks) {
            ventingLockTicks = ticks;
        }
    }
}