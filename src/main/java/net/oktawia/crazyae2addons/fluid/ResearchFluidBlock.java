package net.oktawia.crazyae2addons.fluid;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ResearchFluidBlock extends LiquidBlock {
    public ResearchFluidBlock(FlowingFluid fluid, BlockBehaviour.Properties props) {
        super(fluid, props);
    }
}
