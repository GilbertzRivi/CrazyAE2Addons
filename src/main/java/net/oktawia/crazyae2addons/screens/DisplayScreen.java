package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.misc.SyntaxHighlighter;

public class DisplayScreen<C extends DisplayMenu> extends AEBaseScreen<C> implements CrazyScreen {

    private static final String NAME = "display_screen";
    public MultilineTextFieldWidget value;
    public Button confirm;
    public ToggleButton mode;
    public ToggleButton center;
    public AETextField fontSize;
    public boolean initialized = false;
    public Scrollbar scrollbar;
    private int lastScroll = -1;

    static {
        CrazyScreen.i18n(NAME, "type_here", "Type here");
        CrazyScreen.i18n(NAME, "submit", "Submit");
        CrazyScreen.i18n(NAME, "join_displays", "Join with adjacent displays");
        CrazyScreen.i18n(NAME, "font_size", "Font size");
    }

    public DisplayScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("value", value);
        this.widgets.add("confirm", confirm);
        this.widgets.add("mode", mode);
        this.widgets.add("font", fontSize);
        this.widgets.add("scroll", scrollbar);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!this.initialized) {
            value.setValue(getMenu().displayValue.replace("&nl", "\n"));
            fontSize.setValue(String.valueOf(getMenu().fontSize));
            mode.setState(getMenu().mode);
            this.initialized = true;
        }
    }

    private void setupGui() {
        scrollbar = new Scrollbar();
        scrollbar.setSize(12, 100);
        scrollbar.setRange(0, 100, 4);

        value = new MultilineTextFieldWidget(Minecraft.getInstance().font, 15, 15, 202, 135, l10n(NAME, "type_here"));
        value.setTokenizer(SyntaxHighlighter::colorizeMarkdown);

        confirm = new IconButton(Icon.ENTER, btn -> save());
        confirm.setTooltip(Tooltip.create(l10n(NAME, "submit")));

        mode = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMode);
        mode.setTooltip(Tooltip.create(l10n(NAME, "join_displays")));

        fontSize = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        fontSize.setBordered(false);
        fontSize.setMaxLength(5);
        fontSize.setTooltip(Tooltip.create(l10n(NAME, "font_size")));
        fontSize.setResponder(val -> getMenu().setFont(val));
    }

    private void changeMode(boolean b) {
        this.getMenu().changeMode(b);
        mode.setState(b);
    }

    private void save() {
        getMenu().syncValue(value.getValue().replace("\n", "&nl"));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        if (button == 1 && this.fontSize != null && this.fontSize.isMouseOver(mouseX, mouseY)) {
            this.fontSize.setValue("");
            return true;
        }

        return handled;
    }

    @Override
    public void containerTick() {
        super.containerTick();

        int maxScroll = (int) value.getMaxScroll();
        scrollbar.setRange(0, maxScroll, 4);

        int currentScrollbarPos = scrollbar.getCurrentScroll();
        if (currentScrollbarPos != lastScroll) {
            lastScroll = currentScrollbarPos;
            value.setScrollAmount(currentScrollbarPos);
        } else {
            int currentInputScroll = (int) value.getScrollAmount();
            if (currentInputScroll != currentScrollbarPos) {
                scrollbar.setCurrentScroll(currentInputScroll);
                lastScroll = currentInputScroll;
            }
        }
    }
}
