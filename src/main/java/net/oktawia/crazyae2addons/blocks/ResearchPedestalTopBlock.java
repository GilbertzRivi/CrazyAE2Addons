package net.oktawia.crazyae2addons.blocks;

import appeng.block.AEBaseEntityBlock;
import com.mojang.logging.LogUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.oktawia.crazyae2addons.entities.ResearchPedestalTopBE;
import org.jetbrains.annotations.Nullable;

public class ResearchPedestalTopBlock extends AEBaseEntityBlock<ResearchPedestalTopBE> {

    public ResearchPedestalTopBlock() {
        super(BlockBehaviour.Properties
                .of()
                .strength(0.3F)
                .noOcclusion()
                .isRedstoneConductor((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .isViewBlocking((state, level, pos) -> false));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ResearchPedestalTopBE(pos, state);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos,
                                 Player player, InteractionHand hand, BlockHitResult hit) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ResearchPedestalTopBE pedestal)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);

        if (!level.isClientSide) {

            if (!held.isEmpty()) {
                if (pedestal.isEmpty()) {
                    pedestal.setStoredStack(held.copy());
                    player.setItemInHand(hand, ItemStack.EMPTY);
                }
            } else {
                if (!pedestal.isEmpty()) {
                    ItemStack extracted = pedestal.takeStoredStack();
                    if (!extracted.isEmpty()) {
                        if (!player.addItem(extracted)) {
                            player.drop(extracted, false);
                        }
                    }
                }
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
