package net.oktawia.crazyae2addonslite.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addonslite.menus.CraftingSchedulerMenu;

public class CraftingSchedulerScreen<C extends CraftingSchedulerMenu> extends AEBaseScreen<C> {
    public AETextField amount;
    public boolean initialized = false;

    public CraftingSchedulerScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.amount = new AETextField(style, Minecraft.getInstance().font, 0, 0,0,0);
        this.amount.setPlaceholder(Component.translatable("gui.crazyae2addons.c_scheduler_amount"));
        this.amount.setBordered(false);
        this.amount.setMaxLength(9);
        this.amount.setResponder(str -> this.save());
        this.amount.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.c_scheduler_amount_tt")));
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
        if (this.amount.getValue().chars().allMatch(Character::isDigit) && !this.amount.getValue().isEmpty()){
            this.getMenu().save(Integer.valueOf(this.amount.getValue()));
        }
    }
}