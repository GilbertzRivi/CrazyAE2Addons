package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.AutoEnchanterMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

import java.util.List;

public class AutoEnchanterScreen<C extends AutoEnchanterMenu> extends UpgradeableScreen<C> implements CrazyScreen {
    private static final String NAME = "auto_enchanter";
    public IconButton opt1;
    public IconButton opt2;
    public IconButton opt3;
    public ToggleButton autoSupplyLapis;
    public ToggleButton autoSupplyBooks;

    static {
        CrazyScreen.i18n(NAME, "cheap_enchant", "Cheap enchantment\nApply low-level enchant (cost: 1)");
        CrazyScreen.i18n(NAME, "medium_enchant", "Medium enchantment\nApply mid-level enchant (cost: 2)");
        CrazyScreen.i18n(NAME, "expensive_enchant", "Expensive enchantment\nApply powerful enchant (cost: 3)");
        CrazyScreen.i18n(NAME, "auto_lapis_on", "Automatic lapis supply: Enabled");
        CrazyScreen.i18n(NAME, "auto_lapis_off", "Automatic lapis supply: Disabled");
        CrazyScreen.i18n(NAME, "auto_books_on", "Automatic books supply: Enabled");
        CrazyScreen.i18n(NAME, "auto_books_off", "Automatic books supply: Disabled");
        CrazyScreen.i18n(NAME, "selected_option", "Selected option: %s");
        CrazyScreen.i18n(NAME, "required_xp", "~Required: %s");
    }

    public AutoEnchanterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
    }

    private void setupGui() {
        this.opt1 = new IconButton(Icon.ENTER, (x) -> {this.getMenu().syncOption(1);});
        this.opt2 = new IconButton(Icon.ENTER, (x) -> {this.getMenu().syncOption(2);});
        this.opt3 = new IconButton(Icon.ENTER, (x) -> {this.getMenu().syncOption(3);});
        this.autoSupplyLapis = new ToggleButton(Icon.VALID, Icon.INVALID, this::toggleSupplyLapis);
        this.autoSupplyLapis.setTooltipOn(List.of(l10n(NAME, "auto_lapis_on")));
        this.autoSupplyLapis.setTooltipOff(List.of(l10n(NAME, "auto_lapis_off")));
        this.autoSupplyLapis.setState(getMenu().autoSupplyLapis);
        this.autoSupplyBooks = new ToggleButton(Icon.VALID, Icon.INVALID, this::toggleSupplyBooks);
        this.autoSupplyBooks.setTooltipOn(List.of(l10n(NAME, "auto_books_on")));
        this.autoSupplyBooks.setTooltipOff(List.of(l10n(NAME, "auto_books_off")));
        this.autoSupplyBooks.setState(getMenu().autoSupplyBooks);

        this.opt1.setTooltip(Tooltip.create(l10n(NAME, "cheap_enchant")));
        this.opt2.setTooltip(Tooltip.create(l10n(NAME, "medium_enchant")));
        this.opt3.setTooltip(Tooltip.create(l10n(NAME, "expensive_enchant")));

        this.widgets.add("opt1", this.opt1);
        this.widgets.add("opt2", this.opt2);
        this.widgets.add("opt3", this.opt3);
        this.widgets.add("aslapis", this.autoSupplyLapis);
        this.widgets.add("asbooks", this.autoSupplyBooks);
    }

    private void toggleSupplyLapis(boolean val) {
        this.getMenu().changeAutoSupplyLapis(val);
        this.autoSupplyLapis.setState(getMenu().autoSupplyLapis);
    }
    private void toggleSupplyBooks(boolean val) {
        this.getMenu().changeAutoSupplyBooks(val);
        this.autoSupplyBooks.setState(getMenu().autoSupplyBooks);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        String label = switch (getMenu().option) {
            case 1 -> l10n(NAME, "cheap_enchant").getString();
            case 2 -> l10n(NAME, "medium_enchant").getString();
            case 3 -> l10n(NAME, "expensive_enchant").getString();
            default -> "None";
        };
        this.setTextContent("option", l10n(NAME, "selected_option", label));
        this.setTextContent("xpval", Component.literal(Utils.shortenNumber(getMenu().xp)));
        this.setTextContent("estval", l10n(NAME, "required_xp", getMenu().levelCost));
        this.autoSupplyLapis.setState(getMenu().autoSupplyLapis);
        this.autoSupplyBooks.setState(getMenu().autoSupplyBooks);
    }
}
