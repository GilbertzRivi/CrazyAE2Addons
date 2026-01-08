package net.oktawia.crazyae2addons.compat.GregTech;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class GTAmpereMeterBE extends AmpereMeterBE{
    private long lastTick = -1;
    private int tickAmps = 0;
    private long tickVolt = 0;

    public GTAmpereMeterBE(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    @Override
    public void resetTransfer() {
        String oldUnit = this.unit;

        super.resetTransfer();

        this.lastTick = -1;
        this.tickAmps = 0;
        this.tickVolt = 0;
        this.maxTrans.clear();

        if (oldUnit != null && oldUnit.startsWith("A")) {
            this.unit = oldUnit;
        } else {
            this.unit = "A";
        }

        if (this.getMenu() != null) {
            this.getMenu().unit = this.unit;
        }
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, @Nullable Direction dir) {
        Direction inputSide = this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());
        Direction outputSide = !this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());
        if (cap == ForgeCapabilities.ENERGY && dir == inputSide) {
            return LazyOptional.of(() -> feLogicInput).cast();
        } else if (cap == ForgeCapabilities.ENERGY && dir == outputSide) {
            return LazyOptional.of(() -> feLogicOutput).cast();
        } else if (cap == GTCapability.CAPABILITY_ENERGY_CONTAINER){
            return LazyOptional.of(() -> euLogic).cast();
        }
        return super.getCapability(cap, dir);
    }

    public IEnergyContainer euLogic = new IEnergyContainer() {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long volt, long amp) {
            if (GTAmpereMeterBE.this.getLevel() == null) return 0;

            GTAmpereMeterBE.this.markActive();

            int oldSignal = GTAmpereMeterBE.this.getComparatorSignal();

            Direction outputSide = !GTAmpereMeterBE.this.direction
                    ? Utils.getRightDirection(getBlockState())
                    : Utils.getLeftDirection(getBlockState());

            BlockEntity output = GTAmpereMeterBE.this.getLevel().getBlockEntity(
                    GTAmpereMeterBE.this.getBlockPos().relative(outputSide)
            );

            if (output == null) return 0;

            AtomicLong transferred = new AtomicLong();

            output.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, outputSide.getOpposite()).ifPresent(out -> {
                transferred.set(out.acceptEnergyFromNetwork(outputSide.getOpposite(), volt, amp));
            });

            long currentTick = GTAmpereMeterBE.this.getLevel().getGameTime();
            int transferredAmps = (int) transferred.get();

            if (currentTick == GTAmpereMeterBE.this.lastTick) {
                GTAmpereMeterBE.this.tickAmps += transferredAmps;
                if (volt > GTAmpereMeterBE.this.tickVolt) {
                    GTAmpereMeterBE.this.tickVolt = volt;
                }
            } else {
                GTAmpereMeterBE.this.lastTick = currentTick;
                GTAmpereMeterBE.this.tickAmps = transferredAmps;
                GTAmpereMeterBE.this.tickVolt = volt;
            }

            Map.Entry<Long, String> voltageTier = Utils.voltagesMap.ceilingEntry(GTAmpereMeterBE.this.tickVolt);
            String tierName = voltageTier != null ? voltageTier.getValue() : "???";
            String unitLabel = "A (%s)".formatted(tierName);

            if (!Objects.equals(GTAmpereMeterBE.this.unit, unitLabel)) {
                GTAmpereMeterBE.this.maxTrans.clear();
                GTAmpereMeterBE.this.unit = unitLabel;
            }

            GTAmpereMeterBE.this.maxTrans.put(GTAmpereMeterBE.this.maxTrans.size(), GTAmpereMeterBE.this.tickAmps);

            if (GTAmpereMeterBE.this.maxTrans.size() > 5) {
                GTAmpereMeterBE.this.maxTrans.remove(0);
                HashMap<Integer, Integer> newMap = new HashMap<>();
                int i = 0;
                for (int value : GTAmpereMeterBE.this.maxTrans.values()) {
                    newMap.put(i++, value);
                }
                GTAmpereMeterBE.this.maxTrans = newMap;
            }

            int max = GTAmpereMeterBE.this.maxTrans.values().stream().max(Integer::compare).orElse(0);

            GTAmpereMeterBE.this.transfer = Utils.shortenNumber(max);
            GTAmpereMeterBE.this.numTransfer = max;

            if (GTAmpereMeterBE.this.getMenu() != null) {
                GTAmpereMeterBE.this.getMenu().unit = GTAmpereMeterBE.this.unit;
                GTAmpereMeterBE.this.getMenu().transfer = GTAmpereMeterBE.this.transfer;
            }

            GTAmpereMeterBE.this.setChanged();

            int newSignal = GTAmpereMeterBE.this.getComparatorSignal();
            if (oldSignal != newSignal) {
                GTAmpereMeterBE.this.updateComparator();
            }

            return transferred.get();
        }

        @Override public boolean inputsEnergy(Direction direction) { return true; }
        @Override public long changeEnergy( long l ) { return 0; }
        @Override public long getEnergyStored() { return 0; }
        @Override public long getEnergyCapacity() { return Integer.MAX_VALUE; }

        @Override
        public long getInputAmperage() {
            if (GTAmpereMeterBE.this.getLevel() == null) return 0;
            Direction outputSide = !GTAmpereMeterBE.this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());
            BlockEntity output = GTAmpereMeterBE.this.getLevel().getBlockEntity(GTAmpereMeterBE.this.getBlockPos().relative(outputSide));
            if (output == null) return 0;
            AtomicLong amperage = new AtomicLong();
            output.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, outputSide.getOpposite()).ifPresent(out -> {
                amperage.set(out.getInputAmperage());
            });
            return amperage.get();
        }

        @Override
        public long getInputVoltage() {
            if (GTAmpereMeterBE.this.getLevel() == null) return 0;
            Direction outputSide = !GTAmpereMeterBE.this.direction ? Utils.getRightDirection(getBlockState()) : Utils.getLeftDirection(getBlockState());
            BlockEntity output = GTAmpereMeterBE.this.getLevel().getBlockEntity(GTAmpereMeterBE.this.getBlockPos().relative(outputSide));
            if (output == null) return 0;
            AtomicLong voltage = new AtomicLong();
            output.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, outputSide.getOpposite()).ifPresent(out -> {
                voltage.set(out.getInputVoltage());
            });
            return voltage.get();
        }
    };
}
