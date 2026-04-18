package net.oktawia.crazyae2addons.client.renderer.preview.multiblock;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.oktawia.crazyae2addons.CrazyAddons;

@EventBusSubscriber(modid = CrazyAddons.MODID, value = Dist.CLIENT)
public final class PreviewGuiLayers {

    private static final ResourceLocation PREVIEW_TOOLTIP_LAYER =
            CrazyAddons.makeId("preview_tooltip");

    private PreviewGuiLayers() {
    }

    @SubscribeEvent
    public static void registerLayers(RegisterGuiLayersEvent event) {
        event.registerAboveAll(PREVIEW_TOOLTIP_LAYER, (guiGraphics, deltaTracker) -> {
            Minecraft mc = Minecraft.getInstance();
            PreviewTooltipLayer.render(
                    guiGraphics,
                    mc.getWindow().getGuiScaledWidth(),
                    mc.getWindow().getGuiScaledHeight()
            );
        });
    }
}