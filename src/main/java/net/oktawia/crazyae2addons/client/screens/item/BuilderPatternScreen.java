package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.CrazyLanguages;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternMenu;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.misc.LDLibCodeEditorAdapter;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import net.oktawia.crazyae2addons.network.packets.SendLongStringToClientPacket;
import org.lwjgl.glfw.GLFW;

public class BuilderPatternScreen<C extends BuilderPatternMenu> extends AEBaseScreen<C> {

    private final LDLibCodeEditorAdapter textEditor;
    private final AETextField renameField;
    private IconButton confirmBtn;
    private boolean initialized = false;

    public BuilderPatternScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        textEditor = new LDLibCodeEditorAdapter(0, 0, 202, 135, Component.empty());
        textEditor.setLanguage(CrazyLanguages.PROGRAM);
        textEditor.getEditor().setStyleManager(CrazyLanguages.PROGRAM_STYLE);

        widgets.add("data", textEditor);

        renameField = new AETextField(style, Minecraft.getInstance().font, 0, 0, 120, 12);
        renameField.setBordered(false);
        widgets.add("rename", renameField);

        confirmBtn = new IconButton(Icon.COPY_MODE_ON, btn -> save());
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

        var visualAssistBtn = new IconButton(Icon.CRAFT_HAMMER, btn -> getMenu().openSubMenu());
        visualAssistBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.VISUAL_ASSISTANCE.getTranslationKey())));
        widgets.add("visualAssist", visualAssistBtn);

        SendLongStringToClientPacket.clientHandler = textEditor::setValue;
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();
        textEditor.tick();
        if (!initialized) {
            renameField.setValue(getMenu().name != null ? getMenu().name : "");
            getMenu().requestData();
            initialized = true;
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
            confirmBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.PROGRAM_SAVED.getTranslationKey())));
        } else {
            confirmBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.SYNTAX_ERROR.getTranslationKey()).append(" ").append(result.error)));
        }
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
