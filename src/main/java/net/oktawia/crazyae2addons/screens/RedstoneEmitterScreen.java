package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.RedstoneEmitterMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

import java.util.Arrays;

public class RedstoneEmitterScreen<C extends RedstoneEmitterMenu> extends AEBaseScreen<C> {

    public AETextField name;
    public IconButton confirm;
    public boolean initialized = false;

    public RedstoneEmitterScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        this.name = new AETextField(style, Minecraft.getInstance().font, 0,0,0,0);
        this.name.setBordered(false);
        this.name.setMaxLength(16);
        this.name.setPlaceholder(Component.literal("Name"));
        this.confirm = new IconButton(Icon.ENTER, x -> save());
        this.widgets.add("name", this.name);
        this.widgets.add("confirm", this.confirm);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        if (!initialized){
            this.name.setValue(getMenu().name);
            initialized = true;
        }
    }

    public void save(){
        if (Arrays.stream(this.getMenu().emitterNames.split("\\|")).noneMatch(name -> name.equals(this.name.getValue()))){
            this.name.setTextColor(0x00FF00);
            Runnable setColorFunction = () -> this.name.setTextColor(0xFFFFFF);
            Utils.asyncDelay(setColorFunction, 1);
            this.getMenu().changeName(this.name.getValue());
        } else {
            this.name.setTextColor(0xFF0000);
            Runnable setColorFunction = () -> this.name.setTextColor(0xFFFFFF);
            Utils.asyncDelay(setColorFunction, 1);
        }

    }
}