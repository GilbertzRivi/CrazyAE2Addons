package net.oktawia.crazyae2addons.logic.penrose;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PenroseExplosionSavedData extends SavedData {

    public static final String NAME = "crazyae2addons_penrose_explosions";

    public record Entry(UUID id, BlockPos center, int radius, long[] processed) {
    }

    private final Map<UUID, Entry> entries = new HashMap<>();

    public Collection<Entry> all() {
        return entries.values();
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public void put(Entry entry) {
        entries.put(entry.id(), entry);
        setDirty();
    }

    public void remove(UUID id) {
        if (entries.remove(id) != null) {
            setDirty();
        }
    }

    public static PenroseExplosionSavedData load(CompoundTag tag, HolderLookup.Provider provider) {
        PenroseExplosionSavedData data = new PenroseExplosionSavedData();

        ListTag list = tag.getList("tasks", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag taskTag = list.getCompound(i);
            UUID id = taskTag.getUUID("id");
            BlockPos center = NbtUtils.readBlockPos(taskTag, "center").orElse(BlockPos.ZERO);
            int radius = taskTag.getInt("radius");
            long[] processed = taskTag.getLongArray("processed");

            data.entries.put(id, new Entry(id, center, radius, processed));
        }

        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider provider) {
        ListTag list = new ListTag();

        for (Entry entry : entries.values()) {
            CompoundTag taskTag = new CompoundTag();
            taskTag.putUUID("id", entry.id());
            taskTag.put("center", NbtUtils.writeBlockPos(entry.center()));
            taskTag.putInt("radius", entry.radius());
            taskTag.putLongArray("processed", entry.processed() == null ? new long[0] : entry.processed());
            list.add(taskTag);
        }

        tag.put("tasks", list);
        return tag;
    }
}