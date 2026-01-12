package net.oktawia.crazyae2addonslite.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addonslite.MathParser;
import net.oktawia.crazyae2addonslite.Utils;
import net.oktawia.crazyae2addonslite.menus.CrazyPatternMultiplierMenu;
import net.oktawia.crazyae2addonslite.misc.IconButton;

public class CrazyPatternMultiplierScreen<C extends CrazyPatternMultiplierMenu> extends AEBaseScreen<C> {

    public IconButton clear;
    public IconButton confirm;
    public AETextField value;
    public AETextField limit;
    private boolean initialized = false;

    public CrazyPatternMultiplierScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        this.confirm = new IconButton(Icon.ENTER, this::modify);
        this.confirm.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.pattern_mult_confirm")));

        this.clear = new IconButton(Icon.CLEAR, this::clear);
        this.clear.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.pattern_mult_clear")));

        this.value = new AETextField(style, Minecraft.getInstance().font, 0,0,0,0);
        this.value.setBordered(false);
        this.value.setMaxLength(50);
        this.value.setPlaceholder(Component.translatable("gui.crazyae2addons.pattern_mult_multiplier"));
        this.value.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.pattern_mult_value_tooltip")));

        this.limit = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.limit.setBordered(false);
        this.limit.setMaxLength(16);
        this.limit.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.pattern_mult_limit")));
        this.limit.setResponder(val -> {
            try {
                int parsed = Integer.parseInt(val);
                getMenu().setLimit(Math.max(parsed, 0));
            } catch (Exception ignored) {}
        });

        this.widgets.add("confirm", this.confirm);
        this.widgets.add("value", this.value);
        this.widgets.add("clear", this.clear);
        this.widgets.add("limit", this.limit);
    }

    public void clear(Button button) {
        getMenu().clearPatterns();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.initialized){
            this.value.setValue(String.valueOf(getMenu().mult));
            this.limit.setValue(String.valueOf(getMenu().limit));
            this.initialized = true;
        }
    }

    public void modify(Button btn) {
        double evaled = 0;
        try {
            evaled = MathParser.parse(value.getValue());
            if (evaled <= 0){
                value.setTextColor(0xFF0000);
                Runnable col = () -> value.setTextColor(0xFFFFFF);
                Utils.asyncDelay(col, 1);
                return;
            }
        } catch (Exception ignored){
            value.setTextColor(0xFF0000);
            Runnable col = () -> value.setTextColor(0xFFFFFF);
            Utils.asyncDelay(col, 1);
            return;
        }
        value.setTextColor(0x00FF00);
        Runnable col = () -> value.setTextColor(0xFFFFFF);
        Utils.asyncDelay(col, 1);
        this.getMenu().modifyPatterns(evaled);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (button == 1 && this.value != null && this.value.isMouseOver(mouseX, mouseY)) {
            this.value.setValue("");
            return true;
        }
        if (button == 1 && this.limit != null && this.limit.isMouseOver(mouseX, mouseY)) {
            this.limit.setValue("0");
            return true;
        }

        return handled;
    }
}