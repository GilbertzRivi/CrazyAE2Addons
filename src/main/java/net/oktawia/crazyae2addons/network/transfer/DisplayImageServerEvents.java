package net.oktawia.crazyae2addons.network.transfer;

import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import net.oktawia.crazyae2addons.CrazyAddons;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class DisplayImageServerEvents {

    private DisplayImageServerEvents() {
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        DisplayImageUploadStream.forget(event.getEntity().getUUID());
    }
}
