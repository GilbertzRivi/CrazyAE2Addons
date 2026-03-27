package net.oktawia.crazyae2addons;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;

@Mod.EventBusSubscriber(modid = "crazyae2addons", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class BlackHoleManager {

    private static final List<BlackHoleTask> ACTIVE = new ArrayList<>();

    private static BlackHoleWorldData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                BlackHoleWorldData::load,
                BlackHoleWorldData::new,
                BlackHoleWorldData.NAME
        );
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel lvl)) return;

        ACTIVE.removeIf(t -> t.getLevel() == lvl);

        BlackHoleWorldData d = data(lvl);
        if (d.isEmpty()) return;

        for (BlackHoleWorldData.Entry e : d.all()) {
            ACTIVE.add(new BlackHoleTask(lvl, e.id, e.center, e.radius, e.processed));
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel lvl)) return;

        BlackHoleWorldData d = data(lvl);

        Iterator<BlackHoleTask> it = ACTIVE.iterator();
        while (it.hasNext()) {
            BlackHoleTask t = it.next();
            if (t.getLevel() != lvl) continue;

            if (!t.isDone()) {
                d.put(new BlackHoleWorldData.Entry(
                        t.getId(), t.getCenter(), t.getRadiusBlocks(), t.exportProcessedChunks()
                ));
            }

            it.remove();
        }
    }

    public static void start(ServerLevel level, BlockPos center, int radiusBlocks) {
        if (!level.getServer().isSameThread()) {
            level.getServer().execute(() -> start(level, center, radiusBlocks));
            return;
        }

        BlackHoleTask task = new BlackHoleTask(level, center, radiusBlocks);
        ACTIVE.add(task);

        data(level).put(new BlackHoleWorldData.Entry(
                task.getId(), task.getCenter(), task.getRadiusBlocks(), task.exportProcessedChunks()
        ));

        task.tickImmediate();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel lvl)) return;

        ChunkPos cp = event.getChunk().getPos();

        for (BlackHoleTask task : ACTIVE) {
            if (task.getLevel() == lvl) {
                task.onChunkLoaded(cp);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (ACTIVE.isEmpty()) return;

        Iterator<BlackHoleTask> it = ACTIVE.iterator();
        while (it.hasNext()) {
            BlackHoleTask task = it.next();

            if (task.isDone()) {
                // usuń zapis taska (skończony)
                data(task.getLevel()).remove(task.getId());
                it.remove();
                continue;
            }

            task.tick();

            // zapis postępu co jakiś czas
            if (task.consumePersistDirty()) {
                data(task.getLevel()).put(new BlackHoleWorldData.Entry(
                        task.getId(), task.getCenter(), task.getRadiusBlocks(), task.exportProcessedChunks()
                ));
            }
        }
    }
}
