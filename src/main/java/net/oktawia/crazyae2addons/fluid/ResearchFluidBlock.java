package net.oktawia.crazyae2addons.fluid;

import java.util.function.Supplier;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ResearchFluidBlock extends LiquidBlock {
    public ResearchFluidBlock(Supplier<? extends FlowingFluid> fluid, BlockBehaviour.Properties props) {
        super(fluid, props);
    }
}
