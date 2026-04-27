package net.oktawia.crazyae2addons.logic.structuretool;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ClonerStructureLibraryClientCache {

    private static List<Entry> entries = List.of();
    private static String selectedId = "";

    private ClonerStructureLibraryClientCache() {
    }

    public record Entry(
            String id,
            String name,
            long created,
            long updated,
            int blockCount
    ) {
    }

    public static void set(
            List<Entry> newEntries,
            String newSelectedId
    ) {
        entries = List.copyOf(newEntries);
        selectedId = newSelectedId == null ? "" : newSelectedId;
    }

    public static List<Entry> entries() {
        return entries;
    }

    public static String selectedId() {
        return selectedId;
    }

    public static boolean isSelected(String id) {
        if (id == null) {
            return selectedId.isBlank();
        }

        return selectedId.equals(id);
    }

    public static List<Entry> filtered(String query) {
        if (query == null || query.isBlank()) {
            return entries;
        }

        String normalized = query.toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();

        for (Entry entry : entries) {
            if (entry.name().toLowerCase(Locale.ROOT).contains(normalized)
                    || entry.id().toLowerCase(Locale.ROOT).contains(normalized)) {
                out.add(entry);
            }
        }

        return out;
    }

    public static void clear() {
        entries = List.of();
        selectedId = "";
    }
}