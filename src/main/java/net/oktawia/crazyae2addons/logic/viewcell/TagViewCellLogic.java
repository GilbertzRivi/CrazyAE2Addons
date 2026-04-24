package net.oktawia.crazyae2addons.logic.viewcell;

import lombok.experimental.UtilityClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static net.minecraft.core.registries.Registries.ITEM;

@UtilityClass
public class TagViewCellLogic {

    public static final String FILTER_TAG = "filter";

    public String readFilter(ItemStack stack) {
        var tag = stack.getTag();
        if (tag == null || !tag.contains(FILTER_TAG)) {
            return "";
        }

        return tag.getString(FILTER_TAG);
    }

    public void writeFilter(ItemStack stack, String rawFilter) {
        String normalized = normalizeFilter(rawFilter);
        var tag = stack.getOrCreateTag();

        if (normalized.isEmpty()) {
            tag.remove(FILTER_TAG);
            if (tag.isEmpty()) {
                stack.setTag(null);
            } else {
                stack.setTag(tag);
            }
            return;
        }

        tag.putString(FILTER_TAG, normalized);
        stack.setTag(tag);
    }

    public boolean hasFilter(ItemStack stack) {
        var tag = stack.getTag();
        return tag != null && tag.contains(FILTER_TAG) && !tag.getString(FILTER_TAG).isBlank();
    }

    public String normalizeFilter(String rawFilter) {
        if (rawFilter == null || rawFilter.isBlank()) {
            return "";
        }

        Set<String> unique = new LinkedHashSet<>();

        for (String part : splitRawFilter(rawFilter)) {
            String line = part.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("#")) {
                line = line.substring(1).trim();
            }

            ResourceLocation id = ResourceLocation.tryParse(line);
            if (id != null) {
                unique.add(id.toString());
            }
        }

        return String.join("\n", unique);
    }

    public List<TagKey<Item>> getItemTags(ItemStack stack) {
        return getItemTags(readFilter(stack));
    }

    public List<TagKey<Item>> getItemTags(String rawFilter) {
        List<TagKey<Item>> result = new ArrayList<>();

        for (String part : splitRawFilter(rawFilter)) {
            String line = part.trim();
            if (line.isEmpty()) {
                continue;
            }

            if (line.startsWith("#")) {
                line = line.substring(1).trim();
            }

            ResourceLocation id = ResourceLocation.tryParse(line);
            if (id != null) {
                result.add(TagKey.create(ITEM, id));
            }
        }

        return List.copyOf(result);
    }

    public List<TagKey<Item>> collectTags(Collection<ItemStack> cells) {
        List<TagKey<Item>> result = new ArrayList<>();

        for (ItemStack stack : cells) {
            if (stack == null || stack.isEmpty()) {
                continue;
            }

            result.addAll(getItemTags(stack));
        }

        return List.copyOf(result);
    }

    private List<String> splitRawFilter(String rawFilter) {
        if (rawFilter == null || rawFilter.isBlank()) {
            return List.of();
        }

        return List.of(rawFilter.split("[\\r\\n,;]+"));
    }
}