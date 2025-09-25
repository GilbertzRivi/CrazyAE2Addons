package net.oktawia.crazyae2addons.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.BlockGetter;
import net.neoforged.neoforge.fluids.FluidType;
import net.oktawia.crazyae2addons.defs.regs.CrazyFluidRegistrar;

import java.util.function.Supplier;

public abstract class ResearchFluid extends FlowingFluid {
    private final Supplier<FluidType> type;
    private final Supplier<? extends FlowingFluid> flowing;
    private final Supplier<? extends FlowingFluid> source;
    private final Supplier<? extends LiquidBlock> block;
    private final Supplier<? extends net.minecraft.world.item.Item> bucket;

    protected ResearchFluid(Supplier<FluidType> type,
                            Supplier<? extends FlowingFluid> flowing,
                            Supplier<? extends FlowingFluid> source,
                            Supplier<? extends LiquidBlock> block,
                            Supplier<? extends net.minecraft.world.item.Item> bucket) {
        this.type = type;
        this.flowing = flowing;
        this.source  = source;
        this.block   = block;
        this.bucket  = bucket;
    }

    @Override public Fluid getFlowing() { return flowing.get(); }
    @Override public Fluid getSource()  { return source.get(); }
    @Override public net.minecraft.world.item.Item getBucket() { return bucket.get(); }
    @Override public FluidType getFluidType() { return type.get(); }

    @Override
    protected boolean canConvertToSource(Level level) { return false; }

    protected LiquidBlock getBlock() { return block.get(); }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return getBlock().defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override protected int getSlopeFindDistance(LevelReader level) { return 4; }
    @Override protected int getDropOff(LevelReader level) { return 1; }
    @Override public int getTickDelay(LevelReader level) { return 5; }
    @Override protected float getExplosionResistance() { return 100.0F; }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter world, BlockPos pos,
                                        Fluid other, Direction dir) {
        return false;
    }


    public static class Flowing extends ResearchFluid {
        public Flowing() {
            super(CrazyFluidRegistrar.RESEARCH_FLUID_TYPE::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_FLOWING::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_SOURCE::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_BLOCK::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_BUCKET::get);
        }
        @Override
        protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> b) {
            super.createFluidStateDefinition(b);
            b.add(LEVEL);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            if (level instanceof Level lvl) {
                var be = state.hasBlockEntity() ? lvl.getBlockEntity(pos) : null;
                Block.dropResources(state, lvl, pos, be);
            }
        }

        @Override public int getAmount(FluidState s) { return s.getValue(LEVEL); }
        @Override public boolean isSource(FluidState s) { return false; }
    }

    public static class Source extends ResearchFluid {
        public Source() {
            super(CrazyFluidRegistrar.RESEARCH_FLUID_TYPE::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_FLOWING::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_SOURCE::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_BLOCK::get,
                    CrazyFluidRegistrar.RESEARCH_FLUID_BUCKET::get);
        }

        @Override
        protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
            if (level instanceof Level lvl) {
                var be = state.hasBlockEntity() ? lvl.getBlockEntity(pos) : null;
                Block.dropResources(state, lvl, pos, be);
            }
        }

        @Override public int getAmount(FluidState s) { return 8; }
        @Override public boolean isSource(FluidState s) { return true; }
    }
}
