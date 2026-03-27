package net.oktawia.crazyae2addons;

import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;

import java.util.*;

public class BlackHoleTask {

    private static final BlockState AIR = Blocks.AIR.defaultBlockState();

    private static final long WORK_NANOS_PER_TICK = 100_000_000L;
    private static final long WORK_NANOS_IMMEDIATE = 100_000_000L;
    private static final int FLUSH_EVERY_TICKS = 2;

    private static final int PERSIST_EVERY_PROCESSED_CHUNKS = 16;

    private final ServerLevel level;
    private final UUID id;
    private final BlockPos center;
    private final int radiusBlocks;
    private final long radiusSq;

    private final int minSectionIndex;
    private final int maxSectionIndex;
    private final int[] sectionOrder;

    private final Set<Long> targetChunks = new HashSet<>();

    private final Set<Long> processedChunks = new HashSet<>();

    private final Set<Long> queuedChunks = new HashSet<>();
    private final ArrayDeque<ChunkPos> workQueue = new ArrayDeque<>();

    private final Set<LevelChunk> modifiedChunks = new HashSet<>();
    private int flushCountdown = 0;

    private boolean done = false;

    private boolean persistDirty = false;
    private int processedSincePersist = 0;

    private LevelChunk curChunk;
    private ChunkPos curChunkPos;
    private int curSectionCursor = 0;

    private LevelChunkSection curSection;
    private int curSectionBottomY;
    private int lx = 0, ly = 0, lz = 0;

    private boolean curChunkModified = false;
    private final BlockPos.MutableBlockPos mpos = new BlockPos.MutableBlockPos();

    private final List<ChunkPos> chunkOrder;


    public BlackHoleTask(ServerLevel level, BlockPos center, int radiusBlocks) {
        this(level, UUID.randomUUID(), center, radiusBlocks, null);
    }

    public BlackHoleTask(ServerLevel level, UUID id, BlockPos center, int radiusBlocks, long[] alreadyProcessed) {
        this.level = level;
        this.id = id;
        this.center = center.immutable();
        this.radiusBlocks = radiusBlocks;
        this.radiusSq = (long) radiusBlocks * (long) radiusBlocks;

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight() - 1;
        this.minSectionIndex = level.getSectionIndex(minY);
        this.maxSectionIndex = level.getSectionIndex(maxY);

        this.chunkOrder = buildChunkOrder(this.center, radiusBlocks);
        for (ChunkPos cp : chunkOrder) {
            targetChunks.add(cp.toLong());
        }

        this.sectionOrder = buildSectionOrder();

        if (alreadyProcessed != null) {
            for (long k : alreadyProcessed) processedChunks.add(k);
        }

        seedCurrentlyLoadedChunks();
    }

    public UUID getId() { return id; }
    public ServerLevel getLevel() { return level; }
    public BlockPos getCenter() { return center; }
    public int getRadiusBlocks() { return radiusBlocks; }

    public boolean isDone() { return done; }

    /** Manager woła to na ChunkEvent.Load */
    public void onChunkLoaded(ChunkPos cp) {
        long key = cp.toLong();
        if (!targetChunks.contains(key)) return;
        if (processedChunks.contains(key)) return;
        if (queuedChunks.contains(key)) return;
        if (curChunkPos != null && curChunkPos.toLong() == key) return;

        queuedChunks.add(key);
        workQueue.addLast(cp);
    }

    /** start pracy natychmiast po start() (mały budżet, bez freeza) */
    public void tickImmediate() {
        tickBudget(WORK_NANOS_IMMEDIATE);
    }

    public void tick() {
        tickBudget(WORK_NANOS_PER_TICK);
    }

    /** Czy manager ma zapisać stan do WorldData w tym ticku */
    public boolean consumePersistDirty() {
        boolean d = persistDirty;
        persistDirty = false;
        return d;
    }

    public long[] exportProcessedChunks() {
        long[] out = new long[processedChunks.size()];
        int i = 0;
        for (long v : processedChunks) out[i++] = v;
        return out;
    }

    // --- core loop ---

    private void tickBudget(long budgetNanos) {
        if (done) return;

        long deadline = System.nanoTime() + budgetNanos;

        while (!done && System.nanoTime() < deadline) {
            if (!step()) {
                done = true;
                persistDirty = true; // finalny zapis + manager usunie z WorldData
                break;
            }
        }

        if (--flushCountdown <= 0 || done) {
            flushCountdown = FLUSH_EVERY_TICKS;
            flushModifiedChunks();
        }
    }

    /**
     * Jednostka pracy.
     * false = wszystko zrobione (processedChunks == targetChunks)
     */
    private boolean step() {
        // jeśli wszystko zrobione -> koniec
        if (processedChunks.size() >= targetChunks.size()) {
            return false;
        }

        // kontynuuj obrabianie sekcji
        if (curSection != null) {
            // jeżeli chunk zniknął (unload) w międzyczasie, przerwij chunk i wróć później
            if (!isChunkStillLoaded(curChunkPos)) {
                requeueCurrentChunkIfNeeded();
                dropCurrentWork();
                return true;
            }
            processOneBlock();
            return true;
        }

        // jeżeli obrabiamy chunk, ale został wyładowany, to odpuść (na spokojnie)
        if (curChunkPos != null && !isChunkStillLoaded(curChunkPos)) {
            requeueCurrentChunkIfNeeded();
            dropCurrentWork();
            return true;
        }

        // jeżeli nie mamy aktywnego chunka, spróbuj wziąć jakiś z kolejki (który nadal jest loaded)
        if (curChunk == null) {
            if (workQueue.isEmpty()) {
                // nie ma co robić dopóki gracz nie doładuje chunków
                return true;
            }

            int tries = Math.min(8, workQueue.size());
            while (tries-- > 0 && !workQueue.isEmpty()) {
                ChunkPos cp = workQueue.pollFirst();
                long key = cp.toLong();

                if (processedChunks.contains(key)) {
                    queuedChunks.remove(key);
                    continue;
                }

                LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
                if (chunk == null) {
                    // nie jest loaded -> wrzuć z powrotem na koniec i czekaj aż wróci (albo event doładuje)
                    workQueue.addLast(cp);
                    continue;
                }

                // bierzemy do roboty
                queuedChunks.remove(key);
                curChunk = chunk;
                curChunkPos = cp;
                curSectionCursor = 0;
                curChunkModified = false;
                break;
            }

            return true;
        }

        // wybierz kolejną sekcję w aktualnym chunku
        while (curSectionCursor < sectionOrder.length) {
            int secIndex = sectionOrder[curSectionCursor++];

            // jak chunk zniknął w trakcie selekcji sekcji -> odpuść
            if (!isChunkStillLoaded(curChunkPos)) {
                requeueCurrentChunkIfNeeded();
                dropCurrentWork();
                return true;
            }

            LevelChunkSection section = curChunk.getSection(secIndex);
            if (section == null) continue;

            int sectionY = level.getSectionYFromSectionIndex(secIndex);

            // szybki reject: sześcian sekcji nie dotyka kuli
            int minX = (curChunkPos.x << 4);
            int minY = sectionY;
            int minZ = (curChunkPos.z << 4);
            int maxX = minX + 15;
            int maxY = minY + 15;
            int maxZ = minZ + 15;

            long minDistSq = minDistanceSqToBox(
                    center.getX(), center.getY(), center.getZ(),
                    minX, minY, minZ,
                    maxX, maxY, maxZ
            );
            if (minDistSq > radiusSq) continue;

            // start sekcji
            curSection = section;
            curSectionBottomY = sectionY;
            lx = ly = lz = 0;
            return true;
        }

        // chunk skończony
        if (curChunkModified) {
            modifiedChunks.add(curChunk);
        }

        // zapamiętaj że chunk zrobiony (persist)
        long finishedKey = curChunkPos.toLong();
        if (processedChunks.add(finishedKey)) {
            processedSincePersist++;
            if (processedSincePersist >= PERSIST_EVERY_PROCESSED_CHUNKS) {
                processedSincePersist = 0;
                persistDirty = true;
            }
        }

        dropCurrentWork();
        return true;
    }

    private void processOneBlock() {
        int x = (curChunkPos.x << 4) + lx;
        int y = curSectionBottomY + ly;
        int z = (curChunkPos.z << 4) + lz;

        long dx = (long) x - center.getX();
        long dy = (long) y - center.getY();
        long dz = (long) z - center.getZ();
        long distSq = dx * dx + dy * dy + dz * dz;

        if (distSq <= radiusSq) {
            BlockState old = curSection.getBlockState(lx, ly, lz);
            if (!old.isAir() && !old.is(Blocks.BEDROCK)) {
                mpos.set(x, y, z);

                if (old.hasBlockEntity()) {
                    curChunk.removeBlockEntity(mpos);
                }

                curSection.setBlockState(lx, ly, lz, AIR, false);
                curChunkModified = true;
            }
        }

        // next block in section
        lx++;
        if (lx >= 16) {
            lx = 0;
            lz++;
            if (lz >= 16) {
                lz = 0;
                ly++;
                if (ly >= 16) {
                    curSection = null; // sekcja skończona
                }
            }
        }
    }

    private void flushModifiedChunks() {
        if (modifiedChunks.isEmpty()) return;

        for (LevelChunk chunk : modifiedChunks) {
            chunk.setUnsaved(true);
            chunk.setLightCorrect(false);

            ClientboundLevelChunkWithLightPacket packet =
                    new ClientboundLevelChunkWithLightPacket(chunk, level.getLightEngine(), null, null);

            level.getChunkSource().chunkMap
                    .getPlayers(chunk.getPos(), false)
                    .forEach(player -> player.connection.send(packet));
        }

        modifiedChunks.clear();
    }

    // --- helpers ---

    private boolean isChunkStillLoaded(ChunkPos cp) {
        if (cp == null) return false;
        return level.getChunkSource().getChunkNow(cp.x, cp.z) != null;
    }

    private void dropCurrentWork() {
        curChunk = null;
        curChunkPos = null;
        curSection = null;
        curChunkModified = false;
    }

    private void requeueCurrentChunkIfNeeded() {
        if (curChunkPos == null) return;
        long key = curChunkPos.toLong();
        if (processedChunks.contains(key)) return;
        if (queuedChunks.contains(key)) return;

        queuedChunks.add(key);
        workQueue.addLast(curChunkPos);
    }

    private void seedCurrentlyLoadedChunks() {
        for (ChunkPos cp : chunkOrder) {
            long key = cp.toLong();
            if (processedChunks.contains(key)) continue;

            LevelChunk chunk = level.getChunkSource().getChunkNow(cp.x, cp.z);
            if (chunk != null) {
                if (queuedChunks.add(key)) {
                    workQueue.addLast(cp);
                }
            }
        }
    }

    private int[] buildSectionOrder() {
        int count = (maxSectionIndex - minSectionIndex) + 1;
        Integer[] tmp = new Integer[count];

        int centerSection = level.getSectionIndex(center.getY());
        for (int i = 0; i < count; i++) tmp[i] = minSectionIndex + i;

        Arrays.sort(tmp, Comparator.comparingInt(si -> Math.abs(si - centerSection)));

        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = tmp[i];
        return out;
    }

    private static List<ChunkPos> buildChunkOrder(BlockPos center, int radiusBlocks) {
        long radiusSq = (long) radiusBlocks * (long) radiusBlocks;
        int centerChunkX = center.getX() >> 4;
        int centerChunkZ = center.getZ() >> 4;
        int chunkRadius = (radiusBlocks + 15) >> 4;

        List<ChunkPos> out = new ArrayList<>();
        for (int cx = centerChunkX - chunkRadius; cx <= centerChunkX + chunkRadius; cx++) {
            for (int cz = centerChunkZ - chunkRadius; cz <= centerChunkZ + chunkRadius; cz++) {
                int chunkMinX = (cx << 4);
                int chunkMinZ = (cz << 4);
                int chunkMaxX = chunkMinX + 15;
                int chunkMaxZ = chunkMinZ + 15;

                long dx = 0;
                if (center.getX() < chunkMinX) dx = chunkMinX - center.getX();
                else if (center.getX() > chunkMaxX) dx = center.getX() - chunkMaxX;

                long dz = 0;
                if (center.getZ() < chunkMinZ) dz = chunkMinZ - center.getZ();
                else if (center.getZ() > chunkMaxZ) dz = center.getZ() - chunkMaxZ;

                long minDistSq = dx * dx + dz * dz;
                if (minDistSq <= radiusSq) out.add(new ChunkPos(cx, cz));
            }
        }

        out.sort(Comparator.comparingLong(cp -> {
            long ccx = (cp.x << 4) + 8L;
            long ccz = (cp.z << 4) + 8L;
            long dx = ccx - center.getX();
            long dz = ccz - center.getZ();
            return dx * dx + dz * dz;
        }));

        return out;
    }

    private static long minDistanceSqToBox(int cx, int cy, int cz,
                                           int minX, int minY, int minZ,
                                           int maxX, int maxY, int maxZ) {
        long dx = 0;
        if (cx < minX) dx = minX - cx;
        else if (cx > maxX) dx = cx - maxX;

        long dy = 0;
        if (cy < minY) dy = minY - cy;
        else if (cy > maxY) dy = cy - maxY;

        long dz = 0;
        if (cz < minZ) dz = minZ - cz;
        else if (cz > maxZ) dz = cz - maxZ;

        return dx * dx + dy * dy + dz * dz;
    }
}
