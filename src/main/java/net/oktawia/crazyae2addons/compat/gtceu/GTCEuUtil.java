package net.oktawia.crazyae2addons.compat.gtceu;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.util.NbtUtil;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class GTCEuUtil {
    private GTCEuUtil() {}

    @Nullable
    public static BlockState getFrameState(String frameMaterial) {
        if (frameMaterial == null || frameMaterial.isBlank()) {
            return null;
        }

        String materialPath = frameMaterial;
        int sep = materialPath.indexOf(':');
        if (sep >= 0 && sep + 1 < materialPath.length()) {
            materialPath = materialPath.substring(sep + 1);
        }

        ResourceLocation frameId = new ResourceLocation("gtceu", materialPath + "_frame");
        Block frameBlock = ForgeRegistries.BLOCKS.getValue(frameId);

        if (frameBlock == null || frameBlock == Blocks.AIR) {
            return null;
        }

        return frameBlock.defaultBlockState();
    }

    @Nullable
    public static BlockState getFrameState(CompoundTag tag) {
        if (!tag.contains("frameMaterial")) {
            return null;
        }
        return getFrameState(tag.getString("frameMaterial"));
    }

    public static ItemStack getFrameItem(String frameMaterial) {
        if (frameMaterial == null || frameMaterial.isBlank()) {
            return ItemStack.EMPTY;
        }

        String materialPath = frameMaterial;
        int sep = materialPath.indexOf(':');
        if (sep >= 0 && sep + 1 < materialPath.length()) {
            materialPath = materialPath.substring(sep + 1);
        }

        ResourceLocation frameId = new ResourceLocation("gtceu", materialPath + "_frame");
        Item frameItem = ForgeRegistries.ITEMS.getValue(frameId);

        if (frameItem == null || frameItem == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(frameItem);
    }

    public static void collectGregAttachItems(@Nullable Tag tag, Consumer<ItemStack> sink) {
        if (tag == null) {
            return;
        }

        if (tag instanceof CompoundTag compoundTag) {
            if (compoundTag.contains("attachItem", Tag.TAG_COMPOUND)) {
                ItemStack stack = NbtUtil.tryReadSavedItemStack(compoundTag.getCompound("attachItem"));
                if (!stack.isEmpty()) {
                    sink.accept(stack);
                }
            }

            for (String key : compoundTag.getAllKeys()) {
                collectGregAttachItems(compoundTag.get(key), sink);
            }
            return;
        }

        if (tag instanceof ListTag listTag) {
            for (int i = 0; i < listTag.size(); i++) {
                collectGregAttachItems(listTag.get(i), sink);
            }
        }
    }
}
