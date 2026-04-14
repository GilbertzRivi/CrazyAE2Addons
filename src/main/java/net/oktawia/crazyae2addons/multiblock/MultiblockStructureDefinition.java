package net.oktawia.crazyae2addons.multiblock;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.oktawia.crazyae2addons.CrazyConfig;

import java.util.*;

/**
 * Immutable description of a multiblock structure, loaded from an embedded JSON string.
 *
 * <p>JSON format (identical to the 1.20.1 validators):
 * <pre>{@code
 * {
 *   "symbols": {
 *     "A": ["crazyae2addons:penrose_frame", "crazyae2addons:penrose_mass_emitter"],
 *     "B": ["crazyae2addons:penrose_coil"],
 *     "C": ["crazyae2addons:penrose_controller"],
 *     "P": ["crazyae2addons:penrose_port", "gtceu:*_energy_output_hatch"]
 *   },
 *   "layers": [
 *     [". A A A .", "A B B B A", "A B C B A", "A B B B A", ". A A A ."],
 *     ...
 *   ]
 * }
 * }</pre>
 *
 * <ul>
 *   <li>{@code "."} — air / ignored position.</li>
 *   <li>{@code "C"} — marks the controller position (origin). Exactly one required.</li>
 *   <li>Generic wildcard {@code "*"} in the path, e.g. {@code "somemod:prefix*suffix"}:
 *       iterates all registered blocks matching the pattern. One {@code *} supported.</li>
 *   <li>GregTech tier wildcard {@code "#"} in the path, e.g.
 *       {@code "gtceu:#_energy_output_hatch"}: replaces {@code #} with each tier name
 *       from {@code CrazyConfig.COMMON.PenroseGtTiers} and looks up the result in the
 *       block registry. One {@code #} supported.</li>
 * </ul>
 *
 * <p>The Y axis maps to layer index; layers[0] is the bottom layer (lowest Y relative to
 * origin). Within each layer, rows go along Z and columns along X — matching the 1.20.1
 * convention.
 */
public final class MultiblockStructureDefinition {

    /**
     * Relative shape: key = BlockPos offset from origin (controller pos),
     * value = set of blocks that are valid at that position.
     * Controller position itself (0,0,0 offset, symbol "C") is NOT included.
     */
    private final Map<BlockPos, Set<Block>> shape;

    /** Position of "C" in the pattern grid (column = X, layer = Y, row = Z). */
    private final int originX;
    private final int originY;
    private final int originZ;

    // -------------------------------------------------------------------------
    // Construction
    // -------------------------------------------------------------------------

    public MultiblockStructureDefinition(String structureJson) {
        JsonObject json = JsonParser.parseString(structureJson).getAsJsonObject();

        // Parse symbols → block sets
        Map<String, Set<Block>> symbols = new HashMap<>();
        JsonObject symbolsJson = json.getAsJsonObject("symbols");
        for (Map.Entry<String, JsonElement> entry : symbolsJson.entrySet()) {
            symbols.put(entry.getKey(), resolveBlockEntries(entry.getValue().getAsJsonArray()));
        }

        // Parse layers, find origin, build shape map
        Map<BlockPos, Set<Block>> shapeBuilder = new HashMap<>();
        int ox = -1, oy = -1, oz = -1;

        JsonArray layersJson = json.getAsJsonArray("layers");
        for (int y = 0; y < layersJson.size(); y++) {
            JsonArray layerJson = layersJson.get(y).getAsJsonArray();
            for (int z = 0; z < layerJson.size(); z++) {
                String[] cells = layerJson.get(z).getAsString().split(" ");
                for (int x = 0; x < cells.length; x++) {
                    String sym = cells[x];
                    if (sym.equals(".")) continue;
                    if (sym.equals("C")) {
                        ox = x; oy = y; oz = z;
                        continue; // controller pos not added to shape
                    }
                    Set<Block> allowed = symbols.get(sym);
                    if (allowed == null || allowed.isEmpty()) continue;
                    shapeBuilder.put(new BlockPos(x, y, z), allowed);
                }
            }
        }

        if (ox < 0) throw new IllegalStateException(
                "MultiblockStructureDefinition: pattern has no 'C' (controller) symbol.");

        // Translate all offsets to be relative to origin
        Map<BlockPos, Set<Block>> relative = new HashMap<>();
        for (Map.Entry<BlockPos, Set<Block>> entry : shapeBuilder.entrySet()) {
            BlockPos raw = entry.getKey();
            relative.put(new BlockPos(raw.getX() - ox, raw.getY() - oy, raw.getZ() - oz),
                    Collections.unmodifiableSet(entry.getValue()));
        }

        this.shape = Collections.unmodifiableMap(relative);
        this.originX = ox;
        this.originY = oy;
        this.originZ = oz;
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Builds the absolute expected-structure map for a controller at {@code controllerPos}
     * facing {@code facing}.
     *
     * <p>The returned map contains every non-controller position of the structure as an
     * absolute {@link BlockPos} in the world, paired with the set of blocks that are
     * valid there. The map does NOT include the controller's own position.
     *
     * <p>This method does <em>not</em> touch the world at all — it only rotates and
     * translates the relative shape.
     *
     * @param controllerPos absolute position of the controller
     * @param facing        horizontal facing of the controller
     * @return immutable map: world pos → allowed blocks
     */
    public Map<BlockPos, Set<Block>> buildExpected(BlockPos controllerPos, Direction facing) {
        Map<BlockPos, Set<Block>> result = new HashMap<>();
        for (Map.Entry<BlockPos, Set<Block>> entry : shape.entrySet()) {
            BlockPos rotated = rotateOffset(entry.getKey(), facing);
            BlockPos abs = controllerPos.offset(rotated);
            result.put(abs, entry.getValue());
        }
        return Collections.unmodifiableMap(result);
    }

    /**
     * Returns whether the given block is expected at the given offset from the controller,
     * regardless of world state. Useful for fast "does this position even belong here?"
     * checks without building the full map.
     *
     * @param offset relative offset from controller (already world-oriented, i.e. after rotation)
     * @param block  block to test
     */
    public boolean matchesOffset(BlockPos offset, Block block) {
        Set<Block> allowed = shape.get(offset);
        return allowed != null && allowed.contains(block);
    }

    /** Returns an unmodifiable view of the relative shape map. */
    public Map<BlockPos, Set<Block>> getRelativeShape() {
        return shape;
    }

    /** Number of non-controller positions in the structure. */
    public int size() {
        return shape.size();
    }

    // -------------------------------------------------------------------------
    // Rotation
    // -------------------------------------------------------------------------

    /**
     * Rotates a relative XZ offset according to the controller's facing direction.
     * Y is unchanged. Matches the rotation convention from the 1.20.1 validators.
     *
     * <p>Facing represents the direction the controller "faces" (front face):
     * <ul>
     *   <li>NORTH → no rotation (identity)</li>
     *   <li>SOUTH → 180°</li>
     *   <li>WEST  → 90° CCW</li>
     *   <li>EAST  → 90° CW</li>
     * </ul>
     */
    private static BlockPos rotateOffset(BlockPos offset, Direction facing) {
        int x = offset.getX(), y = offset.getY(), z = offset.getZ();
        return switch (facing) {
            case NORTH -> new BlockPos( x, y,  z);
            case SOUTH -> new BlockPos(-x, y, -z);
            case WEST  -> new BlockPos( z, y, -x);
            case EAST  -> new BlockPos(-z, y,  x);
            default    -> new BlockPos( x, y,  z); // UP/DOWN — treat as NORTH
        };
    }

    // -------------------------------------------------------------------------
    // Block resolution
    // -------------------------------------------------------------------------

    /**
     * Resolves a JSON array of block ID strings (including wildcards) into a set of
     * registered {@link Block} objects.
     *
     * <p>Dispatch rules:
     * <ul>
     *   <li>Contains {@code "#"} → {@link #resolveGtTierWildcard} (config-driven GT tiers)</li>
     *   <li>Contains {@code "*"} → {@link #resolveGenericWildcard} (all registered blocks)</li>
     *   <li>Otherwise → exact registry lookup</li>
     * </ul>
     */
    private static Set<Block> resolveBlockEntries(JsonArray arr) {
        Set<Block> result = new LinkedHashSet<>();
        for (JsonElement el : arr) {
            String raw = el.getAsString().trim();
            if (raw.isEmpty()) continue;

            if (raw.contains("#")) {
                resolveGtTierWildcard(raw, result);
            } else if (raw.contains("*")) {
                resolveGenericWildcard(raw, result);
            } else {
                ResourceLocation id = ResourceLocation.tryParse(raw);
                if (id == null) continue;
                BuiltInRegistries.BLOCK.getOptional(id).ifPresent(result::add);
            }
        }
        return result;
    }

    /**
     * Generic wildcard: expands one {@code *} in the path by iterating all registered
     * blocks and including those whose namespace + path match the pattern.
     *
     * <p>Example: {@code "somemod:prefix*suffix"} matches every registered block in
     * {@code somemod} whose path starts with {@code prefix} and ends with {@code suffix}.
     *
     * <p>Exactly one {@code *} in the path is supported; the namespace must be exact.
     */
    private static void resolveGenericWildcard(String pattern, Set<Block> out) {
        int sep = pattern.indexOf(':');
        if (sep <= 0 || sep == pattern.length() - 1) return;

        String namespace = pattern.substring(0, sep);
        String pathPattern = pattern.substring(sep + 1);

        int star = pathPattern.indexOf('*');
        if (star < 0 || star != pathPattern.lastIndexOf('*')) return; // 0 or 2+ stars — skip

        String prefix = pathPattern.substring(0, star);
        String suffix = pathPattern.substring(star + 1);

        for (var entry : BuiltInRegistries.BLOCK.entrySet()) {
            ResourceLocation id = entry.getKey().location();
            if (!id.getNamespace().equals(namespace)) continue;
            String path = id.getPath();
            if (path.startsWith(prefix) && path.endsWith(suffix)
                    && path.length() >= prefix.length() + suffix.length()) {
                out.add(entry.getValue());
            }
        }
    }

    /**
     * GregTech tier wildcard: replaces {@code #} with each tier name from
     * {@link CrazyConfig.Common#PenroseGtTiers} and looks up the resulting ID in the
     * block registry. Only tiers that resolve to an actually registered block are added.
     *
     * <p>Example: {@code "gtceu:#_energy_output_hatch"} with tiers {@code [lv, mv, hv]}
     * will attempt {@code gtceu:lv_energy_output_hatch}, {@code gtceu:mv_energy_output_hatch},
     * and {@code gtceu:hv_energy_output_hatch}.
     *
     * <p>Exactly one {@code #} in the path is supported; the namespace must be exact.
     */
    private static void resolveGtTierWildcard(String pattern, Set<Block> out) {
        int sep = pattern.indexOf(':');
        if (sep <= 0 || sep == pattern.length() - 1) return;

        String namespace = pattern.substring(0, sep);
        String pathPattern = pattern.substring(sep + 1);

        int hash = pathPattern.indexOf('#');
        if (hash < 0 || hash != pathPattern.lastIndexOf('#')) return; // 0 or 2+ hashes — skip

        String prefix = pathPattern.substring(0, hash);
        String suffix = pathPattern.substring(hash + 1);

        List<? extends String> tiers = CrazyConfig.COMMON.PenroseGtTiers.get();
        for (String tier : tiers) {
            String resolvedPath = prefix + tier.trim().toLowerCase(java.util.Locale.ROOT) + suffix;
            ResourceLocation id = ResourceLocation.tryParse(namespace + ":" + resolvedPath);
            if (id == null) continue;
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(out::add);
        }
    }
}
