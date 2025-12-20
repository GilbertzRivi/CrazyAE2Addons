package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.MathParser;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.CrazyCalculatorMenu;
import net.oktawia.crazyae2addons.menus.CrazyEmitterMultiplierMenu;
import net.oktawia.crazyae2addons.misc.IconButton;


public class CrazyCalculatorScreen<C extends CrazyCalculatorMenu> extends AEBaseScreen<C> {

    public IconButton confirm;
    public AETextField equation;
    public AETextField result;

    public CrazyCalculatorScreen(CrazyCalculatorMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super((C) menu, playerInventory, title, style);

        this.confirm = new IconButton(Icon.ENTER, this::calculate);
        this.equation = new AETextField(style, Minecraft.getInstance().font, 0,0,0,0);
        this.equation.setBordered(false);
        this.equation.setMaxLength(50);
        this.equation.setPlaceholder(Component.translatable("gui.crazyae2addons.calculator_equation"));
        this.result = new AETextField(style, Minecraft.getInstance().font, 0,0,0,0);
        this.result.setBordered(false);
        this.result.setMaxLength(50);
        this.result.setPlaceholder(Component.translatable("gui.crazyae2addons.calculator_result"));
        this.widgets.add("confirm", this.confirm);
        this.widgets.add("equation", this.equation);
        this.widgets.add("result", this.result);
    }

    public void calculate(Button btn) {
        double evaled = 0;
        try {
            evaled = MathParser.parse(equation.getValue());
        } catch (Exception ignored){
            equation.setTextColor(0xFF0000);
            Runnable col = () -> {equation.setTextColor(0xFFFFFF);};
            Utils.asyncDelay(col, 1);
            return;
        }
        equation.setTextColor(0x00FF00);
        Runnable col = () -> {equation.setTextColor(0xFFFFFF);};
        Utils.asyncDelay(col, 1);
        result.setValue(String.valueOf(evaled));
    }
}
