package net.oktawia.crazyae2addons.logic.wormhole;

import lombok.RequiredArgsConstructor;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.wrapper.CombinedInvWrapper;
import net.oktawia.crazyae2addons.parts.p2p.WormholeP2PTunnelPart;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class WormholeP2PCapabilityProxy {

    private final WormholeP2PTunnelPart part;

    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (!part.isActive()) {
            return LazyOptional.empty();
        }

        Level world = part.getLevel();
        if (world == null) {
            return LazyOptional.empty();
        }

        if (part.isOutput()) {
            RemoteTarget target = getInputTarget();
            if (target == null || target.blockEntity() == null) {
                return LazyOptional.empty();
            }
            return target.blockEntity().getCapability(cap, target.side());
        }

        var outputs = part.getOutputs();
        if (outputs.isEmpty()) {
            return LazyOptional.empty();
        }

        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            List<IItemHandlerModifiable> handlers = new ArrayList<>();
            for (var output : outputs) {
                RemoteTarget target = getOutputTarget(output);
                if (target == null || target.blockEntity() == null) {
                    continue;
                }

                var opt = target.blockEntity().getCapability(ForgeCapabilities.ITEM_HANDLER, target.side());
                opt.ifPresent(handler -> {
                    if (handler instanceof IItemHandlerModifiable modifiable) {
                        handlers.add(modifiable);
                    }
                });
            }

            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedInvWrapper(handlers.toArray(new IItemHandlerModifiable[0])));
            }
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            List<IFluidHandler> handlers = new ArrayList<>();
            for (var output : outputs) {
                RemoteTarget target = getOutputTarget(output);
                if (target == null || target.blockEntity() == null) {
                    continue;
                }

                var opt = target.blockEntity().getCapability(ForgeCapabilities.FLUID_HANDLER, target.side());
                opt.ifPresent(handlers::add);
            }

            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedHandlers.Fluid(handlers));
            }
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER_ITEM) {
            List<IFluidHandlerItem> handlers = new ArrayList<>();
            for (var output : outputs) {
                RemoteTarget target = getOutputTarget(output);
                if (target == null || target.blockEntity() == null) {
                    continue;
                }

                var opt = target.blockEntity().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM, target.side());
                opt.ifPresent(handlers::add);
            }

            if (!handlers.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedHandlers.FluidItem(handlers));
            }
        }

        if (cap == ForgeCapabilities.ENERGY) {
            List<IEnergyStorage> storages = new ArrayList<>();
            for (var output : outputs) {
                RemoteTarget target = getOutputTarget(output);
                if (target == null || target.blockEntity() == null) {
                    continue;
                }

                var opt = target.blockEntity().getCapability(ForgeCapabilities.ENERGY, target.side());
                opt.ifPresent(storages::add);
            }

            if (!storages.isEmpty()) {
                return LazyOptional.of(() -> (T) new CombinedHandlers.Energy(storages));
            }
        }

        for (var output : outputs) {
            RemoteTarget target = getOutputTarget(output);
            if (target == null || target.blockEntity() == null) {
                continue;
            }

            var result = target.blockEntity().getCapability(cap, target.side());
            if (result.isPresent()) {
                return result;
            }
        }

        return LazyOptional.empty();
    }

    private @Nullable RemoteTarget getInputTarget() {
        var input = part.getInput();
        if (input == null || input.getHost() == null) {
            return null;
        }

        var remoteHost = input.getHost().getBlockEntity();
        if (remoteHost == null || remoteHost.getLevel() == null) {
            return null;
        }

        var targetPos = remoteHost.getBlockPos().relative(input.getSide());
        var targetBE = remoteHost.getLevel().getBlockEntity(targetPos);
        return new RemoteTarget(targetBE, input.getSide().getOpposite());
    }

    private @Nullable RemoteTarget getOutputTarget(WormholeP2PTunnelPart output) {
        if (output.getHost() == null || output.getHost().getBlockEntity() == null || output.getLevel() == null) {
            return null;
        }

        var remoteHost = output.getHost().getBlockEntity();
        var targetPos = remoteHost.getBlockPos().relative(output.getSide());
        var targetBE = output.getLevel().getBlockEntity(targetPos);
        return new RemoteTarget(targetBE, output.getSide().getOpposite());
    }

    private record RemoteTarget(@Nullable BlockEntity blockEntity, Direction side) {
    }
}