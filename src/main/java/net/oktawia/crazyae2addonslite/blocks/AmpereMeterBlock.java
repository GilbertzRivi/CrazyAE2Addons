package net.oktawia.crazyae2addonslite.blocks;

import appeng.block.AEBaseEntityBlock;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addonslite.IsModLoaded;
import net.oktawia.crazyae2addonslite.compat.GregTech.GTAmpereMeterBE;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addonslite.entities.AmpereMeterBE;
import org.jetbrains.annotations.Nullable;

public class AmpereMeterBlock extends AEBaseEntityBlock<AmpereMeterBE> {

    public AmpereMeterBlock() {
        super(Properties.of().strength(2f).mapColor(MapColor.METAL).sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (IsModLoaded.isGTCEuLoaded()){
            return new GTAmpereMeterBE(pos, state);
        } else {
            return new AmpereMeterBE(pos, state);
        }
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public InteractionResult use(
            BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit
    ) {
        if (level.isClientSide() || player.isShiftKeyDown()) return InteractionResult.SUCCESS;

        var be = level.getBlockEntity(pos);

        if (be instanceof AmpereMeterBE amp) {
            if (!level.isClientSide()) {
                amp.openMenu(player, MenuLocators.forBlockEntity(be));
            }

            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
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
        if (level.isClientSide()) return null;
        if (type != CrazyBlockEntityRegistrar.AMPERE_METER_BE.get()) return null;

        return (lvl, pos, st, be) -> {
            if (be instanceof AmpereMeterBE amp) {
                AmpereMeterBE.serverTick(lvl, pos, st, amp);
            }
        };
    }

}
