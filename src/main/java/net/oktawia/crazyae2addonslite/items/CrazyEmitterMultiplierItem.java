package net.oktawia.crazyae2addonslite.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.api.parts.SelectedPart;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.automation.StorageLevelEmitterPart;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.logic.CrazyEmitterMultiplierHost;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CrazyEmitterMultiplierItem extends AEBaseItem implements IMenuItem {
    public CrazyEmitterMultiplierItem(Properties properties) {
        super(new Properties().stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        if (!level.isClientSide() && !p.isSecondaryUseActive()) {
            MenuOpener.open(CrazyMenuRegistrar.CRAZY_EMITTER_MULTIPLIER_MENU.get(), p, MenuLocators.forHand(p, hand));
        }
        return new InteractionResultHolder<>(
                InteractionResult.sidedSuccess(level.isClientSide()), p.getItemInHand(hand));
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new CrazyEmitterMultiplierHost(player, inventorySlot, stack);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext ctx) {
        if (!ctx.isSecondaryUseActive() || ctx.getLevel().isClientSide)
            return InteractionResult.PASS;

        CompoundTag tag = ctx.getItemInHand().getTag();
        if (tag == null) return InteractionResult.FAIL;
        double value = 0;
        boolean mult = false;
        long amt = 0;
        if (tag.contains("val")){
            value = tag.getDouble("val");
        }
        if (tag.contains("mult")){
            mult = tag.getBoolean("mult");
        }

        if (ctx.getPlayer() == null) return InteractionResult.FAIL;

        IPart part = getClickedPart(ctx);
        if (part instanceof StorageLevelEmitterPart emitter){
            var ogAmt = emitter.getReportingValue();
            if (mult){
                amt = (long)(ogAmt * value);
            } else {
                amt = (long)value;
            }
            emitter.setReportingValue(amt);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    private static IPart getClickedPart(UseOnContext ctx) {
        Vec3 hit = ctx.getClickLocation()
                .subtract(ctx.getClickedPos().getX(), ctx.getClickedPos().getY(), ctx.getClickedPos().getZ());
        IPartHost host = PartHelper.getPartHost(ctx.getLevel(), ctx.getClickedPos());
        SelectedPart sel = host == null ? null : host.selectPartLocal(hit);
        return sel != null ? sel.part : null;
    }
}
