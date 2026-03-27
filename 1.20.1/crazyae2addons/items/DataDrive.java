package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.DataHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class DataDrive extends AEBaseItem implements IMenuItem {

    public DataDrive(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static Set<ResourceLocation> getUnlocked(ItemStack stack) {
        Set<ResourceLocation> out = new java.util.HashSet<>();
        if (stack == null || stack.isEmpty()) return out;

        var tag = stack.getTag();
        if (tag == null) return out;

        if (!tag.contains("keys", net.minecraft.nbt.Tag.TAG_LIST)) return out;

        var list = tag.getList("keys", net.minecraft.nbt.Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            String s = list.getString(i);
            if (!s.isEmpty()) out.add(new ResourceLocation(s));
        }
        return out;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        if (!level.isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.DATA_DRIVE_MENU.get(), p, MenuLocators.forHand(p, hand));
        }
        return new InteractionResultHolder<>(
                InteractionResult.sidedSuccess(level.isClientSide()), p.getItemInHand(hand));
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new DataHost(player, inventorySlot, stack);
    }
}
