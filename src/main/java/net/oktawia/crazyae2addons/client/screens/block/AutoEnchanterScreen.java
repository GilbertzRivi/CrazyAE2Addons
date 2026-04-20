package net.oktawia.crazyae2addons.client.screens.block;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.util.Utils;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.block.AutoEnchanterMenu;

import java.util.List;

public class AutoEnchanterScreen<C extends AutoEnchanterMenu> extends AEBaseScreen<C> {

    private final IconButton opt1;
    private final IconButton opt2;
    private final IconButton opt3;
    private final ToggleButton autoSupplyLapis;
    private final ToggleButton autoSupplyBooks;

    public AutoEnchanterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.opt1 = new IconButton(Icon.ENTER, btn -> this.getMenu().syncOption(1));
        this.opt2 = new IconButton(Icon.ENTER, btn -> this.getMenu().syncOption(2));
        this.opt3 = new IconButton(Icon.ENTER, btn -> this.getMenu().syncOption(3));

        this.autoSupplyLapis = new ToggleButton(Icon.ENTER, Icon.CLEAR, this::toggleSupplyLapis);
        this.autoSupplyBooks = new ToggleButton(Icon.ENTER, Icon.CLEAR, this::toggleSupplyBooks);

        setupGui();
    }

    private void setupGui() {
        this.autoSupplyLapis.setTooltipOn(List.of(Component.translatable(LangDefs.AUTO_SUPPLY_LAPIS_ON.getTranslationKey())));
        this.autoSupplyLapis.setTooltipOff(List.of(Component.translatable(LangDefs.AUTO_SUPPLY_LAPIS_OFF.getTranslationKey())));

        this.autoSupplyBooks.setTooltipOn(List.of(Component.translatable(LangDefs.AUTO_SUPPLY_BOOKS_ON.getTranslationKey())));
        this.autoSupplyBooks.setTooltipOff(List.of(Component.translatable(LangDefs.AUTO_SUPPLY_BOOKS_OFF.getTranslationKey())));

        this.opt1.setTooltip(Tooltip.create(Component.translatable(LangDefs.CHEAP.getTranslationKey())));
        this.opt2.setTooltip(Tooltip.create(Component.translatable(LangDefs.MEDIUM.getTranslationKey())));
        this.opt3.setTooltip(Tooltip.create(Component.translatable(LangDefs.EXPENSIVE.getTranslationKey())));

        this.widgets.add("opt1", this.opt1);
        this.widgets.add("opt2", this.opt2);
        this.widgets.add("opt3", this.opt3);
        this.widgets.add("aslapis", this.autoSupplyLapis);
        this.widgets.add("asbooks", this.autoSupplyBooks);
    }

    private void toggleSupplyLapis(boolean value) {
        this.getMenu().changeAutoSupplyLapis(value);
        this.autoSupplyLapis.setState(getMenu().getHost().isAutoSupplyLapis());
    }

    private void toggleSupplyBooks(boolean value) {
        this.getMenu().changeAutoSupplyBooks(value);
        this.autoSupplyBooks.setState(getMenu().getHost().isAutoSupplyBooks());
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        this.setTextContent("xpval", Component.literal(Utils.shortenNumber(getMenu().getHost().getXp())));

        this.setTextContent(
                "option",
                Component.translatable(
                        LangDefs.SELECTED_OPTION.getTranslationKey(),
                        getSelectedOptionLabel()
                )
        );

        var estVal = getMenu().getHost().getLevelCost();
        if (estVal.isBlank()) {
            estVal = "0.0";
        }

        this.setTextContent(
                "estval",
                Component.translatable(LangDefs.REQUIRED.getTranslationKey(), estVal)
        );

        this.autoSupplyLapis.setState(getMenu().getHost().isAutoSupplyLapis());
        this.autoSupplyBooks.setState(getMenu().getHost().isAutoSupplyBooks());
    }

    private Component getSelectedOptionLabel() {
        return switch (getMenu().getHost().getOption()) {
            case 1 -> Component.translatable(LangDefs.CHEAP.getTranslationKey());
            case 2 -> Component.translatable(LangDefs.MEDIUM.getTranslationKey());
            case 3 -> Component.translatable(LangDefs.EXPENSIVE.getTranslationKey());
            default -> Component.translatable(LangDefs.NONE.getTranslationKey());
        };
    }
}