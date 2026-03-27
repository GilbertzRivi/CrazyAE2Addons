package net.oktawia.crazyae2addons.compat.GregTech;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.config.PowerUnits;
import appeng.api.parts.IPartItem;
import appeng.server.testplots.P2PPlotHelper;
import appeng.util.helpers.P2PHelper;
import com.gregtechceu.gtceu.api.capability.GTCapabilityHelper;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.EUToFEProvider;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import com.gregtechceu.gtceu.common.data.GTConfiguredFeatures;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.parts.WormholeP2PTunnelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import stone.mae2.parts.p2p.EUP2PTunnelPart;

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

            if (GTWormholeP2PTunnelPart.this.isOutput()) return 0;

            var tmpouts = GTWormholeP2PTunnelPart.this.getOutputs();
            if (tmpouts.isEmpty()) return 0;

            var outs = new ArrayList<WormholeP2PTunnelPart>();

            if (!CrazyConfig.COMMON.GregWormholeGoodEuP2P.get()) {
                var tmp = tmpouts.get(0);
                outs.add(tmp);
            } else {
                outs.addAll(tmpouts);
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
            return GTWormholeP2PTunnelPart.this.isActive() && !GTWormholeP2PTunnelPart.this.isOutput();
        }

        @Override public long changeEnergy(long l) { return 0; }
        @Override public long getEnergyStored() { return 0; }
        @Override public long getEnergyCapacity() { return Long.MAX_VALUE; }

        @Override
        public long getInputAmperage() {
            if (!GTWormholeP2PTunnelPart.this.isActive() || GTWormholeP2PTunnelPart.this.isOutput()) return 0;

            long max = 0;
            for (var out : GTWormholeP2PTunnelPart.this.getOutputs()) {
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
            if (!GTWormholeP2PTunnelPart.this.isActive() || GTWormholeP2PTunnelPart.this.isOutput()) return 0;

            long max = 0;
            for (var out : GTWormholeP2PTunnelPart.this.getOutputs()) {
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
            return isOutput() ? LazyOptional.empty() : gtEuBridgeOpt.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void removeFromWorld() {
        super.removeFromWorld();
        this.gtEuBridgeOpt.invalidate();
    }
}
