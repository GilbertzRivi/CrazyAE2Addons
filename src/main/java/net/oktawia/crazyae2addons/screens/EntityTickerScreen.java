package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.EntityTickerMenu;

import static java.lang.Math.pow;

public class EntityTickerScreen<C extends EntityTickerMenu> extends UpgradeableScreen<C> implements CrazyScreen {
    private static final String NAME = "entity_ticker";

    static {
        CrazyScreen.i18n(NAME, "energy_usage", "Energy Usage: %s FE/t");
        CrazyScreen.i18n(NAME, "current_multiplier", "Current multiplier: %d");
        CrazyScreen.i18n(NAME, "info1", "Each card multiplies");
        CrazyScreen.i18n(NAME, "info2", "machines speed by 4");
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        double powerUsage = CrazyConfig.COMMON.EntityTickerCost.get() * pow(4, getMenu().upgradeNum);
        setTextContent("energy", l10n(NAME, "energy_usage", Utils.shortenNumber(powerUsage)));
        setTextContent("speed", l10n(NAME, "current_multiplier", (int) pow(2, getMenu().upgradeNum + 1)));
    }

    public EntityTickerScreen(EntityTickerMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super((C) menu, playerInventory, title, style);
        this.setupGui();
    }

    public void setupGui() {
        setTextContent("info1", l10n(NAME, "info1"));
        setTextContent("info2", l10n(NAME, "info2"));
    }

    public void refreshGui() {
        double powerUsage = CrazyConfig.COMMON.EntityTickerCost.get() * pow(4, getMenu().upgradeNum);
        setTextContent("energy", l10n(NAME, "energy_usage", Utils.shortenNumber(powerUsage)));
        setTextContent("speed", l10n(NAME, "current_multiplier", (int) pow(2, getMenu().upgradeNum + 1)));
    }
}
