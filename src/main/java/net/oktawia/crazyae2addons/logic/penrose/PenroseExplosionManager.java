package net.oktawia.crazyae2addons.logic.penrose;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@EventBusSubscriber(modid = "crazyae2addons")
public final class PenroseExplosionManager {

    private static final List<PenroseExplosionTask> ACTIVE = new ArrayList<>();

    private PenroseExplosionManager() {
    }

    private static PenroseExplosionSavedData data(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(PenroseExplosionSavedData::new, PenroseExplosionSavedData::load),
                PenroseExplosionSavedData.NAME
        );
    }

    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ACTIVE.removeIf(task -> task.getLevel() == level);

        PenroseExplosionSavedData data = data(level);
        if (data.isEmpty()) {
            return;
        }

        for (PenroseExplosionSavedData.Entry entry : data.all()) {
            ACTIVE.add(new PenroseExplosionTask(
                    level,
                    entry.id(),
                    entry.center(),
                    entry.radius(),
                    entry.processed()
            ));
        }
    }

    @SubscribeEvent
    public static void onLevelUnload(LevelEvent.Unload event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        PenroseExplosionSavedData data = data(level);

        Iterator<PenroseExplosionTask> it = ACTIVE.iterator();
        while (it.hasNext()) {
            PenroseExplosionTask task = it.next();
            if (task.getLevel() != level) {
                continue;
            }

            if (!task.isDone()) {
                data.put(new PenroseExplosionSavedData.Entry(
                        task.getId(),
                        task.getCenter(),
                        task.getRadiusBlocks(),
                        task.exportProcessedChunks()
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

        PenroseExplosionTask task = new PenroseExplosionTask(level, center, radiusBlocks);
        ACTIVE.add(task);

        data(level).put(new PenroseExplosionSavedData.Entry(
                task.getId(),
                task.getCenter(),
                task.getRadiusBlocks(),
                task.exportProcessedChunks()
        ));

        task.tickImmediate();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        ChunkPos chunkPos = event.getChunk().getPos();

        for (PenroseExplosionTask task : ACTIVE) {
            if (task.getLevel() == level) {
                task.onChunkLoaded(chunkPos);
            }
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (ACTIVE.isEmpty()) {
            return;
        }

        Iterator<PenroseExplosionTask> it = ACTIVE.iterator();
        while (it.hasNext()) {
            PenroseExplosionTask task = it.next();

            if (task.isDone()) {
                data(task.getLevel()).remove(task.getId());
                it.remove();
                continue;
            }

            task.tick();

            if (task.consumePersistDirty()) {
                data(task.getLevel()).put(new PenroseExplosionSavedData.Entry(
                        task.getId(),
                        task.getCenter(),
                        task.getRadiusBlocks(),
                        task.exportProcessedChunks()
                ));
            }
        }
    }
}