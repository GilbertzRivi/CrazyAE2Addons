package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.block.AmpereMeterMenu;
import lombok.Getter;

import java.util.HashMap;

public class AmpereMeterBE extends AEBaseBlockEntity implements MenuProvider, ISyncPersistRPCBlockEntity {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted @DescSynced
    public boolean direction = false;
    @DescSynced
    public String transfer = "-";
    @DescSynced
    public String unit = "-";
    private Integer numTransfer = 0;
    private HashMap<Integer, Integer> maxTrans = new HashMap<>();

    @Persisted @DescSynced
    public int minFePerTick = 0;
    @Persisted @DescSynced
    public int maxFePerTick = 1000;

    private long lastActiveTick = -1;
    private static final long INACTIVITY_RESET_TICKS = 10;
    private boolean needsNetworkRebuild = false;

    public AmpereMeterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.AMPERE_METER_BE.get(), pos, blockState);
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new AmpereMeterMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.AMPERE_METER_MENU.get(), player, locator);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmpereMeterBE be) {
        if (level.isClientSide()) return;

        if (be.needsNetworkRebuild) {
            be.needsNetworkRebuild = false;
            level.invalidateCapabilities(pos);
            level.updateNeighborsAt(pos, state.getBlock());
        }

        if (be.lastActiveTick < 0) {
            be.lastActiveTick = level.getGameTime();
        }

        long gameTime = level.getGameTime();
        if (gameTime - be.lastActiveTick > INACTIVITY_RESET_TICKS) {
            be.resetTransfer();
        }
    }

    public void resetTransfer() {
        this.lastActiveTick = -1;
        this.transfer = "-";
        this.numTransfer = 0;
        this.unit = "-";
        this.maxTrans.clear();
        setChanged();
    }

    public int getComparatorSignal() {
        if (this.numTransfer <= this.minFePerTick) return 0;
        if (this.numTransfer >= this.maxFePerTick) return 15;
        int range = this.maxFePerTick - this.minFePerTick;
        return Math.max(1, (int) (15.0 * (this.numTransfer - this.minFePerTick) / range));
    }

    public void updateComparator() {
        Level level = getLevel();
        if (level != null) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        needsNetworkRebuild = true;
    }

    public void markActive() {
        if (getLevel() != null){
            this.lastActiveTick = getLevel().getGameTime();
        }
    }

    private void sendUpdate() {
        if (getLevel() != null) {
            setChanged();
            getLevel().sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            getLevel().invalidateCapabilities(getBlockPos());
        }
    }

    public void setDirection(boolean direction) {
        if (this.direction == direction) return;
        this.direction = direction;
        setChanged();
        needsNetworkRebuild = true;
        resetTransfer();
        sendUpdate();
    }

    public void setMinFePerTick(int min) {
        min = Math.max(0, min);
        if (min > this.maxFePerTick) min = this.maxFePerTick;
        if (this.minFePerTick == min) return;

        this.minFePerTick = min;
        setChanged();
        updateComparator();
        sendUpdate();
    }

    public void setMaxFePerTick(int max) {
        max = Math.max(0, max);
        if (max < this.minFePerTick) max = this.minFePerTick;
        if (this.maxFePerTick == max) return;

        this.maxFePerTick = max;
        setChanged();
        updateComparator();
        sendUpdate();
    }

    private static final IEnergyStorage DUMMY_OUTPUT = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return false; }
    };

    public IEnergyStorage getEnergyStorage(Direction dir) {
        Direction inputSide = this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());
        Direction outputSide = !this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());

        if (dir == outputSide) return DUMMY_OUTPUT;
        if (dir != inputSide) return null;

        return new IEnergyStorage() {
            private IEnergyStorage getOutputStorage() {
                Level level = AmpereMeterBE.this.getLevel();
                if (level == null) return null;
                BlockPos outputPos = AmpereMeterBE.this.getBlockPos().relative(outputSide);
                return level.getCapability(Capabilities.EnergyStorage.BLOCK, outputPos, outputSide.getOpposite());
            }

            @Override
            public int receiveEnergy(int maxReceive, boolean simulate) {
                IEnergyStorage output = getOutputStorage();
                if (output == null || !output.canReceive()) return 0;

                int transferred = output.receiveEnergy(maxReceive, simulate);

                if (!simulate && transferred > 0) {
                    AmpereMeterBE.this.markActive();
                    int oldSignal = AmpereMeterBE.this.getComparatorSignal();

                    AmpereMeterBE.this.maxTrans.put(AmpereMeterBE.this.maxTrans.size(), transferred);
                    if (AmpereMeterBE.this.maxTrans.size() > 5) {
                        AmpereMeterBE.this.maxTrans.remove(0);
                        HashMap<Integer, Integer> newMap = new HashMap<>();
                        int i = 0;
                        for (int value : AmpereMeterBE.this.maxTrans.values()) {
                            newMap.put(i++, value);
                        }
                        AmpereMeterBE.this.maxTrans = newMap;
                    }
                    int max = AmpereMeterBE.this.maxTrans.values().stream().max(Integer::compare).orElse(0);
                    AmpereMeterBE.this.numTransfer = max;
                    AmpereMeterBE.this.transfer = Utils.shortenNumber(max);
                    AmpereMeterBE.this.unit = "FE/t";

                    AmpereMeterBE.this.setChanged();
                    int newSignal = AmpereMeterBE.this.getComparatorSignal();
                    if (oldSignal != newSignal) {
                        AmpereMeterBE.this.updateComparator();
                    }
                }

                return transferred;
            }

            @Override
            public int extractEnergy(int maxExtract, boolean simulate) { return 0; }

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
            public boolean canExtract() { return false; }

            @Override
            public boolean canReceive() { return true; }
        };
    }
}
