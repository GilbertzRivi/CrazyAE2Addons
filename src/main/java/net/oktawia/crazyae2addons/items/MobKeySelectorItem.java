package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.MobKeySelectorHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MobKeySelectorItem extends AEBaseItem implements IMenuItem {
    public static final String NBT_MOBKEY = "mob_key";

    public MobKeySelectorItem(Item.Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        if (!level.isClientSide() && !p.isSecondaryUseActive()) {
            MenuOpener.open(CrazyMenuRegistrar.MOB_KEY_SELECTOR_MENU.get(), p, MenuLocators.forHand(p, hand));
        }
        return InteractionResultHolder.sidedSuccess(p.getItemInHand(hand), level.isClientSide());
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int slot, ItemStack stack, @Nullable BlockPos pos) {
        return new MobKeySelectorHost(player, slot, stack);
    }

    public static void setSelectedKeyId(ItemStack stack, String keyId) {
        var tag = stack.getOrCreateTag();
        if (keyId == null || keyId.isEmpty()) tag.remove(NBT_MOBKEY);
        else tag.putString(NBT_MOBKEY, keyId);
    }

    public static String getSelectedKeyId(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(NBT_MOBKEY) ? tag.getString(NBT_MOBKEY) : "";
    }
}
