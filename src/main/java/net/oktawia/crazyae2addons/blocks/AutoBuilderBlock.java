package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseBlock;
import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.CodeEditor;
import com.lowdragmc.lowdraglib2.gui.ui.elements.codeeditor.language.Languages;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addons.entities.AutoBuilderBE;
import net.oktawia.crazyae2addons.logic.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.Nullable;

public class AutoBuilderBlock extends AbstractMenuOpeningBlock<AutoBuilderBE> {

    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public AutoBuilderBlock() {
        super(AEBaseBlock.metalProps().isRedstoneConductor((state, level, pos) -> false));
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(POWERED, false)
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(POWERED);
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite())
                .setValue(POWERED, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AutoBuilderBE(pos, state);
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
            Block block, BlockPos fromPos, boolean isMoving) {
        if (level.isClientSide) return;

        BlockEntity be = level.getBlockEntity(pos);

        if (be instanceof AutoBuilderBE myBE && myBE.isPulsing()) return;

        boolean wasPowered = state.getValue(POWERED);
        boolean isPoweredNow = level.hasNeighborSignal(pos);

        if (!wasPowered && isPoweredNow) {
            if (be instanceof AutoBuilderBE myBE) {
                myBE.onRedstoneActivate();
            }
            level.setBlock(pos, state.setValue(POWERED, true), 3);
        } else if (wasPowered && !isPoweredNow) {
            level.setBlock(pos, state.setValue(POWERED, false), 3);
        }
    }

    @Override
    public boolean canConnectRedstone(BlockState state, BlockGetter level, BlockPos pos, @Nullable Direction side) {
        return true;
    }

    @Override
    public boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    public int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction dir) {
        if (level.getBlockEntity(pos) instanceof AutoBuilderBE be && be.getRedstonePulseTicks() > 0) {
            return 15;
        }
        return 0;
    }
}
