package net.oktawia.crazyae2addonslite.compat.GregTech;

import appeng.api.config.PowerUnits;
import appeng.api.parts.IPartItem;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.oktawia.crazyae2addonslite.CrazyConfig;
import net.oktawia.crazyae2addonslite.parts.WormholeP2PTunnelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class GTWormholeP2PTunnelPart extends WormholeP2PTunnelPart {
    public GTWormholeP2PTunnelPart(IPartItem<?> partItem) {
        super(partItem);
    }

    private boolean gtEuRecursive = false;

    private final IEnergyContainer gtEuBridge = new IEnergyContainer() {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long volt, long amp) {
            if (gtEuRecursive) return 0;
            if (!GTWormholeP2PTunnelPart.this.isActive()) return 0;

            ArrayList<WormholeP2PTunnelPart> tmpOutputs = new ArrayList<>();
            if (GTWormholeP2PTunnelPart.this.isOutput()) {
                tmpOutputs.add(GTWormholeP2PTunnelPart.this.getInput());
            } else {
                tmpOutputs.addAll(GTWormholeP2PTunnelPart.this.getOutputs());
            }

            if (tmpOutputs.isEmpty()) return 0;

            var outs = new ArrayList<WormholeP2PTunnelPart>();

            if (!CrazyConfig.COMMON.GregWormholeGoodEuP2P.get()) {
                var tmp = tmpOutputs.get(0);
                outs.add(tmp);
            } else {
                outs.addAll(tmpOutputs);
            }

            gtEuRecursive = true;
            try {
                long remaining = amp;
                long movedTotal = 0;

                for (var out : outs) {
                    if (remaining <= 0) break;
                    if (out == null || !out.isActive()) continue;
                    if (out.getHost() == null) continue;

                    var outHostBe = out.getHost().getBlockEntity();
                    if (outHostBe == null || outHostBe.getLevel() == null) continue;

                    var outLevel = outHostBe.getLevel();
                    var neighborPos = outHostBe.getBlockPos().relative(out.getSide());
                    var neighborSide = out.getSide().getOpposite();

                    IEnergyContainer target = getGtEnergyContainer(outLevel, neighborPos, neighborSide);
                    if (target == null) continue;
                    if (!target.inputsEnergy(neighborSide)) continue;

                    long moved = target.acceptEnergyFromNetwork(neighborSide, volt, remaining);
                    if (moved > 0) {
                        movedTotal += moved;
                        remaining -= moved;
                    }
                }

                deductEnergyCost(FeCompat.toFe(movedTotal, FeCompat.ratio(false)), PowerUnits.FE);
                return movedTotal;
            } finally {
                gtEuRecursive = false;
            }
        }

        @Override
        public boolean inputsEnergy(Direction direction) {
            return GTWormholeP2PTunnelPart.this.isActive();
        }

        @Override public long changeEnergy(long l) { return 0; }
        @Override public long getEnergyStored() { return 0; }
        @Override public long getEnergyCapacity() { return Long.MAX_VALUE; }

        @Override
        public long getInputAmperage() {
            if (!GTWormholeP2PTunnelPart.this.isActive()) return 0;

            var list = new ArrayList<WormholeP2PTunnelPart>();
            if (GTWormholeP2PTunnelPart.this.isOutput()) {
                list.add(GTWormholeP2PTunnelPart.this.getInput());
            } else {
                list.addAll(GTWormholeP2PTunnelPart.this.getOutputs());
            }
            long max = 0;
            for (var out : list) {
                if (out == null || out.getHost() == null || !out.isActive()) continue;
                var outHostBe = out.getHost().getBlockEntity();
                if (outHostBe == null || outHostBe.getLevel() == null) continue;

                var outLevel = outHostBe.getLevel();
                var neighborPos = outHostBe.getBlockPos().relative(out.getSide());
                var neighborSide = out.getSide().getOpposite();

                IEnergyContainer target = getGtEnergyContainer(outLevel, neighborPos, neighborSide);
                if (target != null) {
                    max = Math.max(max, target.getInputAmperage());
                }
            }
            return max;
        }

        @Override
        public long getInputVoltage() {
            if (!GTWormholeP2PTunnelPart.this.isActive()) return 0;

            var list = new ArrayList<WormholeP2PTunnelPart>();
            if (GTWormholeP2PTunnelPart.this.isOutput()) {
                list.add(GTWormholeP2PTunnelPart.this.getInput());
            } else {
                list.addAll(GTWormholeP2PTunnelPart.this.getOutputs());
            }
            long max = 0;
            for (var out : list) {
                if (out == null || out.getHost() == null || !out.isActive()) continue;
                var outHostBe = out.getHost().getBlockEntity();
                if (outHostBe == null || outHostBe.getLevel() == null) continue;

                var outLevel = outHostBe.getLevel();
                var neighborPos = outHostBe.getBlockPos().relative(out.getSide());
                var neighborSide = out.getSide().getOpposite();

                IEnergyContainer target = getGtEnergyContainer(outLevel, neighborPos, neighborSide);
                if (target != null) {
                    max = Math.max(max, target.getInputVoltage());
                }
            }
            return max;
        }
    };

    private final LazyOptional<IEnergyContainer> gtEuBridgeOpt = LazyOptional.of(() -> gtEuBridge);

    @Nullable
    private IEnergyContainer getGtEnergyContainer(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        return be.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, side).orElse(null);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!isActive()) return LazyOptional.empty();
        var world = getLevel();
        if (world == null) return LazyOptional.empty();

        if (cap == GTCapability.CAPABILITY_ENERGY_CONTAINER && CrazyConfig.COMMON.GregWormholeEUP2P.get()) {
            return gtEuBridgeOpt.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.gtEuBridgeOpt.invalidate();
    }
}
