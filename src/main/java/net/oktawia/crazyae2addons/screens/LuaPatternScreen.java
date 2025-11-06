package net.oktawia.crazyae2addons.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.menus.LuaPatternMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.LuaSyntaxHighlighter;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.lwjgl.glfw.GLFW;

public class LuaPatternScreen<C extends LuaPatternMenu> extends AEBaseScreen<C> {
    private IconButton confirm;
    private MultilineTextFieldWidget input;
    private Scrollbar scrollbar;
    private final AETextField rename;
    private int lastScroll = -1;
    public boolean initialized;
    private String program = "";

    public LuaPatternScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        setupGui();
        this.widgets.add("confirm", confirm);
        this.widgets.add("data", input);
        this.widgets.add("scroll", scrollbar);

        this.rename = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        rename.setBordered(false);
        rename.setPlaceholder(Component.literal("Rename"));
        rename.setValue(getMenu().name);
        rename.setResponder(getMenu()::rename);
        this.widgets.add("rename", rename);

        initialized = false;
        getMenu().requestData();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!initialized) {
            rename.setValue(getMenu().name);
            initialized = true;
        }
    }

    @Override
    public void containerTick() {
        super.containerTick();

        int maxScroll = (int) input.getMaxScroll();
        scrollbar.setRange(0, maxScroll, 4);

        int currentScrollbarPos = scrollbar.getCurrentScroll();
        if (currentScrollbarPos != lastScroll) {
            lastScroll = currentScrollbarPos;
            input.setScrollAmount(currentScrollbarPos);
        } else {
            int currentInputScroll = (int) input.getScrollAmount();
            if (currentInputScroll != currentScrollbarPos) {
                scrollbar.setCurrentScroll(currentInputScroll);
                lastScroll = currentInputScroll;
            }
        }
    }

    private void setupGui() {
        confirm = new IconButton(Icon.ENTER, (btn) -> {
            String text = input.getValue();
            try {
                Globals g = JsePlatform.standardGlobals();
                LuaValue chunk = g.load(text, "item_script");

                confirm.setTooltip(Tooltip.create(Component.literal("OK: compiled successfully")));
                getMenu().updateData(text);

            } catch (LuaError le) {
                String msg = shorten(le.getMessage() == null ? le.toString() : le.getMessage(), 220);
                confirm.setTooltip(Tooltip.create(Component.literal("Lua error: " + msg)));
                getMenu().updateData(text);
            } catch (Throwable t) {
                String msg = shorten(t.getMessage() == null ? t.toString() : t.getMessage(), 220);
                confirm.setTooltip(Tooltip.create(Component.literal("Unexpected error: " + msg)));
                getMenu().updateData(text);
            }
        });
        confirm.setTooltip(Tooltip.create(Component.literal("Confirm")));

        input = new MultilineTextFieldWidget(font, 0, 0, 202, 135, Component.literal("Input Lua"));
        input.setTokenizer(LuaSyntaxHighlighter::tokenize);

        scrollbar = new Scrollbar();
        scrollbar.setSize(12, 100);
        scrollbar.setRange(0, 100, 4);
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? (s.substring(0, max - 1) + "…") : s;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (input.isFocused() && minecraft != null) {
            if (minecraft.options.keyInventory.matches(keyCode, scanCode) ||
                    minecraft.options.keyDrop.matches(keyCode, scanCode) ||
                    minecraft.options.keySocialInteractions.matches(keyCode, scanCode) ||
                    minecraft.options.keySwapOffhand.matches(keyCode, scanCode) ||
                    (keyCode >= GLFW.GLFW_KEY_1 && keyCode <= GLFW.GLFW_KEY_9 &&
                            minecraft.options.keyHotbarSlots[keyCode - GLFW.GLFW_KEY_1].matches(keyCode, scanCode))
            ) {
                return true;
            }

            if (input.keyPressed(keyCode, scanCode, modifiers)) {
                return true;
            }
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (input.isFocused() && input.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    public void setProgram(String data) {
        if ("__RESET__".equals(data)) {
            program = "";
            input.setValue("");
            return;
        }
        program += data;
        input.setValue(program);
    }
}