package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.networking.EnergyCellBlockEntity;
import net.oktawia.crazyae2addons.blocks.EnergyStorage256m;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.events.GridPowerStorageStateChanged;
import appeng.api.networking.events.GridPowerStorageStateChanged.PowerEventType;
import appeng.api.networking.ticking.IGridTickable;
import appeng.me.energy.StoredEnergyAmount;

public class EnergyStorage256mBE extends EnergyCellBlockEntity {

    private final StoredEnergyAmount stored;

    public EnergyStorage256mBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.ENERGY_STORAGE_256M.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(0)
                .addService(IAEPowerStorage.class, this)
                .addService(IGridTickable.class, this);

        this.stored = new StoredEnergyAmount(0, EnergyStorage256m.getMaxEnergy(), this::emitPowerEvent);
    }

    private void emitPowerEvent(PowerEventType type) {
        getMainNode().ifPresent(
                grid -> grid.postEvent(new GridPowerStorageStateChanged(this, type)));
    }
}
