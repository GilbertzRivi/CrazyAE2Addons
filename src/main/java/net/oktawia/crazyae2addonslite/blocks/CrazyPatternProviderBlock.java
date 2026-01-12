package net.oktawia.crazyae2addonslite.blocks;

import appeng.block.crafting.PatternProviderBlock;
import appeng.menu.locator.MenuLocators;
import appeng.util.InteractionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addonslite.CrazyConfig;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addonslite.entities.CrazyPatternProviderBE;
import net.oktawia.crazyae2addonslite.network.NetworkHandler;
import net.oktawia.crazyae2addonslite.network.SyncBlockClientPacket;
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
    public InteractionResult onActivated(Level level, BlockPos pos, Player player,
                                         InteractionHand hand, ItemStack heldItem, BlockHitResult hit) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }

        BlockEntity myBe = level.getBlockEntity(pos);
        if (myBe instanceof CrazyPatternProviderBE crazyProvider) {
            int added = crazyProvider.getAdded();
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new SyncBlockClientPacket(pos, added));
        }

        if (!heldItem.isEmpty() && heldItem.getItem() == CrazyItemRegistrar.CRAZY_UPGRADE.get().asItem()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrazyPatternProviderBE crazyProvider) {
                int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
                int cur = crazyProvider.getAdded();

                if (cur >= maxAdd && maxAdd != -1) {
                    player.displayClientMessage(
                            Component.translatable("gui.crazyae2addons.provider_max"),
                            true
                    );
                    return InteractionResult.SUCCESS;
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

                NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                        new SyncBlockClientPacket(pos, added));

                return InteractionResult.SUCCESS;
            }
        }

        if (InteractionUtil.canWrenchRotate(heldItem)) {
            setSide(level, pos, hit.getDirection());
            return InteractionResult.sidedSuccess(false);
        }

        var be = this.getBlockEntity(level, pos);
        if (be != null) {
            be.openMenu(player, MenuLocators.forBlockEntity(be));
            return InteractionResult.sidedSuccess(false);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        BlockEntity be = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (be instanceof CrazyPatternProviderBE myBe) {
            ItemStack stack = new ItemStack(this);
            CompoundTag tag = new CompoundTag();
            tag.putInt("added", myBe.getAdded());
            myBe.getLogic().writeToNBT(tag);
            appeng.util.inv.AppEngInternalInventory inv =
                    (appeng.util.inv.AppEngInternalInventory) myBe.getLogic().getPatternInv();
            inv.writeToNBT(tag, "dainv");
            myBe.getUpgrades().writeToNBT(tag, "upgrades");
            stack.setTag(tag);
            myBe.getLogic().getPatternInv().clear();
            return List.of(stack);
        }
        return super.getDrops(state, builder);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state,
                            LivingEntity placer, ItemStack stack) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CrazyPatternProviderBE myBe && stack.hasTag()) {
                CompoundTag tag = stack.getOrCreateTag();
                if (tag.contains("added")) {
                    myBe.loadTag(tag);
                    NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                            new SyncBlockClientPacket(pos, tag.getInt("added")));
                }
            }
        }
    }
}
