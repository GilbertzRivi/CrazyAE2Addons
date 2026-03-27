package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.grid.AENetworkBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

public class EntropyCradleCapacitorBE extends AENetworkBlockEntity {

    private EntropyCradleControllerBE controller;

    public EntropyCradleCapacitorBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.ENTROPY_CRADLE_CAPACITOR_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.ENTROPY_CRADLE_CAPACITOR.get().asItem())
                );
    }

    public void notifyRedstoneChanged() {
        if (level == null) return;
        level.updateNeighbourForOutputSignal(worldPosition, getBlockState().getBlock());
    }

    public void setController(EntropyCradleControllerBE controller) {
        this.controller = controller;
        if (controller == null){
            notifyRedstoneChanged();
        }
    }

    public int getStoredEnergy() {
        if (controller == null) {
            return 0;
        }
        return controller.storedEnergy.getEnergyStored();
    }
}
