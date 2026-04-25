package net.oktawia.crazyae2addons.logic.structuretool;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class StructureToolExtensions {
    private static final List<StructureCloneExtension> CLONER_EXTENSIONS = new CopyOnWriteArrayList<>();
    private static final List<StructurePasteExtension> PASTE_EXTENSIONS = new CopyOnWriteArrayList<>();

    private StructureToolExtensions() {
    }

    public static void registerClonerExtension(StructureCloneExtension extension) {
        if (extension == null || CLONER_EXTENSIONS.contains(extension)) {
            return;
        }

        CLONER_EXTENSIONS.add(extension);
    }

    public static List<StructureCloneExtension> clonerExtensions() {
        return CLONER_EXTENSIONS;
    }

    public static void registerPasteExtension(StructurePasteExtension extension) {
        if (extension == null || PASTE_EXTENSIONS.contains(extension)) {
            return;
        }

        PASTE_EXTENSIONS.add(extension);
    }

    public static void notifyTemplatePasted(ServerLevel level, BlockPos placementOrigin, CompoundTag templateTag) {
        for (StructurePasteExtension extension : PASTE_EXTENSIONS) {
            extension.onTemplatePasted(level, placementOrigin, templateTag);
        }
    }
}