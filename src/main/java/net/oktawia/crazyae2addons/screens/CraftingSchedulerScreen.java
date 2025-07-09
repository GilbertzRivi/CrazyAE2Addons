package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.IconButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.AmpereMeterMenu;
import net.oktawia.crazyae2addons.menus.CraftingSchedulerMenu;

public class CraftingSchedulerScreen<C extends CraftingSchedulerMenu> extends AEBaseScreen<C> implements CrazyScreen{
    private static final String NAME = "crafting_scheduler";
    public AETextField amount;
    public boolean initialized = false;
    public IconButton confirm;

    static {
        CrazyScreen.i18n(NAME, "amount_placeholder", "Amount");
        CrazyScreen.i18n(NAME, "save_button", "Save");
    }

    public CraftingSchedulerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.amount = new AETextField(style, Minecraft.getInstance().font, 0, 0,0,0);
        this.amount.setPlaceholder(l10n(NAME, "amount_placeholder"));
        this.amount.setBordered(false);
        this.amount.setMaxLength(9);
        this.confirm = new net.oktawia.crazyae2addons.misc.IconButton(Icon.ENTER, x -> save());
        this.widgets.add("confirm", this.confirm);
        this.widgets.add("amount", this.amount);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        if (!initialized){
            this.amount.setValue(String.valueOf(getMenu().amount));
            initialized = true;
        }
    }

    public void save(){
        if (this.amount.getValue().chars().allMatch(Character::isDigit)){
            this.amount.setTextColor(0x00FF00);
            Runnable setColorFunction = () -> this.amount.setTextColor(0xFFFFFF);
            Utils.asyncDelay(setColorFunction, 1);
            this.getMenu().save(Integer.valueOf(this.amount.getValue()));
        } else {
            this.amount.setTextColor(0xFF0000);
            Runnable setColorFunction = () -> this.amount.setTextColor(0xFFFFFF);
            Utils.asyncDelay(setColorFunction, 1);
        }
    }
}
