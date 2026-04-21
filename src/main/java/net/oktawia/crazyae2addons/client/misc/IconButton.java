package net.oktawia.crazyae2addons.client.misc;

import appeng.client.gui.Icon;
import lombok.Setter;

@Setter
public class IconButton extends appeng.client.gui.widgets.IconButton {

    private Icon icon;

    public IconButton(Icon ico, OnPress prs) {
        super(prs);
        this.icon = ico;
    }

    @Override
    protected Icon getIcon() {
        return this.icon;
    }

}
