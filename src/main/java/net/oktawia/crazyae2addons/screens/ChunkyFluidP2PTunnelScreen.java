package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.ChunkyFluidP2PTunnelMenu;
import net.oktawia.crazyae2addons.menus.DisplayMenu;

public class ChunkyFluidP2PTunnelScreen<C extends ChunkyFluidP2PTunnelMenu> extends AEBaseScreen<C> {

    public AETextField value;
    public Button confirm;
    public boolean initialized = false;

    public ChunkyFluidP2PTunnelScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("value", value);
        this.widgets.add("confirm", confirm);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        if (!this.initialized){
            value.setValue(String.valueOf(getMenu().value));
            this.initialized = true;
        }
    }

    private void setupGui(){
        this.setTextContent("info1", Component.translatable("gui.crazyae2addons.chunky_p2p_desc"));
        value = new AETextField(
                style, Minecraft.getInstance().font, 0, 0, 0, 0
        );
        value.setBordered(false);
        value.setMaxLength(9999);
        confirm = new PlainTextButton(
                0,0,0,0, Component.translatable("gui.crazyae2addons.chunky_p2p_save"), btn -> {save();}, Minecraft.getInstance().font);
    }

    private void save(){
        String input = value.getValue();
        if (input.chars().allMatch(Character::isDigit)){
            value.setTextColor(0x00FF00);
            Runnable setColorFunction = () -> value.setTextColor(0xFFFFFF);
            Utils.asyncDelay(setColorFunction, 1);
            getMenu().syncValue(Integer.parseInt(input));
        } else {
            value.setTextColor(0xFF0000);
            Runnable setColorFunction = () -> value.setTextColor(0xFFFFFF);
            Utils.asyncDelay(setColorFunction, 1);
        }
    }
}