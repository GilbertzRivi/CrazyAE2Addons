package net.oktawia.crazyae2addons.client.renderer.display;

import java.util.List;
import java.util.Map;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import net.oktawia.crazyae2addons.client.misc.DisplayImageTextures;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;

public final class DisplayGuiRenderer {

    private static final int PANEL_BG = 0xAA0B0B0B;
    private static final int PANEL_BORDER = 0xFF3A3A3A;
    private static final int PANEL_INNER = 0xFF161616;
    private static final int PANEL_PAD = 4;

    private static final int SURFACE_OUTER = 0xFF5A5A5A;
    private static final int SURFACE_INNER = 0xFF050505;
    private static final int SURFACE_GRID = 0x223A3A3A;

    private static final float PREVIEW_CONTENT_Z = 20f;

    private DisplayGuiRenderer() {
    }

    public static DisplayRendererCommon.PreparedDisplay preparePreview(
            Font font,
            String textValue,
            Map<String, String> resolvedTokens,
            boolean center,
            boolean margin,
            int gridWidthBlocks,
            int gridHeightBlocks,
            int fontSize) {
        return DisplayRendererCommon.prepare(
                font,
                textValue,
                resolvedTokens,
                center,
                margin,
                Math.max(1, gridWidthBlocks),
                Math.max(1, gridHeightBlocks),
                List.of(),
                Map.of(),
                true,
                fontSize);
    }

    public static DisplayRendererCommon.PreparedDisplay preparePreview(
            Font font,
            String textValue,
            Map<String, String> resolvedTokens,
            boolean center,
            boolean margin,
            int gridWidthBlocks,
            int gridHeightBlocks,
            List<DisplayImageEntry> images,
            Map<String, byte[]> imageData,
            int fontSize) {
        return DisplayRendererCommon.prepare(
                font,
                textValue,
                resolvedTokens,
                center,
                margin,
                Math.max(1, gridWidthBlocks),
                Math.max(1, gridHeightBlocks),
                images == null ? List.of() : images,
                imageData == null ? Map.of() : imageData,
                true,
                fontSize);
    }

    public static void renderPreview(
            GuiGraphics gui,
            int x,
            int y,
            int width,
            int height,
            DisplayRendererCommon.PreparedDisplay prepared) {
        if (width <= 0 || height <= 0) {
            return;
        }

        gui.fill(x - 1, y - 1, x + width + 1, y + height + 1, PANEL_BORDER);
        gui.fill(x, y, x + width, y + height, PANEL_BG);

        int innerX = x + PANEL_PAD;
        int innerY = y + PANEL_PAD;
        int innerW = Math.max(1, width - PANEL_PAD * 2);
        int innerH = Math.max(1, height - PANEL_PAD * 2);

        gui.fill(innerX, innerY, innerX + innerW, innerY + innerH, PANEL_INNER);

        float surfaceW = Math.max(1f, prepared.surfaceWidthPx());
        float surfaceH = Math.max(1f, prepared.surfaceHeightPx());

        float fit = Math.min(
                (float) innerW / surfaceW,
                (float) innerH / surfaceH);

        if (!Float.isFinite(fit) || fit <= 0f) {
            fit = 1f;
        }

        float drawW = surfaceW * fit;
        float drawH = surfaceH * fit;
        float drawX = innerX + (innerW - drawW) * 0.5f;
        float drawY = innerY + (innerH - drawH) * 0.5f;

        int sx0 = round(drawX);
        int sy0 = round(drawY);
        int sx1 = round(drawX + drawW);
        int sy1 = round(drawY + drawH);

        int surfaceColor = prepared.backgroundColor() == null
                ? SURFACE_INNER
                : 0xFF000000 | prepared.backgroundColor();

        gui.fill(sx0 - 1, sy0 - 1, sx1 + 1, sy1 + 1, SURFACE_OUTER);
        gui.fill(sx0, sy0, sx1, sy1, surfaceColor);

        int blocksW = Math.max(1, Math.round(surfaceW / 64f));
        int blocksH = Math.max(1, Math.round(surfaceH / 64f));

        for (int i = 1; i < blocksW; i++) {
            int gx = round(drawX + i * 64f * fit);
            gui.fill(gx, sy0, gx + 1, sy1, SURFACE_GRID);
        }

        for (int i = 1; i < blocksH; i++) {
            int gy = round(drawY + i * 64f * fit);
            gui.fill(sx0, gy, sx1, gy + 1, SURFACE_GRID);
        }

        renderPreviewImages(gui, prepared, drawX, drawY, fit, sx0, sy0, sx1, sy1);

        PoseStack ps = gui.pose();
        ps.pushPose();

        ps.translate(drawX, drawY, PREVIEW_CONTENT_Z);
        ps.scale(fit, fit, 1f);

        DisplayRendererCommon.renderPrepared(
                prepared,
                ps,
                gui.bufferSource(),
                Minecraft.getInstance().font,
                0xF000F0,
                false);

        ps.popPose();
        gui.flush();
    }

    private static void renderPreviewImages(
            GuiGraphics gui,
            DisplayRendererCommon.PreparedDisplay prepared,
            float drawX,
            float drawY,
            float fit,
            int clipX0,
            int clipY0,
            int clipX1,
            int clipY1) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        for (DisplayRendererCommon.DrawCommand cmd : prepared.commands()) {
            if (!(cmd instanceof DisplayRendererCommon.ImageCommand ic)) {
                continue;
            }

            DisplayImageTextures.Entry entry = DisplayImageTextures.get(ic.imageId(), ic.pngBytes());
            if (entry == null || entry.width() <= 0 || entry.height() <= 0) {
                continue;
            }

            float targetX = drawX + ic.x() * fit;
            float targetY = drawY + ic.y() * fit;
            float targetW = ic.widthPx() * fit;
            float targetH = ic.heightPx() * fit;

            if (targetW <= 0f || targetH <= 0f) {
                continue;
            }

            int dstX0 = Math.max(clipX0, Math.round(targetX));
            int dstY0 = Math.max(clipY0, Math.round(targetY));
            int dstX1 = Math.min(clipX1, Math.round(targetX + targetW));
            int dstY1 = Math.min(clipY1, Math.round(targetY + targetH));

            if (dstX1 <= dstX0 || dstY1 <= dstY0) {
                continue;
            }

            float u0 = ic.u0() + (dstX0 - targetX) / targetW * (ic.u1() - ic.u0());
            float v0 = ic.v0() + (dstY0 - targetY) / targetH * (ic.v1() - ic.v0());
            float u1 = ic.u0() + (dstX1 - targetX) / targetW * (ic.u1() - ic.u0());
            float v1 = ic.v0() + (dstY1 - targetY) / targetH * (ic.v1() - ic.v0());

            gui.blit(
                    entry.location(),
                    dstX0,
                    dstY0,
                    dstX1 - dstX0,
                    dstY1 - dstY0,
                    u0 * entry.width(),
                    v0 * entry.height(),
                    Math.max(1, Math.round((u1 - u0) * entry.width())),
                    Math.max(1, Math.round((v1 - v0) * entry.height())),
                    entry.width(),
                    entry.height());
        }

        RenderSystem.disableBlend();
    }

    public static int computePreviewWidth(int leftPos, int preferredWidth, int gap, int minX) {
        int availableLeft = Math.max(0, leftPos - gap - minX);
        return Math.min(preferredWidth, availableLeft);
    }

    public static int clampPreviewX(int leftPos, int previewWidth, int gap, int minX) {
        return Math.max(minX, leftPos - gap - previewWidth);
    }

    public static int clampPreviewY(int topPos, int imageHeight, int previewHeight) {
        return topPos + Math.max(8, (imageHeight - previewHeight) / 2);
    }

    private static int round(float value) {
        return Math.round(value);
    }
}
