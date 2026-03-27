package net.oktawia.crazyae2addons.misc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Utility for storing structures as compressed StructureTemplate-format NBT.
 * Used by PortableSpatialStorage and PortableAutobuilder (not by AutoBuilderBlock).
 */
public class TemplateUtil {

    public record BlockInfo(BlockPos pos, BlockState state) {}

    // ─── NBT compression ─────────────────────────────────────────────────────

    public static byte[] compressNbt(CompoundTag tag) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, baos);
        return baos.toByteArray();
    }

    public static CompoundTag decompressNbt(byte[] bytes) throws IOException {
        return NbtIo.readCompressed(new ByteArrayInputStream(bytes));
    }

    // ─── Base64 helpers ───────────────────────────────────────────────────────

    public static String toBase64(byte[] bytes) {
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] fromBase64(String s) {
        return java.util.Base64.getDecoder().decode(s);
    }

    // ─── File I/O ─────────────────────────────────────────────────────────────

    public static @Nullable byte[] loadBytesFromFile(ItemStack stack, MinecraftServer server) {
        try {
            if (server == null || stack == null || stack.isEmpty() || !stack.hasTag()) return null;
            var tag = stack.getTag();
            if (tag == null || !tag.getBoolean("code") || !tag.contains("program_id")) return null;
            String id = tag.getString("program_id");
            if (id.isEmpty()) return null;
            Path file = server.getWorldPath(new LevelResource("serverdata"))
                    .resolve("autobuilder").resolve(id);
            if (!Files.exists(file)) return null;
            return Files.readAllBytes(file);
        } catch (Exception e) {
            return null;
        }
    }

    public static void saveBytesToFile(String id, byte[] bytes, MinecraftServer server) {
        try {
            Path file = server.getWorldPath(new LevelResource("serverdata"))
                    .resolve("autobuilder").resolve(id);
            Files.createDirectories(file.getParent());
            Files.write(file, bytes);
        } catch (IOException ignored) {}
    }

    public static void clearStoredStructure(ItemStack stack, @Nullable MinecraftServer server) {
        if (!stack.hasTag()) return;
        var tag = stack.getTag();
        String programId = tag.contains("program_id") ? tag.getString("program_id") : null;
        tag.remove("code");
        tag.remove("program_id");
        tag.remove("delay");
        tag.remove("src_facing");
        clearPreview(stack);
        if (server != null && programId != null && !programId.isEmpty()) {
            try {
                Path file = server.getWorldPath(new LevelResource("serverdata"))
                        .resolve("autobuilder").resolve(programId);
                Files.deleteIfExists(file);
            } catch (Exception ignored) {}
        }
    }

    // ─── Preview NBT ─────────────────────────────────────────────────────────

    public static void clearPreview(ItemStack stack) {
        if (!stack.hasTag()) return;
        var tag = stack.getTag();
        tag.remove("preview_palette");
        tag.remove("preview_indices");
        tag.remove("preview_positions");
    }

    public static void rebuildPreviewFromTag(ItemStack stack, CompoundTag templateTag) {
        if (stack == null || templateTag == null) return;
        clearPreview(stack);
        List<BlockInfo> blocks = parseBlocksFromTag(templateTag);
        if (blocks.isEmpty()) return;

        Map<BlockState, Integer> stateToIdx = new LinkedHashMap<>();
        ListTag palList = new ListTag();
        for (BlockInfo info : blocks) {
            if (!stateToIdx.containsKey(info.state())) {
                stateToIdx.put(info.state(), stateToIdx.size());
                palList.add(StringTag.valueOf(blockStateToSpec(info.state())));
            }
        }

        int n = blocks.size();
        int[] indices = new int[n];
        int[] positions = new int[n * 3];
        for (int i = 0; i < n; i++) {
            BlockInfo info = blocks.get(i);
            indices[i] = stateToIdx.get(info.state());
            positions[i * 3]     = info.pos().getX();
            positions[i * 3 + 1] = info.pos().getY();
            positions[i * 3 + 2] = info.pos().getZ();
        }
        var itemTag = stack.getOrCreateTag();
        itemTag.put("preview_palette", palList);
        itemTag.putIntArray("preview_indices", indices);
        itemTag.putIntArray("preview_positions", positions);
    }

    private static String blockStateToSpec(BlockState state) {
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        StringBuilder sb = new StringBuilder(key != null ? key.toString() : "minecraft:air");
        if (!state.getValues().isEmpty()) {
            sb.append("[");
            boolean first = true;
            for (Map.Entry<Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
                if (!first) sb.append(",");
                sb.append(e.getKey().getName()).append("=").append(e.getValue());
                first = false;
            }
            sb.append("]");
        }
        return sb.toString();
    }

    // ─── Template CompoundTag builder ─────────────────────────────────────────

    public static CompoundTag buildTemplateTag(List<BlockInfo> blockInfos) {
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> paletteIndex = new LinkedHashMap<>();
        for (BlockInfo info : blockInfos) {
            if (!paletteIndex.containsKey(info.state())) {
                paletteIndex.put(info.state(), palette.size());
                palette.add(info.state());
            }
        }

        int minX = 0, minY = 0, minZ = 0, maxX = 0, maxY = 0, maxZ = 0;
        if (!blockInfos.isEmpty()) {
            minX = Integer.MAX_VALUE; minY = Integer.MAX_VALUE; minZ = Integer.MAX_VALUE;
            maxX = Integer.MIN_VALUE; maxY = Integer.MIN_VALUE; maxZ = Integer.MIN_VALUE;
            for (BlockInfo info : blockInfos) {
                BlockPos p = info.pos();
                minX = Math.min(minX, p.getX()); maxX = Math.max(maxX, p.getX());
                minY = Math.min(minY, p.getY()); maxY = Math.max(maxY, p.getY());
                minZ = Math.min(minZ, p.getZ()); maxZ = Math.max(maxZ, p.getZ());
            }
        }

        CompoundTag out = new CompoundTag();

        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(maxX - minX + 1));
        sizeTag.add(IntTag.valueOf(maxY - minY + 1));
        sizeTag.add(IntTag.valueOf(maxZ - minZ + 1));
        out.put("size", sizeTag);

        ListTag paletteTag = new ListTag();
        for (BlockState state : palette) {
            CompoundTag stateTag = new CompoundTag();
            ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
            stateTag.putString("Name", key != null ? key.toString() : "minecraft:air");
            if (!state.getValues().isEmpty()) {
                CompoundTag propsTag = new CompoundTag();
                for (Map.Entry<Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
                    propsTag.putString(e.getKey().getName(), e.getValue().toString());
                }
                stateTag.put("Properties", propsTag);
            }
            paletteTag.add(stateTag);
        }
        out.put("palette", paletteTag);

        ListTag blocksTag = new ListTag();
        for (BlockInfo info : blockInfos) {
            CompoundTag blockTag = new CompoundTag();
            blockTag.putInt("state", paletteIndex.get(info.state()));
            ListTag posTag = new ListTag();
            posTag.add(IntTag.valueOf(info.pos().getX()));
            posTag.add(IntTag.valueOf(info.pos().getY()));
            posTag.add(IntTag.valueOf(info.pos().getZ()));
            blockTag.put("pos", posTag);
            blocksTag.add(blockTag);
        }
        out.put("blocks", blocksTag);
        out.put("entities", new ListTag());
        out.putInt("DataVersion",
                net.minecraft.SharedConstants.getCurrentVersion().getDataVersion().getVersion());
        return out;
    }

    // ─── Template CompoundTag parser ──────────────────────────────────────────

    public static List<BlockInfo> parseBlocksFromTag(CompoundTag tag) {
        List<BlockInfo> out = new ArrayList<>();
        if (tag == null) return out;

        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(parseBlockStateFromTag(paletteTag.getCompound(i)));
        }

        ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            int stateIdx = blockTag.getInt("state");
            if (stateIdx < 0 || stateIdx >= palette.size()) continue;
            BlockState state = palette.get(stateIdx);
            if (state == null) continue;
            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            if (posTag.size() < 3) continue;
            BlockPos pos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
            out.add(new BlockInfo(pos, state));
        }
        return out;
    }

    private static @Nullable BlockState parseBlockStateFromTag(CompoundTag tag) {
        ResourceLocation rl = ResourceLocation.tryParse(tag.getString("Name"));
        if (rl == null) return null;
        Block block = ForgeRegistries.BLOCKS.getValue(rl);
        if (block == null) return null;
        BlockState state = block.defaultBlockState();
        if (!tag.contains("Properties", Tag.TAG_COMPOUND)) return state;
        CompoundTag props = tag.getCompound("Properties");
        StateDefinition<?, ?> def = block.getStateDefinition();
        for (String key : props.getAllKeys()) {
            Property<?> prop = def.getProperty(key);
            if (prop == null) continue;
            Optional<?> val = ((Property) prop).getValue(props.getString(key));
            if (val.isPresent()) state = setUnchecked(state, prop, (Comparable) val.get());
        }
        return state;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setUnchecked(BlockState state, Property prop, Comparable value) {
        return state.setValue(prop, value);
    }

    // ─── Transforms ──────────────────────────────────────────────────────────

    private static CompoundTag blockStateToTag(BlockState state) {
        CompoundTag tag = new CompoundTag();
        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        tag.putString("Name", key != null ? key.toString() : "minecraft:air");
        if (!state.getValues().isEmpty()) {
            CompoundTag props = new CompoundTag();
            for (Map.Entry<Property<?>, Comparable<?>> e : state.getValues().entrySet()) {
                props.putString(e.getKey().getName(), e.getValue().toString());
            }
            tag.put("Properties", props);
        }
        return tag;
    }

    /**
     * Mirror around the local-X (horizontal) centre.
     * srcFacing is the direction the player was facing when the structure was recorded —
     * it determines which world mirror axis corresponds to local-X:
     *   NORTH/SOUTH recording → local.x = world east-west → Mirror.FRONT_BACK (flips EAST↔WEST)
     *   EAST/WEST  recording → local.x = world north-south → Mirror.LEFT_RIGHT (flips NORTH↔SOUTH)
     */
    public static CompoundTag applyFlipHToTag(CompoundTag tag, Direction srcFacing) {
        Mirror mirror = (srcFacing.getAxis() == Direction.Axis.Z) ? Mirror.FRONT_BACK : Mirror.LEFT_RIGHT;
        return applyTransform(tag,
                (x, y, z, minX, maxX, minY, maxY, minZ, maxZ) -> new int[]{minX + maxX - x, y, z},
                state -> state.mirror(mirror));
    }

    /** Mirror around the vertical (Y) centre. Block facing states don't have a Y-axis mirror equivalent. */
    public static CompoundTag applyFlipVToTag(CompoundTag tag) {
        return applyTransform(tag,
                (x, y, z, minX, maxX, minY, maxY, minZ, maxZ) -> new int[]{x, minY + maxY - y, z},
                UnaryOperator.identity());
    }

    /** Rotate in XZ plane around the structure's own centre (t=1 CCW, t=3 CW). */
    public static CompoundTag applyRotateCWToTag(CompoundTag tag, int times) {
        times = ((times % 4) + 4) % 4;
        if (times == 0) return tag;
        final int t = times;
        Rotation rotation = switch (t) {
            case 1 -> Rotation.COUNTERCLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.CLOCKWISE_90;
            default -> Rotation.NONE;
        };
        return applyTransform(tag, (x, y, z, minX, maxX, minY, maxY, minZ, maxZ) -> {
            // Rotate around the bounding-box XZ centre, not around origin
            double cx = (minX + maxX) / 2.0;
            double cz = (minZ + maxZ) / 2.0;
            int rx, rz;
            switch (t) {
                case 1 -> { // CCW 90°
                    rx = (int) Math.round(cx - (z - cz));
                    rz = (int) Math.round(cz + (x - cx));
                }
                case 2 -> { // 180° — always integer (same as flipH+flipV)
                    rx = minX + maxX - x;
                    rz = minZ + maxZ - z;
                }
                case 3 -> { // CW 90°
                    rx = (int) Math.round(cx + (z - cz));
                    rz = (int) Math.round(cz - (x - cx));
                }
                default -> { rx = x; rz = z; }
            }
            return new int[]{rx, y, rz};
        }, state -> state.rotate(rotation));
    }

    @FunctionalInterface
    private interface Transform {
        int[] apply(int x, int y, int z, int minX, int maxX, int minY, int maxY, int minZ, int maxZ);
    }

    private static CompoundTag applyTransform(CompoundTag tag, Transform posTransform,
                                               UnaryOperator<BlockState> stateTransform) {
        ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        int n = blocksTag.size();
        if (n == 0) return tag;

        // Parse existing palette into BlockStates
        BlockState[] oldPalette = new BlockState[paletteTag.size()];
        for (int i = 0; i < paletteTag.size(); i++) {
            oldPalette[i] = parseBlockStateFromTag(paletteTag.getCompound(i));
        }

        // Build new palette by applying state transform (deduplicated, preserving first-seen order).
        // Unknown blocks (null parse result) keep their original palette tag unchanged.
        List<BlockState> newPaletteList = new ArrayList<>();
        List<CompoundTag> newPaletteRawList = new ArrayList<>(); // for null-state entries
        Map<BlockState, Integer> newPaletteIndex = new LinkedHashMap<>();
        int[] oldToNew = new int[oldPalette.length];
        for (int i = 0; i < oldPalette.length; i++) {
            BlockState src = oldPalette[i];
            if (src == null) {
                // Unknown block – keep original palette tag, append at end of new palette
                oldToNew[i] = newPaletteList.size();
                newPaletteList.add(null); // placeholder
                newPaletteRawList.add(paletteTag.getCompound(i).copy());
            } else {
                BlockState transformed = stateTransform.apply(src);
                if (transformed == null) transformed = src;
                final BlockState key = transformed;
                oldToNew[i] = newPaletteIndex.computeIfAbsent(key, s -> {
                    int idx = newPaletteList.size();
                    newPaletteList.add(s);
                    newPaletteRawList.add(null); // will be built from state
                    return idx;
                });
            }
        }

        // Collect positions
        int[] xs = new int[n], ys = new int[n], zs = new int[n];
        int[] stateIndices = new int[n];
        for (int i = 0; i < n; i++) {
            CompoundTag b = blocksTag.getCompound(i);
            ListTag pos = b.getList("pos", Tag.TAG_INT);
            xs[i] = pos.getInt(0);
            ys[i] = pos.getInt(1);
            zs[i] = pos.getInt(2);
            stateIndices[i] = b.getInt("state");
        }

        int minX = xs[0], maxX = xs[0], minY = ys[0], maxY = ys[0], minZ = zs[0], maxZ = zs[0];
        for (int i = 1; i < n; i++) {
            minX = Math.min(minX, xs[i]); maxX = Math.max(maxX, xs[i]);
            minY = Math.min(minY, ys[i]); maxY = Math.max(maxY, ys[i]);
            minZ = Math.min(minZ, zs[i]); maxZ = Math.max(maxZ, zs[i]);
        }

        int newMinX = Integer.MAX_VALUE, newMinY = Integer.MAX_VALUE, newMinZ = Integer.MAX_VALUE;
        int newMaxX = Integer.MIN_VALUE, newMaxY = Integer.MIN_VALUE, newMaxZ = Integer.MIN_VALUE;
        ListTag newBlocksTag = new ListTag();
        for (int i = 0; i < n; i++) {
            int[] np = posTransform.apply(xs[i], ys[i], zs[i], minX, maxX, minY, maxY, minZ, maxZ);
            newMinX = Math.min(newMinX, np[0]); newMaxX = Math.max(newMaxX, np[0]);
            newMinY = Math.min(newMinY, np[1]); newMaxY = Math.max(newMaxY, np[1]);
            newMinZ = Math.min(newMinZ, np[2]); newMaxZ = Math.max(newMaxZ, np[2]);

            CompoundTag blockTag = blocksTag.getCompound(i).copy();
            ListTag newPosTag = new ListTag();
            newPosTag.add(IntTag.valueOf(np[0]));
            newPosTag.add(IntTag.valueOf(np[1]));
            newPosTag.add(IntTag.valueOf(np[2]));
            blockTag.put("pos", newPosTag);
            int oldStateIdx = stateIndices[i];
            blockTag.putInt("state", oldStateIdx < oldToNew.length ? oldToNew[oldStateIdx] : 0);
            newBlocksTag.add(blockTag);
        }

        // Build new palette tag
        ListTag newPaletteTag = new ListTag();
        for (int i = 0; i < newPaletteList.size(); i++) {
            BlockState state = newPaletteList.get(i);
            CompoundTag raw = newPaletteRawList.get(i);
            newPaletteTag.add(state != null ? blockStateToTag(state) : (raw != null ? raw : new CompoundTag()));
        }

        CompoundTag result = tag.copy();
        result.put("blocks", newBlocksTag);
        result.put("palette", newPaletteTag);
        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(newMaxX - newMinX + 1));
        sizeTag.add(IntTag.valueOf(newMaxY - newMinY + 1));
        sizeTag.add(IntTag.valueOf(newMaxZ - newMinZ + 1));
        result.put("size", sizeTag);
        return result;
    }
}
