package net.oktawia.crazyae2addons.client.misc;

import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class PortableSpatialClonerRequirementSync {

    private static final Map<Integer, List<ClonerMaterialListWidget.MaterialEntry>> ENTRIES = new HashMap<>();

    private PortableSpatialClonerRequirementSync() {
    }

    public static void setEntries(int containerId, List<ClonerMaterialListWidget.MaterialEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            ENTRIES.remove(containerId);
            return;
        }

        ENTRIES.put(
                containerId,
                entries.stream()
                        .map(entry -> new ClonerMaterialListWidget.MaterialEntry(
                                entry.stack().copy(),
                                entry.available(),
                                entry.required(),
                                entry.craftable()
                        ))
                        .collect(Collectors.toList())
        );
    }

    public static List<ClonerMaterialListWidget.MaterialEntry> getEntries(int containerId) {
        return ENTRIES.getOrDefault(containerId, List.of());
    }

    public static void clear(int containerId) {
        ENTRIES.remove(containerId);
    }
}