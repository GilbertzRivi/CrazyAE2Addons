package net.oktawia.crazyae2addons.blocks.penrose;

import appeng.block.AEBaseBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.oktawia.crazyae2addons.entities.penrose.PenroseControllerBE;
import net.oktawia.crazyae2addons.logic.AbstractMenuOpeningBlock;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PenroseControllerBlock extends AbstractMenuOpeningBlock<PenroseControllerBE> {

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty FORMED = BooleanProperty.create("formed");

    public PenroseControllerBlock() {
        super(AEBaseBlock.metalProps());
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, net.minecraft.core.Direction.NORTH)
                .setValue(FORMED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, FORMED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(FORMED, false);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PenroseControllerBE(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);

        if (level.isClientSide) {
            return;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof PenroseControllerBE ctrl)) {
            return;
        }

        CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            ctrl.loadManagedPersistentData(level.registryAccess(), customData.copyTag());
        }

        if (placer instanceof ServerPlayer sp) {
            ctrl.setPlacer(sp);
        }
    }

    @Override
    public @NotNull List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof PenroseControllerBE ctrl) {
            ItemStack stack = new ItemStack(this);
            CompoundTag tag = new CompoundTag();
            if (ctrl.getLevel() != null) {
                ctrl.saveManagedPersistentData(ctrl.getLevel().registryAccess(), tag, true);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                return List.of(stack);
            }
        }
        return super.getDrops(state, builder);
    }

    @Override
    public @NotNull ItemStack getCloneItemStack(
            @NotNull BlockState state, @NotNull HitResult target,
            @NotNull LevelReader level, @NotNull BlockPos pos, @NotNull Player player
    ) {
        if (level instanceof Level realLevel) {
            BlockEntity be = realLevel.getBlockEntity(pos);
            if (be instanceof PenroseControllerBE ctrl) {
                ItemStack stack = new ItemStack(this);
                CompoundTag tag = new CompoundTag();
                ctrl.saveManagedPersistentData(realLevel.registryAccess(), tag, true);
                stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
                return stack;
            }
        }

        return super.getCloneItemStack(state, target, level, pos, player);
    }
}