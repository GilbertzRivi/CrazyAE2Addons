package net.oktawia.crazyae2addons.misc;

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
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.network.NotificationHudPacket;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = CrazyAddons.MODID)
public class NotificationHudOverlay {

    private static final int MARGIN = 4;
    private static final int ROW_H = 18;
    private static final long TTL_MS = 2500;

    private static final class State {
        List<NotificationHudPacket.Entry> entries = List.of();
        int x = 100;
        int y = 0;
        long lastUpdateMs = 0;
    }

    private static final Map<Integer, State> states = new HashMap<>();

    public static void update(List<NotificationHudPacket.Entry> entries, byte hudX, byte hudY) {
        int x = Math.min(100, Math.max(0, hudX));
        int y = Math.min(100, Math.max(0, hudY));
        int key = (x << 8) | y;

        State st = states.computeIfAbsent(key, k -> new State());
        st.x = x;
        st.y = y;
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
        int maxRowsFit = Math.max(0, (h - 2 * MARGIN) / ROW_H);
        int rows = Math.min(st.entries.size(), maxRowsFit);
        if (rows <= 0) return;

        int widgetH = rows * ROW_H;

        int baseY = (int) Math.round(h * (st.y / 100.0));
        int startY = baseY - widgetH;
        startY = Math.min(Math.max(startY, MARGIN), h - widgetH - MARGIN);

        int baseX = (int) Math.round(w * (st.x / 100.0));
        baseX = Math.min(Math.max(baseX, MARGIN), w - MARGIN);

        for (int i = 0; i < rows; i++) {
            var ent = st.entries.get(i);

            ItemStack icon = ent.icon().copy();
            icon.setCount(1);

            long amount = ent.amount();
            long threshold = ent.threshold();
            if (threshold <= 0) continue;

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

            String txt = Utils.shortenNumber(dispAmount) + "/" + Utils.shortenNumber(dispThreshold);
            int color = pickColor(amount, threshold);
            int textW = mc.font.width(txt);
            int rowW = 16 + 2 + textW;
            int x = baseX - rowW;
            x = Math.min(Math.max(x, MARGIN), w - rowW - MARGIN);
            int y = startY + i * ROW_H;
            if (y < MARGIN || y > h - ROW_H - MARGIN) continue;
            g.renderItem(icon, x, y);
            g.drawString(mc.font, txt, x + 18, y + 4, color, true);
        }
    }

    private static int pickColor(long amount, long threshold) {
        if (threshold <= 0) return 0xFFFFFFFF;
        if (amount >= threshold) return 0xFF55FF55;
        return 0xFFFF5555;
    }
}
