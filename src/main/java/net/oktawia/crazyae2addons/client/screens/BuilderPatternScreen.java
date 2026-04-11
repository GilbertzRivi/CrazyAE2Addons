package net.oktawia.crazyae2addons.client.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.CrazyLanguages;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.BuilderPatternMenu;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.misc.LDLibCodeEditorAdapter;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import net.oktawia.crazyae2addons.network.packets.SendLongStringToClientPacket;
import org.lwjgl.glfw.GLFW;

public class BuilderPatternScreen<C extends BuilderPatternMenu> extends AEBaseScreen<C> {

    private final LDLibCodeEditorAdapter textEditor;
    private final AETextField renameField;
    private final AETextField delayField;
    private IconButton confirmBtn;
    private boolean initialized = false;
    private String statusText = "";
    private int statusColor = 0xFF00CC00;
    private int statusTimer = 0;

    public BuilderPatternScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        textEditor = new LDLibCodeEditorAdapter(0, 0, 202, 135, Component.empty());
        textEditor.setLanguage(CrazyLanguages.PROGRAM);
        textEditor.getEditor().setStyleManager(CrazyLanguages.PROGRAM_STYLE);

        widgets.add("data", textEditor);

        renameField = new AETextField(style, Minecraft.getInstance().font, 0, 0, 120, 12);
        renameField.setBordered(false);
        widgets.add("rename", renameField);

        delayField = new AETextField(style, Minecraft.getInstance().font, 0, 0, 25, 12);
        delayField.setBordered(false);
        widgets.add("delay", delayField);

        confirmBtn = new IconButton(Icon.VALID, btn -> save());
        confirmBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.CONFIRM.getTranslationKey())));
        widgets.add("confirm", confirmBtn);

        var flipHBtn = new IconButton(Icon.ARROW_LEFT, btn -> {
            textEditor.setValue("");
            getMenu().flipH();
        });
        flipHBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.FLIP_HORIZONTAL.getTranslationKey())));
        widgets.add("flipH", flipHBtn);

        var flipVBtn = new IconButton(Icon.ARROW_DOWN, btn -> {
            textEditor.setValue("");
            getMenu().flipV();
        });
        flipVBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.FLIP_VERTICAL.getTranslationKey())));
        widgets.add("flipV", flipVBtn);

        var rotateBtn = new IconButton(Icon.ARROW_RIGHT, btn -> {
            textEditor.setValue("");
            getMenu().rotateCW(1);
        });
        rotateBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.ROTATE_CW.getTranslationKey())));
        widgets.add("rotate", rotateBtn);

        var visualAssistBtn = new IconButton(Icon.HELP, btn -> getMenu().openSubMenu());
        visualAssistBtn.setTooltip(Tooltip.create(Component.literal("Visual Assistance - fill region helper")));
        widgets.add("visualAssist", visualAssistBtn);

        SendLongStringToClientPacket.clientHandler = data -> textEditor.setValue(data);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        textEditor.tick();
        if (statusTimer > 0) statusTimer--;
        if (!initialized) {
            renameField.setValue(getMenu().name != null ? getMenu().name : "");
            delayField.setValue(String.valueOf(getMenu().delay != null ? getMenu().delay : 0));
            getMenu().requestData();
            initialized = true;
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partial) {
        super.render(g, mouseX, mouseY, partial);
        if (statusTimer > 0 && !statusText.isEmpty()) {
            g.drawString(font, statusText, leftPos + 130, topPos + 8, statusColor, false);
        }
    }

    @Override
    public void onClose() {
        textEditor.removed();
        SendLongStringToClientPacket.clientHandler = null;
        super.onClose();
    }


    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (textEditor.isFocused()) {
                textEditor.setFocused(false);
                setFocused(null);
                return true;
            }
            onClose();
            return true;
        }

        if (textEditor.isFocused() && minecraft != null) {
            if (minecraft.options.keyInventory.matches(key, sc)
                    || minecraft.options.keyDrop.matches(key, sc)
                    || minecraft.options.keySocialInteractions.matches(key, sc)
                    || minecraft.options.keySwapOffhand.matches(key, sc)
                    || (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9
                    && minecraft.options.keyHotbarSlots[key - GLFW.GLFW_KEY_1].matches(key, sc))) {
                return true;
            }

            if (textEditor.keyPressed(key, sc, mod)) {
                return true;
            }
        }

        return super.keyPressed(key, sc, mod);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (textEditor.isFocused() && textEditor.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    private void save() {
        String text = textEditor.getValue();
        ProgramExpander.Result result = ProgramExpander.expand(text);
        if (result.success) {
            getMenu().updateData(text);
            String newName = renameField.getValue();
            if (!newName.isEmpty()) getMenu().rename(newName);
            String delayStr = delayField.getValue();
            if (!delayStr.isEmpty()) {
                try {
                    getMenu().updateDelay(Integer.parseInt(delayStr));
                } catch (NumberFormatException ignored) {}
            }
            statusText = Component.translatable(LangDefs.PROGRAM_SAVED.getTranslationKey()).getString();
            statusColor = 0xFF00CC00;
            confirmBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.PROGRAM_SAVED.getTranslationKey())));
        } else {
            statusText = Component.translatable(LangDefs.PROGRAM_INVALID.getTranslationKey()).getString();
            statusColor = 0xFFCC0000;
            confirmBtn.setTooltip(Tooltip.create(Component.literal("Syntax error: " + result.error)));
        }
        statusTimer = 250;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (textEditor.isMouseOver(mouseX, mouseY)) {
            if (textEditor.mouseClicked(mouseX, mouseY, button)) {
                setFocused(textEditor);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (textEditor.isFocused()) {
            if (textEditor.mouseReleased(mouseX, mouseY, button)) {
                return true;
            }
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (textEditor.isFocused()) {
            if (textEditor.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
                return true;
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (textEditor.isMouseOver(mouseX, mouseY)) {
            if (textEditor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
}
