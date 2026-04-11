package net.oktawia.crazyae2addons.client.screens;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.ToggleButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.ConfirmScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.client.misc.CrazyLanguages;
import net.oktawia.crazyae2addons.client.misc.IconButton;
import net.oktawia.crazyae2addons.client.misc.LDLibCodeEditorAdapter;
import net.oktawia.crazyae2addons.client.misc.LDLibColorSelectorAdapter;
import net.oktawia.crazyae2addons.client.renderer.display.DisplayGuiRenderer;
import net.oktawia.crazyae2addons.client.renderer.display.DisplayRendererCommon;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DisplayScreen<C extends DisplayMenu> extends AEBaseScreen<C> {

    private static final Gson GSON = new Gson();
    private static final Type TOKENS_TYPE = new TypeToken<Map<String, String>>() {}.getType();

    private static final int PREVIEW_PREFERRED_W = 116;
    private static final int PREVIEW_PREFERRED_H = 152;
    private static final int PREVIEW_GAP = 8;
    private static final int PREVIEW_MIN_X = 6;

    private static final Pattern BG_COLOR_TOKEN = Pattern.compile("(?i)&b([0-9a-f]{6})");

    private final LDLibCodeEditorAdapter value;
    private final LDLibColorSelectorAdapter backgroundColor;
    private final LDLibColorSelectorAdapter selectedTextColor;
    private final ToggleButton mode;
    private final ToggleButton center;
    private final ToggleButton margin;

    private final Map<String, byte[]> previewImageData = new HashMap<>();

    private boolean initialized = false;
    private boolean modeState = true;
    private boolean centerState = false;
    private boolean marginState = false;

    private boolean allowCloseWithoutPrompt = false;
    private String lastSavedSerializedValue = "";

    public DisplayScreen(C menu, Inventory playerInventory, Component title, ScreenStyle style) {
        super(menu, playerInventory, title, style);

        value = new LDLibCodeEditorAdapter(
                15, 15, 205, 135,
                Component.translatable(LangDefs.INSERT.getTranslationKey())
        );
        value.setLanguage(CrazyLanguages.MARKDOWN);
        value.getEditor().setStyleManager(CrazyLanguages.MARKDOWN_STYLE);

        backgroundColor = new LDLibColorSelectorAdapter(
                0, 0, 16, 16,
                Component.translatable(LangDefs.BACKGROUND_COLOR.getTranslationKey())
        );
        backgroundColor.setTooltip(Tooltip.create(Component.translatable(LangDefs.BACKGROUND_COLOR.getTranslationKey())));
        backgroundColor.setOnColorCommitted(this::applyBackgroundColor);

        selectedTextColor = new LDLibColorSelectorAdapter(
                0, 0, 16, 16,
                Component.translatable(LangDefs.CHANGE_SELECTED_TEXT_COLOR.getTranslationKey())
        );
        selectedTextColor.setTooltip(Tooltip.create(Component.translatable(LangDefs.CHANGE_SELECTED_TEXT_COLOR.getTranslationKey())));
        selectedTextColor.setOnColorCommitted(this::applySelectedTextColor);

        backgroundColor.setOnOpen(() -> selectedTextColor.closePopup(true));
        selectedTextColor.setOnOpen(() -> {
            backgroundColor.closePopup(true);
            value.rememberSelectionForColorApply();
        });

        var imagesBtn = new IconButton(Icon.HELP, btn -> {
            save();
            getMenu().openImages();
        });
        imagesBtn.setTooltip(Tooltip.create(
                Component.translatable(LangDefs.IMAGES.getTranslationKey())
        ));
        widgets.add("images", imagesBtn);

        var confirm = new IconButton(Icon.ENTER, btn -> save());
        confirm.setTooltip(Tooltip.create(Component.translatable(LangDefs.SUBMIT.getTranslationKey())));

        var insertToken = new IconButton(Icon.HELP, btn -> {
            save();
            getMenu().openInsert(value.getCursorPos());
        });
        insertToken.setTooltip(Tooltip.create(Component.translatable(LangDefs.INSERT_TOKEN.getTranslationKey())));

        mode = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMode);
        center = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeCenter);
        margin = new ToggleButton(Icon.VALID, Icon.INVALID, this::changeMargin);

        mode.setTooltip(Tooltip.create(Component.translatable(LangDefs.JOIN_DISPLAYS.getTranslationKey())));
        center.setTooltip(Tooltip.create(Component.translatable(LangDefs.CENTER_TEXT.getTranslationKey())));
        margin.setTooltip(Tooltip.create(Component.translatable(LangDefs.ADD_MARGIN.getTranslationKey())));

        widgets.add("value", value);
        widgets.add("confirm", confirm);
        widgets.add("insertToken", insertToken);
        widgets.add("mode", mode);
        widgets.add("center", center);
        widgets.add("margin", margin);
        widgets.add("backgroundColor", backgroundColor);
        widgets.add("selectedTextColor", selectedTextColor);
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        value.tick();
        backgroundColor.tick();
        selectedTextColor.tick();

        if (!initialized) {
            String loaded = getMenu().displayValue == null ? "" : getMenu().displayValue;
            value.setValue(loaded.replace("&nl", "\n"));
            lastSavedSerializedValue = loaded;

            modeState = getMenu().mode;
            centerState = getMenu().centerText;
            marginState = getMenu().margin;

            mode.setState(modeState);
            center.setState(centerState);
            margin.setState(marginState);

            backgroundColor.setColor(extractBackgroundColor(value.getValue(), 0xFF202020));
            selectedTextColor.setColor(0xFFFFFFFF);

            previewImageData.clear();
            getMenu().requestImages();

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
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderPreview(guiGraphics);

        backgroundColor.renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
        selectedTextColor.renderOverlay(guiGraphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (selectedTextColor.isMouseOver(mouseX, mouseY)) {
            value.rememberSelectionForColorApply();
        }

        if (backgroundColor.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (selectedTextColor.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (backgroundColor.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        if (selectedTextColor.mouseReleased(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (backgroundColor.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (selectedTextColor.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        if (value.mouseDragged(mouseX, mouseY, button, dragX, dragY)) {
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (backgroundColor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (selectedTextColor.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        if (value.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (backgroundColor.keyPressed(key, sc, mod)) {
            return true;
        }
        if (selectedTextColor.keyPressed(key, sc, mod)) {
            return true;
        }

        if (key == GLFW.GLFW_KEY_ESCAPE) {
            if (backgroundColor.isPopupOpen()) {
                backgroundColor.closePopup(false);
                return true;
            }

            if (selectedTextColor.isPopupOpen()) {
                selectedTextColor.closePopup(false);
                return true;
            }

            if (value.isFocused()) {
                value.setFocused(false);
                setFocused(null);
                return true;
            }

            requestCloseWithConfirmation();
            return true;
        }

        if (value.isFocused() && minecraft != null) {
            if (minecraft.options.keyInventory.matches(key, sc)
                    || minecraft.options.keyDrop.matches(key, sc)
                    || minecraft.options.keySocialInteractions.matches(key, sc)
                    || minecraft.options.keySwapOffhand.matches(key, sc)
                    || (key >= GLFW.GLFW_KEY_1 && key <= GLFW.GLFW_KEY_9
                    && minecraft.options.keyHotbarSlots[key - GLFW.GLFW_KEY_1].matches(key, sc))) {
                return true;
            }

            if (value.keyPressed(key, sc, mod)) {
                return true;
            }
        }

        return super.keyPressed(key, sc, mod);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (backgroundColor.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (selectedTextColor.charTyped(codePoint, modifiers)) {
            return true;
        }
        if (value.isFocused() && value.charTyped(codePoint, modifiers)) {
            return true;
        }
        return super.charTyped(codePoint, modifiers);
    }

    public void applyPreviewImageFromServer(String imageId, byte[] pngBytes) {
        if (imageId == null || imageId.isBlank()) {
            return;
        }

        if (pngBytes == null || pngBytes.length == 0) {
            previewImageData.remove(imageId);
            return;
        }

        previewImageData.put(imageId, pngBytes);
    }

    private void renderPreview(GuiGraphics gui) {
        int previewW = DisplayGuiRenderer.computePreviewWidth(leftPos, PREVIEW_PREFERRED_W, PREVIEW_GAP, PREVIEW_MIN_X);
        if (previewW < 52) {
            return;
        }

        int previewH = Math.min(PREVIEW_PREFERRED_H, Math.max(72, imageHeight - 20));
        int previewX = DisplayGuiRenderer.clampPreviewX(leftPos, previewW, PREVIEW_GAP, PREVIEW_MIN_X);
        int previewY = DisplayGuiRenderer.clampPreviewY(topPos, imageHeight, previewH);

        Pair<Integer, Integer> dims = resolvePreviewGridSize();
        Map<String, String> tokens = decodePreviewTokens();
        List<DisplayImageEntry> images = getMenu().getPreviewImages();

        DisplayRendererCommon.PreparedDisplay prepared = DisplayGuiRenderer.preparePreview(
                Minecraft.getInstance().font,
                value.getValue().replace("\n", "&nl"),
                tokens,
                centerState,
                marginState,
                dims.getFirst(),
                dims.getSecond(),
                images,
                previewImageData
        );

        Component label = Component.literal("Preview " + dims.getFirst() + "x" + dims.getSecond());
        gui.drawString(Minecraft.getInstance().font, label, previewX, Math.max(2, previewY - 10), 0xE0E0E0, false);

        DisplayGuiRenderer.renderPreview(gui, previewX, previewY, previewW, previewH, prepared);
    }

    private Pair<Integer, Integer> resolvePreviewGridSize() {
        if (!modeState) {
            return Pair.of(1, 1);
        }

        return Pair.of(
                Math.max(1, getMenu().previewGridWidth),
                Math.max(1, getMenu().previewGridHeight)
        );
    }

    private Map<String, String> decodePreviewTokens() {
        String json = getMenu().previewTokensJson;
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }

        try {
            Map<String, String> decoded = GSON.fromJson(json, TOKENS_TYPE);
            return decoded != null ? decoded : Collections.emptyMap();
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private void applyBackgroundColor(int argb) {
        String token = String.format(Locale.ROOT, "&b%06X", argb & 0xFFFFFF);
        String text = value.getValue();

        Matcher m = BG_COLOR_TOKEN.matcher(text);
        if (m.find()) {
            value.setValue(m.replaceFirst(token));
        } else {
            value.setValue(text.isEmpty() ? token + " " : token + " " + text);
        }
    }

    private void applySelectedTextColor(int argb) {
        value.applySelectedTextColor(argb);
    }

    private static int extractBackgroundColor(String text, int fallback) {
        Matcher m = BG_COLOR_TOKEN.matcher(text == null ? "" : text);
        if (m.find()) {
            try {
                return 0xFF000000 | Integer.parseInt(m.group(1), 16);
            } catch (Exception ignored) {
            }
        }
        return fallback;
    }

    private String currentSerializedValue() {
        return value.getValue().replace("\n", "&nl");
    }

    private boolean hasUnsavedTextChanges() {
        return !currentSerializedValue().equals(lastSavedSerializedValue);
    }

    private void requestCloseWithConfirmation() {
        if (!hasUnsavedTextChanges()) {
            allowCloseWithoutPrompt = true;
            onClose();
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        mc.setScreen(new ConfirmScreen(
                confirmed -> {
                    if (confirmed) {
                        allowCloseWithoutPrompt = true;
                        mc.setScreen(null);
                    } else {
                        mc.setScreen(this);
                    }
                },
                Component.translatable(LangDefs.UNSAVED_CHANGES_TITLE.getTranslationKey()),
                Component.translatable(LangDefs.UNSAVED_CHANGES_TEXT.getTranslationKey())
        ));
    }

    private void save() {
        getMenu().syncValue(currentSerializedValue());
        lastSavedSerializedValue = currentSerializedValue();
    }

    private void changeMode(boolean enabled) {
        modeState = enabled;
        getMenu().changeMode(enabled);
        mode.setState(enabled);
    }

    private void changeCenter(boolean enabled) {
        centerState = enabled;
        getMenu().changeCenter(enabled);
        center.setState(enabled);
    }

    private void changeMargin(boolean enabled) {
        marginState = enabled;
        getMenu().changeMargin(enabled);
        margin.setState(enabled);
    }

    @Override
    public void onClose() {
        if (!allowCloseWithoutPrompt) {
            requestCloseWithConfirmation();
            return;
        }

        backgroundColor.removed();
        selectedTextColor.removed();
        value.removed();
        super.onClose();
    }
}