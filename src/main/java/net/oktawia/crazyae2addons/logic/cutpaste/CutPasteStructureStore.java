package net.oktawia.crazyae2addons.logic.cutpaste;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class CutPasteStructureStore {

    private static final String DIR_NAME = "crazyae2addons/cut_paste_structures";

    private CutPasteStructureStore() {
    }

    private static Path getRoot(MinecraftServer server) throws IOException {
        Path root = server.getWorldPath(LevelResource.ROOT).resolve(DIR_NAME);
        Files.createDirectories(root);
        return root;
    }

    private static Path getPath(MinecraftServer server, String id) throws IOException {
        return getRoot(server).resolve(id + ".nbt");
    }

    public static void save(MinecraftServer server, String id, CompoundTag tag) throws IOException {
        Path path = getPath(server, id);
        try (OutputStream out = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(tag, out);
        }
    }

    public static CompoundTag load(MinecraftServer server, String id) throws IOException {
        Path path = getPath(server, id);
        if (!Files.exists(path)) {
            return null;
        }

        try (InputStream in = Files.newInputStream(path)) {
            return NbtIo.readCompressed(in);
        }
    }

    public static boolean exists(MinecraftServer server, String id) throws IOException {
        return Files.exists(getPath(server, id));
    }

    public static void delete(MinecraftServer server, String id) throws IOException {
        Files.deleteIfExists(getPath(server, id));
    }
}