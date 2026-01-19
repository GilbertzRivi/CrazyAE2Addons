package net.oktawia.crazyae2addonslite.blocks;

import appeng.block.crafting.PatternProviderBlock;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.crazyae2addonslite.CrazyConfig;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addonslite.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addonslite.network.packets.SyncBlockClientPacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrazyPatternProviderBlock extends PatternProviderBlock {

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrazyPatternProviderBE(pos, state);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack heldItem, BlockState state, Level level, BlockPos pos,
                                              Player player, InteractionHand hand, BlockHitResult hit) {
        if (InteractionUtil.canWrenchRotate(heldItem)) {
            this.setSide(level, pos, hit.getDirection());
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }

        if (!heldItem.isEmpty() && heldItem.getItem() == CrazyItemRegistrar.CRAZY_UPGRADE.get()) {
            if (level.isClientSide()) {
                return ItemInteractionResult.SUCCESS;
            }

            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrazyPatternProviderBE crazyProvider) {
                int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
                int cur = crazyProvider.getAdded();
                if (cur >= maxAdd && maxAdd != -1) {
                    player.displayClientMessage(
                            Component.translatable("gui.crazyae2addons.provider_max"),
                            true
                    );
                    return ItemInteractionResult.sidedSuccess(false);
                }
                int next = cur + 1;
                crazyProvider.setAdded(next);
                int added = crazyProvider.getAdded();
                CrazyPatternProviderBE newBe = crazyProvider.refreshLogic(added);
                newBe.setAdded(added);
                level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);
                ItemStack inHand = player.getItemInHand(hand);
                inHand.shrink(1);
                player.setItemInHand(hand, inHand.isEmpty() ? ItemStack.EMPTY : inHand);
                PacketDistributor.sendToPlayersTrackingChunk(
                        (ServerLevel) level,
                        new ChunkPos(pos),
                        new SyncBlockClientPacket(pos, added)
                );
                return ItemInteractionResult.sidedSuccess(false);
            }
            return ItemInteractionResult.sidedSuccess(false);
        }
        return super.useItemOn(heldItem, state, level, pos, player, hand, hit);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        PatternProviderBlockEntity be = this.getBlockEntity(level, pos);
        if (be == null) {
            return InteractionResult.PASS;
        }

        if (!level.isClientSide()) {
            if (be instanceof CrazyPatternProviderBE crazyBe) {
                PacketDistributor.sendToPlayersTrackingChunk(
                        (ServerLevel) level,
                        new ChunkPos(pos),
                        new SyncBlockClientPacket(pos, crazyBe.getAdded())
                );
            }
            be.openMenu(player, MenuLocators.forBlockEntity(be));
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public @NotNull List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof CrazyPatternProviderBE myBe) {
            ItemStack stack = new ItemStack(this);
            CompoundTag tag = new CompoundTag();
            tag.putInt("added", myBe.getAdded());
            myBe.getLogic().writeToNBT(tag, be.getLevel().registryAccess());
            appeng.util.inv.AppEngInternalInventory inv =
                    (appeng.util.inv.AppEngInternalInventory) myBe.getLogic().getPatternInv();
            inv.writeToNBT(tag, "dainv", be.getLevel().registryAccess());
            myBe.getUpgrades().writeToNBT(tag, "upgrades", be.getLevel().registryAccess());
            stack.set(CrazyDataComponents.CRAZY_PROVIDER_DATA.get(), CustomData.of(tag));
            myBe.getLogic().getPatternInv().clear();
            return List.of(stack);
        }
        return super.getDrops(state, builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        if (level.isClientSide()) return;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CrazyPatternProviderBE myBe)) return;

        CustomData data = stack.get(CrazyDataComponents.CRAZY_PROVIDER_DATA.get());
        if (data == null || data.isEmpty()) return;

        CompoundTag tag = data.copyTag();
        if (!tag.contains("added")) return;

        myBe.loadTag(tag, level.registryAccess());
        PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) level,
                new ChunkPos(pos),
                new SyncBlockClientPacket(pos, tag.getInt("added"))
        );
    }
}
