package net.oktawia.crazyae2addons.client.renderer.message;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;

import java.util.List;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientHotbarMessageRenderer {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        ClientHotbarMessage.tick();
    }

    @SubscribeEvent
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (!ClientHotbarMessage.isVisible()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        List<ClientHotbarMessage.Line> lines = ClientHotbarMessage.getLines();

        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        int lineHeight = mc.font.lineHeight + 2;
        int totalHeight = lines.size() * lineHeight;

        int baseY = screenHeight - 59 - totalHeight;

        for (int i = 0; i < lines.size(); i++) {
            ClientHotbarMessage.Line line = lines.get(i);
            String text = line.text().getString();

            int textWidth = mc.font.width(text);
            int x = (screenWidth - textWidth) / 2;
            int y = baseY + i * lineHeight;

            gui.drawString(mc.font, line.text(), x, y, line.color(), true);
        }
    }
}