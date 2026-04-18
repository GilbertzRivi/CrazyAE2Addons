package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addons.CrazyAddons;

@Mod.EventBusSubscriber(
        modid = CrazyAddons.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class PreviewGuiLayers {

    private PreviewGuiLayers() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("preview_tooltip", (gui, guiGraphics, partialTick, screenWidth, screenHeight) -> {
            Minecraft mc = Minecraft.getInstance();
            PreviewTooltipLayer.render(
                    guiGraphics,
                    mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight()
            );
        });
    }
}