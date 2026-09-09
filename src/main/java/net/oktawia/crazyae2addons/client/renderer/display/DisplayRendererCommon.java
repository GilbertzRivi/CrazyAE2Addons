package net.oktawia.crazyae2addons.client.renderer.display;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.WeakHashMap;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;

import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.client.misc.DisplayImageClientCache;
import net.oktawia.crazyae2addons.client.misc.DisplayImageTextures;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayImageAnimation;
import net.oktawia.crazyae2addons.logic.display.DisplayImageEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.DrawEntry;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.FluidIconSeg;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.ItemIconSeg;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.LineSeg;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.RenderLine;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.RichTextWithColors;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.StyledLine;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.TableBlock;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.TableRow;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.TextSeg;
import net.oktawia.crazyae2addons.parts.Display;

public final class DisplayRendererCommon {

    private static final float DISPLAY_OFFSET = 2f;
    private static final float BACKGROUND_LAYER_Z = 0.005f - DISPLAY_OFFSET;
    private static final float IMAGE_LAYER_Z = 0.010f - DISPLAY_OFFSET;
    private static final float IMAGE_LAYER_STEP = 0.02f;
    private static final float CONTENT_LAYER_Z = 0.050f - DISPLAY_OFFSET;
    private static final float ICON_LAYER_DELTA = 0.010f;
    private static final float CLIP_TOLERANCE_PX = 1f;

    private DisplayRendererCommon() {
    }

    private static final Map<Display, CachedPrepared> PREPARED_CACHE = new WeakHashMap<>();

    private record CachedPrepared(long signature, PreparedDisplay prepared) {
    }

    public interface DrawCommand {
    }

    public record PreparedDisplay(
            float surfaceWidthPx,
            float surfaceHeightPx,
            List<DrawCommand> commands,
            @Nullable Integer backgroundColor,
            float contentZ) {
    }

    public record TextCommand(Component text, float x, float y, float scale) implements DrawCommand {
    }

    public record ItemCommand(ItemStack stack, float x, float y, float sizePx) implements DrawCommand {
    }

    public record FluidCommand(FluidStack stack, float x, float y, float sizePx) implements DrawCommand {
    }

    public record RectCommand(float x0, float y0, float x1, float y1, int argb, float z) implements DrawCommand {
    }

    public record ImageCommand(
            String imageId,
            byte[] pngBytes,
            float x,
            float y,
            float widthPx,
            float heightPx,
            float z,
            float u0,
            float v0,
            float u1,
            float v1) implements DrawCommand {
    }

    public static PreparedDisplay prepare(Font font, Display renderOrigin, Set<Display> grid) {
        var dims = DisplayGrid.getGridSize(new ArrayList<>(grid));
        List<DisplayImageEntry> images = renderOrigin.getDisplayImages();
        Map<String, byte[]> imageData = DisplayImageClientCache.collect(images);

        long signature = signatureOf(renderOrigin, dims.getFirst(), dims.getSecond(), images, imageData);
        CachedPrepared cached = PREPARED_CACHE.get(renderOrigin);

        if (cached != null && cached.signature() == signature) {
            return cached.prepared();
        }

        PreparedDisplay prepared = prepare(
                font,
                renderOrigin.getTextValue(),
                renderOrigin.resolvedTokens,
                renderOrigin.getCenterText(),
                renderOrigin.isAddMargin(),
                dims.getFirst(),
                dims.getSecond(),
                images,
                imageData,
                renderOrigin.isPowered(),
                renderOrigin.getFontSize());

        PREPARED_CACHE.put(renderOrigin, new CachedPrepared(signature, prepared));
        return prepared;
    }

    public static void invalidatePreparedCache() {
        PREPARED_CACHE.clear();
    }

    private static long signatureOf(
            Display renderOrigin,
            int widthBlocks,
            int heightBlocks,
            List<DisplayImageEntry> images,
            Map<String, byte[]> imageData) {
        long hash = 1L;

        hash = hash * 31 + Objects.hashCode(renderOrigin.getTextValue());
        hash = hash * 31 + Objects.hashCode(renderOrigin.resolvedTokens);
        hash = hash * 31 + Boolean.hashCode(renderOrigin.getCenterText());
        hash = hash * 31 + Boolean.hashCode(renderOrigin.isAddMargin());
        hash = hash * 31 + Boolean.hashCode(renderOrigin.isPowered());
        hash = hash * 31 + renderOrigin.getFontSize();
        hash = hash * 31 + widthBlocks;
        hash = hash * 31 + heightBlocks;
        hash = hash * 31 + Objects.hashCode(images);
        hash = hash * 31 + imageData.size();
        hash = hash * 31 + Boolean.hashCode(CrazyConfig.COMMON.DISPLAY_ENABLED.get());
        hash = hash * 31 + Boolean.hashCode(CrazyConfig.COMMON.DISPLAY_IMAGES_ENABLED.get());

        for (DisplayImageEntry image : images) {
            hash = hash * 31 + DisplayImageAnimation.frameIndex(image);
        }

        return hash;
    }

    public static PreparedDisplay prepare(
            Font font,
            @Nullable String textValue,
            Map<String, String> resolvedTokens,
            boolean center,
            boolean margin,
            int widthBlocks,
            int heightBlocks,
            List<DisplayImageEntry> images,
            Map<String, byte[]> imageData,
            boolean powered,
            int fontSize) {
        float pxW = Math.max(1f, 64f * Math.max(1, widthBlocks));
        float pxH = Math.max(1f, 64f * Math.max(1, heightBlocks));

        List<DrawCommand> out = new ArrayList<>();
        List<ImageCommand> imageCommands = new ArrayList<>();

        if (!powered || !CrazyConfig.COMMON.DISPLAY_ENABLED.get()) {
            return new PreparedDisplay(pxW, pxH, out, null, CONTENT_LAYER_Z);
        }

        String raw = textValue == null ? "" : textValue;
        String resolved = DisplayRenderData.resolveTokensClientSide(
                raw,
                resolvedTokens == null ? Map.of() : resolvedTokens);
        RichTextWithColors parsed = DisplayRenderData.parseStyledTextWithIcons(resolved);
        List<RenderLine> renderLines = parsed.lines();

        if (parsed.backgroundColor() != null) {
            out.add(new RectCommand(
                    0f, 0f, pxW, pxH,
                    0xFF000000 | parsed.backgroundColor(),
                    BACKGROUND_LAYER_Z));
        }

        if (images != null && imageData != null && CrazyConfig.COMMON.DISPLAY_IMAGES_ENABLED.get()) {
            int listSize = images.size();
            int imageListIdx = 0;
            for (DisplayImageEntry image : images) {
                byte[] pngBytes = imageData.get(image.id());
                if (pngBytes == null || pngBytes.length == 0) {
                    imageListIdx++;
                    continue;
                }

                DisplayImageTextures.Entry cached = DisplayImageTextures.get(image.id(), pngBytes);
                if (cached == null || cached.width() <= 0 || cached.height() <= 0) {
                    imageListIdx++;
                    continue;
                }

                DisplayImageAnimation.Frame frame = DisplayImageAnimation.current(
                        image,
                        cached.width(),
                        cached.height());

                float xPercent = clampPercent(image.x());
                float yPercent = clampPercent(image.y());
                float scalePercent = clampPercent(Math.min(image.width(), image.height()));

                float fit = Math.min(
                        pxW / (float) frame.width(),
                        pxH / (float) frame.height());

                float fitW = frame.width() * fit;
                float fitH = frame.height() * fit;

                float imageW = Math.max(1f, fitW * (scalePercent / 100f));
                float imageH = Math.max(1f, fitH * (scalePercent / 100f));

                float xPx = Math.max(0f, pxW - imageW) * (xPercent / 100f);
                float yPx = Math.max(0f, pxH - imageH) * (yPercent / 100f);

                float imageZ = IMAGE_LAYER_Z + (listSize - 1 - imageListIdx) * IMAGE_LAYER_STEP;

                imageCommands.add(new ImageCommand(
                        image.id(),
                        pngBytes,
                        xPx,
                        yPx,
                        imageW,
                        imageH,
                        imageZ,
                        frame.u0(),
                        frame.v0(),
                        frame.u1(),
                        frame.v1()));
                imageListIdx++;
            }
        }

        Collections.reverse(imageCommands);

        float contentZ = contentLayerZ(imageCommands);

        if (renderLines.isEmpty()) {
            out.addAll(imageCommands);
            return new PreparedDisplay(pxW, pxH, out, parsed.backgroundColor(), contentZ);
        }

        float maxLineWidth = 1f;
        float totalTextHeight = 0f;
        for (RenderLine ln : renderLines) {
            maxLineWidth = Math.max(maxLineWidth, DisplayRenderData.renderLineWidthPx(font, ln));
            totalTextHeight += DisplayRenderData.renderLineHeightPx(font, ln);
        }

        float marginFrac = margin ? 0.03f : 0.0f;
        float marginX = pxW * marginFrac;
        float marginY = pxH * marginFrac;
        float usableW = Math.max(1f, pxW - 2f * marginX);
        float usableH = Math.max(1f, pxH - 2f * marginY);

        float globalScalePx = fontSize > 0
                ? fontSize / (float) font.lineHeight
                : Math.min(
                        usableW / Math.max(1f, maxLineWidth),
                        usableH / Math.max(1f, totalTextHeight));
        if (!Float.isFinite(globalScalePx) || globalScalePx <= 0f) {
            globalScalePx = 1f;
        }

        float availTextW = usableW / globalScalePx;
        float availTextH = usableH / globalScalePx;

        final float EPSILON = 0.05f;
        List<DrawEntry> drawPlan = new ArrayList<>();
        float remainingH = availTextH;

        for (RenderLine ln : renderLines) {
            if (remainingH <= 0f) {
                break;
            }

            if (ln instanceof StyledLine sl) {
                float lh = font.lineHeight * sl.scaleMul();
                if (lh > remainingH + EPSILON) {
                    break;
                }
                drawPlan.add(new DrawEntry(ln, 1));
                remainingH -= lh;
            } else if (ln instanceof TableBlock tb) {
                float rowH = DisplayRenderData.tableRowHeightPx(font) * tb.scaleMul();
                float headerH = rowH + DisplayRenderData.TABLE_HEADER_EXTRA_PX * tb.scaleMul();

                int rowsFit = remainingH + EPSILON < headerH
                        ? 0
                        : 1 + (int) Math.floor((remainingH - headerH + EPSILON) / rowH);
                rowsFit = Math.min(rowsFit, tb.rows().size());

                if (rowsFit <= 0) {
                    break;
                }
                drawPlan.add(new DrawEntry(ln, rowsFit));
                remainingH -= DisplayRenderData.tableBlockHeightPx(font, tb, rowsFit);
            }
        }

        float drawnH = 0f;
        for (DrawEntry de : drawPlan) {
            if (de.line() instanceof StyledLine sl) {
                drawnH += font.lineHeight * sl.scaleMul();
            } else if (de.line() instanceof TableBlock tb) {
                drawnH += DisplayRenderData.tableBlockHeightPx(font, tb, de.tableRowsToDraw());
            }
        }

        float centerYOffsetPx = center ? Math.max(0f, (availTextH - drawnH) * 0.5f) * globalScalePx : 0f;

        float yCursor = 0f;
        for (DrawEntry de : drawPlan) {
            RenderLine ln = de.line();

            if (ln instanceof StyledLine sl) {
                float lineW = DisplayRenderData.renderLineWidthPx(font, sl);
                float xOffset = center ? Math.max(0f, (availTextW - lineW) * 0.5f) : 0f;

                float baseX = marginX + xOffset * globalScalePx;
                float baseY = marginY + centerYOffsetPx + yCursor * globalScalePx;
                float lineScalePx = globalScalePx * sl.scaleMul();

                appendLineSegCommands(out, font, sl.segs(), baseX, baseY, lineScalePx);
                yCursor += font.lineHeight * sl.scaleMul();
            } else if (ln instanceof TableBlock tb) {
                float blockW = renderLineWidthPx(font, tb);
                float xOffset = center ? Math.max(0f, (availTextW - blockW) * 0.5f) : 0f;

                float baseX = marginX + xOffset * globalScalePx;
                float baseY = marginY + centerYOffsetPx + yCursor * globalScalePx;
                float blockScalePx = globalScalePx * tb.scaleMul();

                appendTableCommands(out, font, tb, de.tableRowsToDraw(), baseX, baseY, blockScalePx, contentZ);
                yCursor += DisplayRenderData.tableBlockHeightPx(font, tb, de.tableRowsToDraw());
            }
        }

        clipToSurface(out, font, pxW, pxH);
        out.addAll(imageCommands);
        return new PreparedDisplay(pxW, pxH, out, parsed.backgroundColor(), contentZ);
    }

    private static void clipToSurface(List<DrawCommand> out, Font font, float pxW, float pxH) {
        List<DrawCommand> clipped = new ArrayList<>(out.size());

        for (DrawCommand cmd : out) {
            if (cmd instanceof TextCommand tc) {
                if (tc.x() >= pxW || tc.y() >= pxH) {
                    continue;
                }

                float available = (pxW - tc.x()) / Math.max(0.0001f, tc.scale());

                if (available < 1f) {
                    continue;
                }

                if (font.width(tc.text()) <= available + CLIP_TOLERANCE_PX) {
                    clipped.add(tc);
                    continue;
                }

                Component trimmed = trimToWidth(font, tc.text(), (int) Math.floor(available));

                if (trimmed != null) {
                    clipped.add(new TextCommand(trimmed, tc.x(), tc.y(), tc.scale()));
                }
            } else if (cmd instanceof ItemCommand ic) {
                if (ic.x() + ic.sizePx() <= pxW + CLIP_TOLERANCE_PX
                        && ic.y() + ic.sizePx() <= pxH + CLIP_TOLERANCE_PX) {
                    clipped.add(ic);
                }
            } else if (cmd instanceof FluidCommand fc) {
                if (fc.x() + fc.sizePx() <= pxW + CLIP_TOLERANCE_PX
                        && fc.y() + fc.sizePx() <= pxH + CLIP_TOLERANCE_PX) {
                    clipped.add(fc);
                }
            } else if (cmd instanceof RectCommand rc) {
                if (rc.x0() >= pxW || rc.y0() >= pxH) {
                    continue;
                }

                clipped.add(new RectCommand(
                        rc.x0(),
                        rc.y0(),
                        Math.min(rc.x1(), pxW),
                        Math.min(rc.y1(), pxH),
                        rc.argb(),
                        rc.z()));
            } else {
                clipped.add(cmd);
            }
        }

        out.clear();
        out.addAll(clipped);
    }

    @Nullable
    private static Component trimToWidth(Font font, Component text, int maxWidth) {
        FormattedText head = font.getSplitter().headByWidth(text, maxWidth, Style.EMPTY);
        MutableComponent trimmed = Component.empty();

        head.visit((style, part) -> {
            if (!part.isEmpty()) {
                trimmed.append(Component.literal(part).withStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        return trimmed.getSiblings().isEmpty() ? null : trimmed;
    }

    private static float contentLayerZ(List<ImageCommand> imageCommands) {
        float topImageZ = CONTENT_LAYER_Z;

        for (ImageCommand image : imageCommands) {
            topImageZ = Math.max(topImageZ, image.z() + IMAGE_LAYER_STEP);
        }

        return topImageZ;
    }

    public static void renderPrepared(
            PreparedDisplay prepared,
            PoseStack ps,
            MultiBufferSource buf,
            Font font,
            int light) {
        renderPrepared(prepared, ps, buf, font, light, true);
    }

    public static void renderPrepared(
            PreparedDisplay prepared,
            PoseStack ps,
            MultiBufferSource buf,
            Font font,
            int light,
            boolean includeImages) {
        for (DrawCommand cmd : prepared.commands()) {
            if (!includeImages && cmd instanceof ImageCommand) {
                continue;
            }

            if (cmd instanceof RectCommand rc) {
                drawSolidRect(ps, buf, light, rc.argb(), rc.x0(), rc.y0(), rc.x1(), rc.y1(), rc.z());
            } else if (cmd instanceof TextCommand tc) {
                ps.pushPose();
                ps.translate(tc.x(), tc.y(), prepared.contentZ());
                ps.scale(tc.scale(), tc.scale(), 1f);

                font.drawInBatch(
                        tc.text(),
                        0f,
                        0f,
                        0xFFFFFF,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.POLYGON_OFFSET,
                        0,
                        light);

                ps.popPose();
            } else if (cmd instanceof ItemCommand ic) {
                renderItemFlattened(
                        ic.stack(),
                        ps,
                        buf,
                        light,
                        ic.x(),
                        ic.y(),
                        Math.max(1, Math.round(ic.sizePx())),
                        prepared.contentZ() + ICON_LAYER_DELTA);
            } else if (cmd instanceof FluidCommand fc) {
                TextureAtlasSprite sprite = getFluidSprite(fc.stack());
                int tint = getFluidTint(fc.stack());
                drawSpriteQuad(
                        ps,
                        buf,
                        light,
                        tint,
                        fc.x(),
                        fc.y(),
                        fc.sizePx(),
                        sprite,
                        prepared.contentZ() + ICON_LAYER_DELTA);
            } else if (cmd instanceof ImageCommand ic) {
                DisplayImageTextures.Entry cached = DisplayImageTextures.get(ic.imageId(), ic.pngBytes());
                if (cached != null) {
                    drawClippedTexturedQuad(
                            ps,
                            buf,
                            light,
                            cached.location(),
                            ic.x(),
                            ic.y(),
                            ic.widthPx(),
                            ic.heightPx(),
                            prepared.surfaceWidthPx(),
                            prepared.surfaceHeightPx(),
                            ic.z(),
                            ic.u0(),
                            ic.v0(),
                            ic.u1(),
                            ic.v1());
                }
            }
        }
    }

    private static void appendLineSegCommands(
            List<DrawCommand> out,
            Font font,
            List<LineSeg> segs,
            float baseX,
            float baseY,
            float scalePx) {
        float cursor = 0f;
        float iconSize = font.lineHeight;
        int iconAdv = font.lineHeight + 1;

        for (LineSeg seg : segs) {
            if (seg instanceof TextSeg ts) {
                Component c = ts.c();
                out.add(new TextCommand(c, baseX + cursor * scalePx, baseY, scalePx));
                cursor += font.width(c);
            } else if (seg instanceof ItemIconSeg is) {
                out.add(new ItemCommand(is.stack(), baseX + cursor * scalePx, baseY, iconSize * scalePx));
                cursor += iconAdv;
            } else if (seg instanceof FluidIconSeg fs) {
                out.add(new FluidCommand(fs.stack(), baseX + cursor * scalePx, baseY, iconSize * scalePx));
                cursor += iconAdv;
            }
        }
    }

    private static void appendTableCommands(
            List<DrawCommand> out,
            Font font,
            TableBlock tb,
            int rowsToDraw,
            float baseX,
            float baseY,
            float scalePx,
            float lineZ) {
        var layout = DisplayRenderData.computeTableLayout(font, tb);
        int cols = layout.cols();
        int pad = layout.padPx();
        int barW = layout.barW();
        int[] colW = layout.colContentW();
        float rowH = DisplayRenderData.tableRowHeightPx(font);
        float headerH = rowH + DisplayRenderData.TABLE_HEADER_EXTRA_PX;
        float drawnH = rowsToDraw <= 0 ? 0f : headerH + (rowsToDraw - 1) * rowH;
        float rightEdge = layout.totalW() - barW + 1f;

        int barColor = 0xFFAAAAAA;
        @Nullable
        Component indent = layout.indentText().isEmpty()
                ? null
                : Component.literal(layout.indentText()).withStyle(Style.EMPTY.withColor(0x888888));

        int lineColor = 0x66AAAAAA;

        out.add(new RectCommand(
                baseX + layout.prefixW() * scalePx,
                baseY - 1f * scalePx,
                baseX + rightEdge * scalePx,
                baseY,
                lineColor,
                lineZ));

        out.add(new RectCommand(
                baseX + layout.prefixW() * scalePx,
                baseY + (drawnH - 1f) * scalePx,
                baseX + rightEdge * scalePx,
                baseY + drawnH * scalePx,
                lineColor,
                lineZ));

        if (rowsToDraw > 1) {
            out.add(new RectCommand(
                    baseX + layout.prefixW() * scalePx,
                    baseY + (headerH - 1f) * scalePx,
                    baseX + rightEdge * scalePx,
                    baseY + headerH * scalePx,
                    lineColor,
                    lineZ));
        }

        int drawRows = Math.min(rowsToDraw, tb.rows().size());
        for (int r = 0; r < drawRows; r++) {
            TableRow row = tb.rows().get(r);
            float rowY = baseY
                    + ((r == 0 ? 0f : headerH + (r - 1) * rowH) + DisplayRenderData.TABLE_ROW_TOP_PAD_PX) * scalePx;
            float x = 0f;

            if (indent != null) {
                out.add(new TextCommand(indent, baseX, rowY, scalePx));
                x += layout.prefixW();
            }

            if (r == 0) {
                appendTableBar(out, baseX, baseY, x, drawnH, scalePx, barColor, lineZ);
            }

            x += barW;

            for (int c = 0; c < cols; c++) {
                List<LineSeg> cell = (c < row.cells().size()) ? row.cells().get(c) : List.of();
                int cellW = (cell == null || cell.isEmpty()) ? 0 : DisplayRenderData.segsWidthPx(font, cell);
                int innerW = colW[c];
                int align = (tb.align() != null && c < tb.align().length) ? tb.align()[c] : 1;

                float contentX = switch (align) {
                    case 0 -> x + pad;
                    case 2 -> x + pad + Math.max(0f, innerW - cellW);
                    default -> x + pad + Math.max(0f, (innerW - cellW) * 0.5f);
                };

                appendLineSegCommands(out, font, cell, baseX + contentX * scalePx, rowY, scalePx);

                x += innerW + pad * 2;

                if (r == 0) {
                    appendTableBar(out, baseX, baseY, x, drawnH, scalePx, barColor, lineZ);
                }

                x += barW;
            }
        }
    }

    private static void appendTableBar(
            List<DrawCommand> out,
            float baseX,
            float baseY,
            float x,
            float height,
            float scalePx,
            int color,
            float lineZ) {
        out.add(new RectCommand(
                baseX + x * scalePx,
                baseY,
                baseX + (x + 1f) * scalePx,
                baseY + height * scalePx,
                color,
                lineZ));
    }

    private static float renderLineWidthPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) {
            return DisplayRenderData.segsWidthPx(font, sl.segs()) * sl.scaleMul();
        }
        if (ln instanceof TableBlock tb) {
            return DisplayRenderData.computeTableLayout(font, tb).totalW() * tb.scaleMul();
        }
        return 1f;
    }

    private static float clampPercent(int value) {
        return Math.max(0f, Math.min(100f, value));
    }

    public static void drawSolidRect(
            PoseStack ps,
            MultiBufferSource buf,
            int light,
            int argb,
            float x0,
            float y0,
            float x1,
            float y1,
            float z) {
        VertexConsumer buffer = buf.getBuffer(RenderType.textBackground());
        Matrix4f m = ps.last().pose();

        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        buffer.vertex(m, x0, y1, z).color(r, g, b, a).uv2(light).endVertex();
        buffer.vertex(m, x1, y1, z).color(r, g, b, a).uv2(light).endVertex();
        buffer.vertex(m, x1, y0, z).color(r, g, b, a).uv2(light).endVertex();
        buffer.vertex(m, x0, y0, z).color(r, g, b, a).uv2(light).endVertex();
    }

    public static void drawSpriteQuad(
            PoseStack ps,
            MultiBufferSource buf,
            int light,
            int argb,
            float x,
            float y,
            float sizePx,
            TextureAtlasSprite sprite,
            float z) {
        VertexConsumer buffer = buf.getBuffer(RenderType.textPolygonOffset(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = ps.last().pose();

        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        float x1 = x + sizePx;
        float y1 = y + sizePx;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        buffer.vertex(m, x, y, z).color(r, g, b, a).uv(u0, v0).uv2(light).endVertex();
        buffer.vertex(m, x1, y, z).color(r, g, b, a).uv(u1, v0).uv2(light).endVertex();
        buffer.vertex(m, x1, y1, z).color(r, g, b, a).uv(u1, v1).uv2(light).endVertex();
        buffer.vertex(m, x, y1, z).color(r, g, b, a).uv(u0, v1).uv2(light).endVertex();

        buffer.vertex(m, x, y, z).color(r, g, b, a).uv(u0, v0).uv2(light).endVertex();
        buffer.vertex(m, x, y1, z).color(r, g, b, a).uv(u0, v1).uv2(light).endVertex();
        buffer.vertex(m, x1, y1, z).color(r, g, b, a).uv(u1, v1).uv2(light).endVertex();
        buffer.vertex(m, x1, y, z).color(r, g, b, a).uv(u1, v0).uv2(light).endVertex();
    }

    public static void renderItemFlattened(
            ItemStack stack,
            PoseStack ps,
            MultiBufferSource buf,
            int light,
            float x,
            float y,
            int iconPx,
            float z) {
        Minecraft mc = Minecraft.getInstance();

        ps.pushPose();
        ps.translate(x, y, z);
        ps.translate(iconPx / 2f, iconPx / 2f, 0f);
        ps.scale(1f, -1f, 1f);

        float zFlatten = 1f / 256f;
        ps.scale(iconPx, iconPx, iconPx * zFlatten);

        RenderSystem.disableCull();
        mc.getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.GUI,
                light,
                OverlayTexture.NO_OVERLAY,
                ps,
                buf,
                mc.level,
                0);
        RenderSystem.enableCull();
        ps.popPose();
    }

    public static TextureAtlasSprite getFluidSprite(FluidStack fs) {
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fs.getFluid());
        return Minecraft.getInstance()
                .getTextureAtlas(InventoryMenu.BLOCK_ATLAS)
                .apply(ext.getStillTexture());
    }

    public static int getFluidTint(FluidStack fs) {
        return IClientFluidTypeExtensions.of(fs.getFluid()).getTintColor();
    }

    private static void drawClippedTexturedQuad(
            PoseStack ps,
            MultiBufferSource buf,
            int light,
            ResourceLocation texture,
            float x,
            float y,
            float width,
            float height,
            float clipX1,
            float clipY1,
            float z,
            float frameU0,
            float frameV0,
            float frameU1,
            float frameV1) {
        if (width <= 0f || height <= 0f) {
            return;
        }

        float srcX1 = x + width;
        float srcY1 = y + height;

        float dstX0 = Math.max(x, 0.0f);
        float dstY0 = Math.max(y, 0.0f);
        float dstX1 = Math.min(srcX1, clipX1);
        float dstY1 = Math.min(srcY1, clipY1);

        if (dstX1 <= dstX0 || dstY1 <= dstY0) {
            return;
        }

        float u0 = frameU0 + (dstX0 - x) / width * (frameU1 - frameU0);
        float v0 = frameV0 + (dstY0 - y) / height * (frameV1 - frameV0);
        float u1 = frameU0 + (dstX1 - x) / width * (frameU1 - frameU0);
        float v1 = frameV0 + (dstY1 - y) / height * (frameV1 - frameV0);

        VertexConsumer buffer = buf.getBuffer(RenderType.textPolygonOffset(texture));
        Matrix4f m = ps.last().pose();

        buffer.vertex(m, dstX0, dstY0, z).color(255, 255, 255, 255).uv(u0, v0).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY0, z).color(255, 255, 255, 255).uv(u1, v0).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY1, z).color(255, 255, 255, 255).uv(u1, v1).uv2(light).endVertex();
        buffer.vertex(m, dstX0, dstY1, z).color(255, 255, 255, 255).uv(u0, v1).uv2(light).endVertex();

        buffer.vertex(m, dstX0, dstY0, z).color(255, 255, 255, 255).uv(u0, v0).uv2(light).endVertex();
        buffer.vertex(m, dstX0, dstY1, z).color(255, 255, 255, 255).uv(u0, v1).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY1, z).color(255, 255, 255, 255).uv(u1, v1).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY0, z).color(255, 255, 255, 255).uv(u1, v0).uv2(light).endVertex();
    }
}
