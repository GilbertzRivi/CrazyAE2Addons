package net.oktawia.crazyae2addons.compat.GregTech;

import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;
import net.oktawia.crazyae2addons.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class GTAmpereMeterBE extends AmpereMeterBE {

    private long lastTick = -1;
    private int tickAmps = 0;
    private long tickVolt = 0;

    private final ArrayDeque<Integer> recentAmpTransfers = new ArrayDeque<>(5);
    private LazyOptional<IEnergyContainer> gtEnergyCap = LazyOptional.empty();

    public GTAmpereMeterBE(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        rebuildGtCap();
        this.amperesMode = true;
        clearDisplayedTransfer();

        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        gtEnergyCap.invalidate();
    }

    @Override
    public void reviveCaps() {
        super.reviveCaps();
        rebuildGtCap();
    }

    private void rebuildGtCap() {
        gtEnergyCap.invalidate();
        gtEnergyCap = LazyOptional.of(() -> euLogic);
    }

    @Override
    protected void clearDisplayedTransfer() {
        this.amperesMode = true;
        this.lastTick = -1;
        this.tickAmps = 0;
        this.tickVolt = 0;
        this.recentAmpTransfers.clear();
        super.clearDisplayedTransfer();
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction dir) {
        if (cap == GTCapability.CAPABILITY_ENERGY_CONTAINER && dir != null) {
            if (dir == getInputSide()) {
                return gtEnergyCap.cast();
            }
            return LazyOptional.empty();
        }
        return super.getCapability(cap, dir);
    }

    private final IEnergyContainer euLogic = new IEnergyContainer() {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long volt, long amp) {
            if (GTAmpereMeterBE.this.getLevel() == null) {
                return 0;
            }

            if (amp <= 0) {
                return 0;
            }

            GTAmpereMeterBE.this.markActive();

            Direction outputSide = GTAmpereMeterBE.this.getOutputSide();

            BlockEntity output = GTAmpereMeterBE.this.getLevel().getBlockEntity(
                    GTAmpereMeterBE.this.getBlockPos().relative(outputSide)
            );

            if (output == null) {
                return 0;
            }

            if (output instanceof AmpereMeterBE) {
                return 0;
            }

            AtomicLong forwardedRef = new AtomicLong(0);

            long ampsToForward = Math.max(0L, amp - 1L);

            output.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, outputSide.getOpposite()).ifPresent(out -> {
                forwardedRef.set(out.acceptEnergyFromNetwork(outputSide.getOpposite(), volt, ampsToForward));
            });

            long forwarded = Math.max(0L, Math.min(ampsToForward, forwardedRef.get()));

            long acceptedTotal = Math.min(amp, forwarded + 1L);

            long currentTick = GTAmpereMeterBE.this.getLevel().getGameTime();
            int displayedAmps = (int) Math.min(Integer.MAX_VALUE, forwarded);

            if (currentTick == GTAmpereMeterBE.this.lastTick) {
                GTAmpereMeterBE.this.tickAmps += displayedAmps;
                if (volt > GTAmpereMeterBE.this.tickVolt) {
                    GTAmpereMeterBE.this.tickVolt = volt;
                }
            } else {
                GTAmpereMeterBE.this.lastTick = currentTick;
                GTAmpereMeterBE.this.tickAmps = displayedAmps;
                GTAmpereMeterBE.this.tickVolt = volt;
            }

            Map.Entry<Long, String> voltageTier = Utils.voltagesMap.ceilingEntry(GTAmpereMeterBE.this.tickVolt);
            String tierName = voltageTier != null ? voltageTier.getValue() : "???";
            String unitLabel = "A (%s)".formatted(tierName);

            String currentUnit = GTAmpereMeterBE.this.getUnit();
            if (!Objects.equals(currentUnit, unitLabel)) {
                GTAmpereMeterBE.this.recentAmpTransfers.clear();
            }

            if (GTAmpereMeterBE.this.recentAmpTransfers.size() >= 5) {
                GTAmpereMeterBE.this.recentAmpTransfers.removeFirst();
            }
            GTAmpereMeterBE.this.recentAmpTransfers.addLast(GTAmpereMeterBE.this.tickAmps);

            int max = GTAmpereMeterBE.this.recentAmpTransfers.stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);

            GTAmpereMeterBE.this.setDisplayedTransfer(max, unitLabel, true);

            return acceptedTotal;
        }

        @Override
        public boolean inputsEnergy(Direction direction) {
            return direction == null || direction == GTAmpereMeterBE.this.getInputSide();
        }

        @Override
        public long changeEnergy(long energyToAdd) {
            return 0;
        }

        @Override
        public long getEnergyStored() {
            return 0;
        }

        @Override
        public long getEnergyCapacity() {
            return Integer.MAX_VALUE;
        }

        @Override
        public long getInputAmperage() {
            if (GTAmpereMeterBE.this.getLevel() == null) {
                return 0;
            }

            Direction outputSide = GTAmpereMeterBE.this.getOutputSide();
            BlockEntity output = GTAmpereMeterBE.this.getLevel().getBlockEntity(
                    GTAmpereMeterBE.this.getBlockPos().relative(outputSide)
            );

            if (output == null || output instanceof AmpereMeterBE) {
                return 0;
            }

            AtomicLong amperage = new AtomicLong();

            output.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, outputSide.getOpposite()).ifPresent(out -> {
                amperage.set(out.getInputAmperage());
            });

            long downstream = amperage.get();

            return downstream <= 0 ? 1 : downstream + 1;
        }

        @Override
        public long getInputVoltage() {
            if (GTAmpereMeterBE.this.getLevel() == null) {
                return 0;
            }

            Direction outputSide = GTAmpereMeterBE.this.getOutputSide();
            BlockEntity output = GTAmpereMeterBE.this.getLevel().getBlockEntity(
                    GTAmpereMeterBE.this.getBlockPos().relative(outputSide)
            );

            if (output == null || output instanceof AmpereMeterBE) {
                return 0;
            }

            AtomicLong voltage = new AtomicLong();

            output.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, outputSide.getOpposite()).ifPresent(out -> {
                voltage.set(out.getInputVoltage());
            });

            return voltage.get();
        }
    };
}