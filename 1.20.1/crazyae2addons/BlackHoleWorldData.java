package net.oktawia.crazyae2addons;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtUtils;

import java.util.*;

public class BlackHoleWorldData extends SavedData {
    public static final String NAME = "crazyae2addons_blackholes";

    public static class Entry {
        public UUID id;
        public BlockPos center;
        public int radius;
        public long[] processed;

        public Entry(UUID id, BlockPos center, int radius, long[] processed) {
            this.id = id;
            this.center = center;
            this.radius = radius;
            this.processed = processed;
        }
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public Collection<Entry> all() { return entries.values(); }
    public boolean isEmpty() { return entries.isEmpty(); }

    public void put(Entry e) {
        entries.put(e.id, e);
        setDirty();
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) setDirty();
    }

    public static BlackHoleWorldData load(CompoundTag tag) {
        BlackHoleWorldData data = new BlackHoleWorldData();
        ListTag list = tag.getList("tasks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag t = list.getCompound(i);
            UUID id = t.getUUID("id");
            BlockPos center = NbtUtils.readBlockPos(t.getCompound("center"));
            int radius = t.getInt("radius");
            long[] processed = t.getLongArray("processed");
            data.entries.put(id, new Entry(id, center, radius, processed));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag list = new ListTag();
        for (Entry e : entries.values()) {
            CompoundTag t = new CompoundTag();
            t.putUUID("id", e.id);
            t.put("center", NbtUtils.writeBlockPos(e.center));
            t.putInt("radius", e.radius);
            t.putLongArray("processed", e.processed == null ? new long[0] : e.processed);
            list.add(t);
        }
        tag.put("tasks", list);
        return tag;
    }
}
