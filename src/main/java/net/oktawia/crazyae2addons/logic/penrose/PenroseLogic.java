package net.oktawia.crazyae2addons.logic.penrose;

import appeng.api.config.Actionable;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEItems;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.penrose.PenroseControllerBE;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHawkingVentBE;
import net.oktawia.crazyae2addons.entities.penrose.PenroseHeatVentBE;
import net.oktawia.crazyae2addons.entities.penrose.PenroseInjectionPortBE;
import net.oktawia.crazyae2addons.entities.penrose.PenrosePortBE;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class PenroseLogic {

    public static final int DISK_WINDOW_TICKS =
            Math.max(1, CrazyConfig.COMMON.PenroseDiskWindowSeconds.get()) * 20;

    public static final int DISK_MEAN_TICKS =
            DISK_WINDOW_TICKS / 2;

    public static final double GAUSS_MU_TICKS =
            DISK_WINDOW_TICKS / 2.0;

    public static final double GAUSS_SIGMA_TICKS =
            CrazyConfig.COMMON.PenroseDiskSigmaTicks.get();

    public static final long STARTUP_COST_SINGU =
            CrazyConfig.COMMON.PenroseStartupCostSingu.get();

    public static final long INITIAL_BH_MASS_MU =
            CrazyConfig.COMMON.PenroseInitialMassMu.get();

    public static final long MASS_WINDOW_MU =
            CrazyConfig.COMMON.PenroseMassWindowMu.get();

    public static final long MAX_BH_MASS_MU =
            INITIAL_BH_MASS_MU > Long.MAX_VALUE - MASS_WINDOW_MU
                    ? Long.MAX_VALUE
                    : INITIAL_BH_MASS_MU + MASS_WINDOW_MU;

    public static final long SWEET_SPOT_MASS_MU =
            INITIAL_BH_MASS_MU + (MASS_WINDOW_MU / 2L);

    public static final double MASS_FACTOR_MAX =
            CrazyConfig.COMMON.PenroseMassFactorMax.get();

    public static final double DUTY_COMPENSATION =
            CrazyConfig.COMMON.PenroseDutyCompensation.get();

    public static final double FE_BASE_PER_SINGU_FLOW =
            CrazyConfig.COMMON.PenroseFeBasePerSinguFlow.get();

    public static final double HEAT_PER_SINGU_FLOW =
            CrazyConfig.COMMON.PenroseHeatPerSinguFlow.get();

    public static final double HEAT_PEAK_GK =
            CrazyConfig.COMMON.PenroseHeatPeakMK.get();

    public static final double MAX_HEAT_GK =
            CrazyConfig.COMMON.PenroseMaxHeatMK.get();

    public static final int MAX_FEED_PER_TICK =
            CrazyConfig.COMMON.PenroseMaxFeedPerTick.get();

    public static final boolean FE_OUTPUT_ENABLED =
            CrazyConfig.COMMON.PenroseFEOutputEnabled.get();

    public static final boolean MELTDOWN_EXPLOSIONS_ENABLED =
            CrazyConfig.COMMON.PenroseMeltdownExplosionsEnabled.get();

    public static final int MELTDOWN_FIELD_RADIUS =
            CrazyConfig.COMMON.PenroseMeltdownFieldRadius.get();

    private static final long MAX_STORED_ENERGY = Long.MAX_VALUE;

    public static final double[] DISK_DELTA_FRAC = new double[DISK_WINDOW_TICKS];

    static {
        final double sqrt2 = Math.sqrt(2.0);

        java.util.function.DoubleUnaryOperator cdf = x -> {
            double z = (x - GAUSS_MU_TICKS) / (GAUSS_SIGMA_TICKS * sqrt2);
            return 0.5 * (1.0 + Utils.erf(z));
        };

        double c0 = cdf.applyAsDouble(0.0);
        double cW = cdf.applyAsDouble(DISK_WINDOW_TICKS);
        double denom = cW - c0;
        if (denom <= 0.0) {
            denom = 1.0;
        }

        double sum = 0.0;
        for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
            double a = cdf.applyAsDouble(i);
            double b = cdf.applyAsDouble(i + 1);
            double d = (b - a) / denom;
            if (d < 0.0) {
                d = 0.0;
            }
            DISK_DELTA_FRAC[i] = d;
            sum += d;
        }

        if (sum > 0.0) {
            for (int i = 0; i < DISK_WINDOW_TICKS; i++) {
                DISK_DELTA_FRAC[i] /= sum;
            }
        }
    }

    private final PenroseControllerBE host;

    @Getter
    private final IEnergyStorage energyStorageView = new IEnergyStorage() {
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
            if (maxExtract <= 0 || host.getStoredEnergy() <= 0) {
                return 0;
            }

            int toExtract = (int) Math.min(maxExtract, Math.min(host.getStoredEnergy(), Integer.MAX_VALUE));
            if (!simulate) {
                host.setStoredEnergy(host.getStoredEnergy() - toExtract);
                if (host.getStoredEnergy() < 0) {
                    host.setStoredEnergy(0);
                }
            }
            return toExtract;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public int getEnergyStored() {
            return (int) Math.min(host.getStoredEnergy(), Integer.MAX_VALUE);
        }

        @Override
        public int getMaxEnergyStored() {
            return Integer.MAX_VALUE;
        }
    };

    private record TickSnapshots(
            ArrayList<PenrosePortBE> ports,
            ArrayList<PenroseInjectionPortBE> injectionPorts,
            ArrayList<PenroseHeatVentBE> heatVents,
            ArrayList<PenroseHawkingVentBE> hawkingVents
    ) {
    }

    private record EnergyUse(long paid, long remainingGenBudget) {
    }

    public PenroseLogic(PenroseControllerBE host) {
        this.host = host;
    }

    public void onManagedLoad() {
        ensureDiskArray();
        sanitizeManagedState();
        recomputeUiEnergy();
    }

    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }

    public TickRateModulation tick(int ticksSinceLastCall) {
        Level level = host.getLevel();
        if (level == null || level.isClientSide()) {
            return TickRateModulation.IDLE;
        }

        int ticks = Math.max(1, ticksSinceLastCall);

        resetUiRateFields();
        decayVentingLock(ticks);

        if (!host.isBlackHoleActive()) {
            clearPendingIo();
            clearDisk();
            return TickRateModulation.IDLE;
        }

        boolean ioEnabled = isIoEnabled();
        TickSnapshots snapshots = collectSnapshots(ioEnabled);
        prepareExternalIo(ioEnabled, ticks, snapshots);

        double coolTotal = ioEnabled ? Math.max(0.0, host.getPendingCoolingHeat()) : 0.0;
        long evapTotal = ioEnabled ? Math.max(0L, host.getPendingEvaporation()) : 0L;

        host.setPendingCoolingHeat(0.0);
        host.setPendingEvaporation(0L);

        double coolPerTick = coolTotal / (double) ticks;
        long evapBase = evapTotal / (long) ticks;
        long evapRem = evapTotal % (long) ticks;

        long genGrossAcc = 0L;
        long consumedAcc = 0L;

        for (int step = 0; step < ticks; step++) {
            long massBefore = host.getBhMass();

            int feedThisTick = consumeFeedForTick(ioEnabled);
            host.setLastFeedMu(feedThisTick);

            double injectMu = feedThisTick > 0 ? (double) feedThisTick * PenroseControllerBE.MU_PER_SINGU : 0.0;
            diskAdvanceOneTick(injectMu);

            double diskFlow = Math.max(0.0, host.getDiskMassMu() / (double) DISK_MEAN_TICKS);
            double massFactor = computeMassFactorSweet();

            applyPassiveHeat(diskFlow, massFactor, coolPerTick);
            if (isOverheated()) {
                triggerMeltdown("Accretion disk overheated");
                return TickRateModulation.IDLE;
            }

            long evapExt = evapBase + (step < evapRem ? 1L : 0L);
            if (evapExt > 0L) {
                applyEvaporationInternal(evapExt);
            }

            long generatedGross = computeGeneratedGross(diskFlow, massFactor);
            genGrossAcc += generatedGross;

            long genBudget = generatedGross;
            long consumedThisTick = 0L;

            if (ioEnabled && !snapshots.heatVents().isEmpty()) {
                EnergyUse coolingUse = processHeatVents(snapshots.heatVents(), genBudget);
                consumedThisTick += coolingUse.paid();
                genBudget = coolingUse.remainingGenBudget();
            }

            if (ioEnabled && !snapshots.hawkingVents().isEmpty()) {
                EnergyUse hawkingUse = processHawkingVents(snapshots.hawkingVents(), genBudget);
                consumedThisTick += hawkingUse.paid();
                genBudget = hawkingUse.remainingGenBudget();
            }

            consumedAcc += consumedThisTick;

            if (isOverheated()) {
                triggerMeltdown("Accretion disk overheated");
                return TickRateModulation.IDLE;
            }

            if (isMassOverflowed()) {
                triggerMeltdown("Black hole mass limit exceeded");
                return TickRateModulation.IDLE;
            }

            if (genBudget > 0L) {
                addStoredEnergyCapped(genBudget);
            }

            accumulateMassDelta(massBefore);
        }

        host.setLastGeneratedFePerTickGross(genGrossAcc / (long) ticks);
        host.setLastConsumedFePerTick(consumedAcc / (long) ticks);

        if (FE_OUTPUT_ENABLED) {
            tickPorts(ticks, snapshots.ports());
        }

        recomputeUiEnergy();
        host.setChanged();
        return TickRateModulation.IDLE;
    }

    public boolean canStartBlackHole() {
        if (!host.isFormed() || host.isBlackHoleActive()) {
            return false;
        }

        ItemStack cellStack = host.getDiskInv().getStackInSlot(0);
        if (cellStack.isEmpty() || cellStack.getItem() != AEItems.ITEM_CELL_4K.asItem()) {
            return false;
        }

        var cellInv = StorageCells.getCellInventory(cellStack, null);
        if (cellInv == null) {
            return false;
        }

        var stacks = cellInv.getAvailableStacks();
        if (stacks.isEmpty()) {
            return false;
        }

        var key = AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get());
        long available = stacks.get(key);

        if (available >= STARTUP_COST_SINGU) {
            host.setVentingLockTicks(0);
            return true;
        }

        return false;
    }

    public boolean startBlackHole() {
        if (!canStartBlackHole()) {
            return false;
        }

        ItemStack cellStack = host.getDiskInv().getStackInSlot(0);
        var cellInv = StorageCells.getCellInventory(cellStack, null);
        if (cellInv == null) {
            return false;
        }

        var key = AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get());
        long extracted = cellInv.extract(key, STARTUP_COST_SINGU, Actionable.MODULATE, IActionSource.ofMachine(host));
        if (extracted < STARTUP_COST_SINGU) {
            return false;
        }

        host.setBhMass(INITIAL_BH_MASS_MU);
        host.setBhMassRemainder(0.0);
        host.setHeat(0.0);
        host.setBlackHoleActive(true);

        clearPendingIo();
        host.setLastSecondMassDelta(0L);
        host.setSecTickCounter(0);
        host.setSecMassDeltaAcc(0L);
        host.setVentingLockTicks(0);

        clearDisk();
        host.setChanged();
        return true;
    }

    public void clearPendingAfterDisformed() {
        clearPendingIo();
    }

    public void addFeedMu(int singus) {
        if (singus <= 0 || isVentingLocked()) {
            return;
        }

        long sum = (long) host.getPendingFeedMu() + (long) singus;
        host.setPendingFeedMu(sum > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) sum);
    }

    public boolean isVentingLocked() {
        return host.getVentingLockTicks() > 0 || host.getPendingEvaporation() > 0;
    }

    public long getBlackHoleMass() {
        return host.getBhMass();
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable net.minecraft.core.Direction dir) {
        if (!FE_OUTPUT_ENABLED || !host.isFormed()) {
            return null;
        }
        return energyStorageView;
    }

    public long takeStoredEnergy(long want) {
        if (want <= 0 || host.getStoredEnergy() <= 0) {
            return 0L;
        }

        long got = Math.min(host.getStoredEnergy(), want);
        host.setStoredEnergy(host.getStoredEnergy() - got);
        if (host.getStoredEnergy() < 0) {
            host.setStoredEnergy(0);
        }
        return got;
    }

    public void recomputeUiEnergy() {
        if (!host.isBlackHoleActive()) {
            host.setStoredEnergyInDisk(0L);
            host.setLastGeneratedFePerTickGross(0L);
            host.setLastConsumedFePerTick(0L);
            host.setDiskMassSingu(0L);
            return;
        }

        host.setDiskMassSingu(Math.max(0L, Math.round(host.getDiskMassMu())));

        double massFactor = computeMassFactorSweet();
        double heatEff = computeHeatEff();
        if (host.getDiskMassMu() <= 0.0 || heatEff <= 0.0) {
            host.setStoredEnergyInDisk(0L);
            return;
        }

        double energy = DUTY_COMPENSATION * FE_BASE_PER_SINGU_FLOW * host.getDiskMassMu() * heatEff * massFactor;
        host.setStoredEnergyInDisk(
                (Double.isNaN(energy) || Double.isInfinite(energy) || energy <= 0.0)
                        ? 0L
                        : (energy >= (double) Long.MAX_VALUE ? Long.MAX_VALUE : (long) Math.round(energy))
        );
    }

    public void requestVentingLock(int ticks) {
        bumpVentingLock(Math.max(1, ticks));
    }

    public void clearDisk() {
        host.setDiskAge0Ptr(0);
        host.setDiskMassMu(0.0);
        host.setBhMassRemainder(0.0);

        ensureDiskArray();
        Arrays.fill(host.getDiskBatchMuByAge(), 0.0);

        host.setLastAccretionSingu(0);
        host.setLastFeedMu(0);
        host.setDiskMassSingu(0L);
        host.setStoredEnergyInDisk(0L);
        host.setLastGeneratedFePerTickGross(0L);
        host.setLastConsumedFePerTick(0L);
    }

    private void resetUiRateFields() {
        host.setLastGeneratedFePerTickGross(0L);
        host.setLastConsumedFePerTick(0L);
    }

    private void clearPendingIo() {
        host.setPendingFeedMu(0);
        host.setPendingCoolingHeat(0.0);
        host.setPendingEvaporation(0L);
    }

    private void decayVentingLock(int ticks) {
        if (host.getVentingLockTicks() > 0) {
            host.setVentingLockTicks(Math.max(0, host.getVentingLockTicks() - ticks));
        }
    }

    private boolean isIoEnabled() {
        var grid = host.getMainNode().getGrid();
        return host.isFormed() && grid != null;
    }

    private TickSnapshots collectSnapshots(boolean ioEnabled) {
        if (!ioEnabled) {
            return new TickSnapshots(
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>(),
                    new ArrayList<>()
            );
        }

        return new TickSnapshots(
                snapshotValid(host.getEnergyPorts()),
                snapshotValid(host.getInjectionPorts()),
                snapshotValid(host.getHeatVents()),
                snapshotValid(host.getHawkingVents())
        );
    }

    private void prepareExternalIo(boolean ioEnabled, int ticks, TickSnapshots snapshots) {
        if (!ioEnabled) {
            clearPendingIo();
            return;
        }

        if (hasArmedHawkingVent(snapshots.hawkingVents())) {
            requestVentingLock(Math.min(40, ticks + 1));
        }

        if (!snapshots.injectionPorts().isEmpty() && !isVentingLocked()) {
            for (PenroseInjectionPortBE port : snapshots.injectionPorts()) {
                port.tickFromController(ticks);
            }
        } else if (isVentingLocked()) {
            host.setPendingFeedMu(0);
        }
    }

    private boolean hasArmedHawkingVent(List<PenroseHawkingVentBE> vents) {
        for (PenroseHawkingVentBE vent : vents) {
            Level level = vent.getLevel();
            if (level == null) {
                continue;
            }
            if (vent.getDesiredEvap() <= 0.0) {
                continue;
            }
            if (!level.hasNeighborSignal(vent.getBlockPos())) {
                continue;
            }
            return true;
        }
        return false;
    }

    private int consumeFeedForTick(boolean ioEnabled) {
        if (!ioEnabled || isVentingLocked()) {
            host.setPendingFeedMu(0);
            return 0;
        }

        int available = Math.max(0, host.getPendingFeedMu());
        int feedThisTick = Math.min(MAX_FEED_PER_TICK, available);
        host.setPendingFeedMu(Math.max(0, available - feedThisTick));
        return feedThisTick;
    }

    private void applyPassiveHeat(double diskFlow, double massFactor, double coolPerTick) {
        double heat = host.getHeat();
        heat += HEAT_PER_SINGU_FLOW * diskFlow * massFactor;
        heat -= coolPerTick;
        if (heat < 0.0) {
            heat = 0.0;
        }
        host.setHeat(heat);
    }

    private long computeGeneratedGross(double diskFlow, double massFactor) {
        if (host.getDiskMassMu() <= 0.0) {
            return 0L;
        }

        double heatEff = computeHeatEff();
        double gross = DUTY_COMPENSATION * FE_BASE_PER_SINGU_FLOW * diskFlow * heatEff * massFactor;
        return (long) Math.max(0.0, Math.round(gross));
    }

    private EnergyUse processHeatVents(List<PenroseHeatVentBE> vents, long genBudget) {
        long consumed = 0L;

        for (PenroseHeatVentBE vent : vents) {
            Level level = vent.getLevel();
            if (level == null) {
                continue;
            }
            if (vent.getDesiredCooling() <= 0.0) {
                continue;
            }
            if (!level.hasNeighborSignal(vent.getBlockPos())) {
                continue;
            }

            long cost = PenroseHeatVentBE.computeCostForCooling(vent.getDesiredCooling());
            if (cost <= 0L) {
                continue;
            }

            EnergyUse use = spendEnergy(cost, genBudget, vent.getEnergyStorage());
            genBudget = use.remainingGenBudget();

            if (use.paid() <= 0L) {
                continue;
            }

            consumed += use.paid();

            double fraction = Math.min(1.0, (double) use.paid() / (double) cost);
            host.setHeat(host.getHeat() - vent.getDesiredCooling() * fraction);
            if (host.getHeat() < 0.0) {
                host.setHeat(0.0);
            }
        }

        return new EnergyUse(consumed, genBudget);
    }

    private EnergyUse processHawkingVents(List<PenroseHawkingVentBE> vents, long genBudget) {
        long consumed = 0L;

        for (PenroseHawkingVentBE vent : vents) {
            Level level = vent.getLevel();
            if (level == null) {
                continue;
            }
            if (vent.getDesiredEvap() <= 0.0) {
                continue;
            }
            if (!level.hasNeighborSignal(vent.getBlockPos())) {
                continue;
            }

            bumpVentingLock(2);

            long cost = PenroseHawkingVentBE.computeCostForEvap(vent.getDesiredEvap());
            if (cost <= 0L) {
                continue;
            }

            EnergyUse use = spendEnergy(cost, genBudget, vent.getEnergyStorage());
            genBudget = use.remainingGenBudget();

            if (use.paid() <= 0L) {
                continue;
            }

            consumed += use.paid();

            double fraction = Math.min(1.0, (double) use.paid() / (double) cost);
            long evapMu = Math.round(vent.getDesiredEvap() * fraction);
            if (evapMu > 0L) {
                applyEvaporationInternal(evapMu);
            }
        }

        return new EnergyUse(consumed, genBudget);
    }

    private EnergyUse spendEnergy(long cost, long genBudget, @Nullable IEnergyStorage buffer) {
        long need = cost;

        long fromGen = Math.min(genBudget, need);
        genBudget -= fromGen;
        need -= fromGen;

        long fromController = need > 0 ? takeStoredEnergy(need) : 0L;
        need -= fromController;

        int fromBuffer = 0;
        if (need > 0 && buffer != null) {
            int wanted = (int) Math.min(Integer.MAX_VALUE, need);
            fromBuffer = buffer.extractEnergy(wanted, false);
        }

        long paid = fromGen + fromController + (long) fromBuffer;
        return new EnergyUse(paid, genBudget);
    }

    private void accumulateMassDelta(long massBefore) {
        long delta = host.getBhMass() - massBefore;
        host.setSecMassDeltaAcc(host.getSecMassDeltaAcc() + delta);
        host.setSecTickCounter(host.getSecTickCounter() + 1);

        while (host.getSecTickCounter() >= 20) {
            host.setLastSecondMassDelta(host.getSecMassDeltaAcc());
            host.setSecMassDeltaAcc(0L);
            host.setSecTickCounter(host.getSecTickCounter() - 20);
        }
    }

    private boolean isOverheated() {
        return host.getHeat() >= MAX_HEAT_GK;
    }

    private boolean isMassOverflowed() {
        return host.getBhMass() >= MAX_BH_MASS_MU;
    }

    private void ensureDiskArray() {
        if (host.getDiskBatchMuByAge() == null || host.getDiskBatchMuByAge().length != DISK_WINDOW_TICKS) {
            host.setDiskBatchMuByAge(new double[DISK_WINDOW_TICKS]);
        }
    }

    private void sanitizeManagedState() {
        if (host.getDiskAge0Ptr() < 0 || host.getDiskAge0Ptr() >= DISK_WINDOW_TICKS) {
            host.setDiskAge0Ptr(0);
        }

        if (host.getDiskMassMu() < 0.0) {
            host.setDiskMassMu(0.0);
        }

        if (host.getBhMassRemainder() < 0.0) {
            host.setBhMassRemainder(0.0);
        }

        host.setDiskMassSingu(Math.max(0L, Math.round(host.getDiskMassMu())));
    }

    private double computeMassFactorSweet() {
        double half = (double) MASS_WINDOW_MU / 2.0;
        if (half <= 0.0) {
            return 1.0;
        }

        double exactMass = (double) host.getBhMass() + host.getBhMassRemainder();
        double distance = Math.abs(exactMass - (double) SWEET_SPOT_MASS_MU);
        double k = Mth.clamp(1.0 - (distance / half), 0.0, 1.0);
        return 1.0 + (MASS_FACTOR_MAX - 1.0) * k;
    }

    private double computeHeatEff() {
        double x = host.getHeat() / HEAT_PEAK_GK;
        return Mth.clamp(2.0 * x - x * x, 0.0, 1.0);
    }

    private void diskAdvanceOneTick(double injectedMuThisTick) {
        ensureDiskArray();

        host.setDiskAge0Ptr(host.getDiskAge0Ptr() - 1);
        if (host.getDiskAge0Ptr() < 0) {
            host.setDiskAge0Ptr(host.getDiskAge0Ptr() + DISK_WINDOW_TICKS);
        }

        double[] batches = host.getDiskBatchMuByAge();
        batches[host.getDiskAge0Ptr()] = 0.0;

        if (injectedMuThisTick > 0.0) {
            batches[host.getDiskAge0Ptr()] += injectedMuThisTick;
            host.setDiskMassMu(host.getDiskMassMu() + injectedMuThisTick);
        }

        double leavingMu = 0.0;
        int idx = host.getDiskAge0Ptr();
        for (int age = 0; age < DISK_WINDOW_TICKS; age++) {
            double bucketMu = batches[idx];
            if (bucketMu != 0.0) {
                leavingMu += bucketMu * DISK_DELTA_FRAC[age];
            }

            idx++;
            if (idx >= DISK_WINDOW_TICKS) {
                idx = 0;
            }
        }

        if (leavingMu < 0.0) {
            leavingMu = 0.0;
        }
        if (leavingMu > host.getDiskMassMu()) {
            leavingMu = host.getDiskMassMu();
        }

        host.setDiskMassMu(host.getDiskMassMu() - leavingMu);
        if (host.getDiskMassMu() < 0.0) {
            host.setDiskMassMu(0.0);
        }

        host.setBhMassRemainder(host.getBhMassRemainder() + leavingMu);
        if (host.getBhMassRemainder() >= 1.0) {
            long add = (long) Math.floor(host.getBhMassRemainder());
            host.setBhMass(host.getBhMass() + add);
            host.setBhMassRemainder(host.getBhMassRemainder() - (double) add);
        }

        host.setLastAccretionSingu((int) Mth.clamp(Math.round(leavingMu), 0L, (long) Integer.MAX_VALUE));
        host.setDiskMassSingu(Math.max(0L, Math.round(host.getDiskMassMu())));
    }

    private void addStoredEnergyCapped(long add) {
        if (add <= 0L) {
            return;
        }

        if (host.getStoredEnergy() >= MAX_STORED_ENERGY - add) {
            host.setStoredEnergy(MAX_STORED_ENERGY);
        } else {
            host.setStoredEnergy(host.getStoredEnergy() + add);
        }
    }

    private void applyEvaporationInternal(long evapMu) {
        if (evapMu <= 0L) {
            return;
        }

        double total = (double) host.getBhMass() + host.getBhMassRemainder() - (double) evapMu;
        double minMass = (double) INITIAL_BH_MASS_MU;
        if (total < minMass) {
            total = minMass;
        }

        long newMass = (long) Math.floor(total);
        host.setBhMass(newMass);
        host.setBhMassRemainder(total - (double) newMass);
        bumpVentingLock(2);
    }

    private void triggerMeltdown(String reason) {
        host.setBlackHoleActive(false);
        host.setBhMass(0L);
        host.setBhMassRemainder(0.0);
        host.setHeat(0.0);
        host.setStoredEnergy(0L);
        host.setStoredEnergyInDisk(0L);
        host.setLastGeneratedFePerTickGross(0L);
        host.setLastConsumedFePerTick(0L);
        host.setVentingLockTicks(0);
        clearPendingIo();
        host.setLastSecondMassDelta(0L);
        host.setSecTickCounter(0);
        host.setSecMassDeltaAcc(0L);
        clearDisk();
        host.setChanged();

        CrazyAddons.LOGGER.info("Penrose meltdown triggered: {}", reason);

        Level level = host.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (!MELTDOWN_EXPLOSIONS_ENABLED) {
            return;
        }

        BlockPos center = host.getBlockPos();

        serverLevel.explode(
                null,
                center.getX() + 0.5,
                center.getY() + 0.5,
                center.getZ() + 0.5,
                32f,
                true,
                Level.ExplosionInteraction.TNT
        );

        if (MELTDOWN_FIELD_RADIUS > 0) {
            PenroseExplosionManager.start(serverLevel, center, MELTDOWN_FIELD_RADIUS);
        }
    }

    private void bumpVentingLock(int ticks) {
        if (ticks > host.getVentingLockTicks()) {
            host.setVentingLockTicks(ticks);
        }
    }

    private <T extends BlockEntity> ArrayList<T> snapshotValid(List<T> list) {
        Level level = host.getLevel();
        if (list.isEmpty() || level == null) {
            return new ArrayList<>();
        }

        ArrayList<T> out = new ArrayList<>(list.size());
        for (T be : list) {
            if (be == null) {
                continue;
            }
            if (be.isRemoved()) {
                continue;
            }
            if (be.getLevel() != level) {
                continue;
            }
            out.add(be);
        }

        list.clear();
        list.addAll(out);
        return out;
    }

    private void tickPorts(int ticks, List<PenrosePortBE> snapshot) {
        if (!host.isFormed()) {
            return;
        }

        Level level = host.getLevel();
        if (snapshot.isEmpty() || level == null || level.isClientSide()) {
            return;
        }

        for (int i = 0; i < ticks; i++) {
            if (host.getStoredEnergy() <= 0L) {
                break;
            }

            for (PenrosePortBE port : snapshot) {
                port.tickPort(level, energyStorageView);
                if (host.getStoredEnergy() <= 0L) {
                    break;
                }
            }
        }
    }
}