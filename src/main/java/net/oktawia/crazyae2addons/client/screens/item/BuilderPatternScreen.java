package net.oktawia.crazyae2addons.client.screens.item;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AETextField;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.MultilineTextFieldWidget;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.item.BuilderPatternMenu;
import net.oktawia.crazyae2addons.util.ProgramExpander;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class BuilderPatternScreen<C extends BuilderPatternMenu> extends AEBaseScreen<C> {

    private final MultilineTextFieldWidget textEditor;
    private final AETextField renameField;
    private final IconButton confirmBtn;

    private boolean requestedInitialProgram = false;

    public BuilderPatternScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        textEditor = new MultilineTextFieldWidget(
                Minecraft.getInstance().font,
                0, 0,
                202, 135,
                Component.empty()
        );
        textEditor.setDefaultTextColor(0xFFFFFFFF);
        textEditor.setHighlightRules(List.of(
                new MultilineTextFieldWidget.HighlightRule("\\|\\|", 0xFF64B5F6),
                new MultilineTextFieldWidget.HighlightRule("P\\(\\d+\\)(?:==\\(\\d+\\)|!=\\(\\d+\\))?", 0xFF81D4FA),
                new MultilineTextFieldWidget.HighlightRule("X(?:==\\(\\d+\\)|!=\\(\\d+\\))?", 0xFF81D4FA),
                new MultilineTextFieldWidget.HighlightRule("Z\\(\\d+\\)", 0xFFFFB74D),
                new MultilineTextFieldWidget.HighlightRule("\\[[A-Za-z_][A-Za-z0-9_]*\\]", 0xFFBA68C8),
                new MultilineTextFieldWidget.HighlightRule("[a-z0-9_.-]+:[a-z0-9_./-]+", 0xFF81C784),
                new MultilineTextFieldWidget.HighlightRule("[HFBLRUD]", 0xFF4FC3F7),
                new MultilineTextFieldWidget.HighlightRule("\\d+(?=\\{)", 0xFFFFF176),
                new MultilineTextFieldWidget.HighlightRule("==|!=", 0xFFFFB74D),
                new MultilineTextFieldWidget.HighlightRule("[\\[\\]{}(),]", 0xFFB0BEC5),
                new MultilineTextFieldWidget.HighlightRule("[a-z_][a-z0-9_]*(?==)", 0xFFA5D6A7),
                new MultilineTextFieldWidget.HighlightRule("\\d+", 0xFFFFF176)
        ));
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
    }

    @Override
    protected void init() {
        super.init();

        if (!requestedInitialProgram) {
            renameField.setValue(getMenu().name != null ? getMenu().name : "");
            getMenu().requestData();
            requestedInitialProgram = true;
        }
    }

    public void setProgram(String program) {
        textEditor.setValue(program == null ? "" : program);
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

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (textEditor.mouseClicked(mouseX, mouseY, button)) {
            setFocused(textEditor);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (textEditor.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (textEditor.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scroll) {
        if (textEditor.mouseScrolled(mouseX, mouseY, scroll)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scroll);
    }

    private void save() {
        String text = textEditor.getValue();
        ProgramExpander.Result result = ProgramExpander.expand(text);

        if (result.success) {
            getMenu().updateData(text);

            String newName = renameField.getValue();
            if (!newName.isEmpty()) {
                getMenu().rename(newName);
            }

            confirmBtn.setTooltip(Tooltip.create(
                    Component.translatable(LangDefs.PROGRAM_SAVED.getTranslationKey())
            ));
        } else {
            confirmBtn.setTooltip(Tooltip.create(
                    Component.translatable(LangDefs.SYNTAX_ERROR.getTranslationKey())
                            .append(" ")
                            .append(result.error)
            ));
        }
    }
}