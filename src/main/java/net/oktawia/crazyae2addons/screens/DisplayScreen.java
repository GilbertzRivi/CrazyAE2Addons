package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.UpgradeableScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AECheckbox;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.ToggleButton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
import net.oktawia.crazyae2addons.menus.EntityTickerMenu;
import net.oktawia.crazyae2addons.menus.NBTExportBusMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.misc.SyntaxHighlighter;

public class DisplayScreen<C extends DisplayMenu> extends AEBaseScreen<C> {

    public MultilineTextFieldWidget value;
    public Button confirm;
    public ToggleButton mode;
    public ToggleButton center;
    public ToggleButton margin;
    public boolean initialized = false;
    public Scrollbar scrollbar;
    private int lastScroll = -1;

    public DisplayScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("value", value);
        this.widgets.add("confirm", confirm);
        this.widgets.add("mode", mode);
        this.widgets.add("center", center);
        this.widgets.add("margin", margin);
        this.widgets.add("scroll", scrollbar);
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

    private void setupGui(){
        scrollbar = new Scrollbar();
        scrollbar.setSize(12, 100);
        scrollbar.setRange(0, 100, 4);
        value = new MultilineTextFieldWidget(Minecraft.getInstance().font, 15, 15, 202, 135, Component.literal("Type here"));
        value.setTokenizer(SyntaxHighlighter::colorizeMarkdown);
        confirm = new IconButton(Icon.ENTER, btn -> save());
        confirm.setTooltip(Tooltip.create(Component.literal("Submit")));
        mode = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMode);
        mode.setTooltip(Tooltip.create(Component.literal("Join with adjacent displays")));
        center = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeCenter);
        center.setTooltip(Tooltip.create(Component.literal("Center text")));
        margin = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMargin);
        margin.setTooltip(Tooltip.create(Component.literal("3% margin around the text")));
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