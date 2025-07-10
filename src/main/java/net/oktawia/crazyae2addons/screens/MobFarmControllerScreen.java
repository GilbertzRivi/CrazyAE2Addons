package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.MobFarmControllerMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class MobFarmControllerScreen<C extends MobFarmControllerMenu> extends UpgradeableScreen<C> implements CrazyScreen {
    private static final String NAME = "mob_farm_controller";

    static {
        CrazyScreen.i18n(NAME, "enable_preview", "Enable/Disable preview");
        CrazyScreen.i18n(NAME, "damage_blocks", "Damage blocks: %s%%");
        CrazyScreen.i18n(NAME, "item_to_use", "Item to use:");
        CrazyScreen.i18n(NAME, "speed_info1", "Speed depends on cards");
        CrazyScreen.i18n(NAME, "speed_info2", "inserted and blocks used");
        CrazyScreen.i18n(NAME, "preview", "Preview: %s");
    }

    public MobFarmControllerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        var prevBtn = new IconButton(Icon.ENTER, btn -> getMenu().changePreview(!getMenu().preview));
        prevBtn.setTooltip(Tooltip.create(l10n(NAME, "enable_preview")));
        this.widgets.add("prevbtn", prevBtn);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        this.setTextContent("dmgblocks", l10n(NAME, "damage_blocks", getMenu().damageBlocks));
        this.setTextContent("dmgitem", l10n(NAME, "item_to_use"));
        this.setTextContent("info1", l10n(NAME, "speed_info1"));
        this.setTextContent("info2", l10n(NAME, "speed_info2"));
        setTextContent("prev", l10n(NAME, "preview", getMenu().preview));
    }
}
