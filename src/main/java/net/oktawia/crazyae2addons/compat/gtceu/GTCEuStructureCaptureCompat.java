package net.oktawia.crazyae2addons.compat.gtceu;

import com.gregtechceu.gtceu.api.blockentity.MetaMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.feature.IDataStickInteractable;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.logic.structuretool.AbstractStructureCaptureToolItem.RequirementSink;
import net.oktawia.crazyae2addons.util.NbtUtil;
import org.jetbrains.annotations.Nullable;

public final class GTCEuStructureCaptureCompat {

    private GTCEuStructureCaptureCompat() {
    }

    public static boolean collectAdditionalBlockMetadata(
            @Nullable CompoundTag rawBeTag,
            BlockEntity be,
            Player player,
            RequirementSink requirements,
            CompoundTag blockEntry
    ) {
        CompoundTag gregData = collectGregMetadata(rawBeTag, be, requirements);
        if (gregData.isEmpty()) {
            return false;
        }

        blockEntry.put(GTCEuKeys.CLONE_KEY_GREG, gregData);
        return true;
    }

    private static CompoundTag collectGregMetadata(
            @Nullable CompoundTag rawBeTag,
            BlockEntity be,
            RequirementSink requirements
    ) {
        CompoundTag out = new CompoundTag();

        if (!isGregBlockEntityTag(rawBeTag)) {
            return out;
        }

        if (rawBeTag.contains("cover", Tag.TAG_COMPOUND)) {
            CompoundTag coverTag = rawBeTag.getCompound("cover").copy();
            out.put(GTCEuKeys.CLONE_KEY_GREG_COVER, coverTag);
            collectGregCoverRequirements(coverTag, requirements);
        }

        if (isGregPipeTag(rawBeTag)) {
            CompoundTag pipeTag = new CompoundTag();

            NbtUtil.copyIntIfPresent(rawBeTag, pipeTag, "connections");
            NbtUtil.copyIntIfPresent(rawBeTag, pipeTag, "blockedConnections");
            NbtUtil.copyIntIfPresent(rawBeTag, pipeTag, "paintingColor");

            if (rawBeTag.contains("frameMaterial", Tag.TAG_STRING)) {
                String frameMaterial = rawBeTag.getString("frameMaterial");
                pipeTag.putString("frameMaterial", frameMaterial);
                collectGregPipeFrameRequirement(frameMaterial, requirements);
            }

            if (!pipeTag.isEmpty()) {
                out.put(GTCEuKeys.CLONE_KEY_GREG_PIPE, pipeTag);
            }
        }

        if (isGregMachineTag(rawBeTag)) {
            CompoundTag machineTag = new CompoundTag();

            NbtUtil.copyTagIfPresent(rawBeTag, machineTag, "ownerUUID");
            NbtUtil.copyStringIfPresent(rawBeTag, machineTag, "workingMode");
            NbtUtil.copyStringIfPresent(rawBeTag, machineTag, "voidingMode");
            NbtUtil.copyByteIfPresent(rawBeTag, machineTag, "batchEnabled");
            NbtUtil.copyByteIfPresent(rawBeTag, machineTag, "isWorkingEnabled");
            NbtUtil.copyByteIfPresent(rawBeTag, machineTag, "workingEnabled");
            NbtUtil.copyByteIfPresent(rawBeTag, machineTag, "isMuffled");
            NbtUtil.copyByteIfPresent(rawBeTag, machineTag, "isDistinct");
            NbtUtil.copyIntIfPresent(rawBeTag, machineTag, "paintingColor");
            NbtUtil.copyIntIfPresent(rawBeTag, machineTag, "currentParallel");
            NbtUtil.copyTagIfPresent(rawBeTag, machineTag, "circuitInventory");

            if (rawBeTag.contains("recipeLogic", Tag.TAG_COMPOUND)) {
                CompoundTag recipeLogic = sanitizeGregRecipeLogic(rawBeTag.getCompound("recipeLogic"));
                if (!recipeLogic.isEmpty()) {
                    machineTag.put("recipeLogic", recipeLogic);
                }
            }

            CompoundTag dataStick = collectGregDataStick(be);
            if (!dataStick.isEmpty()) {
                machineTag.put("dataStick", dataStick);
            }

            if (!machineTag.isEmpty()) {
                out.put(GTCEuKeys.CLONE_KEY_GREG_MACHINE, machineTag);
            }
        }

        return out;
    }

    private static CompoundTag collectGregDataStick(BlockEntity be) {
        if (!(be instanceof MetaMachineBlockEntity mmbe)) {
            return new CompoundTag();
        }
        if (!(mmbe.getMetaMachine() instanceof IDataStickInteractable interactable)) {
            return new CompoundTag();
        }
        if (!(be.getLevel() instanceof ServerLevel serverLevel)) {
            return new CompoundTag();
        }

        ItemStack stick = new ItemStack(Items.STICK);
        Player fakePlayer = FakePlayerFactory.getMinecraft(serverLevel);

        try {
            InteractionResult result = interactable.onDataStickShiftUse(fakePlayer, stick);

            if (!result.consumesAction() && result != InteractionResult.SUCCESS) {
                return new CompoundTag();
            }

            CompoundTag tag = stick.getTag();
            return tag == null ? new CompoundTag() : tag.copy();
        } catch (Throwable ignored) {
            return new CompoundTag();
        }
    }

    private static void collectGregPipeFrameRequirement(String frameMaterial, RequirementSink requirements) {
        if (frameMaterial == null || frameMaterial.isBlank()) {
            return;
        }

        String materialPath = frameMaterial;
        int namespaceSeparator = materialPath.indexOf(':');
        if (namespaceSeparator >= 0 && namespaceSeparator + 1 < materialPath.length()) {
            materialPath = materialPath.substring(namespaceSeparator + 1);
        }

        ResourceLocation frameId = new ResourceLocation("gtceu", materialPath + "_frame");
        Item frameItem = ForgeRegistries.ITEMS.getValue(frameId);

        if (frameItem != null && frameItem != Items.AIR) {
            requirements.add(new ItemStack(frameItem));
        }
    }

    private static CompoundTag sanitizeGregRecipeLogic(CompoundTag rawRecipeLogic) {
        CompoundTag out = rawRecipeLogic.copy();

        out.remove("progress");
        out.remove("duration");
        out.remove("isActive");
        out.remove("totalContinuousRunningTime");
        out.remove("chance_cache");
        out.remove("eut");
        out.remove("cwut");
        out.remove("item");
        out.remove("fluid");
        out.remove("tick");
        out.remove("block_state");
        out.remove("consecutiveRecipes");

        return out;
    }

    private static void collectGregCoverRequirements(CompoundTag coverTag, RequirementSink requirements) {
        for (String sideKey : coverTag.getAllKeys()) {
            GTCEuUtil.collectGregAttachItems(coverTag.get(sideKey), requirements::add);
        }
    }

    private static boolean isGregBlockEntityTag(@Nullable CompoundTag tag) {
        if (tag == null) {
            return false;
        }

        String id = tag.getString("id");
        return !id.isBlank() && id.startsWith(GTCEuKeys.GTCEU_ID_PREFIX);
    }

    private static boolean isGregPipeTag(@Nullable CompoundTag tag) {
        if (tag == null) {
            return false;
        }

        String id = tag.getString("id");
        return GTCEuKeys.GT_FLUID_PIPE_ID.equals(id)
                || GTCEuKeys.GT_ITEM_PIPE_ID.equals(id)
                || GTCEuKeys.GT_CABLE_ID.equals(id);
    }

    private static boolean isGregMachineTag(@Nullable CompoundTag tag) {
        if (tag == null) {
            return false;
        }

        String id = tag.getString("id");
        return !id.isBlank()
                && id.startsWith(GTCEuKeys.GTCEU_ID_PREFIX)
                && !isGregPipeTag(tag);
    }
}