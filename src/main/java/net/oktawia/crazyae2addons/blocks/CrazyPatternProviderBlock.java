package net.oktawia.crazyae2addons.blocks;

import appeng.block.crafting.PatternProviderBlock;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
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
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addons.network.packets.SyncBlockClientPacket;
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
            if (be instanceof CrazyPatternProviderBE provider) {
                int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
                int cur = provider.getAdded();
                if (maxAdd != -1 && cur >= maxAdd) {
                    player.displayClientMessage(
                            Component.translatable(LangDefs.PROVIDER_MAX.getTranslationKey()),
                            true
                    );
                    return ItemInteractionResult.sidedSuccess(false);
                }
                provider.setAdded(cur + 1);
                heldItem.shrink(1);
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
            myBe.getUpgrades().writeToNBT(tag, "upgrades", be.getLevel().registryAccess());

            int filled = 0;
            var patternInv = myBe.getLogic().getPatternInv();
            for (int i = 0; i < patternInv.size(); i++) {
                if (!patternInv.getStackInSlot(i).isEmpty()) filled++;
            }
            tag.putInt("filled", filled);

            stack.set(CrazyDataComponents.CRAZY_PROVIDER_DATA.get(), CustomData.of(tag));

            var tooltipTag = new CompoundTag();
            tooltipTag.putInt("added", myBe.getAdded());
            tooltipTag.putInt("filled", filled);
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tooltipTag));

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