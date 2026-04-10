package net.oktawia.crazyae2addons.client.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.client.misc.SyntaxHighlighter;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.DisplayMenu;

public class DisplayScreen<C extends DisplayMenu> extends AEBaseScreen<C> {

    private MultilineTextFieldWidget value;
    private ToggleButton mode;
    private ToggleButton center;
    private ToggleButton margin;

    private boolean initialized = false;

    public DisplayScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        value = new MultilineTextFieldWidget(
                Minecraft.getInstance().font, 15, 15, 205, 135,
                Component.translatable(LangDefs.INSERT.getTranslationKey()));
        value.setTokenizer(SyntaxHighlighter::colorizeMarkdown);

        var confirm = new IconButton(Icon.ENTER, btn -> save());
        confirm.setTooltip(Tooltip.create(Component.translatable(LangDefs.SUBMIT.getTranslationKey())));

        var insertToken = new IconButton(Icon.HELP, btn -> { save(); getMenu().openInsert(value.getCursorPos()); });
        insertToken.setTooltip(Tooltip.create(Component.translatable(LangDefs.INSERT_TOKEN.getTranslationKey())));

        mode   = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMode);
        center = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeCenter);
        margin = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMargin);

        mode.setTooltip(Tooltip.create(Component.translatable(LangDefs.JOIN_DISPLAYS.getTranslationKey())));
        center.setTooltip(Tooltip.create(Component.translatable(LangDefs.CENTER_TEXT.getTranslationKey())));
        margin.setTooltip(Tooltip.create(Component.translatable(LangDefs.ADD_MARGIN.getTranslationKey())));

        widgets.add("value",       value);
        widgets.add("confirm",     confirm);
        widgets.add("insertToken", insertToken);
        widgets.add("mode",        mode);
        widgets.add("center",      center);
        widgets.add("margin",      margin);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (!initialized) {
            value.setValue(getMenu().displayValue.replace("&nl", "\n"));
            mode.setState(getMenu().mode);
            center.setState(getMenu().centerText);
            margin.setState(getMenu().margin);
            initialized = true;
        }

        String pending = getMenu().pendingInsert;
        if (pending != null && !pending.isEmpty()) {
            String current = value.getValue();
            int cur = getMenu().pendingInsertCursor;
            if (cur >= 0 && cur <= current.length()) {
                value.setValue(current.substring(0, cur) + pending + current.substring(cur));
            } else {
                value.setValue(current + pending);
            }
            getMenu().pendingInsert = "";
            getMenu().pendingInsertCursor = -1;
        }
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (value.isMouseOver(x, y)) return value.mouseScrolled(x, y, scrollX, scrollY);
        return super.mouseScrolled(x, y, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        value.keyPressed(key, sc, mod);
        if (key == 256) { onClose(); return true; }
        return true;
    }

    private void save() {
        getMenu().syncValue(value.getValue().replace("\n", "&nl"));
    }

    private void changeMode(boolean b)   { getMenu().changeMode(b);   mode.setState(b); }
    private void changeCenter(boolean b) { getMenu().changeCenter(b); center.setState(b); }
    private void changeMargin(boolean b) { getMenu().changeMargin(b); margin.setState(b); }
}
