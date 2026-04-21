package net.oktawia.crazyae2addons.items.wireless;

import appeng.api.config.FuzzyMode;
import appeng.api.config.RedstoneMode;
import appeng.api.config.Settings;
import appeng.api.util.IConfigManager;
import de.mari_023.ae2wtlib.terminal.IUniversalWirelessTerminalItem;
import de.mari_023.ae2wtlib.terminal.ItemWT;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.oktawia.crazyae2addons.menus.item.WirelessEmitterTerminalMenu;
import org.jetbrains.annotations.NotNull;

public class WirelessEmitterTerminalItem extends ItemWT implements IUniversalWirelessTerminalItem {

    public WirelessEmitterTerminalItem() {
        super();
    }

    @Override
    public @NotNull MenuType<?> getMenuType(@NotNull ItemStack stack) {
        return WirelessEmitterTerminalMenu.TYPE;
    }

    @Override
    public @NotNull IConfigManager getConfigManager(@NotNull ItemStack target) {
        IConfigManager cm = super.getConfigManager(target);
        cm.registerSetting(Settings.FUZZY_MODE, FuzzyMode.IGNORE_ALL);
        cm.registerSetting(Settings.REDSTONE_CONTROLLED, RedstoneMode.IGNORE);
        return cm;
    }
}
