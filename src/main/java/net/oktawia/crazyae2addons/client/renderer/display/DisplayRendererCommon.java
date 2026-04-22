package net.oktawia.crazyae2addons.client.renderer.display;

import appeng.parts.CableBusContainer;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
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
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DisplayRendererCommon {

    private static final float BACKGROUND_LAYER_Z = 0.0005f;
    private static final float TABLE_LINE_LAYER_Z = 0.0010f;
    private static final float TEXT_LAYER_Z =  0.0010f;
    private static final float ICON_LAYER_Z =  0.0010f;
    private static final float IMAGE_LAYER_Z = 0.0025f;

    private DisplayRendererCommon() {}

    private record CachedImage(DynamicTexture texture, ResourceLocation location, int width, int height) {}

    private static final Map<String, CachedImage> IMAGE_CACHE = new HashMap<>();

    public interface DrawCommand {}

    public record PreparedDisplay(
            float surfaceWidthPx,
            float surfaceHeightPx,
            List<DrawCommand> commands
    ) {}

    public record TextCommand(Component text, float x, float y, float scale) implements DrawCommand {
    }

    public record ItemCommand(ItemStack stack, float x, float y, float sizePx) implements DrawCommand {
    }

    public record FluidCommand(FluidStack stack, float x, float y, float sizePx) implements DrawCommand {
    }

    public record RectCommand(float x0, float y0, float x1, float y1, int argb, float z) implements DrawCommand {
    }

    public record ImageCommand(
            String cacheKey,
            byte[] pngBytes,
            float x,
            float y,
            float widthPx,
            float heightPx
    ) implements DrawCommand {
    }

    public static PreparedDisplay prepare(Font font, Display renderOrigin, Set<Display> grid) {
        var dims = DisplayGrid.getGridSize(new ArrayList<>(grid), renderOrigin.getSide());
        return prepare(
                font,
                renderOrigin.getTextValue(),
                renderOrigin.resolvedTokens,
                renderOrigin.getCenterText(),
                renderOrigin.isAddMargin(),
                dims.getFirst(),
                dims.getSecond(),
                renderOrigin.getDisplayImages(),
                renderOrigin.getDisplayImageData(),
                renderOrigin.isPowered()
        );
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
            boolean powered
    ) {
        float pxW = Math.max(1f, 64f * Math.max(1, widthBlocks));
        float pxH = Math.max(1f, 64f * Math.max(1, heightBlocks));

        List<DrawCommand> out = new ArrayList<>();
        List<ImageCommand> imageCommands = new ArrayList<>();

        if (!powered) {
            return new PreparedDisplay(pxW, pxH, out);
        }

        String raw = textValue == null ? "" : textValue;
        String resolved = DisplayRenderData.resolveTokensClientSide(
                raw,
                resolvedTokens == null ? Map.of() : resolvedTokens
        );
        RichTextWithColors parsed = DisplayRenderData.parseStyledTextWithIcons(resolved);
        List<RenderLine> renderLines = parsed.lines();

        if (parsed.backgroundColor() != null) {
            out.add(new RectCommand(
                    0f, 0f, pxW, pxH,
                    0xFF000000 | parsed.backgroundColor(),
                    BACKGROUND_LAYER_Z
            ));
        }

        if (images != null && imageData != null) {
            for (DisplayImageEntry image : images) {
                byte[] pngBytes = imageData.get(image.id());
                if (pngBytes == null || pngBytes.length == 0) {
                    continue;
                }

                String cacheKey = imageCacheKey(image.id(), pngBytes);
                CachedImage cached = getOrCreateCachedImage(cacheKey, pngBytes);
                if (cached == null || cached.width() <= 0 || cached.height() <= 0) {
                    continue;
                }

                float xPercent = clampPercent(image.x());
                float yPercent = clampPercent(image.y());
                float scalePercent = clampPercent(Math.min(image.width(), image.height()));

                float fit = Math.min(
                        pxW / (float) cached.width(),
                        pxH / (float) cached.height()
                );

                float fitW = cached.width() * fit;
                float fitH = cached.height() * fit;

                float imageW = Math.max(1f, fitW * (scalePercent / 100f));
                float imageH = Math.max(1f, fitH * (scalePercent / 100f));

                float xPx = Math.max(0f, pxW - imageW) * (xPercent / 100f);
                float yPx = Math.max(0f, pxH - imageH) * (yPercent / 100f);

                imageCommands.add(new ImageCommand(
                        cacheKey,
                        pngBytes,
                        xPx,
                        yPx,
                        imageW,
                        imageH
                ));
            }
        }

        if (renderLines.isEmpty()) {
            out.addAll(imageCommands);
            return new PreparedDisplay(pxW, pxH, out);
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

        float globalScalePx = Math.min(
                usableW / Math.max(1f, maxLineWidth),
                usableH / Math.max(1f, totalTextHeight)
        );
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
                float rowH = font.lineHeight * tb.scaleMul();
                int rowsFit = Math.min((int) Math.floor((remainingH + EPSILON) / rowH), tb.rows().size());
                if (rowsFit <= 0) {
                    break;
                }
                drawPlan.add(new DrawEntry(ln, rowsFit));
                remainingH -= rowsFit * rowH;
            }
        }

        float drawnH = 0f;
        for (DrawEntry de : drawPlan) {
            if (de.line() instanceof StyledLine sl) {
                drawnH += font.lineHeight * sl.scaleMul();
            } else if (de.line() instanceof TableBlock tb) {
                drawnH += de.tableRowsToDraw() * font.lineHeight * tb.scaleMul();
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

                appendTableCommands(out, font, tb, de.tableRowsToDraw(), baseX, baseY, blockScalePx);
                yCursor += de.tableRowsToDraw() * font.lineHeight * tb.scaleMul();
            }
        }

        out.addAll(imageCommands);
        return new PreparedDisplay(pxW, pxH, out);
    }

    public static void renderPrepared(
            PreparedDisplay prepared,
            PoseStack ps,
            MultiBufferSource buf,
            Font font,
            int light
    ) {
        for (DrawCommand cmd : prepared.commands()) {
            if (cmd instanceof RectCommand rc) {
                drawSolidRect(ps, buf, light, rc.argb(), rc.x0(), rc.y0(), rc.x1(), rc.y1(), rc.z());
            } else if (cmd instanceof TextCommand tc) {
                ps.pushPose();
                ps.translate(tc.x(), tc.y(), TEXT_LAYER_Z);
                ps.scale(tc.scale(), tc.scale(), 1f);
                font.drawInBatch(
                        tc.text(),
                        0f,
                        0f,
                        0xFFFFFF,
                        false,
                        ps.last().pose(),
                        buf,
                        Font.DisplayMode.NORMAL,
                        0,
                        light
                );
                ps.popPose();
            } else if (cmd instanceof ItemCommand ic) {
                renderItemFlattened(
                        ic.stack(),
                        ps,
                        buf,
                        light,
                        ic.x(),
                        ic.y(),
                        Math.max(1, Math.round(ic.sizePx()))
                );
            } else if (cmd instanceof FluidCommand fc) {
                TextureAtlasSprite sprite = getFluidSprite(fc.stack());
                int tint = getFluidTint(fc.stack());
                drawSpriteQuad(ps, buf, light, tint, fc.x(), fc.y(), fc.sizePx(), sprite);
            } else if (cmd instanceof ImageCommand ic) {
                CachedImage cached = getOrCreateCachedImage(ic.cacheKey(), ic.pngBytes());
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
                            prepared.surfaceHeightPx()
                    );
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
            float scalePx
    ) {
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
            float scalePx
    ) {
        var layout = DisplayRenderData.computeTableLayout(font, tb);
        int cols = layout.cols();
        int pad = layout.padPx();
        int barW = layout.barW();
        int[] colW = layout.colContentW();
        float rowH = font.lineHeight;

        Component bar = Component.literal("|").withStyle(Style.EMPTY.withColor(0xAAAAAA));
        @Nullable Component indent = layout.indentText().isEmpty()
                ? null
                : Component.literal(layout.indentText()).withStyle(Style.EMPTY.withColor(0x888888));

        int lineColor = 0x66AAAAAA;

        out.add(new RectCommand(
                baseX + layout.prefixW() * scalePx,
                baseY - 1f * scalePx,
                baseX + layout.totalW() * scalePx,
                baseY,
                lineColor,
                TABLE_LINE_LAYER_Z
        ));

        out.add(new RectCommand(
                baseX + layout.prefixW() * scalePx,
                baseY + (rowsToDraw * rowH - 1f) * scalePx,
                baseX + layout.totalW() * scalePx,
                baseY + rowsToDraw * rowH * scalePx,
                lineColor,
                TABLE_LINE_LAYER_Z
        ));

        if (rowsToDraw > 1) {
            out.add(new RectCommand(
                    baseX + layout.prefixW() * scalePx,
                    baseY + (rowH - 1f) * scalePx,
                    baseX + layout.totalW() * scalePx,
                    baseY + rowH * scalePx,
                    lineColor,
                    TABLE_LINE_LAYER_Z
            ));
        }

        int drawRows = Math.min(rowsToDraw, tb.rows().size());
        for (int r = 0; r < drawRows; r++) {
            TableRow row = tb.rows().get(r);
            float rowY = baseY + r * rowH * scalePx;
            float x = 0f;

            if (indent != null) {
                out.add(new TextCommand(indent, baseX, rowY, scalePx));
                x += layout.prefixW();
            }

            out.add(new TextCommand(bar, baseX + x * scalePx, rowY, scalePx));
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
                out.add(new TextCommand(bar, baseX + x * scalePx, rowY, scalePx));
                x += barW;
            }
        }
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
            float z
    ) {
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
            TextureAtlasSprite sprite
    ) {
        VertexConsumer buffer = buf.getBuffer(RenderType.text(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = ps.last().pose();

        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        float x1 = x + sizePx;
        float y1 = y + sizePx;
        float z = ICON_LAYER_Z;

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
            int iconPx
    ) {
        Minecraft mc = Minecraft.getInstance();

        ps.pushPose();
        ps.translate(x, y, ICON_LAYER_Z);
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
                0
        );
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
            float clipY1
    ) {
        if (width <= 0f || height <= 0f) {
            return;
        }

        float srcX1 = x + width;
        float srcY1 = y + height;

        float dstX0 = Math.max(x, (float) 0.0);
        float dstY0 = Math.max(y, (float) 0.0);
        float dstX1 = Math.min(srcX1, clipX1);
        float dstY1 = Math.min(srcY1, clipY1);

        if (dstX1 <= dstX0 || dstY1 <= dstY0) {
            return;
        }

        float u0 = (dstX0 - x) / width;
        float v0 = (dstY0 - y) / height;
        float u1 = (dstX1 - x) / width;
        float v1 = (dstY1 - y) / height;

        VertexConsumer buffer = buf.getBuffer(RenderType.text(texture));
        Matrix4f m = ps.last().pose();
        float z = IMAGE_LAYER_Z;

        buffer.vertex(m, dstX0, dstY0, z).color(255, 255, 255, 255).uv(u0, v0).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY0, z).color(255, 255, 255, 255).uv(u1, v0).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY1, z).color(255, 255, 255, 255).uv(u1, v1).uv2(light).endVertex();
        buffer.vertex(m, dstX0, dstY1, z).color(255, 255, 255, 255).uv(u0, v1).uv2(light).endVertex();

        buffer.vertex(m, dstX0, dstY0, z).color(255, 255, 255, 255).uv(u0, v0).uv2(light).endVertex();
        buffer.vertex(m, dstX0, dstY1, z).color(255, 255, 255, 255).uv(u0, v1).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY1, z).color(255, 255, 255, 255).uv(u1, v1).uv2(light).endVertex();
        buffer.vertex(m, dstX1, dstY0, z).color(255, 255, 255, 255).uv(u1, v0).uv2(light).endVertex();
    }

    @Nullable
    private static CachedImage getOrCreateCachedImage(String cacheKey, byte[] pngBytes) {
        CachedImage cached = IMAGE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        try {
            NativeImage image = NativeImage.read(new ByteArrayInputStream(pngBytes));
            if (image == null) {
                return null;
            }

            int width = image.getWidth();
            int height = image.getHeight();

            DynamicTexture texture = new DynamicTexture(image);
            ResourceLocation location = Minecraft.getInstance().getTextureManager().register(
                    "crazyae2addons_display_image_" + cacheKey,
                    texture
            );

            CachedImage created = new CachedImage(texture, location, width, height);
            IMAGE_CACHE.put(cacheKey, created);
            return created;
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to create display image texture", e);
            return null;
        }
    }

    private static String imageCacheKey(String imageId, byte[] pngBytes) {
        return imageId + "_" + pngBytes.length + "_" + Arrays.hashCode(pngBytes);
    }
}