package net.oktawia.crazyae2addons.misc;

import com.lowdragmc.lowdraglib.gui.modular.ModularUIContainer;
import com.mojang.logging.LogUtils;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber
public final class WormholeAnchor {
    private WormholeAnchor() {}

    public record Anchor(Level dimension, BlockPos pos) {
        public Vec3 center() {
            return Vec3.atCenterOf(pos);
        }
    }

    private static final Map<UUID, Anchor> ANCHORS = new ConcurrentHashMap<>();

    public static void set(ServerPlayer player, BlockPos pos, ServerLevel world) {
        ANCHORS.put(player.getUUID(), new Anchor(world, pos.immutable()));
    }

    public static void clear(Player player) {
        ANCHORS.remove(player.getUUID());
    }

    @Nullable
    public static Anchor get(Player player) {
        return ANCHORS.get(player.getUUID());
    }

    @SubscribeEvent
    public static void onPlayerTick(net.minecraftforge.event.TickEvent.PlayerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        if (event.player.level().isClientSide) return;
        if (!(event.player instanceof ServerPlayer sp)) return;

        if (get(sp) != null && (sp.containerMenu == sp.inventoryMenu)) {
            clear(sp);
        }
    }
    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        clear(event.getEntity());
    }
}