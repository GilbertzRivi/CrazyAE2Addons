package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseEntityBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.MapColor;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.entities.EntropyCradleCapacitorBE;
import org.jetbrains.annotations.Nullable;

public class EntropyCradleCapacitor extends AEBaseEntityBlock<EntropyCradleCapacitorBE> {

    public static final BooleanProperty FORMED = BooleanProperty.create("formed");
    public static final BooleanProperty POWER = BooleanProperty.create("power");

    public EntropyCradleCapacitor() {
        super(Properties.of().strength(2f).mapColor(MapColor.METAL).sound(SoundType.METAL));
        this.registerDefaultState(this.defaultBlockState().setValue(FORMED, false));
        this.registerDefaultState(this.defaultBlockState().setValue(POWER, false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EntropyCradleCapacitorBE(pos, state);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(FORMED);
        builder.add(POWER);
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof EntropyCradleCapacitorBE cradle && cradle.getStoredEnergy() >= CrazyConfig.COMMON.CradleCapacity.get()) {
            return 15;
        }
        return 0;
    }
}
