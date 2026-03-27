package net.oktawia.crazyae2addons.blocks;

import appeng.block.crafting.PatternProviderBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.SyncBlockClientPacket;
import org.jetbrains.annotations.Nullable;

public class CrazyPatternProviderBlock extends PatternProviderBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrazyPatternProviderBE(pos, state);
    }

    @Override
    public InteractionResult onActivated(Level level, BlockPos pos, Player player,
                                         InteractionHand hand, ItemStack heldItem, BlockHitResult hit) {

        if (level.getBlockEntity(pos) instanceof CrazyPatternProviderBE provider) {
            provider.syncAddedToClients();
        }

        if (!heldItem.isEmpty() && heldItem.getItem() == CrazyItemRegistrar.CRAZY_UPGRADE.get().asItem()) {
            if (level.isClientSide) {
                return InteractionResult.SUCCESS;
            }
            var be = level.getBlockEntity(pos);
            if (be instanceof CrazyPatternProviderBE provider) {
                int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
                int cur = provider.getAdded();

                if (maxAdd != -1 && cur >= maxAdd) {
                    player.displayClientMessage(Component.translatable("gui.crazyae2addons.provider_max"), true);
                    return InteractionResult.SUCCESS;
                }
                provider.setAdded(cur + 1);
                heldItem.shrink(1);
                return InteractionResult.SUCCESS;
            }

            return InteractionResult.PASS;
        }
        return super.onActivated(level, pos, player, hand, heldItem, hit);
    }
}
