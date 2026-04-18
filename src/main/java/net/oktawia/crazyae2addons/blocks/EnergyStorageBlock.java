package net.oktawia.crazyae2addons.blocks;

import appeng.block.networking.EnergyCellBlock;
import lombok.Getter;

public class EnergyStorageBlock extends EnergyCellBlock {
    @Getter
    private final long maxEnergy;

    public EnergyStorageBlock(long maxEnergy) {
        super((double) maxEnergy, (double) maxEnergy, (int) (maxEnergy / 100));
        this.maxEnergy = maxEnergy;
    }
}
