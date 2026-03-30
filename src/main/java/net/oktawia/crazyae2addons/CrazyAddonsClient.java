package net.oktawia.crazyae2addons;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.oktawia.crazyae2addons.defs.Screens;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.client.renderer.preview.AutoBuilderPreviewRenderer;

@Mod(value = CrazyAddons.MODID, dist = Dist.CLIENT)
public class CrazyAddonsClient {

    public CrazyAddonsClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterGeometryLoaders);
        modEventBus.addListener(this::onRegisterRenderers);
        modEventBus.addListener(Screens::register);
        NeoForge.EVENT_BUS.addListener(AutoBuilderPreviewRenderer::onRender);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
    }

    private void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        try {
            CrazyItemRegistrar.registerPartModels();
        } catch (Exception ignored) {
        }
    }

    private void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    }
}
