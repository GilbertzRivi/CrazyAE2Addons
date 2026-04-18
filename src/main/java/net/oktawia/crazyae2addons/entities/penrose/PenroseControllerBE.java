package net.oktawia.crazyae2addons.entities.penrose;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DropSaved;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.blocks.penrose.PenroseControllerBlock;
import net.oktawia.crazyae2addons.blocks.penrose.PenroseFrameBlock;
import net.oktawia.crazyae2addons.client.renderer.preview.multiblock.MultiblockPreviewHost;
import net.oktawia.crazyae2addons.client.renderer.preview.multiblock.MultiblockPreviewInfo;
import net.oktawia.crazyae2addons.client.renderer.preview.multiblock.PreviewRegistry;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.penrose.PenroseLogic;
import net.oktawia.crazyae2addons.logic.interfaces.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseControllerMenu;
import net.oktawia.crazyae2addons.multiblock.AbstractMultiblockControllerBE;
import net.oktawia.crazyae2addons.multiblock.CrazyMultiblocks;
import net.oktawia.crazyae2addons.multiblock.MultiblockDefinition;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PenroseControllerBE extends AbstractMultiblockControllerBE
        implements InternalInventoryHost, MenuProvider, MultiblockPreviewHost, IMenuOpeningBlockEntity {

    public static final long MU_PER_SINGU = 1L;

    private final PenroseLogic logic;

    // ===== core persisted machine state =====

    @Getter @Setter
    @Persisted
    @DropSaved
    private int diskAge0Ptr = 0;

    @Getter @Setter
    @Persisted
    @DropSaved
    private double[] diskBatchMuByAge = new double[PenroseLogic.DISK_WINDOW_TICKS];

    @Getter @Setter
    @Persisted
    @DropSaved
    private double diskMassMu = 0.0;

    @Getter @Setter
    @Persisted
    @DropSaved
    private double bhMassRemainder = 0.0;

    @Getter @Setter
    @Persisted
    @DescSynced
    @DropSaved
    private long storedEnergy = 0L;

    @Getter @Setter
    @Persisted
    @DescSynced
    @DropSaved
    private boolean blackHoleActive = false;

    @Getter @Setter
    @Persisted
    @DescSynced
    @DropSaved
    private long bhMass = 0L;

    @Getter @Setter
    @Persisted
    @DescSynced
    @DropSaved
    private double heat = 0.0;

    @Getter @Setter
    @Persisted
    private int pendingFeedMu = 0;

    @Getter @Setter
    @Persisted
    private double pendingCoolingHeat = 0.0;

    @Getter @Setter
    @Persisted
    private long pendingEvaporation = 0L;

    @Getter @Setter
    @Persisted
    @DropSaved
    private int ventingLockTicks = 0;

    @Getter
    @Persisted(subPersisted = true)
    @DescSynced
    @DropSaved
    private final AppEngInternalInventory diskInv = new AppEngInternalInventory(
            this,
            1,
            1,
            new IAEItemFilter() {
                @Override
                public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                    if (stack.getItem() == AEItems.ITEM_CELL_1K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_4K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_16K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_64K.asItem()
                            || stack.getItem() == AEItems.ITEM_CELL_256K.asItem()) {
                        var cellInv = StorageCells.getCellInventory(stack, null);
                        if (cellInv == null) {
                            return false;
                        }
                        var stacks = cellInv.getAvailableStacks();
                        if (stacks.isEmpty()) {
                            return true;
                        }
                        return stacks.size() == 1
                                && Objects.equals(
                                stacks.getFirstKey(),
                                AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get())
                        );
                    }
                    return false;
                }
            }
    );

    // ===== derived / ui state =====

    @Getter @Setter
    @DescSynced
    private long storedEnergyInDisk = 0L;

    @Getter @Setter
    @DescSynced
    private long lastGeneratedFePerTickGross = 0L;

    @Getter @Setter
    @DescSynced
    private long lastConsumedFePerTick = 0L;

    @Getter @Setter
    @DescSynced
    private long lastSecondMassDelta = 0L;

    @Getter @Setter
    @DescSynced
    private int lastFeedMu = 0;

    @Getter @Setter
    @DescSynced
    private int lastAccretionSingu = 0;

    @Getter @Setter
    @DescSynced
    private long diskMassSingu = 0L;

    @Getter @Setter
    private int secTickCounter = 0;

    @Getter @Setter
    private long secMassDeltaAcc = 0L;

    // ===== client / preview =====

    @DescSynced
    private boolean preview = false;

    @OnlyIn(Dist.CLIENT)
    private MultiblockPreviewInfo previewInfo;

    @Getter @Setter
    @DescSynced
    private double maxHeatGK = PenroseLogic.MAX_HEAT_GK;

    @Getter @Setter
    @DescSynced
    private double heatPeakGk = PenroseLogic.HEAT_PEAK_GK;

    @Getter @Setter
    @DescSynced
    private long initialBhMass = PenroseLogic.INITIAL_BH_MASS_MU;

    @Getter @Setter
    @DescSynced
    private long maxBhMass = PenroseLogic.MAX_BH_MASS_MU;

    @Getter
    @DescSynced
    private boolean formed = false;

    // ===== runtime member lists =====

    @Getter
    private final List<PenroseHeatVentBE> heatVents = new ArrayList<>();

    @Getter
    private final List<PenroseHawkingVentBE> hawkingVents = new ArrayList<>();

    @Getter
    private final List<PenroseInjectionPortBE> injectionPorts = new ArrayList<>();

    @Getter
    private final List<PenrosePortBE> energyPorts = new ArrayList<>();

    @Getter @Setter
    private ServerPlayer placer;

    public PenroseControllerBE(BlockPos pos, BlockState state) {
        super(
                CrazyBlockEntityRegistrar.PENROSE_CONTROLLER_BE.get(),
                pos,
                state,
                new ItemStack(CrazyBlockRegistrar.PENROSE_CONTROLLER.get().asItem()),
                2.0F
        );
        this.logic = new PenroseLogic(this);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (getLevel() != null && getLevel().isClientSide()) {
            PreviewRegistry.register(this);
        }
    }

    @Override
    public void setRemoved() {
        if (getLevel() != null && getLevel().isClientSide()) {
            PreviewRegistry.unregister(this);
        }
        super.setRemoved();
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
        if (level == null) {
            return;
        }

        BlockState state = level.getBlockState(getBlockPos());
        if (state.hasProperty(PenroseControllerBlock.FORMED)
                && state.getValue(PenroseControllerBlock.FORMED) != formed) {
            level.setBlock(getBlockPos(), state.setValue(PenroseControllerBlock.FORMED, formed), 3);
        }
    }

    @Override
    protected void setMemberFormedState(BlockPos pos, boolean formed) {
        Level level = getLevel();
        if (level == null || isClientSide()) {
            return;
        }

        BlockState state = level.getBlockState(pos);
        if (state.hasProperty(PenroseFrameBlock.FORMED)
                && state.getValue(PenroseFrameBlock.FORMED) != formed) {
            level.setBlock(pos, state.setValue(PenroseFrameBlock.FORMED, formed), 3);
        }
    }

    @Override
    public void loadManagedPersistentData(HolderLookup.Provider provider, CompoundTag tag) {
        super.loadManagedPersistentData(provider, tag);
        logic.onManagedLoad();
    }

    @Override
    protected void afterDisformed() {
        logic.clearPendingAfterDisformed();
    }

    public void registerPort(PenrosePortBE port) {
        if (!energyPorts.contains(port)) {
            energyPorts.add(port);
        }
    }

    public void unregisterPort(PenrosePortBE port) {
        energyPorts.remove(port);
    }

    public void registerHeatVent(PenroseHeatVentBE vent) {
        if (!heatVents.contains(vent)) {
            heatVents.add(vent);
        }
    }

    public void unregisterHeatVent(PenroseHeatVentBE vent) {
        heatVents.remove(vent);
    }

    public void registerHawkingVent(PenroseHawkingVentBE vent) {
        if (!hawkingVents.contains(vent)) {
            hawkingVents.add(vent);
        }
    }

    public void unregisterHawkingVent(PenroseHawkingVentBE vent) {
        hawkingVents.remove(vent);
    }

    public void registerInjectionPort(PenroseInjectionPortBE port) {
        if (!injectionPorts.contains(port)) {
            injectionPorts.add(port);
        }
    }

    public void unregisterInjectionPort(PenroseInjectionPortBE port) {
        injectionPorts.remove(port);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return logic.getTickingRequest(node);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        super.tickingRequest(node, ticksSinceLastCall);
        return logic.tick(ticksSinceLastCall);
    }

    public boolean canStartBlackHole() {
        return logic.canStartBlackHole();
    }

    public boolean startBlackHole() {
        return logic.startBlackHole();
    }

    public void addFeedMu(int singus) {
        logic.addFeedMu(singus);
    }

    public boolean isVentingLocked() {
        return logic.isVentingLocked();
    }

    public long getBlackHoleMass() {
        return logic.getBlackHoleMass();
    }

    public @Nullable IEnergyStorage getEnergyStorage(@Nullable Direction dir) {
        return logic.getEnergyStorage(dir);
    }

    public long takeStoredEnergy(long want) {
        return logic.takeStoredEnergy(want);
    }

    public void recomputeUiEnergy() {
        logic.recomputeUiEnergy();
    }

    public void requestVentingLock(int ticks) {
        logic.requestVentingLock(ticks);
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
        return new PenroseControllerMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public void openMenu(Player player, MenuHostLocator menuHostLocator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(), player, menuHostLocator);
    }

    public void setPreviewEnabled(boolean preview) {
        this.preview = preview;
        if (!preview) {
            this.previewInfo = null;
        }
        setChanged();
    }

    @Override
    public boolean isPreviewEnabled() {
        return preview;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public @Nullable MultiblockPreviewInfo getPreviewInfo() {
        return previewInfo;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void setPreviewInfo(@Nullable MultiblockPreviewInfo previewInfo) {
        this.previewInfo = previewInfo;
    }

    @Override
    public MultiblockDefinition getPreviewDefinition() {
        return getMultiblockDefinition();
    }

    @Override
    public BlockPos getPreviewOrigin() {
        return getBlockPos();
    }

    @Override
    public Direction getPreviewFacing() {
        return getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
    }

    @Override
    public BlockState getPreviewState(MultiblockDefinition.PatternEntry entry, MultiblockDefinition.SymbolDef symbol) {
        BlockState state = symbol.blocks().get(0).defaultBlockState();

        if (state.hasProperty(PenroseFrameBlock.FORMED)) {
            state = state.setValue(PenroseFrameBlock.FORMED, true);
        }

        return state;
    }

    @Override
    protected void invalidateMemberCapabilities() {
        super.invalidateMemberCapabilities();

        for (BlockPos pos : this.multiblockState.getBlocksBySymbol('P')) {
            invalidateCapabilitiesAt(pos);
        }
    }
}