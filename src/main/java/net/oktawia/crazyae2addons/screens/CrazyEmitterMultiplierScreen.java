package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.MathParser;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.CrazyEmitterMultiplierMenu;
import net.oktawia.crazyae2addons.misc.IconButton;

public class CrazyEmitterMultiplierScreen<C extends CrazyEmitterMultiplierMenu> extends AEBaseScreen<C> implements CrazyScreen {

    private static final String NAME = "crazy_emitter_multiplier";
    public IconButton confirm;
    public AETextField value;
    public AECheckbox mult;
    private boolean initialized = false;

    static {
        CrazyScreen.i18n(NAME, "multiplier_placeholder", "Multiplier");
        CrazyScreen.i18n(NAME, "checkbox_label", "Multiply instead of set value");
    }

    public CrazyEmitterMultiplierScreen(CrazyEmitterMultiplierMenu menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super((C) menu, playerInventory, title, style);

        this.confirm = new IconButton(Icon.ENTER, this::modify);
        this.value = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        this.value.setBordered(false);
        this.value.setMaxLength(50);
        this.value.setPlaceholder(l10n(NAME, "multiplier_placeholder"));
        this.mult = new AECheckbox(0, 0, 0, 0, style, l10n(NAME, "checkbox_label"));
        this.widgets.add("confirm", this.confirm);
        this.widgets.add("value", this.value);
        this.widgets.add("mult", this.mult);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.initialized) {
            this.value.setValue(String.valueOf(getMenu().value));
            this.mult.setSelected(getMenu().mult);
            this.initialized = true;
        }
    }

    public void modify(Button btn) {
        double evaled = 0;
        try {
            evaled = MathParser.parse(value.getValue());
            if (evaled <= 0) {
                value.setTextColor(0xFF0000);
                Runnable col = () -> value.setTextColor(0xFFFFFF);
                Utils.asyncDelay(col, 1);
                return;
            }
        } catch (Exception ignored) {
            value.setTextColor(0xFF0000);
            Runnable col = () -> value.setTextColor(0xFFFFFF);
            Utils.asyncDelay(col, 1);
            return;
        }
        value.setTextColor(0x00FF00);
        Runnable col = () -> value.setTextColor(0xFFFFFF);
        Utils.asyncDelay(col, 1);
        this.getMenu().saveValue(evaled);
        var foo = this.mult.isSelected();
        this.getMenu().saveMult(foo);
    }
}
