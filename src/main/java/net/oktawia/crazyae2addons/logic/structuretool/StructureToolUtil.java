package net.oktawia.crazyae2addons.logic.structuretool;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.util.TemplateUtil;
import org.jetbrains.annotations.Nullable;

public final class StructureToolUtil {

    private StructureToolUtil() {
    }

    @SafeVarargs
    public static ItemStack findHeld(@Nullable Player player, Class<? extends Item>... toolClasses) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (isAnyOf(mainHand, toolClasses)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (isAnyOf(offHand, toolClasses)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    @SafeVarargs
    public static ItemStack findActive(@Nullable Player player, Class<? extends Item>... toolClasses) {
        if (player == null) {
            return ItemStack.EMPTY;
        }

        ItemStack mainHand = player.getMainHandItem();
        if (isAnyOf(mainHand, toolClasses) && StructureToolStackState.hasStructure(mainHand)) {
            return mainHand;
        }

        ItemStack offHand = player.getOffhandItem();
        if (isAnyOf(offHand, toolClasses) && StructureToolStackState.hasStructure(offHand)) {
            return offHand;
        }

        return ItemStack.EMPTY;
    }

    @SafeVarargs
    private static boolean isAnyOf(ItemStack stack, Class<? extends Item>... toolClasses) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();
        for (Class<? extends Item> toolClass : toolClasses) {
            if (toolClass.isInstance(item)) {
                return true;
            }
        }

        return false;
    }

    public static BlockHitResult rayTrace(Level level, Player player, double maxDistance) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);

        ClipContext context = new ClipContext(
                eye,
                end,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        );

        HitResult result = level.clip(context);
        if (result instanceof BlockHitResult blockHit && result.getType() == HitResult.Type.BLOCK) {
            return blockHit;
        }

        return BlockHitResult.miss(end, Direction.getNearest(look.x, look.y, look.z), BlockPos.containing(end));
    }

    public static double calculatePreviewStructurePower(
            CompoundTag templateTag,
            BlockPos localOrigin,
            double baseCostPerBlock,
            double energyCostMultiplier
    ) {
        if (templateTag == null || baseCostPerBlock <= 0.0D || energyCostMultiplier <= 0.0D) {
            return 0.0D;
        }

        double total = 0.0D;

        for (TemplateUtil.BlockInfo blockInfo : TemplateUtil.parseBlocksFromTag(templateTag)) {
            BlockPos pos = blockInfo.pos();

            double dx = pos.getX() - localOrigin.getX();
            double dy = pos.getY() - localOrigin.getY();
            double dz = pos.getZ() - localOrigin.getZ();

            double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
            total += baseCostPerBlock * distance * energyCostMultiplier;
        }

        return Math.max(0.0D, total);
    }
}