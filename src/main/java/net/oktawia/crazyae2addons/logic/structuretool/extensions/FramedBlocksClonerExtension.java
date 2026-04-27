package net.oktawia.crazyae2addons.logic.structuretool.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.logic.structuretool.AbstractStructureCaptureToolItem;
import net.oktawia.crazyae2addons.logic.structuretool.ClonerPasteContext;
import net.oktawia.crazyae2addons.logic.structuretool.PlacementPlan;
import net.oktawia.crazyae2addons.logic.structuretool.StructureCloneExtension;
import net.oktawia.crazyae2addons.util.NbtUtil;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class FramedBlocksClonerExtension implements StructureCloneExtension {

    private static final String MOD_ID = "framedblocks";

    private static final String NBT_ID = "id";
    private static final String NBT_CAMO = "camo";
    private static final String NBT_STATE = "state";
    private static final String NBT_NAME = "Name";

    private static final String CLONE_KEY_FRAMED = "framed";
    private static final String CLONE_KEY_CAMO = "camo";

    @Override
    public boolean handlesRequirements(BlockState state, @Nullable CompoundTag rawBeTag) {
        return isFramedBlock(state, rawBeTag);
    }

    @Override
    public boolean collectMetadata(
            ServerLevel level,
            BlockPos pos,
            BlockEntity be,
            @Nullable CompoundTag rawBeTag,
            AbstractStructureCaptureToolItem.RequirementSink requirements,
            CompoundTag blockEntry
    ) {
        if (!isFramedBlock(level.getBlockState(pos), rawBeTag)) {
            return false;
        }

        addBaseBlockRequirement(level, pos, requirements);

        CompoundTag framedData = new CompoundTag();

        if (rawBeTag != null && rawBeTag.contains(NBT_CAMO, Tag.TAG_COMPOUND)) {
            CompoundTag camoTag = rawBeTag.getCompound(NBT_CAMO).copy();

            framedData.put(CLONE_KEY_CAMO, camoTag);
            addCamoRequirement(camoTag, requirements);
        }

        if (framedData.isEmpty()) {
            return false;
        }

        blockEntry.put(CLONE_KEY_FRAMED, framedData);
        return true;
    }

    @Override
    public Optional<PlacementPlan> buildPlacementPlan(
            ServerLevel level,
            Player player,
            BlockState state,
            @Nullable CompoundTag rawBeTag,
            @Nullable CompoundTag blockMetadata,
            ClonerPasteContext ctx
    ) {
        if (!isFramedBlock(state, rawBeTag)) {
            return Optional.empty();
        }

        if (rawBeTag == null) {
            return Optional.of(PlacementPlan.none());
        }

        CompoundTag framedData = getFramedMetadata(blockMetadata);
        CompoundTag filteredTag = createWhitelistedFramedTag(rawBeTag, framedData);

        Map<Item, Integer> reserved = new LinkedHashMap<>();
        List<ItemStack> costs = new ArrayList<>();

        ItemStack baseItem = normalizeSingle(ctx.getRequiredBlockItem(state));

        if (!baseItem.isEmpty()) {
            if (!player.isCreative() && !ctx.canReserveForPaste(reserved, baseItem, 1)) {
                return Optional.of(PlacementPlan.none());
            }

            costs.add(baseItem);
        } else if (!player.isCreative()) {
            return Optional.of(PlacementPlan.none());
        }

        if (framedData.contains(CLONE_KEY_CAMO, Tag.TAG_COMPOUND)) {
            CompoundTag camoTag = framedData.getCompound(CLONE_KEY_CAMO);
            ItemStack camoItem = normalizeSingle(getCamoRequirement(camoTag));

            if (!camoItem.isEmpty()) {
                if (!player.isCreative() && !ctx.canReserveForPaste(reserved, camoItem, 1)) {
                    return Optional.of(PlacementPlan.none());
                }

                costs.add(camoItem);
            }
        }

        return Optional.of(new PlacementPlan(true, state, filteredTag, costs));
    }

    @Override
    public void onBlockPlaced(
            ServerLevel level,
            BlockPos pos,
            @Nullable BlockEntity be,
            @Nullable CompoundTag blockMetadata
    ) {
        if (be == null || getFramedMetadata(blockMetadata).isEmpty()) {
            return;
        }

        be.setChanged();

        BlockState state = level.getBlockState(pos);
        level.sendBlockUpdated(pos, state, state, 3);
    }

    private static CompoundTag getFramedMetadata(@Nullable CompoundTag blockMetadata) {
        if (blockMetadata == null) {
            return new CompoundTag();
        }

        if (!blockMetadata.contains(CLONE_KEY_FRAMED, Tag.TAG_COMPOUND)) {
            return new CompoundTag();
        }

        return blockMetadata.getCompound(CLONE_KEY_FRAMED);
    }

    private static CompoundTag createWhitelistedFramedTag(
            CompoundTag rawBeTag,
            CompoundTag framedData
    ) {
        CompoundTag out = new CompoundTag();

        NbtUtil.copyStringIfPresent(rawBeTag, out, NBT_ID);

        if (framedData.contains(CLONE_KEY_CAMO, Tag.TAG_COMPOUND)) {
            out.put(NBT_CAMO, framedData.getCompound(CLONE_KEY_CAMO).copy());
        }

        return out;
    }

    private static void addCamoRequirement(
            CompoundTag camoTag,
            AbstractStructureCaptureToolItem.RequirementSink requirements
    ) {
        ItemStack camoItem = getCamoRequirement(camoTag);

        if (!camoItem.isEmpty()) {
            requirements.add(camoItem);
        }
    }

    private static ItemStack getCamoRequirement(CompoundTag camoTag) {
        if (!camoTag.contains(NBT_STATE, Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }

        CompoundTag stateTag = camoTag.getCompound(NBT_STATE);

        if (!stateTag.contains(NBT_NAME, Tag.TAG_STRING)) {
            return ItemStack.EMPTY;
        }

        ResourceLocation blockId = ResourceLocation.tryParse(stateTag.getString(NBT_NAME));

        if (blockId == null) {
            return ItemStack.EMPTY;
        }

        Block block = ForgeRegistries.BLOCKS.getValue(blockId);

        if (block == null) {
            return ItemStack.EMPTY;
        }

        Item item = block.asItem();

        if (item == Items.AIR) {
            return ItemStack.EMPTY;
        }

        return new ItemStack(item);
    }

    private static boolean isFramedBlock(BlockState state, @Nullable CompoundTag rawBeTag) {
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.getBlock());

        if (blockId != null && MOD_ID.equals(blockId.getNamespace())) {
            return true;
        }

        if (rawBeTag == null) {
            return false;
        }

        String id = rawBeTag.getString(NBT_ID);

        if (id.isBlank()) {
            return false;
        }

        ResourceLocation beId = ResourceLocation.tryParse(id);

        return beId != null && MOD_ID.equals(beId.getNamespace());
    }

    private static void addBaseBlockRequirement(
            ServerLevel level,
            BlockPos pos,
            AbstractStructureCaptureToolItem.RequirementSink requirements
    ) {
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(pos),
                Direction.UP,
                pos,
                false
        );

        ItemStack picked = level.getBlockState(pos).getCloneItemStack(hit, level, pos, null);

        if (!picked.isEmpty()) {
            requirements.add(picked);
            return;
        }

        Item item = level.getBlockState(pos).getBlock().asItem();

        if (item != Items.AIR) {
            requirements.add(new ItemStack(item));
        }
    }

    private static ItemStack normalizeSingle(ItemStack stack) {
        if (stack.isEmpty()) {
            return ItemStack.EMPTY;
        }

        ItemStack copy = stack.copy();

        copy.setCount(1);
        copy.setTag(null);

        return copy;
    }
}