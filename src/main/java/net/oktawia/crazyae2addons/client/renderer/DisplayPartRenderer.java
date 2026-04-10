package net.oktawia.crazyae2addons.client.renderer;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.oktawia.crazyae2addons.logic.display.DisplayGrid;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData;
import net.oktawia.crazyae2addons.logic.display.DisplayRenderData.*;
import net.oktawia.crazyae2addons.parts.DisplayPart;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.*;

@OnlyIn(Dist.CLIENT)
public final class DisplayPartRenderer {

    private static final float RENDER_DIST_SQ = 128f * 128f;

    private DisplayPartRenderer() {}

    public static void onRender(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        PoseStack ps = event.getPoseStack();
        Vec3 cam = mc.gameRenderer.getMainCamera().getPosition();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();
        Frustum frustum = event.getFrustum();

        boolean removedAny = DisplayPart.CLIENT_INSTANCES.removeIf(p -> {
            var be = p.getBlockEntity();
            return be == null || be.isRemoved() || be.getLevel() != mc.level;
        });

        if (removedAny) {
            DisplayGrid.invalidateClientCache();
        }

        Set<DisplayPart> visited = new HashSet<>();

        for (DisplayPart part : DisplayPart.CLIENT_INSTANCES) {
            if (visited.contains(part)) continue;

            DisplayGrid.RenderGroup group = DisplayGrid.getRenderGroup(part);
            Set<DisplayPart> grid = group.parts();

            if (grid.isEmpty()) {
                visited.add(part);
                continue;
            }

            DisplayPart renderOrigin = group.renderOrigin();
            boolean alreadyRendered = visited.contains(renderOrigin);

            visited.add(part);
            visited.addAll(grid);

            if (alreadyRendered) continue;

            AABB aabb = group.aabb();
            Vec3 center = aabb.getCenter();
            if (center.distanceToSqr(cam) > RENDER_DIST_SQ) continue;
            if (!frustum.isVisible(aabb)) continue;

            renderMatrix(renderOrigin, grid, ps, buf, cam);
        }
    }

    private static void renderMatrix(DisplayPart renderOrigin, Set<DisplayPart> grid,
                                     PoseStack ps, MultiBufferSource.BufferSource buf, Vec3 cam) {
        List<DisplayPart> parts = new ArrayList<>(grid);

        if (renderOrigin.textValue == null || renderOrigin.textValue.isEmpty()) return;
        if (!renderOrigin.isPowered()) return;

        Direction side = renderOrigin.getSide();
        var dims = DisplayGrid.getGridSize(parts, side);
        int widthBlocks = dims.getFirst();
        int heightBlocks = dims.getSecond();

        Font font = Minecraft.getInstance().font;

        String resolved = DisplayRenderData.resolveTokensClientSide(renderOrigin.textValue, renderOrigin.resolvedTokens);
        RichTextWithColors parsed = DisplayRenderData.parseStyledTextWithIcons(resolved);
        List<RenderLine> renderLines = parsed.lines();
        Integer bgColor = parsed.backgroundColor();

        float maxLineWidth = 1f;
        float totalTextHeight = 0f;
        for (RenderLine ln : renderLines) {
            maxLineWidth = Math.max(maxLineWidth, DisplayRenderData.renderLineWidthPx(font, ln));
            totalTextHeight += DisplayRenderData.renderLineHeightPx(font, ln);
        }

        float pxW = 64f * widthBlocks;
        float pxH = 64f * heightBlocks;

        float marginFrac = renderOrigin.margin ? 0.03f : 0.0f;
        float marginX = pxW * marginFrac;
        float marginY = pxH * marginFrac;
        float usableW = pxW - 2f * marginX;
        float usableH = pxH - 2f * marginY;

        float scale = (1f / 64f) * Math.min(
                usableW / Math.max(1f, maxLineWidth),
                usableH / Math.max(1f, totalTextHeight)
        );

        BlockPos originPos = renderOrigin.getBlockEntity().getBlockPos();

        if (bgColor != null) {
            ps.pushPose();
            ps.translate(originPos.getX() - cam.x, originPos.getY() - cam.y, originPos.getZ() - cam.z);
            applyFacingTransform(ps, renderOrigin);
            ps.translate(0, 0, 0.51f);
            drawBackground(ps, buf, widthBlocks, heightBlocks, 0xF000F0, 0xFF000000 | bgColor);
            ps.popPose();
            buf.endBatch(RenderType.textBackground());
        }

        ps.pushPose();
        ps.translate(originPos.getX() - cam.x, originPos.getY() - cam.y, originPos.getZ() - cam.z);
        applyFacingTransform(ps, renderOrigin);
        ps.translate(0, 0, 0.52f);
        ps.translate(marginX / 64f, -marginY / 64f, 0);
        ps.scale(scale, -scale, scale);

        float availTextW = (usableW / 64f) / scale;
        float availTextH = (usableH / 64f) / scale;

        final float EPSILON = 0.05f;
        List<DrawEntry> drawPlan = new ArrayList<>();
        float remainingH = availTextH;

        for (RenderLine ln : renderLines) {
            if (remainingH <= 0f) break;

            if (ln instanceof StyledLine sl) {
                float lh = font.lineHeight * sl.scaleMul();
                if (lh > remainingH + EPSILON) break;
                drawPlan.add(new DrawEntry(ln, 1));
                remainingH -= lh;
            } else if (ln instanceof TableBlock tb) {
                float rowH = font.lineHeight * tb.scaleMul();
                int rowsFit = Math.min((int) Math.floor((remainingH + EPSILON) / rowH), tb.rows().size());
                if (rowsFit <= 0) break;
                drawPlan.add(new DrawEntry(ln, rowsFit));
                remainingH -= rowsFit * rowH;
            }
        }

        if (renderOrigin.center) {
            float drawnH = 0f;
            for (DrawEntry de : drawPlan) {
                if (de.line() instanceof StyledLine sl) {
                    drawnH += font.lineHeight * sl.scaleMul();
                } else if (de.line() instanceof TableBlock tb) {
                    drawnH += de.tableRowsToDraw() * font.lineHeight * tb.scaleMul();
                }
            }
            ps.translate(0, Math.max(0f, (availTextH - drawnH) / 2f), 0);
        }

        float yCursor = 0f;
        for (DrawEntry de : drawPlan) {
            RenderLine ln = de.line();

            if (ln instanceof StyledLine sl) {
                float lineScale = sl.scaleMul();
                float lineW = DisplayRenderData.renderLineWidthPx(font, sl);
                float xOffset = renderOrigin.center ? Math.max(0f, (availTextW - lineW) / 2f) : 0f;

                ps.pushPose();
                ps.translate(xOffset, yCursor, 0);
                ps.scale(lineScale, lineScale, 1f);
                drawLineSegments(font, sl.segs(), ps, buf, 0xF000F0, 0f, 0f);
                ps.popPose();

                yCursor += font.lineHeight * lineScale;
            } else if (ln instanceof TableBlock tb) {
                float blockW = renderLineWidthPx(font, tb);
                float xOffset = renderOrigin.center ? Math.max(0f, (availTextW - blockW) / 2f) : 0f;

                ps.pushPose();
                ps.translate(xOffset, yCursor, 0);
                ps.scale(tb.scaleMul(), tb.scaleMul(), 1f);
                drawTableBlock(font, tb, de.tableRowsToDraw(), ps, buf, 0xF000F0);
                ps.popPose();

                yCursor += de.tableRowsToDraw() * font.lineHeight * tb.scaleMul();
            }
        }

        ps.popPose();
        buf.endBatch();
    }

    private record Transformation(float tx, float ty, float tz, float yRot, float xRot) {}

    private static void applyFacingTransform(PoseStack ps, DisplayPart part) {
        Transformation t = getFacingTransformation(part.getSide());
        ps.translate(t.tx, t.ty, t.tz);
        ps.mulPose(Axis.YP.rotationDegrees(t.yRot));
        ps.mulPose(Axis.XP.rotationDegrees(t.xRot));
        if (t.xRot != 0) applySpinTransformation(ps, part, t.xRot);
    }

    private static Transformation getFacingTransformation(Direction facing) {
        return switch (facing) {
            case SOUTH -> new Transformation(0, 1, 0.5f, 0f, 0f);
            case WEST  -> new Transformation(0.5f, 1, 0, -90f, 0f);
            case EAST  -> new Transformation(0.5f, 1, 1, 90f, 0f);
            case NORTH -> new Transformation(1, 1, 0.5f, 180f, 0f);
            case UP    -> new Transformation(0, 0.5f, 0, 0f, -90f);
            case DOWN  -> new Transformation(1, 0.5f, 0, 0f, 90f);
        };
    }

    private static void applySpinTransformation(PoseStack ps, DisplayPart part, float xRot) {
        float spin = 0f;
        if (xRot == 90f) {
            switch (part.spin) {
                case 0 -> { spin = 0f; ps.translate(-1, 1, 0); }
                case 1 -> { spin = 90f; ps.translate(-1, 0, 0); }
                case 2 -> { spin = 180f; ps.translate(0, 0, 0); }
                case 3 -> { spin = -90f; ps.translate(0, 1, 0); }
            }
        } else {
            switch (part.spin) {
                case 0 -> { spin = 0f; ps.translate(0, 0, 0); }
                case 1 -> { spin = -90f; ps.translate(1, 0, 0); }
                case 2 -> { spin = 180f; ps.translate(1, -1, 0); }
                case 3 -> { spin = 90f; ps.translate(0, -1, 0); }
            }
        }
        ps.mulPose(Axis.ZP.rotationDegrees(spin));
    }

    private static void drawBackground(PoseStack ps, MultiBufferSource buf,
                                       int blocksWide, int blocksHigh, int light, int color) {
        var buffer = buf.getBuffer(RenderType.textBackground());
        Matrix4f m = ps.last().pose();
        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>> 8) & 0xFF) / 255f;
        float b = ((color) & 0xFF) / 255f;

        buffer.addVertex(m, 0f, -(float) blocksHigh, 0f).setColor(r, g, b, a).setUv(0, 0).setLight(light);
        buffer.addVertex(m, (float) blocksWide, -(float) blocksHigh, 0f).setColor(r, g, b, a).setUv(0, 0).setLight(light);
        buffer.addVertex(m, (float) blocksWide, 0f, 0f).setColor(r, g, b, a).setUv(0, 0).setLight(light);
        buffer.addVertex(m, 0f, 0f, 0f).setColor(r, g, b, a).setUv(0, 0).setLight(light);
    }

    private static void drawSolidRect(PoseStack ps, MultiBufferSource buf, int light, int argb,
                                      float x0, float y0, float x1, float y1, float z) {
        var buffer = buf.getBuffer(RenderType.textBackground());
        Matrix4f m = ps.last().pose();
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = ((argb) & 0xFF) / 255f;

        buffer.addVertex(m, x0, y1, z).setColor(r, g, b, a).setUv(0, 0).setLight(light);
        buffer.addVertex(m, x1, y1, z).setColor(r, g, b, a).setUv(0, 0).setLight(light);
        buffer.addVertex(m, x1, y0, z).setColor(r, g, b, a).setUv(0, 0).setLight(light);
        buffer.addVertex(m, x0, y0, z).setColor(r, g, b, a).setUv(0, 0).setLight(light);
    }

    private static void drawSpriteQuad(PoseStack ps, MultiBufferSource buf, int light, int argb,
                                       float x, float y, float sizePx,
                                       net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {
        var buffer = buf.getBuffer(RenderType.text(InventoryMenu.BLOCK_ATLAS));
        Matrix4f m = ps.last().pose();
        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>> 8) & 0xFF) / 255f;
        float b = ((argb) & 0xFF) / 255f;

        float x0 = x, y0 = y, x1 = x + sizePx, y1 = y + sizePx, z = 0.001f;
        float u0 = sprite.getU0(), u1 = sprite.getU1(), v0 = sprite.getV0(), v1 = sprite.getV1();

        buffer.addVertex(m, x0, y0, z).setColor(r, g, b, a).setUv(u0, v0).setLight(light);
        buffer.addVertex(m, x1, y0, z).setColor(r, g, b, a).setUv(u1, v0).setLight(light);
        buffer.addVertex(m, x1, y1, z).setColor(r, g, b, a).setUv(u1, v1).setLight(light);
        buffer.addVertex(m, x0, y1, z).setColor(r, g, b, a).setUv(u0, v1).setLight(light);

        buffer.addVertex(m, x0, y0, z).setColor(r, g, b, a).setUv(u0, v0).setLight(light);
        buffer.addVertex(m, x0, y1, z).setColor(r, g, b, a).setUv(u0, v1).setLight(light);
        buffer.addVertex(m, x1, y1, z).setColor(r, g, b, a).setUv(u1, v1).setLight(light);
        buffer.addVertex(m, x1, y0, z).setColor(r, g, b, a).setUv(u1, v0).setLight(light);
    }

    private static void renderItemFlattened(net.minecraft.world.item.ItemStack stack,
                                            PoseStack ps, MultiBufferSource buf,
                                            int light, float x, float y, int iconPx) {
        var mc = Minecraft.getInstance();
        ps.pushPose();
        ps.translate(x, y, 0.001f);
        ps.translate(iconPx / 2f, iconPx / 2f, 0f);
        ps.scale(1f, -1f, 1f);
        float zFlatten = 1f / 256f;
        ps.scale(iconPx, iconPx, iconPx * zFlatten);

        RenderSystem.disableCull();
        mc.getItemRenderer().renderStatic(stack, ItemDisplayContext.GUI, light,
                OverlayTexture.NO_OVERLAY, ps, buf, mc.level, 0);
        RenderSystem.enableCull();
        ps.popPose();
    }

    private static net.minecraft.client.renderer.texture.TextureAtlasSprite getFluidSprite(FluidStack fs) {
        var ext = IClientFluidTypeExtensions.of(fs.getFluid());
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture());
    }

    private static int getFluidTint(FluidStack fs) {
        return IClientFluidTypeExtensions.of(fs.getFluid()).getTintColor();
    }

    private static void drawLineSegments(Font font, List<LineSeg> segs, PoseStack ps,
                                         MultiBufferSource buf, int light, float x, float y) {
        float cursor = x;
        float iconSize = font.lineHeight;
        int iconAdv = font.lineHeight + 1;

        for (LineSeg s : segs) {
            if (s instanceof TextSeg ts) {
                Component c = ts.c();
                font.drawInBatch(c, cursor, y, 0xFFFFFF, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
                cursor += font.width(c);
            } else if (s instanceof ItemIconSeg is) {
                renderItemFlattened(is.stack(), ps, buf, light, cursor, y, (int) iconSize);
                cursor += iconAdv;
            } else if (s instanceof FluidIconSeg fs) {
                var sprite = getFluidSprite(fs.stack());
                int tint = getFluidTint(fs.stack());
                drawSpriteQuad(ps, buf, light, tint, cursor, y, iconSize, sprite);
                cursor += iconAdv;
            }
        }
    }

    private static void drawTableBlock(Font font, TableBlock tb, int rowsToDraw,
                                       PoseStack ps, MultiBufferSource buf, int light) {
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
        float z = -0.01f;
        float xLine0 = layout.prefixW();
        float xLine1 = layout.totalW();

        drawSolidRect(ps, buf, light, lineColor, xLine0, -1f, xLine1, 0f, z);
        drawSolidRect(ps, buf, light, lineColor, xLine0, rowsToDraw * rowH - 1f, xLine1, rowsToDraw * rowH, z);
        if (rowsToDraw > 1) {
            drawSolidRect(ps, buf, light, lineColor, xLine0, rowH - 1f, xLine1, rowH, z);
        }

        int drawRows = Math.min(rowsToDraw, tb.rows().size());
        for (int r = 0; r < drawRows; r++) {
            TableRow row = tb.rows().get(r);
            float y = r * rowH;
            float x = 0f;

            if (indent != null) {
                font.drawInBatch(indent, 0f, y, 0xFFFFFF, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
                x += layout.prefixW();
            }

            font.drawInBatch(bar, x, y, 0xFFFFFF, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
            x += barW;

            for (int c = 0; c < cols; c++) {
                List<LineSeg> cell = (c < row.cells().size()) ? row.cells().get(c) : List.of();
                int cellW = (cell == null || cell.isEmpty()) ? 0 : DisplayRenderData.segsWidthPx(font, cell);
                int innerW = colW[c];
                int a = (tb.align() != null && c < tb.align().length) ? tb.align()[c] : 1;

                float contentX = switch (a) {
                    case 0 -> x + pad;
                    case 2 -> x + pad + Math.max(0f, innerW - cellW);
                    default -> x + pad + Math.max(0f, (innerW - cellW) / 2f);
                };

                drawLineSegments(font, cell, ps, buf, light, contentX, y);

                x += innerW + pad * 2;
                font.drawInBatch(bar, x, y, 0xFFFFFF, false, ps.last().pose(), buf, Font.DisplayMode.NORMAL, 0, light);
                x += barW;
            }
        }
    }

    private static float renderLineWidthPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) return DisplayRenderData.segsWidthPx(font, sl.segs()) * sl.scaleMul();
        if (ln instanceof TableBlock tb) return DisplayRenderData.computeTableLayout(font, tb).totalW() * tb.scaleMul();
        return 1f;
    }
}