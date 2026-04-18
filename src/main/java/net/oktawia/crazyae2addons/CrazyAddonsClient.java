package net.oktawia.crazyae2addons;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.oktawia.crazyae2addons.client.renderer.display.DisplayWorldRenderer;
import net.oktawia.crazyae2addons.client.renderer.preview.builder.AutoBuilderPreviewRenderer;
import net.oktawia.crazyae2addons.client.textures.ConnectedTextures;
import net.oktawia.crazyae2addons.defs.Screens;

@Mod(value = CrazyAddons.MODID, dist = Dist.CLIENT)
public class CrazyAddonsClient {

    public CrazyAddonsClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);

        modEventBus.addListener(Screens::register);
        ConnectedTextures.init(modEventBus);

        NeoForge.EVENT_BUS.addListener(AutoBuilderPreviewRenderer::onRender);
        NeoForge.EVENT_BUS.addListener(DisplayWorldRenderer::onRender);
    }
}