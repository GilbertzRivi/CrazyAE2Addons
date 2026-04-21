package net.oktawia.crazyae2addons.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.UnaryOperator;

public final class TemplateUtil {

    public record BlockInfo(BlockPos pos, BlockState state, @Nullable CompoundTag blockEntityTag) {}

    private static final String AE2_CABLE_BUS_ID = "ae2:cable_bus";

    private static final String KEY_NORTH = "north";
    private static final String KEY_SOUTH = "south";
    private static final String KEY_EAST = "east";
    private static final String KEY_WEST = "west";
    private static final String KEY_UP = "up";
    private static final String KEY_DOWN = "down";
    private static final String KEY_CABLE = "cable";

    private TemplateUtil() {
    }

    public static byte[] compressNbt(CompoundTag tag) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        NbtIo.writeCompressed(tag, output);
        return output.toByteArray();
    }

    public static CompoundTag decompressNbt(byte[] bytes) throws IOException {
        return NbtIo.readCompressed(new ByteArrayInputStream(bytes));
    }

    public static String toBase64(byte[] bytes) {
        return java.util.Base64.getEncoder().encodeToString(bytes);
    }

    public static byte[] fromBase64(String value) {
        return java.util.Base64.getDecoder().decode(value);
    }

    public static List<BlockInfo> parseBlocksFromTag(CompoundTag tag) {
        List<BlockInfo> out = new ArrayList<>();
        if (tag == null) {
            return out;
        }

        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);
        List<BlockState> palette = new ArrayList<>(paletteTag.size());
        for (int i = 0; i < paletteTag.size(); i++) {
            palette.add(parseBlockStateFromTag(paletteTag.getCompound(i)));
        }

        ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            int stateIdx = blockTag.getInt("state");
            if (stateIdx < 0 || stateIdx >= palette.size()) {
                continue;
            }

            BlockState state = palette.get(stateIdx);
            if (state == null) {
                continue;
            }

            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            if (posTag.size() < 3) {
                continue;
            }

            BlockPos pos = new BlockPos(posTag.getInt(0), posTag.getInt(1), posTag.getInt(2));
            CompoundTag blockEntityTag = blockTag.contains("nbt", Tag.TAG_COMPOUND)
                    ? blockTag.getCompound("nbt").copy()
                    : null;

            out.add(new BlockInfo(pos, state, blockEntityTag));
        }

        return out;
    }

    public static CompoundTag applyFlipHToTag(CompoundTag tag, Direction sourceFacing) {
        Mirror mirror = sourceFacing.getAxis() == Direction.Axis.Z
                ? Mirror.FRONT_BACK
                : Mirror.LEFT_RIGHT;

        CableBusTransform cableBusTransform = sourceFacing.getAxis() == Direction.Axis.Z
                ? CableBusTransform.FLIP_H_AXIS_Z
                : CableBusTransform.FLIP_H_AXIS_X;

        return applyTransform(
                tag,
                (x, y, z, minX, maxX, minY, maxY, minZ, maxZ) -> new int[]{minX + maxX - x, y, z},
                state -> state.mirror(mirror),
                cableBusTransform
        );
    }

    public static CompoundTag applyFlipVToTag(CompoundTag tag) {
        return applyTransform(
                tag,
                (x, y, z, minX, maxX, minY, maxY, minZ, maxZ) ->
                        new int[]{x, minY + maxY - y, z},
                TemplateUtil::flipVerticalState,
                CableBusTransform.FLIP_V
        );
    }

    public static CompoundTag applyRotateCWToTag(CompoundTag tag, int times) {
        int normalizedTurns = ((times % 4) + 4) % 4;
        if (normalizedTurns == 0) {
            return tag;
        }

        Rotation rotation = switch (normalizedTurns) {
            case 1 -> Rotation.CLOCKWISE_90;
            case 2 -> Rotation.CLOCKWISE_180;
            case 3 -> Rotation.COUNTERCLOCKWISE_90;
            default -> Rotation.NONE;
        };

        CableBusTransform cableBusTransform = switch (normalizedTurns) {
            case 1 -> CableBusTransform.ROTATE_CW;
            case 2 -> CableBusTransform.ROTATE_180;
            case 3 -> CableBusTransform.ROTATE_CCW;
            default -> CableBusTransform.NONE;
        };

        return applyTransform(
                tag,
                (x, y, z, minX, maxX, minY, maxY, minZ, maxZ) -> switch (normalizedTurns) {
                    case 1 -> new int[]{
                            minX + (maxZ - z),
                            y,
                            minZ + (x - minX)
                    };
                    case 2 -> new int[]{
                            minX + (maxX - x),
                            y,
                            minZ + (maxZ - z)
                    };
                    case 3 -> new int[]{
                            minX + (z - minZ),
                            y,
                            minZ + (maxX - x)
                    };
                    default -> new int[]{x, y, z};
                },
                state -> state.rotate(rotation),
                cableBusTransform
        );
    }

    @FunctionalInterface
    private interface Transform {
        int[] apply(int x, int y, int z, int minX, int maxX, int minY, int maxY, int minZ, int maxZ);
    }

    private enum CableBusTransform {
        NONE,
        ROTATE_CW,
        ROTATE_180,
        ROTATE_CCW,
        FLIP_H_AXIS_Z,
        FLIP_H_AXIS_X,
        FLIP_V
    }

    private static CompoundTag applyTransform(
            CompoundTag tag,
            Transform positionTransform,
            UnaryOperator<BlockState> stateTransform,
            CableBusTransform cableBusTransform
    ) {
        ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);

        if (blocksTag.isEmpty() || paletteTag.isEmpty()) {
            return tag;
        }

        BlockState[] oldPalette = new BlockState[paletteTag.size()];
        for (int i = 0; i < paletteTag.size(); i++) {
            oldPalette[i] = parseBlockStateFromTag(paletteTag.getCompound(i));
        }

        List<BlockState> newPaletteStates = new ArrayList<>();
        List<CompoundTag> newPaletteFallbacks = new ArrayList<>();
        Map<BlockState, Integer> newPaletteIndex = new LinkedHashMap<>();
        int[] oldToNew = new int[oldPalette.length];

        for (int i = 0; i < oldPalette.length; i++) {
            BlockState oldState = oldPalette[i];
            if (oldState == null) {
                oldToNew[i] = newPaletteStates.size();
                newPaletteStates.add(null);
                newPaletteFallbacks.add(paletteTag.getCompound(i).copy());
                continue;
            }

            BlockState transformedState = stateTransform.apply(oldState);
            if (transformedState == null) {
                transformedState = oldState;
            }

            BlockState key = transformedState;
            oldToNew[i] = newPaletteIndex.computeIfAbsent(key, ignored -> {
                int index = newPaletteStates.size();
                newPaletteStates.add(key);
                newPaletteFallbacks.add(null);
                return index;
            });
        }

        int blockCount = blocksTag.size();
        int[] tx = new int[blockCount];
        int[] ty = new int[blockCount];
        int[] tz = new int[blockCount];
        int[] mappedStateIndex = new int[blockCount];
        CompoundTag[] copiedEntries = new CompoundTag[blockCount];

        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (int i = 0; i < blockCount; i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            if (posTag.size() < 3) {
                continue;
            }

            int x = posTag.getInt(0);
            int y = posTag.getInt(1);
            int z = posTag.getInt(2);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);
        }

        int newMinX = Integer.MAX_VALUE;
        int newMinY = Integer.MAX_VALUE;
        int newMinZ = Integer.MAX_VALUE;

        for (int i = 0; i < blockCount; i++) {
            CompoundTag blockTag = blocksTag.getCompound(i).copy();
            copiedEntries[i] = blockTag;

            ListTag posTag = blockTag.getList("pos", Tag.TAG_INT);
            if (posTag.size() < 3) {
                tx[i] = 0;
                ty[i] = 0;
                tz[i] = 0;
            } else {
                int[] transformed = positionTransform.apply(
                        posTag.getInt(0),
                        posTag.getInt(1),
                        posTag.getInt(2),
                        minX,
                        maxX,
                        minY,
                        maxY,
                        minZ,
                        maxZ
                );

                tx[i] = transformed[0];
                ty[i] = transformed[1];
                tz[i] = transformed[2];

                newMinX = Math.min(newMinX, tx[i]);
                newMinY = Math.min(newMinY, ty[i]);
                newMinZ = Math.min(newMinZ, tz[i]);
            }

            int oldStateIndex = blockTag.getInt("state");
            mappedStateIndex[i] = oldStateIndex >= 0 && oldStateIndex < oldToNew.length
                    ? oldToNew[oldStateIndex]
                    : 0;

            if (blockTag.contains("nbt", Tag.TAG_COMPOUND)) {
                CompoundTag transformedBlockEntityTag = transformBlockEntityTag(
                        blockTag.getCompound("nbt"),
                        cableBusTransform
                );
                blockTag.put("nbt", transformedBlockEntityTag);
            }
        }

        int newMaxX = Integer.MIN_VALUE;
        int newMaxY = Integer.MIN_VALUE;
        int newMaxZ = Integer.MIN_VALUE;

        ListTag newBlocksTag = new ListTag();
        for (int i = 0; i < blockCount; i++) {
            CompoundTag blockTag = copiedEntries[i];

            int normalizedX = tx[i] - newMinX;
            int normalizedY = ty[i] - newMinY;
            int normalizedZ = tz[i] - newMinZ;

            ListTag newPosTag = new ListTag();
            newPosTag.add(IntTag.valueOf(normalizedX));
            newPosTag.add(IntTag.valueOf(normalizedY));
            newPosTag.add(IntTag.valueOf(normalizedZ));

            blockTag.put("pos", newPosTag);
            blockTag.putInt("state", mappedStateIndex[i]);

            newMaxX = Math.max(newMaxX, normalizedX);
            newMaxY = Math.max(newMaxY, normalizedY);
            newMaxZ = Math.max(newMaxZ, normalizedZ);

            newBlocksTag.add(blockTag);
        }

        ListTag newPaletteTag = new ListTag();
        for (int i = 0; i < newPaletteStates.size(); i++) {
            BlockState state = newPaletteStates.get(i);
            CompoundTag fallback = newPaletteFallbacks.get(i);
            newPaletteTag.add(state != null ? blockStateToTag(state) : fallback.copy());
        }

        CompoundTag result = tag.copy();
        result.put("blocks", newBlocksTag);
        result.put("palette", newPaletteTag);

        ListTag sizeTag = new ListTag();
        sizeTag.add(IntTag.valueOf(newMaxX + 1));
        sizeTag.add(IntTag.valueOf(newMaxY + 1));
        sizeTag.add(IntTag.valueOf(newMaxZ + 1));
        result.put("size", sizeTag);

        return result;
    }

    private static CompoundTag transformBlockEntityTag(CompoundTag tag, CableBusTransform transform) {
        if (transform == CableBusTransform.NONE) {
            return tag.copy();
        }

        String id = tag.getString("id");
        if (!AE2_CABLE_BUS_ID.equals(id)) {
            return tag.copy();
        }

        return transformCableBusTag(tag, transform);
    }

    private static CompoundTag transformCableBusTag(CompoundTag tag, CableBusTransform transform) {
        CompoundTag result = tag.copy();

        Tag north = tag.get(KEY_NORTH);
        Tag south = tag.get(KEY_SOUTH);
        Tag east = tag.get(KEY_EAST);
        Tag west = tag.get(KEY_WEST);
        Tag up = tag.get(KEY_UP);
        Tag down = tag.get(KEY_DOWN);
        Tag cable = tag.get(KEY_CABLE);

        result.remove(KEY_NORTH);
        result.remove(KEY_SOUTH);
        result.remove(KEY_EAST);
        result.remove(KEY_WEST);
        result.remove(KEY_UP);
        result.remove(KEY_DOWN);
        result.remove(KEY_CABLE);

        putMovedSide(result, transform, Direction.NORTH, north);
        putMovedSide(result, transform, Direction.SOUTH, south);
        putMovedSide(result, transform, Direction.EAST, east);
        putMovedSide(result, transform, Direction.WEST, west);
        putMovedSide(result, transform, Direction.UP, up);
        putMovedSide(result, transform, Direction.DOWN, down);

        if (cable != null) {
            result.put(KEY_CABLE, cable.copy());
        }

        return result;
    }

    private static void putMovedSide(CompoundTag target, CableBusTransform transform, Direction fromSide, @Nullable Tag sideTag) {
        if (sideTag == null) {
            return;
        }

        Direction toSide = mapCableBusSide(fromSide, transform);
        target.put(directionKey(toSide), sideTag.copy());
    }

    private static Direction mapCableBusSide(Direction side, CableBusTransform transform) {
        return switch (transform) {
            case ROTATE_CW -> switch (side) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
            case ROTATE_180 -> switch (side) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
            case ROTATE_CCW -> switch (side) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
            case FLIP_H_AXIS_Z -> switch (side) {
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                default -> side;
            };
            case FLIP_H_AXIS_X -> switch (side) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                default -> side;
            };
            case FLIP_V -> switch (side) {
                case UP -> Direction.DOWN;
                case DOWN -> Direction.UP;
                default -> side;
            };
            case NONE -> side;
        };
    }

    private static String directionKey(Direction direction) {
        return switch (direction) {
            case NORTH -> KEY_NORTH;
            case SOUTH -> KEY_SOUTH;
            case EAST -> KEY_EAST;
            case WEST -> KEY_WEST;
            case UP -> KEY_UP;
            case DOWN -> KEY_DOWN;
        };
    }

    private static CompoundTag blockStateToTag(BlockState state) {
        CompoundTag tag = new CompoundTag();

        ResourceLocation key = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        tag.putString("Name", key != null ? key.toString() : "minecraft:air");

        if (!state.getValues().isEmpty()) {
            CompoundTag properties = new CompoundTag();
            for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
                properties.putString(entry.getKey().getName(), entry.getValue().toString());
            }
            tag.put("Properties", properties);
        }

        return tag;
    }

    private static @Nullable BlockState parseBlockStateFromTag(CompoundTag tag) {
        ResourceLocation resourceLocation = ResourceLocation.tryParse(tag.getString("Name"));
        if (resourceLocation == null) {
            return null;
        }

        Block block = ForgeRegistries.BLOCKS.getValue(resourceLocation);
        if (block == null) {
            return null;
        }

        BlockState state = block.defaultBlockState();
        if (!tag.contains("Properties", Tag.TAG_COMPOUND)) {
            return state;
        }

        CompoundTag properties = tag.getCompound("Properties");
        StateDefinition<?, ?> definition = block.getStateDefinition();

        for (String key : properties.getAllKeys()) {
            Property<?> property = definition.getProperty(key);
            if (property == null) {
                continue;
            }

            Optional<?> value = ((Property<?>) property).getValue(properties.getString(key));
            if (value.isPresent()) {
                state = setUnchecked(state, property, (Comparable<?>) value.get());
            }
        }

        return state;
    }

    private static BlockState setUnchecked(BlockState state, Property property, Comparable value) {
        return state.setValue(property, value);
    }

    private static BlockState flipVerticalState(BlockState state) {
        BlockState result = state;

        result = remapPropertyValues(result, "half", java.util.Map.of(
                "top", "bottom",
                "bottom", "top",
                "upper", "lower",
                "lower", "upper"
        ));

        result = remapPropertyValues(result, "face", java.util.Map.of(
                "floor", "ceiling",
                "ceiling", "floor"
        ));

        result = flipVerticalDirectionProperties(result);

        return result;
    }

    private static BlockState remapPropertyValues(BlockState state, String propertyName, java.util.Map<String, String> mapping) {
        Property property = state.getBlock().getStateDefinition().getProperty(propertyName);
        if (property == null) {
            return state;
        }

        Object currentValue = state.getValue(property);
        if (currentValue == null) {
            return state;
        }

        String targetValueName = mapping.get(currentValue.toString());
        if (targetValueName == null) {
            return state;
        }

        Optional parsed = property.getValue(targetValueName);
        if (parsed.isEmpty()) {
            return state;
        }

        return setUnchecked(state, property, (Comparable) parsed.get());
    }

    private static BlockState flipVerticalDirectionProperties(BlockState state) {
        BlockState result = state;

        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();

            if (value instanceof Direction direction && direction.getAxis() == Direction.Axis.Y) {
                Direction flipped = direction == Direction.UP ? Direction.DOWN : Direction.UP;
                result = setUnchecked(result, property, flipped);
            }
        }

        return result;
    }
}