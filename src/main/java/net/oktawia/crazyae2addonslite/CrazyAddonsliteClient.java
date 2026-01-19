package net.oktawia.crazyae2addonslite;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.oktawia.crazyae2addonslite.defs.Screens;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyItemRegistrar;

@Mod(value = CrazyAddonslite.MODID, dist = Dist.CLIENT)
public class CrazyAddonsliteClient {

    public CrazyAddonsliteClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::onClientSetup);
        modEventBus.addListener(this::onRegisterGeometryLoaders);
        modEventBus.addListener(Screens::register);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
    }

    private void onRegisterGeometryLoaders(ModelEvent.RegisterGeometryLoaders event) {
        try {
            CrazyItemRegistrar.registerPartModels();
        } catch (Exception ignored) {
        }
    }
}
