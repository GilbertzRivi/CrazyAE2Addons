package net.oktawia.crazyae2addons.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Implemented by any BlockEntity that can be part of a multiblock structure.
 *
 * <p>Lifecycle:
 * <ol>
 *   <li>Block placed → {@code onLoad()} in the BE checks neighbours for a controller and
 *       calls {@link IMultiblockController#tryAcceptMember}.
 *   <li>Controller accepts → calls {@link #onJoinStructure}.
 *   <li>Block destroyed → {@code setRemoved()} in the BE calls
 *       {@link IMultiblockController#onMemberDestroyed}.
 *   <li>Structure invalidated → controller calls {@link #onLeaveStructure} on every member.
 * </ol>
 */
public interface IMultiblockMember {

    /**
     * Called by the controller when this block officially joins the structure.
     * Implementations must persist {@code controllerPos} in NBT.
     */
    void onJoinStructure(BlockPos controllerPos);

    /**
     * Called by the controller when the structure is invalidated.
     * Implementations must clear the stored controller pos from NBT.
     */
    void onLeaveStructure();

    /**
     * Returns the controller position stored from the last {@link #onJoinStructure} call,
     * or {@code null} if this member is not currently part of any structure.
     */
    @Nullable
    BlockPos getControllerPos();

    /**
     * Returns {@code true} if this member is currently registered to a structure.
     */
    default boolean isInStructure() {
        return getControllerPos() != null;
    }

    /**
     * Notifies the controller (if loaded) that this member is being removed.
     * Call this from {@code BlockEntity.setRemoved()}.
     *
     * @param level the current level
     * @param selfPos position of this block entity
     */
    default void notifyControllerOfRemoval(Level level, BlockPos selfPos) {
        BlockPos ctrlPos = getControllerPos();
        if (ctrlPos == null) return;
        if (!level.isLoaded(ctrlPos)) return;
        BlockEntity be = level.getBlockEntity(ctrlPos);
        if (be instanceof IMultiblockController ctrl) {
            ctrl.onMemberDestroyed(selfPos);
        }
    }

    /**
     * Tries to discover and join a controller by scanning the 6 direct neighbours.
     * A neighbour qualifies if it is itself a controller, or if it is a member that
     * already has a controller reference.
     * Call this from {@code BlockEntity.onLoad()} on the server side when
     * {@link #getControllerPos()} is {@code null}.
     *
     * @param level   the current level
     * @param selfPos position of this block entity
     * @param selfBlock the block type at this position (used for matching)
     */
    default void tryDiscoverController(Level level, BlockPos selfPos,
                                       net.minecraft.world.level.block.Block selfBlock) {
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighborPos = selfPos.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            BlockEntity neighbor = level.getBlockEntity(neighborPos);

            // Neighbour is a controller directly
            if (neighbor instanceof IMultiblockController ctrl) {
                ctrl.tryAcceptMember(selfPos, selfBlock);
                return;
            }

            // Neighbour is a member that knows a controller
            if (neighbor instanceof IMultiblockMember m) {
                BlockPos ctrlPos = m.getControllerPos();
                if (ctrlPos == null) continue;
                if (!level.isLoaded(ctrlPos)) continue;
                BlockEntity ctrlBe = level.getBlockEntity(ctrlPos);
                if (ctrlBe instanceof IMultiblockController ctrl) {
                    ctrl.tryAcceptMember(selfPos, selfBlock);
                    return;
                }
            }
        }
    }

    /**
     * Verifies that the stored controller still knows about this position.
     * If not, clears the controller reference (stale state after chunk reload).
     * Call from {@code BlockEntity.onLoad()} when {@link #getControllerPos()} is not null.
     *
     * @param level   the current level
     * @param selfPos position of this block entity
     */
    default void verifyControllerStillKnowsUs(Level level, BlockPos selfPos) {
        BlockPos ctrlPos = getControllerPos();
        if (ctrlPos == null) return;
        if (!level.isLoaded(ctrlPos)) return; // controller unloaded — wait, assume OK
        BlockEntity be = level.getBlockEntity(ctrlPos);
        if (!(be instanceof IMultiblockController ctrl) || !ctrl.knowsPosition(selfPos)) {
            onLeaveStructure(); // stale reference — clean up
        }
    }
}
