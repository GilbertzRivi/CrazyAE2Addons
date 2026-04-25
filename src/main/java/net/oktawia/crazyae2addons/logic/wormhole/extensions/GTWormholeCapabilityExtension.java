package net.oktawia.crazyae2addons.logic.wormhole.extensions;

import appeng.api.config.PowerUnits;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.api.capability.compat.FeCompat;
import com.gregtechceu.gtceu.api.capability.forge.GTCapability;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.parts.p2p.WormholeP2PTunnelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GTWormholeCapabilityExtension implements WormholeP2PTunnelPart.WormholeCapabilityExtension {

    private final WormholeP2PTunnelPart part;
    private boolean gtEuRecursive = false;

    public GTWormholeCapabilityExtension(WormholeP2PTunnelPart part) {
        this.part = part;
    }

    private final IEnergyContainer gtEuBridge = new IEnergyContainer() {
        @Override
        public long acceptEnergyFromNetwork(Direction side, long volt, long amp) {
            if (gtEuRecursive) return 0;
            if (!part.isActive()) return 0;
            if (part.isOutput()) return 0;

            var outs = part.getOutputs();
            if (outs.isEmpty()) return 0;

            var out = outs.get(0);
            if (out == null || !out.isActive()) return 0;
            if (out.getHost() == null) return 0;

            gtEuRecursive = true;
            try {
                var outHostBe = out.getHost().getBlockEntity();
                if (outHostBe == null || outHostBe.getLevel() == null) return 0;

                var outLevel = outHostBe.getLevel();
                var neighborPos = outHostBe.getBlockPos().relative(out.getSide());
                var neighborSide = out.getSide().getOpposite();

                IEnergyContainer target = getGtEnergyContainer(outLevel, neighborPos, neighborSide);
                if (target == null) return 0;
                if (!target.inputsEnergy(neighborSide)) return 0;

                long moved = target.acceptEnergyFromNetwork(neighborSide, volt, amp);

                if (moved > 0) {
                    part.drainPower(FeCompat.toFe(moved, FeCompat.ratio(false)), PowerUnits.FE);
                }

                return moved;
            } finally {
                gtEuRecursive = false;
            }
        }

        @Override
        public boolean inputsEnergy(Direction direction) {
            return part.isActive() && !part.isOutput();
        }

        @Override public long changeEnergy(long l) { return 0; }
        @Override public long getEnergyStored() { return 0; }
        @Override public long getEnergyCapacity() { return Long.MAX_VALUE; }

        @Override
        public long getInputAmperage() {
            if (!part.isActive() || part.isOutput()) return 0;

            var outs = part.getOutputs();
            if (outs.isEmpty()) return 0;

            var out = outs.get(0);
            if (out == null || out.getHost() == null || !out.isActive()) return 0;

            var outHostBe = out.getHost().getBlockEntity();
            if (outHostBe == null || outHostBe.getLevel() == null) return 0;

            IEnergyContainer target = getGtEnergyContainer(
                    outHostBe.getLevel(),
                    outHostBe.getBlockPos().relative(out.getSide()),
                    out.getSide().getOpposite()
            );
            return target != null ? target.getInputAmperage() : 0;
        }

        @Override
        public long getInputVoltage() {
            if (!part.isActive() || part.isOutput()) return 0;

            var outs = part.getOutputs();
            if (outs.isEmpty()) return 0;

            var out = outs.get(0);
            if (out == null || out.getHost() == null || !out.isActive()) return 0;

            var outHostBe = out.getHost().getBlockEntity();
            if (outHostBe == null || outHostBe.getLevel() == null) return 0;

            IEnergyContainer target = getGtEnergyContainer(
                    outHostBe.getLevel(),
                    outHostBe.getBlockPos().relative(out.getSide()),
                    out.getSide().getOpposite()
            );
            return target != null ? target.getInputVoltage() : 0;
        }
    };

    private final LazyOptional<IEnergyContainer> gtEuBridgeOpt = LazyOptional.of(() -> gtEuBridge);

    @Override
    public boolean handles(Capability<?> cap) {
        return cap == GTCapability.CAPABILITY_ENERGY_CONTAINER;
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!CrazyConfig.COMMON.WORMHOLE_ENABLED.get() || !CrazyConfig.COMMON.WORMHOLE_EU_PROXY_ENABLED.get()) {
            return LazyOptional.empty();
        }
        if (!part.isActive() || part.getLevel() == null) {
            return LazyOptional.empty();
        }
        if (part.isOutput()) {
            return getCapabilityFromInputTarget(cap);
        }
        return gtEuBridgeOpt.cast();
    }

    @Override
    public void onRemoveFromWorld() {
        gtEuBridgeOpt.invalidate();
    }

    private <T> @NotNull LazyOptional<T> getCapabilityFromInputTarget(@NotNull Capability<T> cap) {
        var input = part.getInput();
        if (input == null || input.getHost() == null) return LazyOptional.empty();
        var remoteHost = input.getHost().getBlockEntity();
        if (remoteHost == null || remoteHost.getLevel() == null) return LazyOptional.empty();
        var targetPos = remoteHost.getBlockPos().relative(input.getSide());
        var targetBE = remoteHost.getLevel().getBlockEntity(targetPos);
        if (targetBE == null) return LazyOptional.empty();
        return targetBE.getCapability(cap, input.getSide().getOpposite());
    }

    @Nullable
    private IEnergyContainer getGtEnergyContainer(Level level, BlockPos pos, Direction side) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be == null) return null;
        return be.getCapability(GTCapability.CAPABILITY_ENERGY_CONTAINER, side).orElse(null);
    }
}
