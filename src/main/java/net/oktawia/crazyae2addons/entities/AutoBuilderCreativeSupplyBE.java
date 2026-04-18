package net.oktawia.crazyae2addons.entities;

import appeng.blockentity.grid.AENetworkedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;

public class AutoBuilderCreativeSupplyBE extends AENetworkedBlockEntity {

    public AutoBuilderCreativeSupplyBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.AUTO_BUILDER_CREATIVE_SUPPLY_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(0)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.AUTO_BUILDER_CREATIVE_SUPPLY_BLOCK.get().asItem())
                );
    }
}
