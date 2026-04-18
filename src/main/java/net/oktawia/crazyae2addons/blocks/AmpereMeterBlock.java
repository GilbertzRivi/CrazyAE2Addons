package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;
import net.oktawia.crazyae2addons.util.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.Nullable;

public class AmpereMeterBlock extends AbstractMenuOpeningBlock<AmpereMeterBE> {

    public AmpereMeterBlock() {
        super(AEBaseBlock.metalProps());
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        var be = level.getBlockEntity(pos);
        if (be instanceof AmpereMeterBE amp) {
            return amp.getComparatorSignal();
        }
        return 0;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide()) {
            return null;
        }
        if (type != CrazyBlockEntityRegistrar.AMPERE_METER_BE.get()) {
            return null;
        }

        return (lvl, pos, st, be) -> {
            if (be instanceof AmpereMeterBE amp) {
                AmpereMeterBE.serverTick(lvl, pos, st, amp);
            }
        };
    }
}