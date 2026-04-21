package net.oktawia.crazyae2addons.logic.cutpaste;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.Nullable;

public final class CutPasteStackState {

    private static final String TAG_SEL_A = "sel_a";
    private static final String TAG_SEL_B = "sel_b";
    private static final String TAG_ORIGIN = "origin";
    private static final String TAG_SRC_FACING = "src_facing";
    private static final String TAG_STRUCTURE_ID = "structure_id";
    public static final String TAG_PREVIEW_SIDE_MAP = "crazy_preview_side_map";

    private CutPasteStackState() {
    }

    public static void setSelectionA(ItemStack stack, @Nullable BlockPos pos) {
        setPos(stack, TAG_SEL_A, pos);
    }

    public static void setSelectionB(ItemStack stack, @Nullable BlockPos pos) {
        setPos(stack, TAG_SEL_B, pos);
    }

    public static void setOrigin(ItemStack stack, @Nullable BlockPos pos) {
        setPos(stack, TAG_ORIGIN, pos);
    }

    public static void setSourceFacing(ItemStack stack, Direction direction) {
        stack.getOrCreateTag().putString(TAG_SRC_FACING, direction.getName());
    }

    public static void setStructureId(ItemStack stack, @Nullable String id) {
        CompoundTag tag = stack.getOrCreateTag();
        if (id == null || id.isBlank()) {
            tag.remove(TAG_STRUCTURE_ID);
        } else {
            tag.putString(TAG_STRUCTURE_ID, id);
        }
    }

    public static BlockPos getSelectionA(ItemStack stack) {
        return getPos(stack, TAG_SEL_A);
    }

    public static BlockPos getSelectionB(ItemStack stack) {
        return getPos(stack, TAG_SEL_B);
    }

    public static BlockPos getOrigin(ItemStack stack) {
        return getPos(stack, TAG_ORIGIN);
    }

    public static Direction getSourceFacing(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_SRC_FACING)) {
            return Direction.NORTH;
        }

        Direction direction = Direction.byName(tag.getString(TAG_SRC_FACING));
        return direction != null && direction.getAxis().isHorizontal() ? direction : Direction.NORTH;
    }

    public static String getStructureId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? "" : tag.getString(TAG_STRUCTURE_ID);
    }

    public static boolean hasStructure(ItemStack stack) {
        String id = getStructureId(stack);
        return !id.isBlank();
    }

    public static void clearSelection(ItemStack stack) {
        CompoundTag tag = stack.getOrCreateTag();
        tag.remove(TAG_SEL_A);
        tag.remove(TAG_SEL_B);
        tag.remove(TAG_ORIGIN);
        tag.remove(TAG_SRC_FACING);
    }

    public static void clearStructure(ItemStack stack) {
        setStructureId(stack, null);
    }

    private static void setPos(ItemStack stack, String key, @Nullable BlockPos pos) {
        CompoundTag tag = stack.getOrCreateTag();
        if (pos == null) {
            tag.remove(key);
            return;
        }

        tag.putIntArray(key, new int[]{pos.getX(), pos.getY(), pos.getZ()});
    }

    public static int[] getPreviewSideMap(ItemStack stack) {
        int[] identity = identitySideMap();
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_PREVIEW_SIDE_MAP, Tag.TAG_INT_ARRAY)) return identity;
        int[] raw = tag.getIntArray(TAG_PREVIEW_SIDE_MAP);
        if (raw.length != Direction.values().length) return identity;
        for (Direction side : Direction.values()) {
            int mapped = raw[side.ordinal()];
            if (mapped < 0 || mapped >= Direction.values().length) return identity;
        }
        return raw;
    }

    public static void resetPreviewSideMap(ItemStack stack) {
        stack.getOrCreateTag().putIntArray(TAG_PREVIEW_SIDE_MAP, identitySideMap());
    }

    public static int[] identitySideMap() {
        int[] map = new int[Direction.values().length];
        for (Direction side : Direction.values()) map[side.ordinal()] = side.ordinal();
        return map;
    }

    private static BlockPos getPos(ItemStack stack, String key) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(key)) {
            return null;
        }

        int[] arr = tag.getIntArray(key);
        if (arr.length != 3) {
            return null;
        }

        return new BlockPos(arr[0], arr[1], arr[2]);
    }
}