package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.block.AmpereMeterMenu;
import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import net.oktawia.crazyae2addons.util.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;

public class AmpereMeterBE extends AEBaseBlockEntity
        implements MenuProvider, IManagedBEHelper, IMenuOpeningBlockEntity {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(AmpereMeterBE.class);

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @Getter
    private boolean direction = false;

    @DescSynced
    @Getter
    private String transfer = "-";

    @DescSynced
    @Getter
    private String unit = "FE/t";

    private int numTransfer = 0;
    protected final ArrayDeque<Integer> recentTransfers = new ArrayDeque<>(5);

    @Persisted
    @DescSynced
    @Getter
    private int minFePerTick = 0;

    @Persisted
    @DescSynced
    @Getter
    private int maxFePerTick = 1000;

    @DescSynced
    @Getter
    protected boolean amperesMode = false;

    protected long lastActiveTick = -1;
    private static final long INACTIVITY_RESET_TICKS = 10;

    private LazyOptional<IEnergyStorage> inputCap = LazyOptional.empty();
    private LazyOptional<IEnergyStorage> outputCap = LazyOptional.empty();

    private static final IEnergyStorage DUMMY_OUTPUT = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            return 0;
        }

        @Override
        public int getMaxEnergyStored() {
            return 0;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    private final IEnergyStorage INPUT_STORAGE = new IEnergyStorage() {
        private IEnergyStorage getOutputStorage() {
            Level level = AmpereMeterBE.this.getLevel();
            if (level == null) {
                return null;
            }

            Direction outputSide = getOutputSide();
            BlockPos outputPos = AmpereMeterBE.this.getBlockPos().relative(outputSide);
            BlockEntity be = level.getBlockEntity(outputPos);
            if (be == null) {
                return null;
            }

            if (be instanceof AmpereMeterBE) {
                return null;
            }

            return be.getCapability(ForgeCapabilities.ENERGY, outputSide.getOpposite()).orElse(null);
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            IEnergyStorage output = getOutputStorage();
            if (output == null || !output.canReceive()) {
                return 0;
            }

            int transferred = output.receiveEnergy(maxReceive, simulate);

            if (!simulate && transferred > 0) {
                AmpereMeterBE.this.markActive();

                if (AmpereMeterBE.this.recentTransfers.size() >= 5) {
                    AmpereMeterBE.this.recentTransfers.removeFirst();
                }
                AmpereMeterBE.this.recentTransfers.addLast(transferred);

                int max = AmpereMeterBE.this.recentTransfers.stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElse(0);

                AmpereMeterBE.this.setDisplayedTransfer(max, "FE/t", false);
            }

            return transferred;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            IEnergyStorage output = getOutputStorage();
            return output != null ? output.getEnergyStored() : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            IEnergyStorage output = getOutputStorage();
            return output != null ? output.getMaxEnergyStored() : Integer.MAX_VALUE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public AmpereMeterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.AMPERE_METER_BE.get(), pos, blockState);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveManagedData(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        loadManagedData(tag);
        super.loadTag(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveManagedData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        loadManagedData(tag);
        super.handleUpdateTag(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AmpereMeterMenu(id, inventory, this);
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        if (getLevel() != null && !getLevel().isClientSide()) {
            forceSyncManaged();
        }
        MenuOpener.open(CrazyMenuRegistrar.AMPERE_METER_MENU.get(), player, locator);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        rebuildCaps();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        inputCap.invalidate();
        outputCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        rebuildCaps();
    }

    private void rebuildCaps() {
        inputCap.invalidate();
        outputCap.invalidate();

        inputCap = LazyOptional.of(() -> INPUT_STORAGE);
        outputCap = LazyOptional.of(() -> DUMMY_OUTPUT);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ENERGY && side != null) {
            if (side == getInputSide()) {
                return inputCap.cast();
            }
            if (side == getOutputSide()) {
                return outputCap.cast();
            }
        }

        return super.getCapability(cap, side);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmpereMeterBE be) {
        if (level.isClientSide()) {
            return;
        }

        long gameTime = level.getGameTime();
        if (be.lastActiveTick >= 0 && gameTime - be.lastActiveTick > INACTIVITY_RESET_TICKS) {
            be.clearDisplayedTransfer();
        }
    }

    protected Direction getInputSide() {
        return this.direction
                ? Utils.getRightDirection(getBlockState())
                : Utils.getLeftDirection(getBlockState());
    }

    protected Direction getOutputSide() {
        return !this.direction
                ? Utils.getRightDirection(getBlockState())
                : Utils.getLeftDirection(getBlockState());
    }

    public void markActive() {
        if (getLevel() != null) {
            this.lastActiveTick = getLevel().getGameTime();
        }
    }

    public int getComparatorSignal() {
        if (this.numTransfer <= this.minFePerTick) {
            return 0;
        }
        if (this.numTransfer >= this.maxFePerTick) {
            return 15;
        }

        int range = this.maxFePerTick - this.minFePerTick;
        if (range <= 0) {
            return 15;
        }

        return Math.max(1, (int) (15.0 * (this.numTransfer - this.minFePerTick) / range));
    }

    public void updateComparator() {
        Level level = getLevel();
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    private void syncVisuals() {
        setChanged();
        if (getLevel() != null) {
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
        syncManaged();
    }

    public void setDirection(boolean direction) {
        if (this.direction == direction) {
            return;
        }

        this.direction = direction;
        clearDisplayedTransfer();
        rebuildCaps();

        if (getLevel() != null) {
            getLevel().updateNeighborsAt(getBlockPos(), getBlockState().getBlock());
        }

        syncVisuals();
    }

    public void setMinFePerTick(int min) {
        min = Math.max(0, min);
        if (min > this.maxFePerTick) {
            min = this.maxFePerTick;
        }
        if (this.minFePerTick == min) {
            return;
        }

        int oldSignal = getComparatorSignal();

        this.minFePerTick = min;
        setChanged();
        syncManaged();

        int newSignal = getComparatorSignal();
        if (oldSignal != newSignal) {
            updateComparator();
        }
    }

    public void setMaxFePerTick(int max) {
        max = Math.max(0, max);
        if (max < this.minFePerTick) {
            max = this.minFePerTick;
        }
        if (this.maxFePerTick == max) {
            return;
        }

        int oldSignal = getComparatorSignal();

        this.maxFePerTick = max;
        setChanged();
        syncManaged();

        int newSignal = getComparatorSignal();
        if (oldSignal != newSignal) {
            updateComparator();
        }
    }

    protected void setDisplayedTransfer(int transferValue, String unitLabel, boolean ampsMode) {
        int oldSignal = getComparatorSignal();

        this.amperesMode = ampsMode;
        this.numTransfer = Math.max(0, transferValue);
        this.transfer = transferValue > 0 ? Utils.shortenNumber(transferValue) : "-";
        this.unit = unitLabel != null ? unitLabel : (ampsMode ? "A" : "FE/t");

        setChanged();
        syncManaged();

        int newSignal = getComparatorSignal();
        if (oldSignal != newSignal) {
            updateComparator();
        }
    }

    protected void clearDisplayedTransfer() {
        int oldSignal = getComparatorSignal();

        this.lastActiveTick = -1;
        this.numTransfer = 0;
        this.transfer = "-";
        this.unit = this.amperesMode ? "A" : "FE/t";
        this.recentTransfers.clear();

        setChanged();
        syncManaged();

        int newSignal = getComparatorSignal();
        if (oldSignal != newSignal) {
            updateComparator();
        }
    }
}