package net.oktawia.crazyae2addons.fluid;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.oktawia.crazyae2addons.defs.regs.CrazyFluidRegistrar;

import java.util.function.Supplier;

public abstract class ResearchFluid extends ForgeFlowingFluid {
    public static final ForgeFlowingFluid.Properties PROPERTIES;
    static {
        Supplier<FluidType> type = CrazyFluidRegistrar.RESEARCH_FLUID_TYPE::get;
        Supplier<FlowingFluid> flowing = CrazyFluidRegistrar.RESEARCH_FLUID_FLOWING::get;
        Supplier<FlowingFluid> source  = CrazyFluidRegistrar.RESEARCH_FLUID_SOURCE::get;

        ForgeFlowingFluid.Properties props = new ForgeFlowingFluid.Properties(type, source, flowing)
                .bucket(CrazyFluidRegistrar.RESEARCH_FLUID_BUCKET::get)
                .block(CrazyFluidRegistrar.RESEARCH_FLUID_BLOCK::get);
        PROPERTIES = props;
    }

    public ResearchFluid(Properties properties) { super(properties); }

    @Override public Fluid getFlowing() { return CrazyFluidRegistrar.RESEARCH_FLUID_FLOWING.get(); }
    @Override public Fluid getSource()   { return CrazyFluidRegistrar.RESEARCH_FLUID_SOURCE.get(); }
    @Override protected boolean canConvertToSource(Level lvl) { return false; }

    public static class Flowing extends ResearchFluid {
        public Flowing(Properties props) { super(props); }
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> b) {
            super.createFluidStateDefinition(b); b.add(LEVEL);
        }
        @Override public int getAmount(FluidState s) { return s.getValue(LEVEL); }
        @Override public boolean isSource(FluidState s) { return false; }
    }
    public static class Source extends ResearchFluid {
        public Source(Properties props) { super(props); }
        @Override public int getAmount(FluidState s) { return 8; }
        @Override public boolean isSource(FluidState s) { return true; }
    }
}