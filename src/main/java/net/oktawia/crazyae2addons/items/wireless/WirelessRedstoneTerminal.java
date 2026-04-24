package net.oktawia.crazyae2addons.items.wireless;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import appeng.menu.locator.MenuLocators;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class WirelessRedstoneTerminal extends ItemWT implements IUniversalWirelessTerminalItem {
    public WirelessRedstoneTerminal() {
        super();
    }

    public @NotNull MenuType<?> getMenuType(@NotNull ItemStack stack) {
        return CrazyMenuRegistrar.WIRELESS_REDSTONE_TERMINAL_MENU.get();
    }

    public @NotNull IConfigManager getConfigManager(@NotNull ItemStack target) {
        IConfigManager configManager = super.getConfigManager(target);
        configManager.registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        configManager.registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        return configManager;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag advancedTooltips) {
        super.appendHoverText(stack, level, tooltip, advancedTooltips);

        if (!CrazyConfig.COMMON.WIRELESS_REDSTONE_TERMINAL_ENABLED.get()) {
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED.getTranslationKey())
                    .withStyle(ChatFormatting.RED));
            tooltip.add(Component.translatable(LangDefs.FEATURE_DISABLED_CONFIG.getTranslationKey())
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level w, Player player, InteractionHand hand) {
        ItemStack is = player.getItemInHand(hand);
        if (!CrazyConfig.COMMON.REDSTONE_EMITTER_TERMINAL_ENABLED.get() || !CrazyConfig.COMMON.WIRELESS_REDSTONE_TERMINAL_ENABLED.get()) {
            return new InteractionResultHolder(InteractionResult.FAIL, is);
        }
        if (this.checkUniversalPreconditions(is, player)) {
            this.open(player, is, MenuLocators.forHand(player, hand), false);
            return new InteractionResultHolder(InteractionResult.SUCCESS, is);
        } else {
            return new InteractionResultHolder(InteractionResult.FAIL, is);
        }
    }
}
