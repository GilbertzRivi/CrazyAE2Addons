package net.oktawia.crazyae2addonslite.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.oktawia.crazyae2addonslite.CrazyAddons;
import net.oktawia.crazyae2addonslite.CrazyConfig;
import net.oktawia.crazyae2addonslite.IsModLoaded;

@Mod.EventBusSubscriber(modid = CrazyAddons.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class WormholeCrossPortalClickEvents {

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel localLevel)) return;

        if (!CrazyConfig.COMMON.ImmersiveP2PWormhole.get()) return;
        if (!IsModLoaded.isIPLoaded()) return;

        ServerLevel remoteLevel;
        try {
            remoteLevel = WormholeCrossPortalClickEventsIP.isLookingAtOurPortal(sp, localLevel);
        } catch (NoClassDefFoundError err) {
            return;
        }

        if (remoteLevel == null) return;
        wrapWithAnchor(sp, remoteLevel, e.getPos());
    }

    @SubscribeEvent
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        if (!(sp.level() instanceof ServerLevel localLevel)) return;

        if (!CrazyConfig.COMMON.ImmersiveP2PWormhole.get()) return;
        if (!IsModLoaded.isIPLoaded()) return;

        ServerLevel remoteLevel;
        try {
            remoteLevel = WormholeCrossPortalClickEventsIP.isLookingAtOurPortal(sp, localLevel);
        } catch (NoClassDefFoundError err) {
            return;
        }

        if (remoteLevel == null) return;
        wrapWithAnchor(sp, remoteLevel, e.getTarget().blockPosition());
    }

    private static void wrapWithAnchor(ServerPlayer sp, ServerLevel targetWorld, BlockPos targetPos) {
        WormholeAnchor.set(sp, targetPos, targetWorld);
        var containerBefore = sp.containerMenu;
        sp.serverLevel().getServer().execute(() -> {
            sp.serverLevel().getServer().execute(() -> {
                if (sp.containerMenu == containerBefore) {
                    WormholeAnchor.clear(sp);
                }
            });
        });
    }
}
