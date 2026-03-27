package net.oktawia.crazyae2addons.renderer.preview;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class AutoBuilderPreviewStateCache {
    private static final Map<String, BlockState> CACHE = new ConcurrentHashMap<>();

    static BlockState parseBlockState(String key) {
        return CACHE.computeIfAbsent(key, AutoBuilderPreviewStateCache::parse);
    }

    private static BlockState parse(String key) {
        try {
            String id = key;
            String propStr = null;
            int idx = key.indexOf('[');
            if (idx > 0 && key.endsWith("]")) {
                id = key.substring(0, idx);
                propStr = key.substring(idx + 1, key.length() - 1);
            }
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(id));
            if (block == null || block == Blocks.AIR) return null;

            BlockState state = block.defaultBlockState();
            if (propStr != null && !propStr.isEmpty()) {
                String[] pairs = propStr.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length != 2) continue;
                    Property<?> prop = state.getBlock().getStateDefinition().getProperty(kv[0]);
                    if (prop == null) continue;
                    state = applyProperty(state, prop, kv[1]);
                }
            }
            return state;
        } catch (Exception e) {
            return null;
        }
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String valueStr) {
        try {
            if (property instanceof BooleanProperty bp) {
                return state.setValue(bp, Boolean.parseBoolean(valueStr));
            }
            if (property instanceof IntegerProperty ip) {
                return state.setValue(ip, Integer.parseInt(valueStr));
            }
            var opt = property.getValue(valueStr);
            if (opt.isPresent()) return state.setValue(property, opt.get());
        } catch (Exception ignored) {}
        return state;
    }
}
