package net.oktawia.crazyae2addons.client.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.menus.block.penrose.PenroseControllerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class BlackHoleWidget extends AbstractWidget {

    private static final double DISK_FULL_AT_MU =
            Math.max(1L, CrazyConfig.COMMON.PenroseDiskRenderFullAtMu.get());

    private static final int PAD = 6;
    private static final int BAR_H = 14;
    private static final int BAR_GAP = 2;
    private static final int PREVIEW_TO_BARS_GAP = 4;

    private static final float BH_CENTER_X = 0.70f;
    private static final float BH_CENTER_Y = 0.50f;
    private static final float BH_HOLE_R_FACTOR = 0.46f / 3.0f;
    private static final float DISK_Y_SCALE = 0.86f;

    private final PenroseControllerMenu menu;

    public BlackHoleWidget(PenroseControllerMenu menu) {
        super(0, 0, 0, 0, Component.empty());
        this.menu = menu;
        this.active = true;
        this.visible = true;
    }

    public @Nullable List<Component> getTooltipLines(int mouseX, int mouseY) {
        if (!visible || !isMouseOver(mouseX, mouseY)) return null;

        var r = computeRects();

        if (inside(mouseX, mouseY, r.massBarX, r.massBarY, r.barW, BAR_H)) {
            return tooltipMassBar();
        }

        if (inside(mouseX, mouseY, r.heatBarX, r.heatBarY, r.barW, BAR_H)) {
            return tooltipHeatBar();
        }

        if (inside(mouseX, mouseY, r.previewX, r.previewY, r.previewW, r.previewH)) {
            DiskGeom g = computePreviewDiskGeom(r);
            float dx = mouseX - g.cx;
            float dy = mouseY - g.cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            if (dist <= g.shadowR * 1.03f) return tooltipBlackHole();
            if (dist >= g.diskInnerR && dist <= g.diskOuterR) return tooltipDisk();
            return tooltipOverview();
        }

        return null;
    }

    @Override
    protected void renderWidget(GuiGraphics gg, int mouseX, int mouseY, float partialTick) {
        if (!visible) return;

        var r = computeRects();

        drawPanel(gg, getX(), getY(), width, height);
        gg.fill(r.previewX, r.previewY, r.previewX + r.previewW, r.previewY + r.previewH, 0xFF0B0C10);
        drawSoftVignette(gg, r.previewX, r.previewY, r.previewW, r.previewH);

        float t = getAnimTime(partialTick);

        drawAccretionDisk(gg, r, t);
        drawBlackHole(gg, r);
        drawOverlayText(gg, r.previewX + 8, r.previewY + 6);

        drawMassBar(gg, r.massBarX, r.massBarY, r.barW, BAR_H, mouseX, mouseY);
        drawHeatBar(gg, r.heatBarX, r.heatBarY, r.barW, BAR_H, mouseX, mouseY);

        if (inside(mouseX, mouseY, r.previewX, r.previewY, r.previewW, r.previewH)) {
            gg.fill(r.previewX, r.previewY, r.previewX + r.previewW, r.previewY + 1, 0x55FFFFFF);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput ignored) {
    }

    private List<Component> tooltipBlackHole() {
        var host = menu.getHost();
        var out = new ArrayList<Component>();

        out.add(Component.translatable(LangDefs.PENROSE_TITLE_BLACK_HOLE.getTranslationKey())
                .withStyle(ChatFormatting.GOLD));

        double massRatio = host.isBlackHoleActive()
                ? ((double) host.getBlackHoleMass() / (double) hostInitialMass())
                : 0.0;

        out.add(Component.translatable(
                LangDefs.PENROSE_BH_MASS_HEAT.getTranslationKey(),
                String.format("%.4f", massRatio),
                String.format("%.0fGK", host.getHeat())
        ));

        long delta = host.getLastSecondMassDelta();
        double deltaRatio = (host.isBlackHoleActive() && host.getBlackHoleMass() > 0)
                ? ((double) delta / (double) host.getBlackHoleMass() * 100.0)
                : 0.0;

        out.add(Component.translatable(
                LangDefs.PENROSE_BH_MASS_DELTA.getTranslationKey(),
                Utils.shortenNumber(delta),
                String.format("%.4f%%", deltaRatio)
        ));

        return out;
    }

    private List<Component> tooltipDisk() {
        var host = menu.getHost();
        var out = new ArrayList<Component>();

        out.add(Component.translatable(LangDefs.PENROSE_TITLE_ACCRETION_DISK.getTranslationKey())
                .withStyle(ChatFormatting.GOLD));

        out.add(Component.translatable(
                LangDefs.PENROSE_DISK_MASS.getTranslationKey(),
                Utils.shortenNumber(host.diskMassSingu)
        ).withStyle(ChatFormatting.GRAY));

        out.add(Component.empty());
        out.add(Component.translatable(
                LangDefs.GEN_FE_PER_TICK.getTranslationKey(),
                Utils.shortenNumber(host.getLastGeneratedFePerTickGross())
        ).withStyle(ChatFormatting.AQUA));
        out.add(Component.translatable(
                LangDefs.USE_FE_PER_TICK.getTranslationKey(),
                Utils.shortenNumber(host.getLastConsumedFePerTick())
        ).withStyle(ChatFormatting.RED));
        out.add(Component.translatable(
                LangDefs.PENROSE_FE_IN_DISK.getTranslationKey(),
                Utils.shortenNumber(host.getStoredEnergyInDisk())
        ).withStyle(ChatFormatting.AQUA));

        return out;
    }

    private List<Component> tooltipOverview() {
        var host = menu.getHost();
        var out = new ArrayList<Component>();

        out.add(Component.translatable(LangDefs.OVERVIEW.getTranslationKey())
                .withStyle(ChatFormatting.GOLD));

        out.add(Component.translatable(
                LangDefs.STORED_FE.getTranslationKey(),
                Utils.shortenNumber(host.getStoredEnergy())
        ));

        out.add(Component.translatable(
                LangDefs.PENROSE_DISK_MASS.getTranslationKey(),
                Utils.shortenNumber(host.diskMassSingu)
        ).withStyle(ChatFormatting.GRAY));

        out.add(Component.translatable(
                LangDefs.PENROSE_HEAT_GK.getTranslationKey(),
                String.format("%.0f", host.getHeat())
        ).withStyle(ChatFormatting.GRAY));

        out.add(Component.translatable(
                LangDefs.GEN_FE_PER_TICK.getTranslationKey(),
                Utils.shortenNumber(host.getLastGeneratedFePerTickGross())
        ).withStyle(ChatFormatting.AQUA));

        out.add(Component.translatable(
                LangDefs.USE_FE_PER_TICK.getTranslationKey(),
                Utils.shortenNumber(host.getLastConsumedFePerTick())
        ).withStyle(ChatFormatting.RED));

        return out;
    }

    private List<Component> tooltipMassBar() {
        var host = menu.getHost();
        var out = new ArrayList<Component>();

        out.add(Component.translatable(LangDefs.PENROSE_TITLE_MASS.getTranslationKey())
                .withStyle(ChatFormatting.GOLD));

        out.add(Component.translatable(
                LangDefs.PENROSE_MASS_CURRENT_MU.getTranslationKey(),
                Utils.shortenNumber(host.getBlackHoleMass())
        ).withStyle(ChatFormatting.GRAY));

        out.add(Component.translatable(
                LangDefs.PENROSE_MASS_INITIAL_MU.getTranslationKey(),
                Utils.shortenNumber(hostInitialMass())
        ).withStyle(ChatFormatting.DARK_GRAY));

        out.add(Component.translatable(
                LangDefs.PENROSE_MASS_MAX_MU.getTranslationKey(),
                Utils.shortenNumber(hostMaxMass())
        ).withStyle(ChatFormatting.DARK_GRAY));

        out.add(Component.translatable(
                LangDefs.EFFICIENCY.getTranslationKey(),
                String.format("%.1f%%", getSweetEfficiency01() * 100.0)
        ).withStyle(ChatFormatting.GREEN));

        return out;
    }

    private List<Component> tooltipHeatBar() {
        var host = menu.getHost();
        var out = new ArrayList<Component>();

        out.add(Component.translatable(LangDefs.HEAT.getTranslationKey())
                .withStyle(ChatFormatting.GOLD));

        out.add(Component.translatable(
                LangDefs.PENROSE_HEAT_GK.getTranslationKey(),
                String.format("%.0f", host.getHeat())
        ).withStyle(ChatFormatting.GRAY));

        out.add(Component.translatable(
                LangDefs.PENROSE_HEAT_MAX_GK.getTranslationKey(),
                String.format("%.0f", hostMaxHeatGK())
        ).withStyle(ChatFormatting.DARK_GRAY));

        out.add(Component.translatable(
                LangDefs.EFFICIENCY.getTranslationKey(),
                String.format("%.1f%%", heatEfficiency(host.getHeat()) * 100.0)
        ).withStyle(ChatFormatting.GREEN));

        return out;
    }

    private void drawPanel(GuiGraphics gg, int x, int y, int w, int h) {
        gg.fill(x + 2, y + 2, x + w + 2, y + h + 2, 0x55000000);
        gg.fill(x, y, x + w, y + h, 0xFF111217);
        gg.fill(x, y, x + w, y + 1, 0xFF2A2D3A);
        gg.fill(x, y + h - 1, x + w, y + h, 0xFF07080C);
        gg.fill(x, y, x + 1, y + h, 0xFF2A2D3A);
        gg.fill(x + w - 1, y, x + w, y + h, 0xFF07080C);
    }

    private void drawSoftVignette(GuiGraphics gg, int x, int y, int w, int h) {
        gg.fillGradient(x, y, x + w, y + h, 0x22000000, 0x88000000);
        gg.fillGradient(x, y, x + w, y + h / 2, 0x11000000, 0x00000000);
    }

    private void drawOverlayText(GuiGraphics gg, int x, int y) {
        var host = menu.getHost();
        Font font = Minecraft.getInstance().font;

        gg.pose().pushPose();
        gg.pose().translate(x, y, 0);
        gg.pose().scale(0.82f, 0.82f, 1);

        int yy = 0;
        int line = font.lineHeight + 3;

        drawTS(gg, font, tr(
                LangDefs.PENROSE_OVERLAY_BH_MASS.getTranslationKey(),
                Utils.shortenNumber(host.getBlackHoleMass())
        ), 0, yy);
        yy += line;

        drawTS(gg, font, tr(
                LangDefs.PENROSE_OVERLAY_DISK_MASS.getTranslationKey(),
                Utils.shortenNumber(host.diskMassSingu)
        ), 0, yy);
        yy += line;

        drawTS(gg, font, tr(
                LangDefs.GEN_FE_PER_TICK.getTranslationKey(),
                Utils.shortenNumber(host.getLastGeneratedFePerTickGross())
        ), 0, yy);
        yy += line;

        drawTS(gg, font, tr(
                LangDefs.USE_FE_PER_TICK.getTranslationKey(),
                Utils.shortenNumber(host.getLastConsumedFePerTick())
        ), 0, yy);
        yy += line;

        drawTS(gg, font, tr(
                LangDefs.PENROSE_OVERLAY_FE_STORED.getTranslationKey(),
                Utils.shortenNumber(host.getStoredEnergy())
        ), 0, yy);
        yy += line;

        drawTS(gg, font, tr(
                LangDefs.PENROSE_OVERLAY_FE_IN_DISK.getTranslationKey(),
                Utils.shortenNumber(host.getStoredEnergyInDisk())
        ), 0, yy);

        gg.pose().popPose();
    }

    private static String tr(String key, Object... args) {
        return Component.translatable(key, args).getString();
    }

    private void drawTS(GuiGraphics gg, Font font, String s, int x, int y) {
        gg.drawString(font, s, x + 1, y + 1, 0xAA000000, false);
        gg.drawString(font, s, x, y, 0xEDEDED, false);
    }

    private void drawBlackHole(GuiGraphics gg, Rects r) {
        DiskGeom g = computePreviewDiskGeom(r);
        drawSolidCircle(gg, g.cx, g.cy, g.shadowR, 96, 0xFF000000);
    }

    private void drawAccretionDisk(GuiGraphics gg, Rects r, float t) {
        float massP = diskMass01();
        if (massP <= 0.0001f) return;

        DiskGeom g = computePreviewDiskGeom(r);
        if (g.diskOuterR - g.diskInnerR < 2.0f) return;

        drawDiskAsConstantRings(gg, g, t);
    }

    private void drawDiskAsConstantRings(GuiGraphics gg, DiskGeom g, float t) {
        var host = menu.getHost();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        float heatP = (float) Mth.clamp(host.getHeat() / hostMaxHeatGK(), 0.0, 1.0);
        float massP = diskMass01();

        int baseA = host.isBlackHoleActive() ? 235 : 120;
        int alphaBase = (int) Mth.clamp(baseA * (0.30f + 0.70f * massP), 0f, 255f);

        float inner = g.diskInnerR;
        float outer = g.diskOuterR;
        float totalW = outer - inner;

        final float ringW = 1.8f;
        final float overlap = 0.55f;
        int ringCount = Mth.clamp((int) Math.ceil(totalW / ringW), 1, 512);

        final float armCount = 2.0f;
        final float twistPerPx = 0.95f;
        final float warpAmp = 0.010f;
        final float radialHeatK = 6.2f;
        final float centerBoost = 0.70f - 0.25f * heatP;
        final float edgeCool = 0.10f + 0.28f * heatP;
        float rot = t * 0.26f;

        int seg = 320;

        for (int ring = 0; ring < ringCount; ring++) {
            float rI = inner + ring * ringW - (overlap * 0.5f);
            float rO = rI + ringW + overlap;
            if (rO <= inner) continue;
            if (rI < inner) rI = inner;
            if (rO > outer) rO = outer;
            if (rI >= outer) break;
            if (rO - rI <= 0.001f) continue;

            float radialP01Mid = totalW <= 0.0001f
                    ? 0f
                    : Mth.clamp(((rI + rO) * 0.5f - inner) / totalW, 0f, 1f);
            int alpha = (int) (alphaBase * (1.0f - 0.55f * radialP01Mid));

            Matrix4f m = gg.pose().last().pose();
            BufferBuilder bb = Tesselator.getInstance().begin(
                    VertexFormat.Mode.TRIANGLE_STRIP,
                    DefaultVertexFormat.POSITION_COLOR
            );

            for (int i = 0; i <= seg; i++) {
                float ang = (float) ((Math.PI * 2.0) * i / seg);
                float ca = Mth.cos(ang);
                float sa = Mth.sin(ang);

                {
                    float radialPx = rO - inner;
                    float radialP01 = totalW <= 0.0001f ? 0f : Mth.clamp(radialPx / totalW, 0f, 1f);
                    float radialHot = (float) Math.exp(-radialHeatK * radialP01 * radialP01);
                    float heatAdj = Mth.clamp(
                            heatP + centerBoost * radialHot - edgeCool * (1.0f - radialHot),
                            0f, 1f
                    );
                    float spiralPhase = (ang * armCount) - (radialPx * twistPerPx) + (rot * 1.15f);
                    float arm = (float) Math.pow(0.5f + 0.5f * Mth.sin(spiralPhase), 3.0f);
                    float doppler = 0.55f + 0.45f * (float) Math.pow(
                            Math.max(0.0f, Mth.cos(ang + rot)),
                            2.2f
                    );
                    float inten = Mth.clamp(0.16f + 0.62f * doppler + 0.72f * arm, 0f, 1f)
                            * (0.72f + 0.28f * (1.0f - radialHot));
                    inten = Mth.clamp(inten, 0f, 1f);
                    float warp = 1.0f
                            + warpAmp * Mth.sin(spiralPhase)
                            + (warpAmp * 0.55f) * Mth.sin(spiralPhase * 2.0f);
                    int col = diskColor(heatAdj, radialP01, inten * 0.72f, (int) (alpha * 0.92f));

                    bb.addVertex(m, g.cx + ca * (rO * warp), g.cy + sa * (rO * warp) * DISK_Y_SCALE, 0)
                            .setColor(col);
                }

                {
                    float radialPx = rI - inner;
                    float radialP01 = totalW <= 0.0001f ? 0f : Mth.clamp(radialPx / totalW, 0f, 1f);
                    float radialHot = (float) Math.exp(-radialHeatK * radialP01 * radialP01);
                    float heatAdj = Mth.clamp(
                            heatP + centerBoost * radialHot - edgeCool * (1.0f - radialHot),
                            0f, 1f
                    );
                    float spiralPhase = (ang * armCount) - (radialPx * twistPerPx) + (rot * 1.15f);
                    float arm = (float) Math.pow(0.5f + 0.5f * Mth.sin(spiralPhase), 3.0f);
                    float doppler = 0.55f + 0.45f * (float) Math.pow(
                            Math.max(0.0f, Mth.cos(ang + rot)),
                            2.2f
                    );
                    float inten = Mth.clamp(0.16f + 0.62f * doppler + 0.72f * arm, 0f, 1f)
                            * (0.72f + 0.28f * (1.0f - radialHot));
                    inten = Mth.clamp(inten, 0f, 1f);
                    float warp = 1.0f
                            + warpAmp * Mth.sin(spiralPhase)
                            + (warpAmp * 0.55f) * Mth.sin(spiralPhase * 2.0f);
                    int col = diskColor(heatAdj, radialP01, inten, alpha);

                    bb.addVertex(m, g.cx + ca * (rI * warp), g.cy + sa * (rI * warp) * DISK_Y_SCALE, 0)
                            .setColor(col);
                }
            }

            BufferUploader.drawWithShader(bb.buildOrThrow());
        }

        RenderSystem.disableBlend();
    }

    private int diskColor(float heatP, float radialP, float intensity, int alpha) {
        heatP = Mth.clamp(heatP, 0f, 1f);
        radialP = Mth.clamp(radialP, 0f, 1f);
        intensity = Mth.clamp(intensity, 0f, 1f);

        int cOrange = argb(255, 255, 140, 35);
        int cRed = argb(255, 255, 45, 25);
        int cPurple = argb(255, 155, 55, 235);
        int cBlue = argb(255, 60, 160, 255);
        int cWhiteHot = argb(255, 255, 245, 235);

        int base;
        if (heatP < 0.35f) {
            base = lerpColor(cOrange, cRed, heatP / 0.35f);
        } else if (heatP < 0.75f) {
            base = lerpColor(cRed, cPurple, (heatP - 0.35f) / 0.40f);
        } else {
            base = lerpColor(cPurple, cBlue, (heatP - 0.75f) / 0.25f);
        }

        float i = (float) Math.pow(intensity, 0.90f);
        float darkMul = 1.0f - 0.62f * (float) Math.pow(1.0f - i, 1.35f);
        float core = (float) Math.exp(-6.0f * radialP * radialP);
        float hot = Mth.clamp((heatP - 0.92f) / 0.08f, 0f, 1f);
        float coreMix = Mth.clamp(core + hot * 0.35f * core, 0f, 1f);
        float coreMixDetail = Mth.lerp(i, 1.0f, coreMix);
        base = lerpColor(base, cWhiteHot, coreMix);

        float radialMul = 0.62f + 0.38f * (1.0f - radialP);
        float bright = Mth.clamp((0.05f + 0.95f * coreMixDetail) * darkMul * radialMul, 0f, 1f);

        int br = (int) Mth.clamp(((base >>> 16) & 0xFF) * bright, 0, 255);
        int bg = (int) Mth.clamp(((base >>> 8) & 0xFF) * bright, 0, 255);
        int bb = (int) Mth.clamp((base & 0xFF) * bright, 0, 255);

        return argb(alpha, br, bg, bb);
    }

    private void drawMassBar(GuiGraphics gg, int x, int y, int w, int h, int mx, int my) {
        double progress = getMassProgress();

        drawBarBase(gg, x, y, w, h, inside(mx, my, x, y, w, h));

        int fillW = (int) Math.floor((w - 2) * progress);
        if (fillW > 0) {
            float hue = (float) (0.33 * (1.0 - progress));
            gg.fillGradient(
                    x + 1, y + 1, x + 1 + fillW, y + h - 1,
                    hsvToRgb(hue, 0.85f, 0.85f, 255),
                    hsvToRgb(hue, 1f, 1f, 255)
            );
        }

        drawCenteredBarLabel(
                gg,
                Component.translatable(
                        LangDefs.PENROSE_MASS_BAR.getTranslationKey(),
                        String.format("%.2f%%", progress * 100.0)
                ).getString(),
                x, y, w, h
        );
    }

    private void drawHeatBar(GuiGraphics gg, int x, int y, int w, int h, int mx, int my) {
        double maxHeat = hostMaxHeatGK();
        double progress = Mth.clamp(menu.getHost().getHeat() / maxHeat, 0.0, 1.0);

        drawBarBase(gg, x, y, w, h, inside(mx, my, x, y, w, h));

        int fillW = (int) Math.floor((w - 2) * progress);
        if (fillW > 0) {
            float hue = (float) (0.58 * (1.0 - progress));
            gg.fillGradient(
                    x + 1, y + 1, x + 1 + fillW, y + h - 1,
                    hsvToRgb(hue, 0.90f, 0.80f, 255),
                    hsvToRgb(hue, 1f, 1f, 255)
            );
        }

        drawCenteredBarLabel(
                gg,
                Component.translatable(
                        LangDefs.PENROSE_HEAT_BAR.getTranslationKey(),
                        String.format("%.0f", menu.getHost().getHeat()),
                        String.format("%.0f", maxHeat)
                ).getString(),
                x, y, w, h
        );
    }

    private void drawBarBase(GuiGraphics gg, int x, int y, int w, int h, boolean hover) {
        gg.fill(x, y, x + w, y + h, hover ? 0xFF1A1D27 : 0xFF141620);
        gg.fill(x, y, x + w, y + 1, 0xFF2A2D3A);
        gg.fill(x, y + h - 1, x + w, y + h, 0xFF07080C);
        gg.fill(x, y, x + 1, y + h, 0xFF2A2D3A);
        gg.fill(x + w - 1, y, x + w, y + h, 0xFF07080C);
        gg.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF2B2E38);
    }

    private void drawCenteredBarLabel(GuiGraphics gg, String s, int x, int y, int w, int h) {
        Font font = Minecraft.getInstance().font;
        int tx = x + (w - font.width(s)) / 2;
        int ty = y + (h - font.lineHeight) / 2;
        gg.drawString(font, s, tx + 1, ty + 1, 0xAA000000, false);
        gg.drawString(font, s, tx, ty, 0xFFFFFFFF, false);
    }

    private float diskMass01() {
        return (float) Mth.clamp(menu.getHost().diskMassSingu / DISK_FULL_AT_MU, 0.0, 1.0);
    }

    private double getMassProgress() {
        if (!menu.getHost().isBlackHoleActive()) return 0.0;

        long initialMass = hostInitialMass();
        long maxMass = hostMaxMass();
        if (maxMass <= initialMass) return 0.0;

        return Mth.clamp(
                (double) (menu.getHost().getBlackHoleMass() - initialMass) / (double) (maxMass - initialMass),
                0.0,
                1.0
        );
    }

    private double getSweetEfficiency01() {
        if (!menu.getHost().isBlackHoleActive()) return 0.0;

        long initialMass = hostInitialMass();
        long maxMass = hostMaxMass();

        double span = (double) (maxMass - initialMass);
        if (span <= 0.0) return 0.0;

        double sweet = (double) initialMass + span * 0.5;
        double half = span * 0.5;
        double current = (double) menu.getHost().getBlackHoleMass();

        return Mth.clamp(1.0 - (Math.abs(current - sweet) / half), 0.0, 1.0);
    }

    private double heatEfficiency(double heatGK) {
        double peak = hostHeatPeakGK();
        double x = heatGK / peak;
        return Mth.clamp(2.0 * x - x * x, 0.0, 1.0);
    }

    private float getAnimTime(float partialTick) {
        var mc = Minecraft.getInstance();
        long ticks = mc.level != null ? mc.level.getGameTime() : (System.currentTimeMillis() / 50L);
        return (ticks % 24000L) + partialTick;
    }

    private static void drawSolidCircle(GuiGraphics gg, float cx, float cy, float r, int segments, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f m = gg.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().begin(
                VertexFormat.Mode.TRIANGLES,
                DefaultVertexFormat.POSITION_COLOR
        );

        for (int i = 0; i < segments; i++) {
            double a0 = (Math.PI * 2.0) * i / segments;
            double a1 = (Math.PI * 2.0) * (i + 1) / segments;

            bb.addVertex(m, cx, cy, 0).setColor(argb);
            bb.addVertex(m, cx + (float) (Math.cos(a0) * r), cy + (float) (Math.sin(a0) * r), 0).setColor(argb);
            bb.addVertex(m, cx + (float) (Math.cos(a1) * r), cy + (float) (Math.sin(a1) * r), 0).setColor(argb);
        }

        BufferUploader.drawWithShader(bb.buildOrThrow());
        RenderSystem.disableBlend();
    }

    private static int lerpColor(int c0, int c1, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a = (int) (((c0 >>> 24) & 0xFF) + (((c1 >>> 24) & 0xFF) - ((c0 >>> 24) & 0xFF)) * t);
        int r = (int) (((c0 >>> 16) & 0xFF) + (((c1 >>> 16) & 0xFF) - ((c0 >>> 16) & 0xFF)) * t);
        int g = (int) (((c0 >>> 8) & 0xFF) + (((c1 >>> 8) & 0xFF) - ((c0 >>> 8) & 0xFF)) * t);
        int b = (int) (((c0) & 0xFF) + (((c1) & 0xFF) - ((c0) & 0xFF)) * t);
        return argb(a, r, g, b);
    }

    private static int hsvToRgb(float h, float s, float v, int alpha) {
        h = (h % 1f + 1f) % 1f;
        s = Mth.clamp(s, 0f, 1f);
        v = Mth.clamp(v, 0f, 1f);

        float c = v * s;
        float x = c * (1 - Math.abs((h * 6f) % 2 - 1));
        float mv = v - c;
        float hh = h * 6f;

        float rp = 0, gp = 0, bp = 0;
        if (hh < 1) {
            rp = c; gp = x;
        } else if (hh < 2) {
            rp = x; gp = c;
        } else if (hh < 3) {
            gp = c; bp = x;
        } else if (hh < 4) {
            gp = x; bp = c;
        } else if (hh < 5) {
            rp = x; bp = c;
        } else {
            rp = c; bp = x;
        }

        return argb(
                alpha,
                (int) ((rp + mv) * 255f),
                (int) ((gp + mv) * 255f),
                (int) ((bp + mv) * 255f)
        );
    }

    private static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < x + w && my < y + h;
    }

    private DiskGeom computePreviewDiskGeom(Rects r) {
        float cx = r.previewX + r.previewW * BH_CENTER_X;
        float cy = r.previewY + r.previewH * BH_CENTER_Y;

        float left = cx - r.previewX;
        float right = (r.previewX + r.previewW) - cx;
        float top = cy - r.previewY;
        float bottom = (r.previewY + r.previewH) - cy;

        float rad = Math.min(Math.min(left, right), Math.min(top, bottom));
        if (rad < 12f) {
            rad = Math.min(r.previewW, r.previewH) * 0.48f;
        }

        float maxR = rad;
        float bhHoleR = maxR * BH_HOLE_R_FACTOR;
        float shadowR = bhHoleR * 1.55f;
        float inner = shadowR * 1.02f;

        float massP = diskMass01();
        float outer = Math.min(
                inner + 1.2f + (maxR * 0.62f) * (float) Math.pow(massP, 0.90),
                maxR * 0.98f
        );

        return new DiskGeom(cx, cy, bhHoleR, shadowR, inner, outer);
    }

    private Rects computeRects() {
        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        int barW = Math.max(20, w - PAD * 2);
        int heatBarY = y + h - PAD - BAR_H;
        int massBarY = heatBarY - BAR_GAP - BAR_H;
        int previewH = Math.max(24, massBarY - PREVIEW_TO_BARS_GAP - (y + PAD));

        return new Rects(
                x + PAD, y + PAD, barW, previewH,
                x + PAD, massBarY,
                x + PAD, heatBarY,
                barW
        );
    }

    private long hostInitialMass() {
        return Math.max(1L, menu.getHost().initialBhMass);
    }

    private long hostMaxMass() {
        return Math.max(hostInitialMass(), menu.getHost().maxBhMass);
    }

    private double hostMaxHeatGK() {
        return Math.max(1.0, menu.getHost().maxHeatGK);
    }

    private double hostHeatPeakGK() {
        return Math.max(1.0, menu.getHost().heatPeakGk);
    }

    private record DiskGeom(float cx, float cy, float bhHoleR, float shadowR, float diskInnerR, float diskOuterR) {
    }

    private record Rects(
            int previewX, int previewY, int previewW, int previewH,
            int massBarX, int massBarY,
            int heatBarX, int heatBarY,
            int barW
    ) {
    }
}