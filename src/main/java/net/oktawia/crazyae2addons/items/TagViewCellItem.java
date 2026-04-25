package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.items.storage.ViewCellItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.viewcell.TagViewCellHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TagViewCellItem extends ViewCellItem implements IMenuItem {

    public TagViewCellItem(Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand hand
    ) {
        if (CrazyConfig.COMMON.TAG_VIEW_CELL_ENABLED.get()) {
            if (!level.isClientSide()) {
                MenuOpener.open(
                        CrazyMenuRegistrar.TAG_VIEW_CELL_MENU.get(),
                        player,
                        MenuLocators.forHand(player, hand)
                );
            }
        }

        return new InteractionResultHolder<>(
                InteractionResult.sidedSuccess(level.isClientSide()),
                player.getItemInHand(hand)
        );
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new TagViewCellHost(player, inventorySlot, stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, level, tooltip, advancedTooltips);

        if (!CrazyConfig.COMMON.TAG_VIEW_CELL_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}