package net.oktawia.crazyae2addons.util;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
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
    private static final String GTCEU_ID_PREFIX = "gtceu:";

    private static final String KEY_NORTH = "north";
    private static final String KEY_SOUTH = "south";
    private static final String KEY_EAST = "east";
    private static final String KEY_WEST = "west";
    private static final String KEY_UP = "up";
    private static final String KEY_DOWN = "down";
    private static final String KEY_CABLE = "cable";
    private static final String KEY_COVER = "cover";

    public static final String TEMPLATE_OFFSET_X_KEY = "crazy_template_offset_x";
    public static final String TEMPLATE_OFFSET_Y_KEY = "crazy_template_offset_y";
    public static final String TEMPLATE_OFFSET_Z_KEY = "crazy_template_offset_z";

    public static final String ENERGY_ORIGIN_KEY = "energyOrigin";

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

    public static BlockPos getTemplateOffset(@Nullable CompoundTag tag) {
        if (tag == null) {
            return BlockPos.ZERO;
        }

        return new BlockPos(
                clampOffset(tag.getInt(TEMPLATE_OFFSET_X_KEY)),
                clampOffset(tag.getInt(TEMPLATE_OFFSET_Y_KEY)),
                clampOffset(tag.getInt(TEMPLATE_OFFSET_Z_KEY))
        );
    }

    public static void setTemplateOffset(CompoundTag tag, BlockPos offset) {
        if (tag == null) {
            return;
        }

        tag.putInt(TEMPLATE_OFFSET_X_KEY, clampOffset(offset.getX()));
        tag.putInt(TEMPLATE_OFFSET_Y_KEY, clampOffset(offset.getY()));
        tag.putInt(TEMPLATE_OFFSET_Z_KEY, clampOffset(offset.getZ()));
    }

    public static CompoundTag applyOffsetToTag(CompoundTag tag, int dx, int dy, int dz) {
        CompoundTag result = tag.copy();
        BlockPos current = getTemplateOffset(result);

        setTemplateOffset(result, new BlockPos(
                current.getX() + dx,
                current.getY() + dy,
                current.getZ() + dz
        ));

        return result;
    }

    private static int clampOffset(int value) {
        return Mth.clamp(value, -99, 99);
    }

    public static BlockPos getEnergyOrigin(@Nullable CompoundTag tag) {
        if (tag == null || !tag.contains(ENERGY_ORIGIN_KEY, Tag.TAG_COMPOUND)) {
            return BlockPos.ZERO;
        }

        CompoundTag origin = tag.getCompound(ENERGY_ORIGIN_KEY);
        return new BlockPos(origin.getInt("x"), origin.getInt("y"), origin.getInt("z"));
    }

    public static void setEnergyOrigin(CompoundTag tag, BlockPos pos) {
        CompoundTag origin = new CompoundTag();
        origin.putInt("x", pos.getX());
        origin.putInt("y", pos.getY());
        origin.putInt("z", pos.getZ());
        tag.put(ENERGY_ORIGIN_KEY, origin);
    }

    public static void copyPreviewTransformState(CompoundTag source, CompoundTag target) {
        setTemplateOffset(target, getTemplateOffset(source));
        setEnergyOrigin(target, getEnergyOrigin(source));
    }

    public static List<BlockInfo> parseBlocksFromTag(CompoundTag tag) {
        return parseBlocksFromTag(tag, true);
    }

    public static List<BlockInfo> parseRawBlocksFromTag(CompoundTag tag) {
        return parseBlocksFromTag(tag, false);
    }

    private static List<BlockInfo> parseBlocksFromTag(CompoundTag tag, boolean applyTemplateOffset) {
        List<BlockInfo> out = new ArrayList<>();
        if (tag == null) {
            return out;
        }

        BlockPos templateOffset = applyTemplateOffset ? getTemplateOffset(tag) : BlockPos.ZERO;

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

            BlockPos pos = new BlockPos(
                    posTag.getInt(0),
                    posTag.getInt(1),
                    posTag.getInt(2)
            ).offset(templateOffset);

            CompoundTag blockEntityTag = blockTag.contains("nbt", Tag.TAG_COMPOUND)
                    ? blockTag.getCompound("nbt").copy()
                    : null;

            out.add(new BlockInfo(pos, state, blockEntityTag));
        }

        return out;
    }

    public static CompoundTag stripAirFromTag(CompoundTag tag) {
        if (tag == null) {
            return new CompoundTag();
        }

        CompoundTag result = tag.copy();

        ListTag paletteTag = result.getList("palette", Tag.TAG_COMPOUND);
        ListTag blocksTag = result.getList("blocks", Tag.TAG_COMPOUND);

        if (paletteTag.isEmpty() || blocksTag.isEmpty()) {
            return result;
        }

        BlockState[] palette = new BlockState[paletteTag.size()];
        for (int i = 0; i < paletteTag.size(); i++) {
            palette[i] = parseBlockStateFromTag(paletteTag.getCompound(i));
        }

        ListTag filteredBlocks = new ListTag();

        for (int i = 0; i < blocksTag.size(); i++) {
            CompoundTag blockTag = blocksTag.getCompound(i);
            int stateIdx = blockTag.getInt("state");

            if (stateIdx < 0 || stateIdx >= palette.length) {
                continue;
            }

            BlockState state = palette[stateIdx];
            if (state == null || state.isAir()) {
                continue;
            }

            filteredBlocks.add(blockTag.copy());
        }

        result.put("blocks", filteredBlocks);
        return result;
    }

    public static CompoundTag applyFlipHToTag(CompoundTag tag, Direction sourceFacing) {
        CableBusTransform cableBusTransform = sourceFacing.getAxis() == Direction.Axis.Z
                ? CableBusTransform.FLIP_H_AXIS_Z
                : CableBusTransform.FLIP_H_AXIS_X;

        return applyTransform(
                tag,
                (x, y, z, minX, maxX, minY, maxY, minZ, maxZ) -> new int[]{minX + maxX - x, y, z},
                state -> flipHorizontalState(state, sourceFacing),
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
                state -> rotateState(state, rotation),
                cableBusTransform
        );
    }

    public static CompoundTag applyFlipHAroundOriginToTag(CompoundTag tag, Direction sourceFacing) {
        CompoundTag transformed = applyFlipHToTag(tag, sourceFacing);
        setTemplateOffset(transformed, flipHorizontalOffset(getTemplateOffset(tag), sourceFacing));
        return transformed;
    }

    public static CompoundTag applyFlipVAroundOriginToTag(CompoundTag tag) {
        CompoundTag transformed = applyFlipVToTag(tag);
        setTemplateOffset(transformed, flipVerticalOffset(getTemplateOffset(tag)));
        return transformed;
    }

    public static CompoundTag applyRotateCWAroundOriginToTag(CompoundTag tag, int times) {
        int normalizedTurns = ((times % 4) + 4) % 4;
        if (normalizedTurns == 0) {
            return tag;
        }

        CompoundTag transformed = applyRotateCWToTag(tag, times);
        setTemplateOffset(transformed, rotateOffsetCW(getTemplateOffset(tag), normalizedTurns));
        return transformed;
    }

    private static BlockPos rotateOffsetCW(BlockPos offset, int normalizedTurns) {
        return switch (normalizedTurns) {
            case 1 -> new BlockPos(
                    clampOffset(-offset.getZ()),
                    clampOffset(offset.getY()),
                    clampOffset(offset.getX())
            );
            case 2 -> new BlockPos(
                    clampOffset(-offset.getX()),
                    clampOffset(offset.getY()),
                    clampOffset(-offset.getZ())
            );
            case 3 -> new BlockPos(
                    clampOffset(offset.getZ()),
                    clampOffset(offset.getY()),
                    clampOffset(-offset.getX())
            );
            default -> offset;
        };
    }

    private static BlockPos flipHorizontalOffset(BlockPos offset, Direction sourceFacing) {
        if (sourceFacing.getAxis() == Direction.Axis.Z) {
            return new BlockPos(
                    clampOffset(-offset.getX()),
                    clampOffset(offset.getY()),
                    clampOffset(offset.getZ())
            );
        }

        return new BlockPos(
                clampOffset(offset.getX()),
                clampOffset(offset.getY()),
                clampOffset(-offset.getZ())
        );
    }

    private static BlockPos flipVerticalOffset(BlockPos offset) {
        return new BlockPos(
                clampOffset(offset.getX()),
                clampOffset(-offset.getY()),
                clampOffset(offset.getZ())
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
        return applyTransformInternal(tag, positionTransform, stateTransform, cableBusTransform);
    }

    private static CompoundTag applyTransformInternal(
            CompoundTag tag,
            Transform positionTransform,
            UnaryOperator<BlockState> stateTransform,
            CableBusTransform cableBusTransform
    ) {
        ListTag blocksTag = tag.getList("blocks", Tag.TAG_COMPOUND);
        ListTag paletteTag = tag.getList("palette", Tag.TAG_COMPOUND);

        if (blocksTag.isEmpty() || paletteTag.isEmpty()) {
            return tag.copy();
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

        BlockPos oldOrigin = getEnergyOrigin(tag);
        BlockPos oldOffset = getTemplateOffset(tag);

        int[] transformedOrigin = positionTransform.apply(
                oldOrigin.getX(),
                oldOrigin.getY(),
                oldOrigin.getZ(),
                minX,
                maxX,
                minY,
                maxY,
                minZ,
                maxZ
        );

        BlockPos newOrigin = new BlockPos(
                transformedOrigin[0] - newMinX,
                transformedOrigin[1] - newMinY,
                transformedOrigin[2] - newMinZ
        );

        setEnergyOrigin(result, newOrigin);
        setTemplateOffset(result, new BlockPos(
                clampOffset(oldOffset.getX() + newOrigin.getX() - oldOrigin.getX()),
                clampOffset(oldOffset.getY() + newOrigin.getY() - oldOrigin.getY()),
                clampOffset(oldOffset.getZ() + newOrigin.getZ() - oldOrigin.getZ())
        ));

        return result;
    }

    private static CompoundTag transformBlockEntityTag(CompoundTag tag, CableBusTransform transform) {
        if (transform == CableBusTransform.NONE) {
            return tag.copy();
        }

        String id = tag.getString("id");

        if (AE2_CABLE_BUS_ID.equals(id)) {
            return transformCableBusTag(tag, transform);
        }

        if (!id.isBlank() && id.startsWith(GTCEU_ID_PREFIX)) {
            return transformGregBlockEntityTag(tag, transform);
        }

        return tag.copy();
    }

    private static CompoundTag transformGregBlockEntityTag(CompoundTag tag, CableBusTransform transform) {
        CompoundTag result = tag.copy();

        if (result.contains("connections", Tag.TAG_INT)) {
            result.putInt("connections", remapGregConnectionMask(result.getInt("connections"), transform));
        }

        if (result.contains("blockedConnections", Tag.TAG_INT)) {
            result.putInt("blockedConnections", remapGregConnectionMask(result.getInt("blockedConnections"), transform));
        }

        if (result.contains(KEY_COVER, Tag.TAG_COMPOUND)) {
            CompoundTag transformedCoverTag = transformGregPipeCoverTag(result.getCompound(KEY_COVER), transform);
            result.put(KEY_COVER, transformedCoverTag);
        }

        remapGregDirectionalFieldsInPlace(result, transform);

        return result;
    }

    private static void remapGregDirectionalFieldsInPlace(CompoundTag tag, CableBusTransform transform) {
        List<String> keys = new ArrayList<>(tag.getAllKeys());

        for (String key : keys) {
            if (KEY_COVER.equals(key)) {
                continue;
            }

            Tag value = tag.get(key);
            if (value == null) {
                continue;
            }

            if (value instanceof CompoundTag childTag) {
                remapGregDirectionalFieldsInPlace(childTag, transform);
                continue;
            }

            if (value instanceof ListTag listTag) {
                remapGregDirectionalListInPlace(listTag, transform);
                continue;
            }

            if (!looksLikeGregDirectionalKey(key)) {
                continue;
            }

            if (value.getId() == Tag.TAG_STRING) {
                Direction direction = directionFromName(tag.getString(key));
                if (direction != null) {
                    tag.putString(key, directionName(mapCableBusSide(direction, transform)));
                }
                continue;
            }

            if (value.getId() == Tag.TAG_INT) {
                int raw = tag.getInt(key);
                if (raw >= 0 && raw < Direction.values().length) {
                    Direction direction = Direction.values()[raw];
                    tag.putInt(key, mapCableBusSide(direction, transform).ordinal());
                }
            }
        }
    }

    private static void remapGregDirectionalListInPlace(ListTag listTag, CableBusTransform transform) {
        for (int i = 0; i < listTag.size(); i++) {
            Tag entry = listTag.get(i);

            if (entry instanceof CompoundTag childTag) {
                remapGregDirectionalFieldsInPlace(childTag, transform);
                continue;
            }

            if (entry instanceof ListTag childList) {
                remapGregDirectionalListInPlace(childList, transform);
            }
        }
    }

    private static boolean looksLikeGregDirectionalKey(String key) {
        String normalized = key.toLowerCase();

        return normalized.contains("facing")
                || normalized.endsWith("side")
                || normalized.contains("outputside")
                || normalized.contains("inputside")
                || normalized.contains("frontside")
                || normalized.contains("backside")
                || normalized.equals("front")
                || normalized.equals("back");
    }

    private static @Nullable Direction directionFromName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        return switch (name.toLowerCase()) {
            case "down" -> Direction.DOWN;
            case "up" -> Direction.UP;
            case "north" -> Direction.NORTH;
            case "south" -> Direction.SOUTH;
            case "west" -> Direction.WEST;
            case "east" -> Direction.EAST;
            default -> null;
        };
    }

    private static String directionName(Direction direction) {
        return switch (direction) {
            case DOWN -> "down";
            case UP -> "up";
            case NORTH -> "north";
            case SOUTH -> "south";
            case WEST -> "west";
            case EAST -> "east";
        };
    }

    private static CompoundTag transformGregPipeCoverTag(CompoundTag coverTag, CableBusTransform transform) {
        CompoundTag result = new CompoundTag();

        for (String key : coverTag.getAllKeys()) {
            Direction side = directionFromKey(key);
            Tag rawValue = coverTag.get(key);

            if (rawValue == null) {
                continue;
            }

            if (side == null) {
                result.put(key, rawValue.copy());
                continue;
            }

            if (!(rawValue instanceof CompoundTag sideCoverTag)) {
                result.put(key, rawValue.copy());
                continue;
            }

            Direction mappedSide = mapCableBusSide(side, transform);
            CompoundTag movedCoverTag = sideCoverTag.copy();

            if (movedCoverTag.contains("uid", Tag.TAG_COMPOUND)) {
                CompoundTag uidTag = movedCoverTag.getCompound("uid").copy();
                uidTag.putInt("side", mappedSide.ordinal());
                movedCoverTag.put("uid", uidTag);
            }

            result.put(directionKey(mappedSide), movedCoverTag);
        }

        return result;
    }

    private static @Nullable Direction directionFromKey(String key) {
        return switch (key) {
            case KEY_NORTH -> Direction.NORTH;
            case KEY_SOUTH -> Direction.SOUTH;
            case KEY_EAST -> Direction.EAST;
            case KEY_WEST -> Direction.WEST;
            case KEY_UP -> Direction.UP;
            case KEY_DOWN -> Direction.DOWN;
            default -> null;
        };
    }

    private static int remapGregConnectionMask(int mask, CableBusTransform transform) {
        int out = 0;

        for (Direction side : Direction.values()) {
            int bit = gregBit(side);
            if ((mask & bit) == 0) {
                continue;
            }

            Direction mapped = mapCableBusSide(side, transform);
            out |= gregBit(mapped);
        }

        return out;
    }

    private static int gregBit(Direction side) {
        return 1 << side.ordinal();
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

            Optional<?> value = property.getValue(properties.getString(key));
            if (value.isPresent()) {
                state = setUnchecked(state, property, (Comparable<?>) value.get());
            }
        }

        return state;
    }

    private static BlockState setUnchecked(BlockState state, Property property, Comparable value) {
        return state.setValue(property, value);
    }

    private static BlockState flipHorizontalState(BlockState state, Direction sourceFacing) {
        Mirror mirror = sourceFacing.getAxis() == Direction.Axis.Z
                ? Mirror.FRONT_BACK
                : Mirror.LEFT_RIGHT;

        BlockState mirrored = state.mirror(mirror);

        if (hasHorizontalDirectionPropertyChange(state, mirrored)) {
            return mirrored;
        }

        return remapHorizontalDirectionProperties(mirrored, sourceFacing.getAxis());
    }

    private static boolean hasHorizontalDirectionPropertyChange(BlockState before, BlockState after) {
        for (Map.Entry<Property<?>, Comparable<?>> entry : before.getValues().entrySet()) {
            Comparable<?> beforeValue = entry.getValue();
            if (!(beforeValue instanceof Direction beforeDirection) || !beforeDirection.getAxis().isHorizontal()) {
                continue;
            }

            Comparable<?> afterValue = after.getValue(entry.getKey());
            if (afterValue instanceof Direction afterDirection && afterDirection != beforeDirection) {
                return true;
            }
        }

        return false;
    }

    private static boolean hasDirectionPropertyChange(BlockState before, BlockState after) {
        for (Map.Entry<Property<?>, Comparable<?>> entry : before.getValues().entrySet()) {
            Comparable<?> beforeValue = entry.getValue();
            if (!(beforeValue instanceof Direction beforeDirection)) {
                continue;
            }

            Comparable<?> afterValue = after.getValue(entry.getKey());
            if (afterValue instanceof Direction afterDirection && afterDirection != beforeDirection) {
                return true;
            }
        }

        return false;
    }

    private static BlockState remapHorizontalDirectionProperties(BlockState state, Direction.Axis sourceAxis) {
        BlockState result = state;

        for (Map.Entry<Property<?>, Comparable<?>> entry : state.getValues().entrySet()) {
            Property<?> property = entry.getKey();
            Comparable<?> value = entry.getValue();

            if (!(value instanceof Direction direction) || !direction.getAxis().isHorizontal()) {
                continue;
            }

            Direction flipped = switch (sourceAxis) {
                case Z -> switch (direction) {
                    case EAST -> Direction.WEST;
                    case WEST -> Direction.EAST;
                    default -> direction;
                };
                case X -> switch (direction) {
                    case NORTH -> Direction.SOUTH;
                    case SOUTH -> Direction.NORTH;
                    default -> direction;
                };
                default -> direction;
            };

            if (flipped != direction) {
                result = setUnchecked(result, property, flipped);
            }
        }

        return result;
    }

    private static BlockState flipVerticalState(BlockState state) {
        BlockState result = state;

        result = remapPropertyValues(result, "half", Map.of(
                "top", "bottom",
                "bottom", "top",
                "upper", "lower",
                "lower", "upper"
        ));

        result = remapPropertyValues(result, "face", Map.of(
                "floor", "ceiling",
                "ceiling", "floor"
        ));

        result = remapPropertyValues(result, "upwards_facing", Map.of(
                "north", "south",
                "south", "north"
        ));

        result = flipVerticalDirectionProperties(result);

        return result;
    }

    private static BlockState rotateState(BlockState state, Rotation rotation) {
        BlockState rotated = state.rotate(rotation);

        if (rotation == Rotation.NONE) {
            return rotated;
        }

        if (hasDirectionPropertyChange(state, rotated)) {
            return rotated;
        }

        return rotateFacingProperty(rotated, rotation);
    }

    private static BlockState rotateFacingProperty(BlockState state, Rotation rotation) {
        Property property = state.getBlock().getStateDefinition().getProperty("facing");
        if (property == null) {
            return state;
        }

        Object currentValue = state.getValue(property);
        if (!(currentValue instanceof Direction direction)) {
            return state;
        }

        Direction rotated = rotateDirection(direction, rotation);
        if (rotated == direction) {
            return state;
        }

        return setUnchecked(state, property, rotated);
    }

    private static Direction rotateDirection(Direction direction, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90 -> switch (direction) {
                case NORTH -> Direction.EAST;
                case EAST -> Direction.SOUTH;
                case SOUTH -> Direction.WEST;
                case WEST -> Direction.NORTH;
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
            case CLOCKWISE_180 -> switch (direction) {
                case NORTH -> Direction.SOUTH;
                case SOUTH -> Direction.NORTH;
                case EAST -> Direction.WEST;
                case WEST -> Direction.EAST;
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
            case COUNTERCLOCKWISE_90 -> switch (direction) {
                case NORTH -> Direction.WEST;
                case WEST -> Direction.SOUTH;
                case SOUTH -> Direction.EAST;
                case EAST -> Direction.NORTH;
                case UP -> Direction.UP;
                case DOWN -> Direction.DOWN;
            };
            case NONE -> direction;
        };
    }

    private static BlockState remapPropertyValues(BlockState state, String propertyName, Map<String, String> mapping) {
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

        Optional<?> parsed = property.getValue(targetValueName);
        if (parsed.isEmpty()) {
            return state;
        }

        return setUnchecked(state, property, (Comparable<?>) parsed.get());
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