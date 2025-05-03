package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.items.XpShardItem;
import net.oktawia.crazyae2addons.menus.AutoEnchanterMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

import java.util.List;

public class AutoEnchanterScreen<C extends AutoEnchanterMenu> extends UpgradeableScreen<C> {
    public IconButton opt1;
    public IconButton opt2;
    public IconButton opt3;
    public ToggleButton autoSupplyLapis;
    public ToggleButton autoSupplyBooks;

    public AutoEnchanterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
    }

    private void setupGui() {
        this.opt1 = new IconButton(Icon.ENTER, (x) -> {this.getMenu().syncOption(1);});
        this.opt2 = new IconButton(Icon.ENTER, (x) -> {this.getMenu().syncOption(2);});
        this.opt3 = new IconButton(Icon.ENTER, (x) -> {this.getMenu().syncOption(3);});
        this.autoSupplyLapis = new ToggleButton(Icon.VALID, Icon.INVALID, this::toggleSupplyLapis);
        this.autoSupplyLapis.setTooltipOn(List.of(Component.literal("Automatic lapis supply: Enabled")));
        this.autoSupplyLapis.setTooltipOff(List.of(Component.literal("Automatic lapis supply: Disabled")));
        this.autoSupplyLapis.setState(getMenu().autoSupplyLapis);
        this.autoSupplyBooks = new ToggleButton(Icon.VALID, Icon.INVALID, this::toggleSupplyBooks);
        this.autoSupplyBooks.setTooltipOn(List.of(Component.literal("Automatic books supply: Enabled")));
        this.autoSupplyBooks.setTooltipOff(List.of(Component.literal("Automatic books supply: Disabled")));
        this.autoSupplyBooks.setState(getMenu().autoSupplyBooks);

        this.opt1.setTooltip(Tooltip.create(
                Component.literal("Cheap enchantment\nApply low-level enchant (cost: 1)")
        ));

        this.opt2.setTooltip(Tooltip.create(
                Component.literal("Medium enchantment\nApply mid-level enchant (cost: 2)")
        ));

        this.opt3.setTooltip(Tooltip.create(
                Component.literal("Expensive enchantment\nApply powerful enchant (cost: 3)")
        ));

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
            case 1 -> "Cheap";
            case 2 -> "Medium";
            case 3 -> "Exp";
            default -> "None";
        };
        this.setTextContent("option", Component.literal("Selected option: " + label));
        this.setTextContent("xpval", Component.literal(Utils.shortenNumber(getMenu().xp * XpShardItem.XP_VAL)));
        this.setTextContent("estval", Component.literal(String.format("~Cost: %s", getMenu().levelCost)));
        this.autoSupplyLapis.setState(getMenu().autoSupplyLapis);
        this.autoSupplyBooks.setState(getMenu().autoSupplyBooks);
    }
}