package net.oktawia.crazyae2addons.logic.display;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;

import net.oktawia.crazyae2addons.CrazyConfig;

public class DisplayImageStore extends SavedData {

    private static final String NAME = "crazyae2_display_images";

    private static final String NBT_VERSION = "version";
    private static final String NBT_ENTRIES = "entries";
    private static final String NBT_ID = "id";
    private static final String NBT_REFS = "refs";
    private static final String NBT_DATA = "data";

    private static final int VERSION = 2;

    private final Map<String, byte[]> images = new HashMap<>();
    private final Map<String, Integer> references = new HashMap<>();

    private long usedBytes = 0L;

    public static DisplayImageStore get(Level level) {
        if (!(level instanceof ServerLevel serverLevel)) {
            throw new IllegalStateException("DisplayImageStore is server-only");
        }
        ServerLevel overworld = serverLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(
                DisplayImageStore::load,
                DisplayImageStore::new,
                NAME);
    }

    public static long budgetBytes() {
        return CrazyConfig.COMMON.DISPLAY_IMAGES_TOTAL_BYTES.get();
    }

    public static int maxImageBytes() {
        return CrazyConfig.COMMON.DISPLAY_IMAGE_MAX_BYTES.get();
    }

    public static int maxImageDimension() {
        return CrazyConfig.COMMON.DISPLAY_IMAGE_MAX_DIMENSION.get();
    }

    @Nullable
    public byte[] getImage(String id) {
        return images.get(id);
    }

    public long usedBytes() {
        return usedBytes;
    }

    public long freeBytes() {
        return Math.max(0L, budgetBytes() - usedBytes);
    }

    @Nullable
    public String putImageIfFits(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }

        String id = contentId(bytes);

        if (images.containsKey(id)) {
            addReference(id);
            setDirty();
            return id;
        }

        if (usedBytes + bytes.length > budgetBytes()) {
            return null;
        }

        images.put(id, bytes);
        usedBytes += bytes.length;
        addReference(id);
        setDirty();
        return id;
    }

    public boolean acquire(String id) {
        if (id == null || !images.containsKey(id)) {
            return false;
        }

        addReference(id);
        setDirty();
        return true;
    }

    public void release(String id) {
        if (id == null) {
            return;
        }

        Integer count = references.get(id);
        if (count == null) {
            dropImage(id);
            return;
        }

        if (count <= 1) {
            references.remove(id);
            dropImage(id);
            return;
        }

        references.put(id, count - 1);
        setDirty();
    }

    private void dropImage(String id) {
        byte[] removed = images.remove(id);
        if (removed != null) {
            usedBytes -= removed.length;
            setDirty();
        }
    }

    private void addReference(String id) {
        references.merge(id, 1, Integer::sum);
    }

    private static String contentId(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            StringBuilder out = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                out.append(Character.forDigit((b >> 4) & 0xF, 16));
                out.append(Character.forDigit(b & 0xF, 16));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    public static DisplayImageStore load(CompoundTag tag) {
        DisplayImageStore store = new DisplayImageStore();

        if (tag.contains(NBT_ENTRIES, Tag.TAG_LIST)) {
            ListTag entries = tag.getList(NBT_ENTRIES, Tag.TAG_COMPOUND);
            for (int i = 0; i < entries.size(); i++) {
                CompoundTag entry = entries.getCompound(i);
                String id = entry.getString(NBT_ID);
                byte[] data = entry.getByteArray(NBT_DATA);
                if (id.isEmpty() || data.length == 0) {
                    continue;
                }
                store.images.put(id, data);
                store.references.put(id, Math.max(1, entry.getInt(NBT_REFS)));
                store.usedBytes += data.length;
            }
            return store;
        }

        for (String key : tag.getAllKeys()) {
            byte[] data = tag.getByteArray(key);
            if (data.length == 0) {
                continue;
            }
            store.images.put(key, data);
            store.references.put(key, 1);
            store.usedBytes += data.length;
        }

        return store;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag entries = new ListTag();

        for (Map.Entry<String, byte[]> image : images.entrySet()) {
            CompoundTag entry = new CompoundTag();
            entry.putString(NBT_ID, image.getKey());
            entry.putInt(NBT_REFS, references.getOrDefault(image.getKey(), 1));
            entry.putByteArray(NBT_DATA, image.getValue());
            entries.add(entry);
        }

        tag.putInt(NBT_VERSION, VERSION);
        tag.put(NBT_ENTRIES, entries);
        return tag;
    }
}
