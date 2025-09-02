package net.oktawia.crazyae2addons.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public final class WormholeAnchor {
    private WormholeAnchor() {}

    private static final Map<UUID, BlockPos> ANCHORS = new ConcurrentHashMap<>();

    public static void set(ServerPlayer player, BlockPos pos) {
        ANCHORS.put(player.getUUID(), pos);
    }

    public static void clear(Player player) {
        ANCHORS.remove(player.getUUID());
    }

    @Nullable
    public static BlockPos get(Player player) {
        return ANCHORS.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        clear(event.getEntity());
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }
}
