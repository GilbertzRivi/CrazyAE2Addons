package net.oktawia.crazyae2addons.compat.GregTech;

import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.TickRateModulation;
import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.capability.IEnergyContainer;
import com.gregtechceu.gtceu.common.machine.multiblock.part.EnergyHatchPartMachine;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.entities.PenroseControllerBE;

import java.util.ArrayList;

public class GTPenroseControllerBE extends PenroseControllerBE {

    public GTPenroseControllerBE(BlockPos pos, BlockState blockState) {
        super(pos, blockState);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        TickRateModulation result = super.tickingRequest(node, ticksSinceLastCall);

        if (!CrazyConfig.COMMON.PenroseEUOutputEnabled.get()) {
            return result;
        }

        if (!formed || level == null || level.isClientSide) {
            return result;
        }

        if (storedEnergy <= 0L || gregHatches.isEmpty()) {
            return result;
        }

        int t = Math.max(1, ticksSinceLastCall);

        var snapshot = new ArrayList<>(gregHatches);
        snapshot.removeIf(be -> be == null || be.isRemoved() || be.getLevel() != this.level);
        gregHatches.retainAll(snapshot);

        for (int i = 0; i < t; i++) {
            if (storedEnergy <= 0L) break;

            for (BlockEntity be : snapshot) {
                if (storedEnergy <= 0L) break;
                if (be == null || be.isRemoved()) continue;

                EnergyHatchPartMachine outputHatch = null;

                if (be instanceof MetaMachineBlockEntity machineBe) {
                    var machine = machineBe.getMetaMachine();
                    if (machine instanceof EnergyHatchPartMachine machineContainer) {
                        outputHatch = machineContainer;
                    }
                }

                if (outputHatch == null) continue;

                long canInsert = Math.max(0L, outputHatch.energyContainer.getEnergyCanBeInserted());
                if (canInsert <= 0L) continue;

                long toSend = Math.min(storedEnergy, canInsert);
                long accepted = outputHatch.energyContainer.changeEnergy(toSend);

                if (accepted > 0L) {
                    takeStoredEnergy(accepted);
                }
            }
        }

        recomputeUiEnergy();
        setChanged();
        return result;
    }
}
