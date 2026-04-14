package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.misc.SyntaxHighlighter;

public class DisplayScreen<C extends DisplayMenu> extends AEBaseScreen<C> {

    public MultilineTextFieldWidget value;
    public Button confirm;
    public Button insertToken;
    public ToggleButton mode;
    public ToggleButton center;
    public ToggleButton margin;
    public boolean initialized = false;

    public DisplayScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("value", value);
        this.widgets.add("confirm", confirm);
        this.widgets.add("insertToken", insertToken);
        this.widgets.add("mode", mode);
        this.widgets.add("center", center);
        this.widgets.add("margin", margin);
    }

    @Override
    protected void updateBeforeRender(){
        super.updateBeforeRender();
        if (!this.initialized){
            value.setValue(getMenu().displayValue.replace("&nl", "\n"));
            mode.setState(getMenu().mode);
            center.setState(getMenu().centerText);
            margin.setState(getMenu().margin);
            this.initialized = true;
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double delta) {
        if (value.isMouseOver(x, y)) return value.mouseScrolled(x, y, delta);
        return super.mouseScrolled(x, y, delta);
    }

    private void setupGui(){
        value = new MultilineTextFieldWidget(Minecraft.getInstance().font, 15, 15, 205, 135, Component.translatable("gui.crazyae2addons.display_type_here"));
        value.setTokenizer(SyntaxHighlighter::colorizeMarkdown);

        confirm = new IconButton(Icon.ENTER, btn -> save());
        confirm.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.display_submit")));

        insertToken = new IconButton(Icon.HELP, btn ->
                Minecraft.getInstance().setScreen(new DisplayInsertScreen(this, token -> value.insertText(token))));
        insertToken.setTooltip(Tooltip.create(LangDefs.DISPLAY_INSERT_TOKEN.text()));

        mode = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMode);
        mode.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.display_join")));

        center = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeCenter);
        center.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.display_center")));

        margin = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMargin);
        margin.setTooltip(Tooltip.create(Component.translatable("gui.crazyae2addons.display_margin")));
    }

    private void changeMode(boolean b) {
        this.getMenu().changeMode(b);
        mode.setState(b);
    }
    private void changeCenter(boolean b) {
        this.getMenu().changeCenter(b);
        center.setState(b);
    }
    private void changeMargin(boolean b) {
        this.getMenu().changeMargin(b);
        margin.setState(b);
    }

    private void save(){
        getMenu().syncValue(value.getValue().replace("\n", "&nl"));
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        value.keyPressed(key, sc, mod);
        if (key == 256) { this.onClose(); return true; }
        return true;
    }
}