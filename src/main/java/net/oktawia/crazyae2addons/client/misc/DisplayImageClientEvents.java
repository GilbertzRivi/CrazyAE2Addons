package net.oktawia.crazyae2addons.client.misc;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.client.renderer.display.DisplayRendererCommon;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public final class DisplayImageClientEvents {

    private DisplayImageClientEvents() {
    }

    @SubscribeEvent
    public static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        DisplayImageClientCache.clear();
        DisplayImageTextures.clear();
        DisplayRendererCommon.invalidatePreparedCache();
    }
}
