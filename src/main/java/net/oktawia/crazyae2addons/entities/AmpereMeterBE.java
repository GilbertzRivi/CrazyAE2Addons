package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.AEBaseBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.menus.AmpereMeterMenu;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class AmpereMeterBE extends AEBaseBlockEntity implements MenuProvider {

    public AmpereMeterMenu menu;
    public boolean direction = false;
    public String transfer = "-";
    public String unit = "-";
    public Integer numTransfer = 0;
    public HashMap<Integer, Integer> maxTrans = new HashMap<>();
    private int lastTick = 0;
    private long secondBuffer = 0;

    public int minFePerTick = 0;
    public int maxFePerTick = 1000;

    private long lastActiveTick = -1;
    private static final long INACTIVITY_RESET_TICKS = 10;

    public AmpereMeterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.AMPERE_METER_BE.get(), pos, blockState);
    }

    public void setMenu(AmpereMeterMenu menu){
        this.menu = menu;
    }

    public AmpereMeterMenu getMenu(){
        return this.menu;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player pPlayer) {
        return new AmpereMeterMenu(i, inventory, this);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if(data.contains("dir")){
            this.direction = data.getBoolean("dir");
        }
        if (data.contains("minFe")) {
            this.minFePerTick = data.getInt("minFe");
        }
        if (data.contains("maxFe")) {
            this.maxFePerTick = data.getInt("maxFe");
        }

        if (this.minFePerTick < 0) this.minFePerTick = 0;
        if (this.maxFePerTick < this.minFePerTick) this.maxFePerTick = this.minFePerTick;
    }

    @Override
    public void saveAdditional(CompoundTag data){
        super.saveAdditional(data);
        data.putBoolean("dir", this.direction);
        data.putInt("minFe", this.minFePerTick);
        data.putInt("maxFe", this.maxFePerTick);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.ampere_meter");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.AMPERE_METER_MENU.get(), player, locator);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, AmpereMeterBE be) {
        if (level.isClientSide()) return;

        if (be.lastActiveTick == -1) return;

        long now = level.getGameTime();
        if ((now - be.lastActiveTick) >= INACTIVITY_RESET_TICKS) {
            be.lastActiveTick = -1;

            boolean hasValue = be.numTransfer != null && be.numTransfer != 0;
            boolean hasBuffer = be.secondBuffer != 0;

            if (hasValue || hasBuffer) {
                be.resetTransfer();
            }
        }
    }

    public void markActive() {
        if (this.level == null) return;
        this.lastActiveTick = this.level.getGameTime();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction dir) {
        Direction inputSide = this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());
        Direction outputSide = !this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());
        if (cap == ForgeCapabilities.ENERGY && dir == inputSide) {
            return LazyOptional.of(() -> feLogicInput).cast();
        } else if (cap == ForgeCapabilities.ENERGY && dir == outputSide) {
            return LazyOptional.of(() -> feLogicOutput).cast();
        }
        return super.getCapability(cap, dir);
    }

    public int getComparatorSignal() {
        int min = this.minFePerTick;
        int max = this.maxFePerTick;
        int value = this.numTransfer == null ? 0 : this.numTransfer;

        if (value < 0) value = 0;
        if (min < 0) min = 0;
        if (max < min) max = min;

        if (max == min) {
            return value >= max ? 15 : 0;
        }

        if (value <= min) return 0;
        if (value >= max) return 15;

        long scaled = (long) (value - min) * 15L;
        long range = (long) (max - min);

        return (int) (scaled / range);
    }

    public void updateComparator() {
        if (level != null && !level.isClientSide()) {
            level.updateNeighbourForOutputSignal(getBlockPos(), getBlockState().getBlock());
        }
    }

    public void resetTransfer() {
        this.secondBuffer = 0;
        this.lastTick = -1;
        this.lastActiveTick = -1;

        this.numTransfer = 0;
        this.transfer = "0";

        if (this.getMenu() != null) {
            this.getMenu().unit = this.unit;
            this.getMenu().transfer = this.transfer;
        }

        this.setChanged();
        this.updateComparator();
    }

    public IEnergyStorage feLogicInput = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (AmpereMeterBE.this.getLevel() == null) return 0;

            if (!simulate) {
                AmpereMeterBE.this.markActive();
            }

            Direction outputSide = !AmpereMeterBE.this.direction
                    ? Utils.getRightDirection(getBlockState())
                    : Utils.getLeftDirection(getBlockState());

            BlockEntity output = AmpereMeterBE.this.getLevel().getBlockEntity(
                    AmpereMeterBE.this.getBlockPos().relative(outputSide)
            );

            if (output == null) return 0;

            AtomicInteger transferred = new AtomicInteger();

            output.getCapability(ForgeCapabilities.ENERGY, outputSide.getOpposite()).ifPresent(out -> {
                transferred.set(out.receiveEnergy(maxReceive, simulate));
            });

            if (level != null && level.getServer() != null) {
                int currentTick = level.getServer().getTickCount();

                if (!simulate) {
                    secondBuffer += transferred.get();
                }

                if (lastTick != -1 && (currentTick - lastTick) >= 20) {
                    int fePerTick = Math.toIntExact(secondBuffer / (currentTick - lastTick));
                    AmpereMeterBE.this.unit = "FE/t";
                    AmpereMeterBE.this.numTransfer = fePerTick;
                    AmpereMeterBE.this.transfer = Utils.shortenNumber(fePerTick);

                    if (AmpereMeterBE.this.getMenu() != null) {
                        AmpereMeterBE.this.getMenu().unit = AmpereMeterBE.this.unit;
                        AmpereMeterBE.this.getMenu().transfer = AmpereMeterBE.this.transfer;
                    }

                    AmpereMeterBE.this.updateComparator();

                    secondBuffer = 0;
                    lastTick = currentTick;
                } else if (lastTick == -1) {
                    lastTick = currentTick;
                }
            }

            return transferred.get();
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) { return 0; }
        @Override public int getEnergyStored() { return 0; }
        @Override public int getMaxEnergyStored() { return Integer.MAX_VALUE; }
        @Override public boolean canExtract() { return false; }
        @Override public boolean canReceive() { return true; }
    };

    public IEnergyStorage feLogicOutput = new IEnergyStorage() {
        @Override public int receiveEnergy(int maxReceive, boolean simulate) { return 0; }
        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (AmpereMeterBE.this.getLevel() == null) return 0;

            if (!simulate) {
                AmpereMeterBE.this.markActive();
            }

            Direction inputSide = AmpereMeterBE.this.direction
                    ? Utils.getRightDirection(getBlockState())
                    : Utils.getLeftDirection(getBlockState());

            BlockEntity input = AmpereMeterBE.this.getLevel().getBlockEntity(
                    AmpereMeterBE.this.getBlockPos().relative(inputSide)
            );

            if (input == null) return 0;

            AtomicInteger transferred = new AtomicInteger();

            input.getCapability(ForgeCapabilities.ENERGY, inputSide.getOpposite()).ifPresent(out -> {
                transferred.set(out.receiveEnergy(maxExtract, simulate));
            });

            if (level != null && level.getServer() != null) {
                int currentTick = level.getServer().getTickCount();

                if (!simulate) {
                    secondBuffer += transferred.get();
                }

                if (lastTick != -1 && (currentTick - lastTick) >= 20) {
                    int fePerTick = Math.toIntExact(secondBuffer / (currentTick - lastTick));

                    AmpereMeterBE.this.unit = "FE/t";
                    AmpereMeterBE.this.numTransfer = fePerTick;
                    AmpereMeterBE.this.transfer = Utils.shortenNumber(fePerTick);

                    if (AmpereMeterBE.this.getMenu() != null) {
                        AmpereMeterBE.this.getMenu().unit = AmpereMeterBE.this.unit;
                        AmpereMeterBE.this.getMenu().transfer = AmpereMeterBE.this.transfer;
                    }

                    AmpereMeterBE.this.updateComparator();

                    secondBuffer = 0;
                    lastTick = currentTick;
                } else if (lastTick == -1) {
                    lastTick = currentTick;
                }
            }

            return transferred.get();
        }

        @Override public int getEnergyStored() { return Integer.MAX_VALUE; }
        @Override public int getMaxEnergyStored() { return 0; }
        @Override public boolean canExtract() { return true; }
        @Override public boolean canReceive() { return false; }
    };
}
