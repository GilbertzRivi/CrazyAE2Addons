package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.CrazyPatternModifierHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrazyPatternModifierItem extends AEBaseItem implements IMenuItem {
    public CrazyPatternModifierItem(Properties properties) {
        super(new Item.Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        if (!level.isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU.get(), p, MenuLocators.forHand(p, hand));
        }
        return new InteractionResultHolder<>(
                InteractionResult.sidedSuccess(level.isClientSide()), p.getItemInHand(hand));
    }

    @Override
    public @NotNull InteractionResult useOn(net.minecraft.world.item.context.UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        BlockPos pos = ctx.getClickedPos();

        if (level.isClientSide() || player == null) {
            return InteractionResult.SUCCESS;
        }

        InteractionHand hand = ctx.getHand();
        ItemStack stack = player.getItemInHand(hand);

        CompoundTag tag = stack.getOrCreateTag();
        tag.remove("ppos");
        stack.setTag(tag);

        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PatternProviderBlockEntity) {
            tag = stack.getOrCreateTag();
            tag.putLong("ppos", pos.asLong());
            stack.setTag(tag);

            player.setItemInHand(hand, stack);
            player.getInventory().setChanged();
            player.inventoryMenu.broadcastChanges();

            MenuOpener.open(CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU_PP.get(),
                    player, MenuLocators.forHand(player, hand));
            return InteractionResult.SUCCESS;
        }

        player.setItemInHand(hand, stack);
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();

        MenuOpener.open(CrazyMenuRegistrar.CRAZY_PATTERN_MODIFIER_MENU.get(),
                player, MenuLocators.forHand(player, hand));
        return InteractionResult.SUCCESS;
    }


    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new CrazyPatternModifierHost(player, inventorySlot, stack);
    }
}
