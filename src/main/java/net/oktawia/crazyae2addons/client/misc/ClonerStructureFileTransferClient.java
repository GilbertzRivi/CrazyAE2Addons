package net.oktawia.crazyae2addons.client.misc;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerStructureLibraryStore;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.structures.ExportClonerStructurePacket;
import net.oktawia.crazyae2addons.network.packets.structures.ImportClonerStructurePacket;
import org.lwjgl.util.tinyfd.TinyFileDialogs;

import java.nio.file.Files;
import java.nio.file.Path;

@OnlyIn(Dist.CLIENT)
public final class ClonerStructureFileTransferClient {

    private static final String EXTENSION = ".crazyae2structure";
    private static final int MAX_IMPORT_BYTES = 16 * 1024 * 1024;

    private static Path pendingExportPath = null;

    private ClonerStructureFileTransferClient() {
    }

    public static void beginExport(int containerId, String id, String displayName) {
        if (id == null || id.isBlank()) {
            return;
        }

        String safeName = ClonerStructureLibraryStore.sanitizeName(displayName);

        if (safeName.isBlank()) {
            safeName = "structure";
        }

        String selected = TinyFileDialogs.tinyfd_saveFileDialog(
                "Export structure",
                ensureExtension(safeName),
                null,
                "CrazyAE2 structure"
        );

        if (selected == null || selected.isBlank()) {
            return;
        }

        try {
            pendingExportPath = Path.of(stripQuotes(selected.trim()));
            pendingExportPath = ensurePathExtension(pendingExportPath);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("invalid cloner structure export path", e);
            pendingExportPath = null;
            return;
        }

        NetworkHandler.sendToServer(new ExportClonerStructurePacket(containerId, id));
    }

    public static void completeExport(String id, byte[] bytes) {
        if (pendingExportPath == null || bytes == null || bytes.length == 0) {
            pendingExportPath = null;
            return;
        }

        try {
            Files.write(pendingExportPath, bytes);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to export cloner structure", e);
        } finally {
            pendingExportPath = null;
        }
    }

    public static void beginImport(int containerId) {
        String selected = TinyFileDialogs.tinyfd_openFileDialog(
                "Import structure",
                "",
                null,
                "CrazyAE2 structure",
                false
        );

        if (selected == null || selected.isBlank()) {
            return;
        }

        try {
            Path path = Path.of(stripQuotes(selected.trim()));

            if (!Files.isRegularFile(path)) {
                return;
            }

            byte[] bytes = Files.readAllBytes(path);

            if (bytes.length <= 0 || bytes.length > MAX_IMPORT_BYTES) {
                return;
            }

            NetworkHandler.sendToServer(new ImportClonerStructurePacket(
                    containerId,
                    nameFromFile(path.getFileName().toString()),
                    bytes
            ));
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to import cloner structure", e);
        }
    }

    private static String ensureExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "structure" + EXTENSION;
        }

        if (fileName.endsWith(EXTENSION)) {
            return fileName;
        }

        return fileName + EXTENSION;
    }

    private static Path ensurePathExtension(Path path) {
        String fileName = path.getFileName().toString();

        if (fileName.endsWith(EXTENSION)) {
            return path;
        }

        Path parent = path.getParent();

        if (parent == null) {
            return Path.of(fileName + EXTENSION);
        }

        return parent.resolve(fileName + EXTENSION);
    }

    private static String nameFromFile(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "";
        }

        String name = fileName;

        if (name.endsWith(EXTENSION)) {
            name = name.substring(0, name.length() - EXTENSION.length());
        }

        int dot = name.lastIndexOf('.');

        if (dot > 0) {
            name = name.substring(0, dot);
        }

        return ClonerStructureLibraryStore.sanitizeName(name);
    }

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }

        return s;
    }
}