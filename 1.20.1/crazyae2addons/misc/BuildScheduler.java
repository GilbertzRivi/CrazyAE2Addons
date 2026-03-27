package net.oktawia.crazyae2addons.logic;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber
public class BuildScheduler {

    private static final List<Job> JOBS = new ArrayList<>();

    public static void enqueue(ServerLevel level,
                               UUID playerId,
                               int rate,
                               List<Runnable> ops,
                               Runnable onFinish) {
        if (ops.isEmpty()) {
            if (onFinish != null) onFinish.run();
            return;
        }
        JOBS.add(new Job(level, playerId, Math.max(1, rate), new ArrayDeque<>(ops), onFinish));
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        if (JOBS.isEmpty()) return;

        for (Iterator<Job> it = JOBS.iterator(); it.hasNext(); ) {
            Job job = it.next();

            int toRun = Math.min(job.rate, job.ops.size());
            for (int i = 0; i < toRun; i++) {
                Runnable r = job.ops.pollFirst();
                if (r != null) {
                    try {
                        r.run();
                    } catch (Throwable ignored) {}
                }
            }

            if (job.ops.isEmpty()) {
                if (job.onFinish != null) {
                    try { job.onFinish.run(); } catch (Throwable ignored) {}
                }
                ServerPlayer sp = job.level.getServer().getPlayerList().getPlayer(job.playerId);
                if (sp != null) sp.displayClientMessage(net.minecraft.network.chat.Component.literal("Done."), true);

                it.remove();
            }
        }
    }

    private record Job(ServerLevel level, UUID playerId, int rate, Deque<Runnable> ops, Runnable onFinish) {}
}
