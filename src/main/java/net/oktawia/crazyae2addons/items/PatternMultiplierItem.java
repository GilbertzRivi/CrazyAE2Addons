package net.oktawia.crazyae2addons.items;

import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartHelper;
import appeng.api.parts.SelectedPart;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.items.AEBaseItem;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.patternmultiplier.PatternMultiplierHost;
import net.oktawia.crazyae2addons.logic.patternmultiplier.PatternMultiplierLogic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PatternMultiplierItem extends AEBaseItem implements IMenuItem {

    public static final String MULT_TAG = "mult";
    public static final String LIMIT_TAG = "limit";
    public static final String INV_TAG = "inv";

    public PatternMultiplierItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(
            @NotNull Level level,
            @NotNull Player player,
            @NotNull InteractionHand hand
    ) {
        if (CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            if (!level.isClientSide() && !player.isSecondaryUseActive()) {
                MenuOpener.open(
                        CrazyMenuRegistrar.PATTERN_MULTIPLIER_MENU.get(),
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
        return new PatternMultiplierHost(player, inventorySlot, stack);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            return InteractionResult.PASS;
        }
        if (!context.isSecondaryUseActive() || context.getLevel().isClientSide) {
            return InteractionResult.PASS;
        }

        var player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        var toolStack = context.getItemInHand();
        if (!hasStoredConfig(toolStack)) {
            return InteractionResult.FAIL;
        }

        double multiplier = readMultiplier(toolStack);
        int limit = readLimit(toolStack);

        if (multiplier <= 0) {
            return InteractionResult.FAIL;
        }

        var part = getClickedPart(context);

        if (part instanceof InterfaceLogicHost interfaceHost
                && PatternMultiplierLogic.applyToInterface(interfaceHost, multiplier, limit)) {
            return InteractionResult.SUCCESS;
        }

        if (part instanceof PatternProviderLogicHost patternProviderHost
                && PatternMultiplierLogic.applyToPatternProvider(patternProviderHost, multiplier, limit)) {
            return InteractionResult.SUCCESS;
        }

        var blockEntity = context.getLevel().getBlockEntity(context.getClickedPos());

        if (blockEntity instanceof InterfaceLogicHost interfaceHost
                && PatternMultiplierLogic.applyToInterface(interfaceHost, multiplier, limit)) {
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof PatternProviderLogicHost patternProviderHost
                && PatternMultiplierLogic.applyToPatternProvider(patternProviderHost, multiplier, limit)) {
            return InteractionResult.SUCCESS;
        }

        if (blockEntity instanceof Container container
                && PatternMultiplierLogic.applyToContainer(container, multiplier, limit, context.getLevel())) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.FAIL;
    }

    public static boolean isAllowedInMultiplier(ItemStack stack) {
        return stack.isEmpty()
                || appeng.core.definitions.AEItems.PROCESSING_PATTERN.isSameAs(stack)
                || appeng.core.definitions.AEItems.CRAFTING_PATTERN.isSameAs(stack)
                || appeng.core.definitions.AEItems.BLANK_PATTERN.isSameAs(stack);
    }

    public static void writeConfig(ItemStack stack, double multiplier, int limit) {
        var tag = stack.getOrCreateTag();
        tag.putDouble(MULT_TAG, multiplier);
        tag.putInt(LIMIT_TAG, Math.max(0, limit));
    }

    public static double readMultiplier(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null ? 0.0D : tag.getDouble(MULT_TAG);
    }

    public static int readLimit(ItemStack stack) {
        var tag = stack.getTag();
        return tag == null ? 0 : Math.max(0, tag.getInt(LIMIT_TAG));
    }

    public static boolean hasStoredConfig(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(MULT_TAG) && tag.contains(LIMIT_TAG);
    }

    private static @Nullable IPart getClickedPart(UseOnContext context) {
        Vec3 hit = context.getClickLocation().subtract(
                context.getClickedPos().getX(),
                context.getClickedPos().getY(),
                context.getClickedPos().getZ()
        );

        IPartHost host = PartHelper.getPartHost(context.getLevel(), context.getClickedPos());
        SelectedPart selected = host == null ? null : host.selectPartLocal(hit);
        return selected == null ? null : selected.part;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, level, tooltip, advancedTooltips);

        if (!CrazyConfig.COMMON.PATTERN_MULTIPLIER_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
    }
}