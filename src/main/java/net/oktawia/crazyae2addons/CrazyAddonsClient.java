package net.oktawia.crazyae2addons;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.oktawia.crazyae2addons.defs.Screens;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;

import java.util.HashSet;
import java.util.Set;

@Mod(value = CrazyAddons.MODID, dist = Dist.CLIENT)
public class CrazyAddonsClient {

    public CrazyAddonsClient(IEventBus modEventBus, ModContainer container) {
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
