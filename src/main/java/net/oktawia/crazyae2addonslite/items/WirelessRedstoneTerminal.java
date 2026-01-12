package net.oktawia.crazyae2addonslite.items;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import org.jetbrains.annotations.NotNull;


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
}
