package net.oktawia.crazyae2addonslite.misc;

import net.minecraftforge.energy.IEnergyStorage;

import java.util.List;

public class CombinedEnergyStorage implements IEnergyStorage {

    private final List<IEnergyStorage> storages;

    public CombinedEnergyStorage(List<IEnergyStorage> storages) {
        this.storages = storages;
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {
        int received = 0;
        for (IEnergyStorage storage : storages) {
            received += storage.receiveEnergy(maxReceive - received, simulate);
            if (received >= maxReceive) break;
        }
        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {
        int extracted = 0;
        for (IEnergyStorage storage : storages) {
            extracted += storage.extractEnergy(maxExtract - extracted, simulate);
            if (extracted >= maxExtract) break;
        }
        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return storages.stream().mapToInt(IEnergyStorage::getEnergyStored).sum();
    }

    @Override
    public int getMaxEnergyStored() {
        return storages.stream().mapToInt(IEnergyStorage::getMaxEnergyStored).sum();
    }

    @Override
    public boolean canExtract() {
        return storages.stream().anyMatch(IEnergyStorage::canExtract);
    }

    @Override
    public boolean canReceive() {
        return storages.stream().anyMatch(IEnergyStorage::canReceive);
    }
}
