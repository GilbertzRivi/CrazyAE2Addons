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
import net.oktawia.crazyae2addons.menus.BuilderPatternMenu;
import net.oktawia.crazyae2addons.misc.IconButton;
import net.oktawia.crazyae2addons.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import net.oktawia.crazyae2addons.misc.SyntaxHighlighter;
import org.lwjgl.glfw.GLFW;

public class BuilderPatternScreen<C extends BuilderPatternMenu> extends AEBaseScreen<C> {
    private IconButton confirm;
    private MultilineTextFieldWidget input;
    private Scrollbar scrollbar;
    private AETextField delay;
    private final AETextField rename;
    private int lastScroll = -1;
    public boolean initialized;
    private String program = "";
    private IconButton flipHBtn;
    private IconButton flipVBtn;
    private IconButton rotateBtn;

    public BuilderPatternScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);
        delay = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        delay.setBordered(false);
        delay.setValue(String.valueOf(getMenu().delay));
        delay.setMaxLength(5);
        delay.setTooltip(Tooltip.create(Component.literal("Amount of ticks to wait after each action")));
        setupGui();
        this.widgets.add("confirm", confirm);
        this.widgets.add("data", input);
        this.widgets.add("scroll", scrollbar);
        this.widgets.add("delay", delay);
        this.rename = new AETextField(style, Minecraft.getInstance().font, 0, 0, 0, 0);
        rename.setBordered(false);
        rename.setPlaceholder(Component.literal("Rename"));
        rename.setValue(getMenu().name);
        rename.setResponder(x -> {
            getMenu().rename(x);
        });
        this.widgets.add("rename", rename);
        initialized = false;
        this.widgets.add("flipH", flipHBtn);
        this.widgets.add("flipV", flipVBtn);
        this.widgets.add("rotate", rotateBtn);
        getMenu().requestData();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        if (!initialized) {
            delay.setValue(String.valueOf(getMenu().delay));
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

    private Tooltip wrapErrorText(String text) {
        return Tooltip.create(Component.literal(text));
    }

    private void setupGui() {
        confirm = new IconButton(Icon.ENTER, (btn) -> {
            String text = input.getValue();
            ProgramExpander.Result result = ProgramExpander.expand(text);
            if (result.success) {
                confirm.setTooltip(Tooltip.create(Component.literal("Confirm")));
            } else {
                confirm.setTooltip(wrapErrorText("Syntax error: " + result.error));
            }
            getMenu().updateData(text);
            try{
                getMenu().updateDelay(Integer.parseInt(delay.getValue()));
            } catch (Exception ignored) {
                getMenu().updateDelay(0);
            }
        });
        confirm.setTooltip(Tooltip.create(Component.literal("Confirm")));

        input = new MultilineTextFieldWidget(
                font, 0, 0, 202, 135,
                Component.literal("Input program")
        );
        input.setTokenizer(SyntaxHighlighter::tokenize);

        scrollbar = new Scrollbar();
        scrollbar.setSize(12, 100);
        scrollbar.setRange(0, 100, 4);

        flipHBtn = new IconButton(Icon.ARROW_RIGHT, (btn) -> {
            this.input.setValue("");
            getMenu().flipH();
        });
        flipHBtn.setTooltip(Tooltip.create(Component.literal("Horizontal flip")));

        flipVBtn = new IconButton(Icon.ARROW_UP, (btn) -> {
            this.input.setValue("");
            getMenu().flipV();
        });
        flipVBtn.setTooltip(Tooltip.create(Component.literal("Vertical flip")));

        rotateBtn = new IconButton(Icon.SCHEDULING_DEFAULT, (btn) -> {
            this.input.setValue("");
            getMenu().rotateCW(1);
        });
        rotateBtn.setTooltip(Tooltip.create(Component.literal("Rotate by 90 deg")));
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
