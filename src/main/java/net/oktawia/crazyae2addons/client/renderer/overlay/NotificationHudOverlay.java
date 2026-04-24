package net.oktawia.crazyae2addons.client.renderer.overlay;

import appeng.api.stacks.GenericStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.network.packets.NotificationHudPacket;
import net.oktawia.crazyae2addons.util.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CrazyAddons.MODID)
public class NotificationHudOverlay {

    private static final int MARGIN = 4;
    private static final int ROW_H = 20;
    private static final long TTL_MS = 2500;
    private static final int COLUMN_GAP = 8;
    private static final int ICON_SIZE = 16;
    private static final int TEXT_GAP = 4;
    private static final int OVERLAY_PADDING = 4;
    private static final int BACKGROUND_COLOR = 0x00000000;

    private static final class State {
        List<NotificationHudPacket.Entry> entries = List.of();
        int x = 100;
        int y = 0;
        long lastUpdateMs = 0;
        int scale = 100;
    }

    private static final Map<Integer, State> states = new HashMap<>();

    public static void update(List<NotificationHudPacket.Entry> entries, byte hudX, byte hudY, byte hudScale) {
        int x = Math.min(100, Math.max(0, hudX));
        int y = Math.min(100, Math.max(0, hudY));
        int scale = Math.min(100, Math.max(0, hudScale));

        int key = (x << 8) | y;

        State st = states.computeIfAbsent(key, k -> new State());
        st.x = x;
        st.y = y;
        st.scale = scale;
        st.entries = (entries == null) ? List.of() : new ArrayList<>(entries);
        st.lastUpdateMs = System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onRender(RenderGuiOverlayEvent.Post e) {
        if (!e.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id())) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, State>> it = states.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            if (now - en.getValue().lastUpdateMs > TTL_MS) it.remove();
        }
        if (states.isEmpty()) return;

        GuiGraphics g = e.getGuiGraphics();
        int w = mc.getWindow().getGuiScaledWidth();
        int h = mc.getWindow().getGuiScaledHeight();

        for (State st : states.values()) {
            if (st.entries == null || st.entries.isEmpty()) continue;
            renderOne(g, mc, w, h, st);
        }
    }

    private static void renderOne(GuiGraphics g, Minecraft mc, int w, int h, State st) {
        if (st.scale <= 0 || st.entries == null || st.entries.isEmpty()) {
            return;
        }

        List<ItemStack> icons = new ArrayList<>();
        List<String> texts = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        int maxTextWidth = 0;

        for (NotificationHudPacket.Entry ent : st.entries) {
            long threshold = ent.threshold();
            if (threshold <= 0) {
                continue;
            }

            ItemStack icon = ent.icon().copy();
            icon.setCount(1);

            long amount = ent.amount();

            double dispAmount = amount;
            double dispThreshold = threshold;

            var gs = GenericStack.fromItemStack(icon);
            if (gs != null && gs.what() != null) {
                long perUnit = gs.what().getAmountPerUnit();
                if (perUnit > 1) {
                    dispAmount = amount / (double) perUnit;
                    dispThreshold = threshold / (double) perUnit;
                }
            }

            String txt = Utils.shortenNumber(dispAmount, 2) + "/" + Utils.shortenNumber(dispThreshold, 2);

            icons.add(icon);
            texts.add(txt);
            colors.add(pickColor(amount, threshold));

            maxTextWidth = Math.max(maxTextWidth, mc.font.width(txt));
        }

        if (icons.isEmpty()) {
            return;
        }

        int count = icons.size();
        int availableW = Math.max(1, w - 2 * MARGIN);
        int availableH = Math.max(1, h - 2 * MARGIN);

        int textColumnWidth = maxTextWidth;
        int entryWidth = ICON_SIZE + TEXT_GAP + textColumnWidth;

        int contentW = entryWidth;
        int contentH = count * ROW_H;

        float autoScale = Math.min(
                1.0f,
                Math.min(
                        availableW / (float) Math.max(1, contentW + OVERLAY_PADDING * 2),
                        availableH / (float) Math.max(1, contentH + OVERLAY_PADDING * 2)
                )
        );

        float manualScale = st.scale / 100.0f;
        float finalScale = autoScale * manualScale;

        if (finalScale <= 0.0001f) {
            return;
        }

        int widgetW = Math.max(1, Math.round((contentW + OVERLAY_PADDING * 2) * finalScale));
        int widgetH = Math.max(1, Math.round((contentH + OVERLAY_PADDING * 2) * finalScale));

        int baseX = (int) Math.round(w * (st.x / 100.0));
        int baseY = (int) Math.round(h * (st.y / 100.0));

        int startX = baseX - widgetW;
        int startY = baseY - widgetH;

        startX = Math.min(Math.max(startX, MARGIN), w - widgetW - MARGIN);
        startY = Math.min(Math.max(startY, MARGIN), h - widgetH - MARGIN);

        g.pose().pushPose();
        g.pose().scale(finalScale, finalScale, 1.0f);

        float scaledStartX = startX / finalScale;
        float scaledStartY = startY / finalScale;

        int bgX0 = Math.round(scaledStartX);
        int bgY0 = Math.round(scaledStartY);
        int bgX1 = Math.round(scaledStartX + contentW + OVERLAY_PADDING * 2);
        int bgY1 = Math.round(scaledStartY + contentH + OVERLAY_PADDING * 2);

        g.fill(bgX0, bgY0, bgX1, bgY1, BACKGROUND_COLOR);

        float contentStartX = scaledStartX + OVERLAY_PADDING;
        float contentStartY = scaledStartY + OVERLAY_PADDING;

        for (int i = 0; i < count; i++) {
            float entryX = contentStartX;
            float entryY = contentStartY + i * ROW_H;

            int textWidth = mc.font.width(texts.get(i));

            int iconX = Math.round(entryX);
            int iconY = Math.round(entryY + (ROW_H - ICON_SIZE) * 0.5f);

            int textX = Math.round(entryX + ICON_SIZE + TEXT_GAP + (textColumnWidth - textWidth));
            int textY = Math.round(entryY + (ROW_H - mc.font.lineHeight) * 0.5f);

            g.renderItem(icons.get(i), iconX, iconY);
            g.drawString(mc.font, texts.get(i), textX, textY, colors.get(i), true);
        }

        g.pose().popPose();
    }

    private static int pickColor(long amount, long threshold) {
        if (threshold <= 0) return 0xFFFFFFFF;
        if (amount >= threshold) return 0xFF55FF55;
        return 0xFFFF5555;
    }
}