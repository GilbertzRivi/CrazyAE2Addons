package net.oktawia.crazyae2addons.entities;

import appeng.api.networking.GridHelper;
import appeng.api.networking.IGridConnection;
import appeng.blockentity.grid.AENetworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import org.jetbrains.annotations.NotNull;


public class PenrosePortBE extends AENetworkBlockEntity {

    private PenroseControllerBE controller;

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new IEnergyStorage() {
        @Override
        public int getEnergyStored() {
            if (controller == null) return 0;
            var srcOpt = controller.getCapability(ForgeCapabilities.ENERGY, null);
            IEnergyStorage src = srcOpt.orElse(null);
            return src != null ? src.getEnergyStored() : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            if (controller == null) return 0;
            var srcOpt = controller.getCapability(ForgeCapabilities.ENERGY, null);
            IEnergyStorage src = srcOpt.orElse(null);
            return src != null ? src.getMaxEnergyStored() : 0;
        }

        @Override
        public boolean canExtract() {
            return controller != null;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (controller == null || maxExtract <= 0) return 0;
            var srcOpt = controller.getCapability(ForgeCapabilities.ENERGY, null);
            IEnergyStorage src = srcOpt.orElse(null);
            if (src == null) return 0;
            return src.extractEnergy(maxExtract, simulate);
        }

        @Override
        public boolean canReceive() {
            return false;
        }

        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }
    });

    public PenrosePortBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.PENROSE_PORT_BE.get(), pos, state);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PENROSE_PORT.get().asItem())
                );
    }

    public void setController(PenroseControllerBE controller) {
        if (this.controller == controller) return;

        if (this.controller != null) {
            this.controller.unregisterPort(this);
        }

        this.controller = controller;

        if (this.controller != null) {
            this.controller.registerPort(this);
            if (getMainNode().getNode() != null
                    && this.controller.getMainNode().getNode() != null
                    && getMainNode().getNode().getConnections().stream()
                    .noneMatch(x -> (x.a() == this.controller.getMainNode().getNode()
                            || x.b() == this.controller.getMainNode().getNode()))) {
                GridHelper.createConnection(getMainNode().getNode(), this.controller.getMainNode().getNode());
            }
        } else if (getMainNode().getNode() != null) {
            getMainNode().getNode().getConnections().stream()
                    .filter(x -> (!x.isInWorld()))
                    .forEach(IGridConnection::destroy);
        }
    }

    public void tickPort() {
        if (controller == null || level == null) {
            return;
        }

        IEnergyStorage source = controller.getCapability(ForgeCapabilities.ENERGY, null)
                .orElse(null);
        if (source == null) {
            return;
        }

        for (Direction dir : Direction.values()) {
            BlockEntity neighbor = level.getBlockEntity(worldPosition.relative(dir));
            if (neighbor == null
                    || neighbor instanceof PenroseFrameBE
                    || neighbor instanceof PenroseCoilBE
                    || neighbor instanceof PenroseControllerBE
                    || neighbor instanceof PenrosePortBE) {
                continue;
            }

            LazyOptional<IEnergyStorage> cap =
                    neighbor.getCapability(ForgeCapabilities.ENERGY, dir.getOpposite());

            cap.ifPresent(target -> {
                int available = source.getEnergyStored();
                if (available <= 0) return;

                int maxAccept = target.receiveEnergy(available, true);
                if (maxAccept <= 0) return;

                int toSend = Math.min(available, maxAccept);

                int extracted = source.extractEnergy(toSend, false);
                if (extracted > 0) {
                    target.receiveEnergy(extracted, false);
                }
            });
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (controller != null) {
            controller.unregisterPort(this);
            controller = null;
        }
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        }
        return super.getCapability(cap);
    }
}
