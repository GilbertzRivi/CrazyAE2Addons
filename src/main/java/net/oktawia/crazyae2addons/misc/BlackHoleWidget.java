package net.oktawia.crazyae2addons.misc;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.menus.PenroseControllerMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class BlackHoleWidget extends AbstractWidget {

    public static final long INITIAL_BH_MASS = 268_435_456L;
    public static final long DEFAULT_MAX_BH_MASS = INITIAL_BH_MASS + 1_113_600L;
    public static final double MAX_HEAT_GK = 100_000.0;

    // pełny dysk (max grubość / max ringi) już przy 30k MU
    private static final double DISK_FULL_AT_MU = 30_000.0;

    private final PenroseControllerMenu menu;

    private long initialMass = INITIAL_BH_MASS;
    private long maxMass = DEFAULT_MAX_BH_MASS;

    private static final int PAD = 6;
    private static final int BAR_H = 14;
    private static final int BAR_GAP = 2;
    private static final int PREVIEW_TO_BARS_GAP = 4;

    // BH po prawej jak wcześniej
    private static final float BH_CENTER_X = 0.70f;
    private static final float BH_CENTER_Y = 0.50f;

    private static final float BH_HOLE_R_FACTOR = 0.46f / 3.0f;

    // 1px ring (bez subpixeli)
    private static final float DISK_RING_WIDTH_PX = 1.0f;

    // overlap żeby nie było czarnych szczelin na granicy ringów (przez warp)
    private static final float DISK_RING_OVERLAP_PX = 0.35f;

    // delikatny tilt
    private static final float DISK_Y_SCALE = 0.86f;

    public BlackHoleWidget(PenroseControllerMenu menu) {
        super(0, 0, 0, 0, Component.empty());
        this.menu = menu;
        this.active = true;
        this.visible = true;
    }

    public void setMassLimits(long initial, long max) {
        this.initialMass = Math.max(1L, initial);
        this.maxMass = Math.max(this.initialMass, max);
    }

    public @Nullable List<Component> getTooltipLines(int mouseX, int mouseY) {
        if (!this.visible) return null;
        if (!isMouseOver(mouseX, mouseY)) return null;

        var r = computeRects();
        int mx = mouseX;
        int my = mouseY;

        if (inside(mx, my, r.massBarX, r.massBarY, r.barW, BAR_H)) {
            return tooltipMassBar();
        }

        if (inside(mx, my, r.heatBarX, r.heatBarY, r.barW, BAR_H)) {
            return tooltipHeatBar();
        }

        if (inside(mx, my, r.previewX, r.previewY, r.previewW, r.previewH)) {
            DiskGeom g = computePreviewDiskGeom(r);

            float dx = mx - g.cx;
            float dy = my - g.cy;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);

            float holeHitR = g.shadowR * 1.03f;

            if (dist <= holeHitR) return tooltipBlackHole();
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
            gg.fill(r.previewX, r.previewY + r.previewH - 1, r.previewX + r.previewW, r.previewY + r.previewH, 0x22000000);
        }
    }

    @Override
    protected void updateWidgetNarration(@NotNull NarrationElementOutput ignored) {}

    // ---------------- Tooltips ----------------

    private List<Component> tooltipBlackHole() {
        var out = new ArrayList<Component>();
        out.add(Component.translatable("gui.crazyae2addons.penrose_title_black_hole").withStyle(ChatFormatting.GOLD));

        double massRatio = menu.blackHoleActive ? ((double) menu.bhMass / (double) INITIAL_BH_MASS) : 0.0;
        String massStr = String.format("%.4f", massRatio);
        String heatStr = String.format("%.0fGK", menu.heat);
        out.add(Component.translatable("gui.crazyae2addons.penrose_bh_mass_heat", massStr, heatStr));

        long delta = menu.massDeltaPerSec;
        String deltaPretty = Utils.shortenNumber(delta);
        double deltaRatio = (menu.blackHoleActive && menu.bhMass > 0) ? ((double) delta / (double) menu.bhMass * 100.0) : 0.0;
        String deltaPercentStr = String.format("%.4f%%", deltaRatio);
        out.add(Component.translatable("gui.crazyae2addons.penrose_bh_mass_delta", deltaPretty, deltaPercentStr));

        return out;
    }

    private List<Component> tooltipDisk() {
        var out = new ArrayList<Component>();
        out.add(Component.translatable("gui.crazyae2addons.penrose_title_accretion_disk").withStyle(ChatFormatting.GOLD));

        String diskMassPretty = Utils.shortenNumber(menu.diskMassSingu);
        out.add(Component.translatable("gui.crazyae2addons.penrose_line_disk_mass", diskMassPretty).withStyle(ChatFormatting.GRAY));

        long genGross = menu.feGeneratedGrossPerTick;
        long consumed = menu.feConsumedPerTick;
        long feInDisk = menu.storedEnergyInDisk;

        out.add(Component.empty());
        out.add(Component.translatable("gui.crazyae2addons.penrose_line_gen_t_fe", Utils.shortenNumber(genGross)).withStyle(ChatFormatting.AQUA));
        out.add(Component.translatable("gui.crazyae2addons.penrose_line_use_t_fe", Utils.shortenNumber(consumed)).withStyle(ChatFormatting.RED));
        out.add(Component.translatable("gui.crazyae2addons.penrose_line_fe_in_disk", Utils.shortenNumber(feInDisk)).withStyle(ChatFormatting.AQUA));

        return out;
    }

    private List<Component> tooltipOverview() {
        var out = new ArrayList<Component>();
        out.add(Component.translatable("gui.crazyae2addons.penrose_title_preview").withStyle(ChatFormatting.GOLD));

        String storedPretty = Utils.shortenNumber(menu.storedEnergy);
        out.add(Component.translatable("gui.crazyae2addons.penrose_bh_power", storedPretty));

        String diskMassPretty = Utils.shortenNumber(menu.diskMassSingu);
        out.add(Component.translatable("gui.crazyae2addons.penrose_line_disk_mass", diskMassPretty).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("gui.crazyae2addons.penrose_line_heat_mk", String.format("%.0f", menu.heat)).withStyle(ChatFormatting.GRAY));

        out.add(Component.translatable("gui.crazyae2addons.penrose_line_gen_t_fe", Utils.shortenNumber(menu.feGeneratedGrossPerTick)).withStyle(ChatFormatting.AQUA));
        out.add(Component.translatable("gui.crazyae2addons.penrose_line_use_t_fe", Utils.shortenNumber(menu.feConsumedPerTick)).withStyle(ChatFormatting.RED));
        return out;
    }

    private List<Component> tooltipMassBar() {
        var out = new ArrayList<Component>();
        out.add(Component.translatable("gui.crazyae2addons.penrose_title_mass").withStyle(ChatFormatting.GOLD));

        out.add(Component.translatable("gui.crazyae2addons.penrose_mass_current_mu", Utils.shortenNumber(menu.bhMass)).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("gui.crazyae2addons.penrose_mass_initial_mu", Utils.shortenNumber(initialMass)).withStyle(ChatFormatting.DARK_GRAY));
        out.add(Component.translatable("gui.crazyae2addons.penrose_mass_max_mu", Utils.shortenNumber(maxMass)).withStyle(ChatFormatting.DARK_GRAY));

        double eff = getSweetEfficiency01();
        out.add(Component.translatable("gui.crazyae2addons.penrose_efficiency", String.format("%.1f%%", eff * 100.0)).withStyle(ChatFormatting.GREEN));
        return out;
    }

    private List<Component> tooltipHeatBar() {
        var out = new ArrayList<Component>();
        out.add(Component.translatable("gui.crazyae2addons.penrose_title_heat").withStyle(ChatFormatting.GOLD));

        out.add(Component.translatable("gui.crazyae2addons.penrose_heat_current_gk", String.format("%.0f", menu.heat)).withStyle(ChatFormatting.GRAY));
        out.add(Component.translatable("gui.crazyae2addons.penrose_heat_max_gk", String.format("%.0f", MAX_HEAT_GK)).withStyle(ChatFormatting.DARK_GRAY));

        double eff = heatEfficiency(menu.heat);
        out.add(Component.translatable("gui.crazyae2addons.penrose_efficiency", String.format("%.1f%%", eff * 100.0)).withStyle(ChatFormatting.GREEN));
        return out;
    }

    // ---------------- Panel / Background ----------------

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

    // ---------------- Overlay text ----------------

    private void drawOverlayText(GuiGraphics gg, int x, int y) {
        Font font = Minecraft.getInstance().font;

        String bhMass = Utils.shortenNumber(menu.bhMass);
        String diskMass = Utils.shortenNumber(menu.diskMassSingu);
        String gen = Utils.shortenNumber(menu.feGeneratedGrossPerTick);
        String use = Utils.shortenNumber(menu.feConsumedPerTick);
        String feStored = Utils.shortenNumber(menu.storedEnergy);
        String feInDisk = Utils.shortenNumber(menu.storedEnergyInDisk);

        float scale = 0.82f;
        gg.pose().pushPose();
        gg.pose().translate(x, y, 0);
        gg.pose().scale(scale, scale, 1);

        int yy = 0;
        int line = (int) (font.lineHeight + 3);

        drawTextShadow(gg, font, Component.translatable("gui.crazyae2addons.penrose_overlay_bh_mass", bhMass).getString(), 0, yy, 0xEDEDED); yy += line;
        drawTextShadow(gg, font, Component.translatable("gui.crazyae2addons.penrose_overlay_disk_mass", diskMass).getString(), 0, yy, 0xEDEDED); yy += line;
        drawTextShadow(gg, font, Component.translatable("gui.crazyae2addons.penrose_overlay_gen", gen).getString(), 0, yy, 0xEDEDED); yy += line;
        drawTextShadow(gg, font, Component.translatable("gui.crazyae2addons.penrose_overlay_use", use).getString(), 0, yy, 0xEDEDED); yy += line;
        drawTextShadow(gg, font, Component.translatable("gui.crazyae2addons.penrose_overlay_fe_stored", feStored).getString(), 0, yy, 0xEDEDED); yy += line;
        drawTextShadow(gg, font, Component.translatable("gui.crazyae2addons.penrose_overlay_fe_in_disk", feInDisk).getString(), 0, yy, 0xEDEDED);

        gg.pose().popPose();
    }

    private void drawTextShadow(GuiGraphics gg, Font font, String s, int x, int y, int color) {
        gg.drawString(font, s, x + 1, y + 1, 0xAA000000, false);
        gg.drawString(font, s, x, y, color, false);
    }

    // ---------------- BH + Disk render ----------------

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
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f m = gg.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();

        int seg = 320;
        float rot = t * 0.26f;

        float heatP = (float) Mth.clamp(menu.heat / MAX_HEAT_GK, 0.0, 1.0);
        float massP = diskMass01();

        int baseA = menu.blackHoleActive ? 235 : 120;
        int alphaBase = (int) Mth.clamp(baseA * (0.30f + 0.70f * massP), 0f, 255f);

        float inner = g.diskInnerR;
        float outer = g.diskOuterR;
        float totalW = outer - inner;

        final float ringW = 1.8f;
        final float overlap = 0.55f;

        int ringCount = Math.max(1, (int) Math.ceil(totalW / ringW));
        ringCount = Math.min(ringCount, 512);

        final float armCount = 2.0f;
        final float twistPerPx = 0.95f;
        final float warpAmp = 0.010f;

        final float radialHeatK = 6.2f;

        final float centerBoost = 0.70f - 0.25f * heatP;
        final float edgeCool    = 0.10f + 0.28f * heatP;

        for (int ring = 0; ring < ringCount; ring++) {
            float rI = inner + ring * ringW - (overlap * 0.5f);
            float rO = rI + ringW + overlap;

            if (rO <= inner) continue;
            if (rI < inner) rI = inner;
            if (rO > outer) rO = outer;
            if (rI >= outer) break;
            if (rO - rI <= 0.001f) continue;

            float rMid = (rI + rO) * 0.5f;
            float radialP01Mid = (totalW <= 0.0001f) ? 0f : Mth.clamp((rMid - inner) / totalW, 0f, 1f);

            float ringFade = 1.0f - 0.55f * radialP01Mid;
            int alpha = (int) (alphaBase * ringFade);

            bb.begin(VertexFormat.Mode.TRIANGLE_STRIP, DefaultVertexFormat.POSITION_COLOR);

            for (int i = 0; i <= seg; i++) {
                float ang = (float) ((Math.PI * 2.0) * i / seg);
                float ca = Mth.cos(ang);
                float sa = Mth.sin(ang);

                // ---------- OUT ----------
                {
                    float radialPx = rO - inner;
                    float radialP01 = (totalW <= 0.0001f) ? 0f : Mth.clamp(radialPx / totalW, 0f, 1f);

                    float radialHot = (float) Math.exp(-radialHeatK * radialP01 * radialP01); // 1 (inner) -> 0 (outer)
                    float heatAdj = Mth.clamp(
                            heatP + centerBoost * radialHot - edgeCool * (1.0f - radialHot),
                            0f, 1f
                    );

                    float spiralPhase = (ang * armCount) - (radialPx * twistPerPx) + (rot * 1.15f);

                    float arm = 0.5f + 0.5f * Mth.sin(spiralPhase);
                    arm = (float) Math.pow(arm, 3.0f);

                    float doppler = 0.55f + 0.45f * (float) Math.pow(Math.max(0.0f, Mth.cos(ang + rot)), 2.2f);
                    float inten = Mth.clamp(0.16f + 0.62f * doppler + 0.72f * arm, 0f, 1f);

                    inten *= (0.72f + 0.28f * (1.0f - radialHot));
                    inten = Mth.clamp(inten, 0f, 1f);

                    float warp = 1.0f + warpAmp * Mth.sin(spiralPhase) + (warpAmp * 0.55f) * Mth.sin(spiralPhase * 2.0f);

                    int colOuter = diskColor(heatAdj, radialP01, inten * 0.72f, (int) (alpha * 0.92f));

                    float xo = g.cx + ca * (rO * warp);
                    float yo = g.cy + sa * (rO * warp) * DISK_Y_SCALE;
                    putVertex(bb, m, xo, yo, colOuter);
                }

                // ---------- IN ----------
                {
                    float radialPx = rI - inner;
                    float radialP01 = (totalW <= 0.0001f) ? 0f : Mth.clamp(radialPx / totalW, 0f, 1f);

                    float radialHot = (float) Math.exp(-radialHeatK * radialP01 * radialP01);
                    float heatAdj = Mth.clamp(
                            heatP + centerBoost * radialHot - edgeCool * (1.0f - radialHot),
                            0f, 1f
                    );

                    float spiralPhase = (ang * armCount) - (radialPx * twistPerPx) + (rot * 1.15f);

                    float arm = 0.5f + 0.5f * Mth.sin(spiralPhase);
                    arm = (float) Math.pow(arm, 3.0f);

                    float doppler = 0.55f + 0.45f * (float) Math.pow(Math.max(0.0f, Mth.cos(ang + rot)), 2.2f);
                    float inten = Mth.clamp(0.16f + 0.62f * doppler + 0.72f * arm, 0f, 1f);

                    inten *= (0.72f + 0.28f * (1.0f - radialHot));
                    inten = Mth.clamp(inten, 0f, 1f);

                    float warp = 1.0f + warpAmp * Mth.sin(spiralPhase) + (warpAmp * 0.55f) * Mth.sin(spiralPhase * 2.0f);

                    int colInner = diskColor(heatAdj, radialP01, inten, alpha);

                    float xi = g.cx + ca * (rI * warp);
                    float yi = g.cy + sa * (rI * warp) * DISK_Y_SCALE;
                    putVertex(bb, m, xi, yi, colInner);
                }
            }

            BufferUploader.drawWithShader(bb.end());
        }

        RenderSystem.disableBlend();
    }

    private int diskColor(float heatP, float radialP, float intensity, int alpha) {
        heatP = Mth.clamp(heatP, 0f, 1f);
        radialP = Mth.clamp(radialP, 0f, 1f);
        intensity = Mth.clamp(intensity, 0f, 1f);

        // paleta
        int cOrange   = argb(255, 255, 140,  35);
        int cRed      = argb(255, 255,  45,  25);
        int cPurple   = argb(255, 155,  55, 235);
        int cBlue     = argb(255,  60, 160, 255);
        int cWhiteHot = argb(255, 255, 245, 235);

        // HEAT -> baza koloru (agresywnie w fiolet/niebieski)
        int base;
        if (heatP < 0.35f) {
            base = lerpColor(cOrange, cRed, heatP / 0.35f);
        } else if (heatP < 0.75f) {
            base = lerpColor(cRed, cPurple, (heatP - 0.35f) / 0.40f);
        } else {
            base = lerpColor(cPurple, cBlue, (heatP - 0.75f) / 0.25f);
        }

        // intensywność jako jasność (bez cofania w czerwienie)
        float i = (float) Math.pow(intensity, 0.90f);
        float shadow = 1.0f - i;
        float darkMul = 1.0f - 0.62f * (float) Math.pow(shadow, 1.35f);

        // --- WHITE CORE (radial) ---
        // radialP=0 => 1.0 (100% białe), radialP->1 => szybko spada do ~0
        // wykładniczo, żeby wyglądało jak gorący core
        float core = (float) Math.exp(-6.0f * radialP * radialP);

        // wymuszenie: na samym środku ma być 100% biały niezależnie od intensity
        // ale dalej od środka biel zależy od intensity i temperatury (żeby nie zrobiło “placka”)
        float hot = Mth.clamp((heatP - 0.92f) / 0.08f, 0f, 1f);

        // coreMix: 1.0 przy radialP=0, potem maleje; dodatkowo wzmocnienie przy wysokim heat
        float coreMix = core;
        coreMix = Mth.clamp(coreMix + hot * 0.35f * core, 0f, 1f);

        // Dodatkowo: poza samym środkiem niech biel trochę zależy od intensity, żeby zachować detale spirali
        float coreMixDetail = Mth.lerp(i, 1.0f, coreMix); // core -> podbija i do 1

        // 100% white na środku: gdy radialP==0 => coreMix==1 => wynik = white
        base = lerpColor(base, cWhiteHot, coreMix);

        // outer trochę ciemniej (depth)
        float radialMul = 0.62f + 0.38f * (1.0f - radialP);

        float bright = (0.05f + 0.95f * coreMixDetail) * darkMul * radialMul;
        bright = Mth.clamp(bright, 0f, 1f);

        int br = (base >>> 16) & 0xFF;
        int bg = (base >>> 8) & 0xFF;
        int bb = (base) & 0xFF;

        br = (int) Mth.clamp(br * bright, 0, 255);
        bg = (int) Mth.clamp(bg * bright, 0, 255);
        bb = (int) Mth.clamp(bb * bright, 0, 255);

        return argb(alpha, br, bg, bb);
    }

    private void drawMassBar(GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY) {
        double p = getMassProgress();
        drawBarBase(gg, x, y, w, h, inside(mouseX, mouseY, x, y, w, h));

        int fillW = (int) Math.floor((w - 2) * p);
        if (fillW > 0) {
            float hue = (float) (0.33 * (1.0 - p));
            int c0 = hsvToRgb(hue, 0.85f, 0.85f, 255);
            int c1 = hsvToRgb(hue, 1.00f, 1.00f, 255);

            gg.fillGradient(x + 1, y + 1, x + 1 + fillW, y + h - 1, c0, c1);
            gg.fillGradient(x + 1, y + 1, x + 1 + fillW, y + 1 + (h / 2), 0x22FFFFFF, 0x00000000);
        }

        String pct = String.format("%.2f%%", p * 100.0);
        String label = Component.translatable("gui.crazyae2addons.penrose_mass_bar", pct).getString();
        drawCenteredBarLabel(gg, label, x, y, w, h);
    }

    private void drawHeatBar(GuiGraphics gg, int x, int y, int w, int h, int mouseX, int mouseY) {
        double p = Mth.clamp(menu.heat / MAX_HEAT_GK, 0.0, 1.0);
        drawBarBase(gg, x, y, w, h, inside(mouseX, mouseY, x, y, w, h));

        int fillW = (int) Math.floor((w - 2) * p);
        if (fillW > 0) {
            float hue = (float) (0.58 * (1.0 - p));
            int c0 = hsvToRgb(hue, 0.90f, 0.80f, 255);
            int c1 = hsvToRgb(hue, 1.00f, 1.00f, 255);

            gg.fillGradient(x + 1, y + 1, x + 1 + fillW, y + h - 1, c0, c1);
            gg.fillGradient(x + 1, y + 1, x + 1 + fillW, y + 1 + (h / 2), 0x22FFFFFF, 0x00000000);
        }

        String label = Component.translatable(
                "gui.crazyae2addons.penrose_heat_bar",
                String.format("%.0f", menu.heat),
                String.format("%.0f", MAX_HEAT_GK)
        ).getString();

        drawCenteredBarLabel(gg, label, x, y, w, h);
    }

    private void drawBarBase(GuiGraphics gg, int x, int y, int w, int h, boolean hover) {
        int bg = hover ? 0xFF1A1D27 : 0xFF141620;
        gg.fill(x, y, x + w, y + h, bg);
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

    // ---------------- Data helpers ----------------

    private float diskMass01() {
        double m = Math.max(0.0, (double) menu.diskMassSingu);
        if (m <= 0.0) return 0f;

        double p = m / DISK_FULL_AT_MU;
        return (float) Mth.clamp(p, 0.0, 1.0);
    }

    private double getMassProgress() {
        if (!menu.blackHoleActive) return 0.0;
        long cur = menu.bhMass;
        if (maxMass <= initialMass) return 0.0;
        return Mth.clamp((double) (cur - initialMass) / (double) (maxMass - initialMass), 0.0, 1.0);
    }

    private double getSweetEfficiency01() {
        if (!menu.blackHoleActive) return 0.0;
        double span = (double) (maxMass - initialMass);
        if (span <= 0.0) return 0.0;

        double sweet = (double) initialMass + span * 0.5;
        double half = span * 0.5;

        double cur = (double) menu.bhMass;
        double e = 1.0 - (Math.abs(cur - sweet) / half);
        return Mth.clamp(e, 0.0, 1.0);
    }

    private static double heatEfficiency(double heat) {
        double h = Mth.clamp(heat / MAX_HEAT_GK, 0.0, 1.0);
        double eff = 4.0 * h * (1.0 - h);
        return Mth.clamp(eff, 0.0, 1.0);
    }

    private float getAnimTime(float partialTick) {
        var mc = Minecraft.getInstance();
        long ticks = mc.level != null ? mc.level.getGameTime() : (System.currentTimeMillis() / 50L);
        long wrapped = ticks % 24000L;
        return wrapped + partialTick;
    }

    // ---------------- Low-level draw ----------------

    private static void drawSolidCircle(GuiGraphics gg, float cx, float cy, float r, int segments, int argb) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);

        Matrix4f m = gg.pose().last().pose();
        BufferBuilder bb = Tesselator.getInstance().getBuilder();

        int a = (argb >>> 24) & 0xFF;
        int rr = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = (argb) & 0xFF;

        bb.begin(VertexFormat.Mode.TRIANGLE_FAN, DefaultVertexFormat.POSITION_COLOR);
        bb.vertex(m, cx, cy, 0).color(rr, g, b, a).endVertex();

        for (int i = 0; i <= segments; i++) {
            double ang = (Math.PI * 2.0) * (double) i / (double) segments;
            float x = cx + (float) (Math.cos(ang) * r);
            float y = cy + (float) (Math.sin(ang) * r);
            bb.vertex(m, x, y, 0).color(rr, g, b, a).endVertex();
        }

        BufferUploader.drawWithShader(bb.end());
        RenderSystem.disableBlend();
    }

    private static void putVertex(BufferBuilder bb, Matrix4f m, float x, float y, int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = (argb) & 0xFF;
        bb.vertex(m, x, y, 0).color(r, g, b, a).endVertex();
    }

    // ---------------- Color helpers ----------------

    private static int lerpColor(int c0, int c1, float t) {
        t = Mth.clamp(t, 0f, 1f);
        int a0 = (c0 >>> 24) & 0xFF, r0 = (c0 >>> 16) & 0xFF, g0 = (c0 >>> 8) & 0xFF, b0 = c0 & 0xFF;
        int a1 = (c1 >>> 24) & 0xFF, r1 = (c1 >>> 16) & 0xFF, g1 = (c1 >>> 8) & 0xFF, b1 = c1 & 0xFF;
        int a = (int) (a0 + (a1 - a0) * t);
        int r = (int) (r0 + (r1 - r0) * t);
        int g = (int) (g0 + (g1 - g0) * t);
        int b = (int) (b0 + (b1 - b0) * t);
        return argb(a, r, g, b);
    }

    private static int hsvToRgb(float h, float s, float v, int alpha) {
        h = (h % 1f + 1f) % 1f;
        s = Mth.clamp(s, 0f, 1f);
        v = Mth.clamp(v, 0f, 1f);

        float c = v * s;
        float x = c * (1 - Math.abs((h * 6f) % 2 - 1));
        float m = v - c;

        float rp = 0, gp = 0, bp = 0;
        float hh = h * 6f;

        if (0 <= hh && hh < 1) { rp = c; gp = x; bp = 0; }
        else if (1 <= hh && hh < 2) { rp = x; gp = c; bp = 0; }
        else if (2 <= hh && hh < 3) { rp = 0; gp = c; bp = x; }
        else if (3 <= hh && hh < 4) { rp = 0; gp = x; bp = c; }
        else if (4 <= hh && hh < 5) { rp = x; gp = 0; bp = c; }
        else { rp = c; gp = 0; bp = x; }

        int r = (int) ((rp + m) * 255f);
        int g = (int) ((gp + m) * 255f);
        int b = (int) ((bp + m) * 255f);

        return argb(alpha, r, g, b);
    }

    private static int argb(int a, int r, int g, int b) {
        return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) | (b & 0xFF);
    }

    // ---------------- Geometry helpers ----------------

    private static boolean inside(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && my >= y && mx < (x + w) && my < (y + h);
    }

    private DiskGeom computePreviewDiskGeom(Rects r) {
        float cx = previewCenterX(r);
        float cy = previewCenterY(r);

        float base = computeBaseDiameter(r, cx, cy);
        float maxR = base * 0.5f;

        float bhHoleR = maxR * BH_HOLE_R_FACTOR;
        float shadowR = bhHoleR * 1.55f;

        float inner = shadowR * 1.02f;
        float maxOuter = maxR * 0.98f;

        float massP = diskMass01();
        float thickness = 1.2f + (maxR * 0.62f) * (float) Math.pow(massP, 0.90);

        float outer = inner + thickness;
        if (outer > maxOuter) outer = maxOuter;

        return new DiskGeom(cx, cy, bhHoleR, shadowR, inner, outer);
    }

    private static final class DiskGeom {
        final float cx, cy;
        final float bhHoleR;
        final float shadowR;
        final float diskInnerR;
        final float diskOuterR;

        DiskGeom(float cx, float cy, float bhHoleR, float shadowR, float diskInnerR, float diskOuterR) {
            this.cx = cx;
            this.cy = cy;
            this.bhHoleR = bhHoleR;
            this.shadowR = shadowR;
            this.diskInnerR = diskInnerR;
            this.diskOuterR = diskOuterR;
        }
    }

    private Rects computeRects() {
        int x = getX();
        int y = getY();
        int w = width;
        int h = height;

        int barW = Math.max(20, w - PAD * 2);

        int heatBarX = x + PAD;
        int heatBarY = y + h - PAD - BAR_H;

        int massBarX = x + PAD;
        int massBarY = heatBarY - BAR_GAP - BAR_H;

        int previewX = x + PAD;
        int previewY = y + PAD;
        int previewW = barW;

        int previewH = massBarY - PREVIEW_TO_BARS_GAP - previewY;
        if (previewH < 24) previewH = 24;

        return new Rects(previewX, previewY, previewW, previewH, massBarX, massBarY, heatBarX, heatBarY, barW);
    }

    private float previewCenterX(Rects r) { return r.previewX + r.previewW * BH_CENTER_X; }
    private float previewCenterY(Rects r) { return r.previewY + r.previewH * BH_CENTER_Y; }

    private float computeBaseDiameter(Rects r, float cx, float cy) {
        float left = cx - r.previewX;
        float right = (r.previewX + r.previewW) - cx;
        float top = cy - r.previewY;
        float bottom = (r.previewY + r.previewH) - cy;

        float rad = Math.min(Math.min(left, right), Math.min(top, bottom)) - 0.0f;
        if (rad < 12f) rad = Math.min(r.previewW, r.previewH) * 0.48f;

        return rad * 2.0f;
    }

    private record Rects(int previewX, int previewY, int previewW, int previewH,
                         int massBarX, int massBarY,
                         int heatBarX, int heatBarY,
                         int barW) {}
}
