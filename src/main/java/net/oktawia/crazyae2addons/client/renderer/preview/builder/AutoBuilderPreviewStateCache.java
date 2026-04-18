package net.oktawia.crazyae2addons.client.renderer.preview.builder;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.oktawia.crazyae2addons.logic.builder.BuilderCoordMath;

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
            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(id)).orElse(null);
            if (block == null || block == Blocks.AIR) return null;

            BlockState state = block.defaultBlockState();
            if (propStr != null && !propStr.isEmpty()) {
                String[] pairs = propStr.split(",");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length != 2) continue;
                    Property<?> prop = state.getBlock().getStateDefinition().getProperty(kv[0]);
                    if (prop == null) continue;
                    state = BuilderCoordMath.applyProperty(state, prop, kv[1]);
                }
            }
            return state;
        } catch (Exception e) {
            return null;
        }
    }

}
