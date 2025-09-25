package net.oktawia.crazyae2addons.blocks;

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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.compat.GregTech.GTAmpereMeterBE;
import net.oktawia.crazyae2addons.entities.AmpereMeterBE;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AmpereMeterBlock extends AEBaseEntityBlock<AmpereMeterBE> {

    public AmpereMeterBlock() {
        super(Properties.of()
                .strength(2f)
                .mapColor(MapColor.METAL)
                .sound(SoundType.METAL));
        registerDefaultState(stateDefinition.any()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (IsModLoaded.isGTCEuLoaded()) {
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
        return defaultBlockState()
                .setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                                                        BlockHitResult hit) {
        if (level.isClientSide() || player.isShiftKeyDown()) {
            return InteractionResult.SUCCESS;
        }

        var be = level.getBlockEntity(pos);

        if (be instanceof AmpereMeterBE meter) {
            if (!level.isClientSide()) {
                meter.openMenu(player, MenuLocators.forBlockEntity(be));
            }
            return InteractionResult.sidedSuccess(level.isClientSide());
        }

        return InteractionResult.PASS;
    }
}
