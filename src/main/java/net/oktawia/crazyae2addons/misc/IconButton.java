package net.oktawia.crazyae2addons.misc;

import appeng.client.gui.Icon;

public class IconButton extends appeng.client.gui.widgets.IconButton {

    public Icon icon;

    public IconButton(Icon ico, OnPress prs) {
        super(prs);
        this.icon = ico;
    }

    @Override
    protected Icon getIcon() {
        return this.icon;
    }

    public void setIcon(Icon icon) { this.icon = icon; }
}
