package net.oktawia.crazyae2addons.logic.structuretool;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public final class ClonerStructureLibraryStore {

    public static final int MAX_NAME_LENGTH = 32;

    private static final String DIR_NAME = "crazyae2addons/structure_tools/cloner_library";
    private static final String STRUCTURES_DIR_NAME = "structures";
    private static final String INDEX_FILE_NAME = "index.nbt";

    private static final String KEY_ENTRIES = "entries";
    private static final String KEY_ID = "id";
    private static final String KEY_NAME = "name";
    private static final String KEY_CREATED = "created";
    private static final String KEY_UPDATED = "updated";
    private static final String KEY_BLOCK_COUNT = "blockCount";

    private ClonerStructureLibraryStore() {
    }

    public record Entry(
            String id,
            String name,
            long created,
            long updated,
            int blockCount
    ) {
    }

    private static Path getRoot(MinecraftServer server) throws IOException {
        Path root = server.getWorldPath(LevelResource.ROOT).resolve(DIR_NAME);
        Files.createDirectories(root);
        return root;
    }

    private static Path getOwnerRoot(MinecraftServer server, UUID owner) throws IOException {
        Path root = getRoot(server).resolve(owner.toString());
        Files.createDirectories(root);
        Files.createDirectories(root.resolve(STRUCTURES_DIR_NAME));
        return root;
    }

    private static Path getIndexPath(MinecraftServer server, UUID owner) throws IOException {
        return getOwnerRoot(server, owner).resolve(INDEX_FILE_NAME);
    }

    private static Path getStructuresRoot(MinecraftServer server, UUID owner) throws IOException {
        Path root = getOwnerRoot(server, owner).resolve(STRUCTURES_DIR_NAME);
        Files.createDirectories(root);
        return root;
    }

    private static Path getStructurePath(MinecraftServer server, UUID owner, String id) throws IOException {
        return getStructuresRoot(server, owner).resolve(id + ".nbt");
    }

    public static List<Entry> list(MinecraftServer server, UUID owner) throws IOException {
        CompoundTag index = readIndex(server, owner);

        if (!index.contains(KEY_ENTRIES, Tag.TAG_LIST)) {
            return List.of();
        }

        ListTag entriesTag = index.getList(KEY_ENTRIES, Tag.TAG_COMPOUND);
        List<Entry> entries = new ArrayList<>();

        for (int i = 0; i < entriesTag.size(); i++) {
            CompoundTag row = entriesTag.getCompound(i);
            String id = row.getString(KEY_ID);

            if (id.isBlank()) {
                continue;
            }

            entries.add(new Entry(
                    id,
                    sanitizeName(row.getString(KEY_NAME)),
                    row.getLong(KEY_CREATED),
                    row.getLong(KEY_UPDATED),
                    Math.max(0, row.getInt(KEY_BLOCK_COUNT))
            ));
        }

        entries.sort(Comparator
                .comparingLong(Entry::updated)
                .reversed()
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));

        return entries;
    }

    public static @Nullable Entry get(MinecraftServer server, UUID owner, String id) throws IOException {
        if (id == null || id.isBlank()) {
            return null;
        }

        for (Entry entry : list(server, owner)) {
            if (entry.id().equals(id)) {
                return entry;
            }
        }

        return null;
    }

    public static boolean exists(MinecraftServer server, UUID owner, String id) throws IOException {
        return id != null
                && !id.isBlank()
                && Files.exists(getStructurePath(server, owner, id));
    }

    public static @Nullable CompoundTag load(MinecraftServer server, UUID owner, String id) throws IOException {
        if (id == null || id.isBlank()) {
            return null;
        }

        Path path = getStructurePath(server, owner, id);

        if (!Files.exists(path)) {
            return null;
        }

        try (InputStream in = Files.newInputStream(path)) {
            return NbtIo.readCompressed(in);
        }
    }

    public static Entry saveNew(
            MinecraftServer server,
            UUID owner,
            CompoundTag tag,
            @Nullable String requestedName
    ) throws IOException {
        String id = UUID.randomUUID().toString();
        long now = System.currentTimeMillis();

        List<Entry> entries = new ArrayList<>(list(server, owner));
        String name = sanitizeName(requestedName);

        if (name.isBlank()) {
            name = nextDefaultName(entries);
        }

        Entry entry = new Entry(
                id,
                name,
                now,
                now,
                countBlocks(tag)
        );

        writeStructure(server, owner, id, tag);

        entries.add(entry);
        writeIndex(server, owner, entries);

        return entry;
    }

    public static @Nullable Entry saveExisting(
            MinecraftServer server,
            UUID owner,
            String id,
            CompoundTag tag
    ) throws IOException {
        if (id == null || id.isBlank()) {
            return null;
        }

        List<Entry> entries = new ArrayList<>(list(server, owner));
        long now = System.currentTimeMillis();

        for (int i = 0; i < entries.size(); i++) {
            Entry old = entries.get(i);

            if (!old.id().equals(id)) {
                continue;
            }

            Entry updated = new Entry(
                    old.id(),
                    old.name(),
                    old.created(),
                    now,
                    countBlocks(tag)
            );

            writeStructure(server, owner, id, tag);
            entries.set(i, updated);
            writeIndex(server, owner, entries);

            return updated;
        }

        return null;
    }

    public static Entry saveForCurrentSelection(
            MinecraftServer server,
            UUID owner,
            ItemStack stack,
            CompoundTag tag
    ) throws IOException {
        Entry created = saveNew(server, owner, tag, null);

        StructureToolStackState.setSelectedClonerLibraryEntry(
                stack,
                owner,
                created.id()
        );

        return created;
    }

    public static boolean rename(
            MinecraftServer server,
            UUID owner,
            String id,
            String requestedName
    ) throws IOException {
        if (id == null || id.isBlank()) {
            return false;
        }

        String name = sanitizeName(requestedName);

        if (name.isBlank()) {
            return false;
        }

        List<Entry> entries = new ArrayList<>(list(server, owner));
        boolean changed = false;

        for (int i = 0; i < entries.size(); i++) {
            Entry old = entries.get(i);

            if (!old.id().equals(id)) {
                continue;
            }

            entries.set(i, new Entry(
                    old.id(),
                    name,
                    old.created(),
                    System.currentTimeMillis(),
                    old.blockCount()
            ));

            changed = true;
            break;
        }

        if (changed) {
            writeIndex(server, owner, entries);
        }

        return changed;
    }

    public static boolean delete(MinecraftServer server, UUID owner, String id) throws IOException {
        if (id == null || id.isBlank()) {
            return false;
        }

        boolean deleted = Files.deleteIfExists(getStructurePath(server, owner, id));

        if (!deleted) {
            return false;
        }

        List<Entry> entries = new ArrayList<>(list(server, owner));
        entries.removeIf(entry -> entry.id().equals(id));
        writeIndex(server, owner, entries);

        return true;
    }

    public static @Nullable CompoundTag loadSelectedOrMigrateLegacy(
            MinecraftServer server,
            UUID fallbackOwner,
            ItemStack stack
    ) throws IOException {
        String selectedId = StructureToolStackState.getStructureId(stack);

        if (selectedId.isBlank()) {
            return null;
        }

        UUID owner = StructureToolStackState.getClonerLibraryOwner(stack);

        if (owner != null) {
            CompoundTag tag = load(server, owner, selectedId);

            if (tag != null) {
                return tag;
            }

            return null;
        }

        CompoundTag legacyTag = StructureToolStructureStore.load(server, selectedId);

        if (legacyTag == null) {
            return null;
        }

        Entry migrated = saveNew(server, fallbackOwner, legacyTag, null);
        StructureToolStackState.setSelectedClonerLibraryEntry(stack, fallbackOwner, migrated.id());

        return legacyTag;
    }

    public static byte[] exportBytes(MinecraftServer server, UUID owner, String id) throws IOException {
        CompoundTag tag = load(server, owner, id);

        if (tag == null) {
            return new byte[0];
        }

        return TemplateUtil.compressNbt(tag);
    }

    public static Entry importBytes(
            MinecraftServer server,
            UUID owner,
            byte[] bytes,
            @Nullable String requestedName
    ) throws IOException {
        CompoundTag tag = TemplateUtil.decompressNbt(bytes);
        return saveNew(server, owner, tag, requestedName);
    }

    public static String sanitizeName(@Nullable String raw) {
        if (raw == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();

        for (int i = 0; i < raw.length() && out.length() < MAX_NAME_LENGTH; i++) {
            char c = raw.charAt(i);

            if (Character.isISOControl(c)) {
                continue;
            }

            out.append(c);
        }

        return out.toString().trim();
    }

    private static String nextDefaultName(List<Entry> existing) {
        int index = existing.size() + 1;

        while (true) {
            String candidate = "Clone " + index;
            boolean used = false;

            for (Entry entry : existing) {
                if (entry.name().equalsIgnoreCase(candidate)) {
                    used = true;
                    break;
                }
            }

            if (!used) {
                return candidate;
            }

            index++;
        }
    }

    private static int countBlocks(CompoundTag tag) {
        return TemplateUtil.parseRawBlocksFromTag(tag).size();
    }

    private static void writeStructure(
            MinecraftServer server,
            UUID owner,
            String id,
            CompoundTag tag
    ) throws IOException {
        Path path = getStructurePath(server, owner, id);

        try (OutputStream out = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(tag, out);
        }
    }

    private static CompoundTag readIndex(MinecraftServer server, UUID owner) throws IOException {
        Path path = getIndexPath(server, owner);

        if (!Files.exists(path)) {
            return new CompoundTag();
        }

        try (InputStream in = Files.newInputStream(path)) {
            CompoundTag tag = NbtIo.readCompressed(in);
            return tag == null ? new CompoundTag() : tag;
        }
    }

    private static void writeIndex(
            MinecraftServer server,
            UUID owner,
            List<Entry> entries
    ) throws IOException {
        CompoundTag root = new CompoundTag();
        ListTag entriesTag = new ListTag();

        entries.sort(Comparator
                .comparingLong(Entry::updated)
                .reversed()
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));

        for (Entry entry : entries) {
            CompoundTag row = new CompoundTag();

            row.putString(KEY_ID, entry.id());
            row.putString(KEY_NAME, sanitizeName(entry.name()));
            row.putLong(KEY_CREATED, entry.created());
            row.putLong(KEY_UPDATED, entry.updated());
            row.putInt(KEY_BLOCK_COUNT, Math.max(0, entry.blockCount()));

            entriesTag.add(row);
        }

        root.put(KEY_ENTRIES, entriesTag);

        Path path = getIndexPath(server, owner);

        try (OutputStream out = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(root, out);
        }
    }
}