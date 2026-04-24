package net.oktawia.crazyae2addons.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

public final class NbtUtil {
    private NbtUtil() {}

    public static ItemStack tryReadSavedItemStack(CompoundTag tag) {
        if (!tag.contains("id", Tag.TAG_STRING)) {
            return ItemStack.EMPTY;
        }

        CompoundTag copy = tag.copy();
        if (!copy.contains("Count", Tag.TAG_BYTE)) {
            copy.putByte("Count", (byte) 1);
        }

        ItemStack stack = ItemStack.of(copy);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    public static void copyTagIfPresent(CompoundTag from, CompoundTag to, String key) {
        if (from.contains(key)) {
            Tag value = from.get(key);
            if (value != null) {
                to.put(key, value.copy());
            }
        }
    }

    public static void copyStringIfPresent(CompoundTag from, CompoundTag to, String key) {
        if (from.contains(key, Tag.TAG_STRING)) {
            to.putString(key, from.getString(key));
        }
    }

    public static void copyByteIfPresent(CompoundTag from, CompoundTag to, String key) {
        if (from.contains(key, Tag.TAG_BYTE)) {
            to.putByte(key, from.getByte(key));
        }
    }

    public static void copyIntIfPresent(CompoundTag from, CompoundTag to, String key) {
        if (from.contains(key, Tag.TAG_INT)) {
            to.putInt(key, from.getInt(key));
        }
    }
}
