package net.oktawia.crazyae2addons.client.screens.part;

import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.Icon;
import appeng.client.gui.implementations.AESubScreen;
import appeng.client.gui.style.ScreenStyle;
import appeng.client.gui.widgets.AE2Button;
import appeng.client.gui.widgets.AETextField;
import appeng.client.gui.widgets.Scrollbar;
import appeng.client.gui.widgets.TabButton;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.client.misc.DisplayImageUploadClient;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.menus.part.DisplayImagesSubMenu;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.List;

public class DisplayImagesSubScreen extends AEBaseScreen<DisplayImagesSubMenu> {

    private static final int MIN_PERCENT = 0;
    private static final int MAX_PERCENT = 100;
    private static final int MIN_SCALE_PERCENT = 1;

    private final AETextField xField;
    private final AETextField yField;
    private final AETextField scaleField;

    private final Scrollbar xSlider;
    private final Scrollbar ySlider;
    private final Scrollbar scaleSlider;

    private final ImageListWidget imageListWidget;

    private String lastSelectedId = null;

    private Component statusText = Component.empty();
    private int statusColor = 0xFFAAAAAA;
    private int statusTimer = 0;

    private DynamicTexture previewTexture;
    private ResourceLocation previewTextureLocation;
    private int previewTextureWidth = 0;
    private int previewTextureHeight = 0;
    private String previewImageId = "";

    private String lastAutoAppliedImageId = null;
    private int lastAutoAppliedX = Integer.MIN_VALUE;
    private int lastAutoAppliedY = Integer.MIN_VALUE;
    private int lastAutoAppliedScale = Integer.MIN_VALUE;

    public DisplayImagesSubScreen(DisplayImagesSubMenu menu, Inventory inv, Component title, ScreenStyle style) {
        super(menu, inv, title, style);

        var font = Minecraft.getInstance().font;

        var backBtn = new TabButton(
                Icon.BACK,
                Component.translatable(LangDefs.BACK.getTranslationKey()),
                btn -> AESubScreen.goBack()
        );
        widgets.add("back", backBtn);

        var pickFileBtn = new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.PICK_FILE.getTranslationKey()),
                btn -> onPickFile()
        );
        pickFileBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.PICK_FILE_TOOLTIP.getTranslationKey())));
        widgets.add("pickFile", pickFileBtn);

        var removeBtn = new AE2Button(
                0, 0, 0, 0,
                Component.translatable(LangDefs.REMOVE.getTranslationKey()),
                btn -> {
                    DisplayImageEntry selected = getMenu().getSelectedImage();
                    if (selected != null) {
                        getMenu().removeImage(selected.id());
                        syncControlsFromSelection();
                    }
                }
        );
        removeBtn.setTooltip(Tooltip.create(Component.translatable(LangDefs.REMOVE_IMAGE_TOOLTIP.getTranslationKey())));
        widgets.add("remove", removeBtn);

        xField = new AETextField(style, font, 0, 0, 0, 0);
        yField = new AETextField(style, font, 0, 0, 0, 0);
        scaleField = new AETextField(style, font, 0, 0, 0, 0);

        xField.setMaxLength(3);
        yField.setMaxLength(3);
        scaleField.setMaxLength(3);

        xField.setBordered(false);
        yField.setBordered(false);
        scaleField.setBordered(false);

        xField.setHint(Component.translatable(LangDefs.X_PERCENT.getTranslationKey()));
        yField.setHint(Component.translatable(LangDefs.Y_PERCENT.getTranslationKey()));
        scaleField.setHint(Component.translatable(LangDefs.SCALE_PERCENT.getTranslationKey()));

        xField.setTooltip(Tooltip.create(Component.translatable(LangDefs.X_PERCENT_TOOLTIP.getTranslationKey())));
        yField.setTooltip(Tooltip.create(Component.translatable(LangDefs.Y_PERCENT_TOOLTIP.getTranslationKey())));
        scaleField.setTooltip(Tooltip.create(Component.translatable(LangDefs.SCALE_PERCENT_TOOLTIP.getTranslationKey())));

        widgets.add("x", xField);
        widgets.add("y", yField);
        widgets.add("scale", scaleField);

        xSlider = new Scrollbar();
        ySlider = new Scrollbar();
        scaleSlider = new Scrollbar();

        xSlider.setRange(MIN_PERCENT, MAX_PERCENT, 1);
        xSlider.setCurrentScroll(0);

        ySlider.setRange(MIN_PERCENT, MAX_PERCENT, 1);
        ySlider.setCurrentScroll(0);

        scaleSlider.setRange(MIN_SCALE_PERCENT, MAX_PERCENT, 1);
        scaleSlider.setCurrentScroll(100);

        widgets.add("xSlider", xSlider);
        widgets.add("ySlider", ySlider);
        widgets.add("scaleSlider", scaleSlider);

        imageListWidget = new ImageListWidget();
        imageListWidget.setTooltip(Tooltip.create(Component.translatable(LangDefs.IMAGE_LIST_TOOLTIP.getTranslationKey())));
        widgets.add("imageList", imageListWidget);
    }

    @Override
    protected void init() {
        super.init();
        addRenderableOnly(new StatusWidget());
        syncControlsFromSelection();
        getMenu().requestPreview();
    }

    @Override
    protected void updateBeforeRender() {
        super.updateBeforeRender();

        if (statusTimer > 0) {
            statusTimer--;
        }

        DisplayImageEntry selected = getMenu().getSelectedImage();
        String selectedId = selected == null ? null : selected.id();

        if ((selectedId == null && lastSelectedId != null)
                || (selectedId != null && !selectedId.equals(lastSelectedId))) {
            syncControlsFromSelection();
            if (selectedId == null || !selectedId.equals(previewImageId)) {
                getMenu().requestPreview();
            }
        }

        syncPair(xField, xSlider, MIN_PERCENT, MAX_PERCENT);
        syncPair(yField, ySlider, MIN_PERCENT, MAX_PERCENT);
        syncPair(scaleField, scaleSlider, MIN_SCALE_PERCENT, MAX_PERCENT);

        autoApplyIfChanged();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (imageListWidget != null && imageListWidget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int key, int sc, int mod) {
        if (key == 256) {
            AESubScreen.goBack();
            return true;
        }

        if (Screen.isPaste(key) && !isEditingBoundsField()) {
            onPasteShortcut();
            return true;
        }

        if (xField != null && xField.keyPressed(key, sc, mod)) return true;
        if (yField != null && yField.keyPressed(key, sc, mod)) return true;
        if (scaleField != null && scaleField.keyPressed(key, sc, mod)) return true;

        return super.keyPressed(key, sc, mod);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (xField != null && xField.charTyped(codePoint, modifiers)) return true;
        if (yField != null && yField.charTyped(codePoint, modifiers)) return true;
        if (scaleField != null && scaleField.charTyped(codePoint, modifiers)) return true;
        return super.charTyped(codePoint, modifiers);
    }

    @Override
    public void onFilesDrop(@NotNull List<Path> paths) {
        var result = DisplayImageUploadClient.uploadDroppedFiles(paths);
        setStatus(result.message(), result.color());
    }

    @Override
    public void removed() {
        clearPreviewTexture();
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    public void applyPreviewFromServer(String imageId, byte[] pngBytes) {
        clearPreviewTexture();

        previewImageId = imageId == null ? "" : imageId;

        if (pngBytes == null || pngBytes.length == 0) {
            return;
        }

        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(pngBytes));

            previewTextureWidth = image.getWidth();
            previewTextureHeight = image.getHeight();

            previewTexture = new DynamicTexture(image);
            previewTexture.upload();

            previewTextureLocation = Minecraft.getInstance().getTextureManager().register(
                    "crazyae2addons_display_preview_" + System.nanoTime(),
                    previewTexture
            );
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to load display image preview texture", e);
            clearPreviewTexture();
        }
    }

    private void syncPair(AETextField field, Scrollbar slider, int min, int max) {
        int sliderValue = Mth.clamp(slider.getCurrentScroll(), min, max);
        String fieldValue = field.getValue();

        if (field.isFocused()) {
            try {
                int typed = Integer.parseInt(fieldValue.trim());
                int clamped = Mth.clamp(typed, min, max);
                if (clamped != sliderValue) {
                    slider.setCurrentScroll(clamped);
                }
            } catch (Throwable e) {
                CrazyAddons.LOGGER.debug("invalid numeric input in display image field", e);
            }
        } else {
            String normalized = String.valueOf(sliderValue);
            if (!normalized.equals(fieldValue)) {
                field.setValue(normalized);
            }
        }
    }

    private void autoApplyIfChanged() {
        DisplayImageEntry selected = getMenu().getSelectedImage();
        if (selected == null) {
            lastAutoAppliedImageId = null;
            lastAutoAppliedX = Integer.MIN_VALUE;
            lastAutoAppliedY = Integer.MIN_VALUE;
            lastAutoAppliedScale = Integer.MIN_VALUE;
            return;
        }

        int x = getLivePercentX();
        int y = getLivePercentY();
        int scale = getLivePercentScale();

        boolean changed =
                !selected.id().equals(lastAutoAppliedImageId)
                        || x != lastAutoAppliedX
                        || y != lastAutoAppliedY
                        || scale != lastAutoAppliedScale;

        if (!changed) {
            return;
        }

        lastAutoAppliedImageId = selected.id();
        lastAutoAppliedX = x;
        lastAutoAppliedY = y;
        lastAutoAppliedScale = scale;

        getMenu().updateImage(selected.id(), x, y, scale, scale);
    }

    private boolean isEditingBoundsField() {
        return (xField != null && xField.isFocused())
                || (yField != null && yField.isFocused())
                || (scaleField != null && scaleField.isFocused());
    }

    private void onPickFile() {
        var result = DisplayImageUploadClient.pickAndUpload();
        setStatus(result.message(), result.color());

        if (result.success()) {
            getMenu().requestPreview();
        }
    }

    private void onPasteShortcut() {
        var result = DisplayImageUploadClient.pasteAndUpload();
        setStatus(result.message(), result.color());

        if (result.success()) {
            getMenu().requestPreview();
        }
    }

    private void syncControlsFromSelection() {
        DisplayImageEntry selected = getMenu().getSelectedImage();
        if (selected == null) {
            lastSelectedId = null;

            xField.setValue("");
            yField.setValue("");
            scaleField.setValue("");

            xSlider.setCurrentScroll(0);
            ySlider.setCurrentScroll(0);
            scaleSlider.setCurrentScroll(100);

            lastAutoAppliedImageId = null;
            lastAutoAppliedX = Integer.MIN_VALUE;
            lastAutoAppliedY = Integer.MIN_VALUE;
            lastAutoAppliedScale = Integer.MIN_VALUE;

            imageListWidget.clampScroll();
            return;
        }

        int x = Mth.clamp(selected.x(), MIN_PERCENT, MAX_PERCENT);
        int y = Mth.clamp(selected.y(), MIN_PERCENT, MAX_PERCENT);
        int scale = Mth.clamp(Math.min(selected.width(), selected.height()), MIN_SCALE_PERCENT, MAX_PERCENT);

        lastSelectedId = selected.id();

        xSlider.setCurrentScroll(x);
        ySlider.setCurrentScroll(y);
        scaleSlider.setCurrentScroll(scale);

        xField.setValue(String.valueOf(x));
        yField.setValue(String.valueOf(y));
        scaleField.setValue(String.valueOf(scale));

        lastAutoAppliedImageId = selected.id();
        lastAutoAppliedX = x;
        lastAutoAppliedY = y;
        lastAutoAppliedScale = scale;

        imageListWidget.ensureVisible(selected.id());
    }

    private int getLivePercentX() {
        try {
            return Mth.clamp(Integer.parseInt(xField.getValue().trim()), MIN_PERCENT, MAX_PERCENT);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("invalid x position in display image field", e);
            return Mth.clamp(xSlider.getCurrentScroll(), MIN_PERCENT, MAX_PERCENT);
        }
    }

    private int getLivePercentY() {
        try {
            return Mth.clamp(Integer.parseInt(yField.getValue().trim()), MIN_PERCENT, MAX_PERCENT);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("invalid y position in display image field", e);
            return Mth.clamp(ySlider.getCurrentScroll(), MIN_PERCENT, MAX_PERCENT);
        }
    }

    private int getLivePercentScale() {
        try {
            return Mth.clamp(Integer.parseInt(scaleField.getValue().trim()), MIN_SCALE_PERCENT, MAX_PERCENT);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("invalid scale in display image field", e);
            return Mth.clamp(scaleSlider.getCurrentScroll(), MIN_SCALE_PERCENT, MAX_PERCENT);
        }
    }

    private void setStatus(Component text, int color) {
        statusText = text == null ? Component.empty() : text;
        statusColor = color;
        statusTimer = 80;
    }

    private void clearPreviewTexture() {
        if (previewTextureLocation != null) {
            Minecraft.getInstance().getTextureManager().release(previewTextureLocation);
            previewTextureLocation = null;
        }

        if (previewTexture != null) {
            previewTexture.close();
            previewTexture = null;
        }

        previewTextureWidth = 0;
        previewTextureHeight = 0;
        previewImageId = "";
    }

    private final class ImageListWidget extends AbstractWidget {

        private static final int ROW_H = 14;
        private int scrollOff = 0;

        private ImageListWidget() {
            super(0, 0, 0, 0, Component.empty());
        }

        private int visibleRows() {
            return Math.max(0, (height - 4) / ROW_H);
        }

        private int maxScroll() {
            return Math.max(0, getMenu().getImages().size() - visibleRows());
        }

        private void clampScroll() {
            scrollOff = Mth.clamp(scrollOff, 0, maxScroll());
        }

        private void ensureVisible(String id) {
            List<DisplayImageEntry> images = getMenu().getImages();
            int idx = -1;

            for (int i = 0; i < images.size(); i++) {
                if (images.get(i).id().equals(id)) {
                    idx = i;
                    break;
                }
            }

            if (idx < 0) {
                clampScroll();
                return;
            }

            int vis = visibleRows();
            if (vis <= 0) {
                scrollOff = 0;
                return;
            }

            if (idx < scrollOff) {
                scrollOff = idx;
            } else if (idx >= scrollOff + vis) {
                scrollOff = idx - vis + 1;
            }

            clampScroll();
        }

        @Override
        public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
            clampScroll();

            int x = getX();
            int y = getY();

            g.fill(x, y, x + width, y + height, 0xFF1E1E1E);
            g.fill(x, y, x + width, y + 1, 0xFF909090);
            g.fill(x, y + height - 1, x + width, y + height, 0xFF909090);
            g.fill(x, y, x + 1, y + height, 0xFF909090);
            g.fill(x + width - 1, y, x + width, y + height, 0xFF909090);

            List<DisplayImageEntry> images = getMenu().getImages();
            DisplayImageEntry selected = getMenu().getSelectedImage();
            String selectedId = selected == null ? null : selected.id();

            int vis = visibleRows();

            for (int row = 0; row < vis; row++) {
                int idx = row + scrollOff;
                if (idx >= images.size()) {
                    break;
                }

                int ry = y + 2 + row * ROW_H;
                DisplayImageEntry entry = images.get(idx);

                boolean sel = entry.id().equals(selectedId);
                boolean hov = mouseX >= x + 1 && mouseX < x + width - 1 && mouseY >= ry && mouseY < ry + ROW_H;

                if (sel) {
                    g.fill(x + 1, ry, x + width - 1, ry + ROW_H, 0xFF3A1A1A);
                } else if (hov) {
                    g.fill(x + 1, ry, x + width - 1, ry + ROW_H, 0xFF2D2D2D);
                }

                String label = font.plainSubstrByWidth(entry.sourceName(), width - 8);
                g.drawString(font, label, x + 4, ry + 3, sel ? 0xFFFF5555 : 0xFFCCCCCC, false);
            }

            if (images.size() > vis && vis > 0) {
                int trackX1 = x + width - 3;
                int trackX2 = x + width - 1;
                int trackY1 = y + 2;
                int trackY2 = y + height - 2;
                int trackH = trackY2 - trackY1;

                int thumbH = Math.max(12, (int) ((vis / (float) images.size()) * trackH));
                int maxScroll = maxScroll();
                int thumbY = trackY1;

                if (maxScroll > 0) {
                    thumbY += (int) ((scrollOff / (float) maxScroll) * (trackH - thumbH));
                }

                g.fill(trackX1, trackY1, trackX2, trackY2, 0xFF2A2A2A);
                g.fill(trackX1, thumbY, trackX2, thumbY + thumbH, 0xFF777777);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }

            List<DisplayImageEntry> images = getMenu().getImages();
            int localY = (int) mouseY - getY() - 2;
            int row = localY / ROW_H;
            int idx = row + scrollOff;

            if (row >= 0 && idx >= 0 && idx < images.size()) {
                getMenu().selectImage(images.get(idx).id());
                syncControlsFromSelection();
                return true;
            }

            return false;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
            if (!isMouseOver(mouseX, mouseY)) {
                return false;
            }

            int delta = (int) Math.signum(scrollY);
            if (delta == 0) {
                return false;
            }

            scrollOff = Mth.clamp(scrollOff - delta, 0, maxScroll());
            return true;
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {
        }
    }

    private final class StatusWidget extends AbstractWidget {

        private StatusWidget() {
            super(leftPos, topPos, imageWidth, imageHeight, Component.empty());
        }

        @Override
        public void renderWidget(@NotNull GuiGraphics g, int mx, int my, float partial) {
            DisplayImageEntry selected = getMenu().getSelectedImage();

            int previewAreaW = 96;
            int previewAreaH = 96;

            int previewAreaX = leftPos + imageWidth - previewAreaW - 8;
            int previewAreaY = topPos + imageHeight - previewAreaH - 18;

            int gridW = Math.max(1, getMenu().previewGridWidth);
            int gridH = Math.max(1, getMenu().previewGridHeight);
            float gridAspect = gridW / (float) gridH;

            int previewW = previewAreaW;
            int previewH = Math.max(18, Math.round(previewW / gridAspect));

            if (previewH > previewAreaH) {
                previewH = previewAreaH;
                previewW = Math.max(24, Math.round(previewH * gridAspect));
            }

            previewW = Math.clamp(previewW, 24, previewAreaW);
            previewH = Math.clamp(previewH, 18, previewAreaH);

            int clipX0 = previewAreaX + (previewAreaW - previewW) / 2;
            int clipY0 = previewAreaY + (previewAreaH - previewH) / 2;
            int previewY2 = clipY0 + previewH;

            drawPreviewBackground(g, clipX0, clipY0, previewW, previewH, gridW, gridH);

            if (selected != null
                    && previewTextureLocation != null
                    && previewTextureWidth > 0
                    && previewTextureHeight > 0
                    && selected.id().equals(previewImageId)) {

                int percentX = getLivePercentX();
                int percentY = getLivePercentY();
                int percentScale = getLivePercentScale();

                float fit = Math.min(
                        previewW / (float) previewTextureWidth,
                        previewH / (float) previewTextureHeight
                );

                float fitW = previewTextureWidth * fit;
                float fitH = previewTextureHeight * fit;

                int targetW = Math.max(1, Math.round(fitW * (percentScale / 100.0f)));
                int targetH = Math.max(1, Math.round(fitH * (percentScale / 100.0f)));

                int targetX = clipX0 + Math.round((previewW - targetW) * (percentX / 100.0f));
                int targetY = clipY0 + Math.round((previewH - targetH) * (percentY / 100.0f));

                int clipX1 = clipX0 + previewW;
                int clipY1 = clipY0 + previewH;

                int drawX0 = Math.max(targetX, clipX0);
                int drawY0 = Math.max(targetY, clipY0);
                int drawX1 = Math.min(targetX + targetW, clipX1);
                int drawY1 = Math.min(targetY + targetH, clipY1);

                if (drawX1 > drawX0 && drawY1 > drawY0) {
                    float u0 = (drawX0 - targetX) / (float) targetW;
                    float v0 = (drawY0 - targetY) / (float) targetH;
                    float u1 = (drawX1 - targetX) / (float) targetW;
                    float v1 = (drawY1 - targetY) / (float) targetH;

                    g.blit(
                            previewTextureLocation,
                            drawX0,
                            drawY0,
                            drawX1 - drawX0,
                            drawY1 - drawY0,
                            u0 * previewTextureWidth,
                            v0 * previewTextureHeight,
                            (int) ((u1 - u0) * previewTextureWidth),
                            (int) ((v1 - v0) * previewTextureHeight),
                            previewTextureWidth,
                            previewTextureHeight
                    );
                }
            }

            if (selected != null) {
                int textY = Math.min(topPos + imageHeight - 12, previewY2 + 4);
                g.drawCenteredString(
                        font,
                        getLivePercentScale() + "% @ " + getLivePercentX() + "," + getLivePercentY(),
                        previewAreaX + previewAreaW / 2,
                        textY,
                        0xFFFFFFFF
                );
            }

            if (statusTimer > 0 && !statusText.getString().isEmpty()) {
                g.drawString(font, statusText, leftPos + 6, topPos + imageHeight - 12, statusColor, false);
            }
        }

        private void drawPreviewBackground(GuiGraphics g, int x, int y, int w, int h, int gridW, int gridH) {
            g.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF909090);
            g.fill(x, y, x + w, y + h, 0xFF1A1A1A);

            int cell = 6;
            for (int yy = 0; yy < h; yy += cell) {
                for (int xx = 0; xx < w; xx += cell) {
                    boolean dark = (((xx / cell) + (yy / cell)) & 1) == 0;
                    int color = dark ? 0xFF2A2A2A : 0xFF3A3A3A;
                    g.fill(x + xx, y + yy, Math.min(x + xx + cell, x + w), Math.min(y + yy + cell, y + h), color);
                }
            }

            if (gridW > 1) {
                for (int i = 1; i < gridW; i++) {
                    int gx = x + Math.round((w * i) / (float) gridW);
                    g.fill(gx, y, gx + 1, y + h, 0x44777777);
                }
            }

            if (gridH > 1) {
                for (int i = 1; i < gridH; i++) {
                    int gy = y + Math.round((h * i) / (float) gridH);
                    g.fill(x, gy, x + w, gy + 1, 0x44777777);
                }
            }
        }

        @Override
        protected void updateWidgetNarration(@NotNull NarrationElementOutput out) {
        }
    }
}