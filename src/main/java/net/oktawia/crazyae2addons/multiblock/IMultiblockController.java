package net.oktawia.crazyae2addons.multiblock;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Set;

/**
 * Implemented by the controller BlockEntity of a multiblock structure.
 *
 * <p>Formation flow:
 * <ol>
 *   <li>Controller placed or loaded → calls {@link #rebuildExpected()} then {@link #initialSweep()}.
 *   <li>New member placed nearby → member calls {@link #tryAcceptMember}.
 *   <li>All expected positions filled → controller sets {@code formed = true} and fires
 *       {@link #onFormed()}.
 *   <li>Any member destroyed → {@link #onMemberDestroyed} is called, structure invalidates,
 *       {@link #onInvalidated()} fires.
 * </ol>
 *
 * <p>Health check (periodic, ~every 200 ticks):
 * For each position in the "actual members" set that is currently chunk-loaded, verify the
 * correct block is still present. Skip unloaded chunks — do NOT force-load.
 */
public interface IMultiblockController {

    /**
     * Returns the structure definition describing which blocks belong where.
     * Typically a static field on the implementing class.
     */
    MultiblockStructureDefinition getStructureDefinition();

    /**
     * Returns the current Level.
     */
    Level getLevel();

    /**
     * Returns the position of the controller block entity.
     */
    BlockPos getControllerPos();

    /**
     * Returns {@code true} if the structure is currently fully formed.
     */
    boolean isFormed();

    /**
     * Returns the set of member positions currently known to the controller.
     * This set is persisted in NBT.
     */
    Set<BlockPos> getActualMembers();

    /**
     * Returns {@code true} if {@code pos} is in the current actual members set.
     * Used by {@link IMultiblockMember#verifyControllerStillKnowsUs}.
     */
    default boolean knowsPosition(BlockPos pos) {
        return getActualMembers().contains(pos);
    }

    /**
     * Rebuilds the {@code expectedStructure} map from the structure definition and the
     * controller's current facing direction.
     * Must be called whenever the controller loads or its facing changes.
     * The expected map is NOT persisted — always recomputed.
     */
    void rebuildExpected();

    /**
     * Scans all positions in the expected structure map (only loaded chunks).
     * For each position that already has the correct block, adds it to actual members
     * and calls {@link IMultiblockMember#onJoinStructure} on the block entity there.
     * Then calls {@link #checkFormation()}.
     * <p>
     * Called once on controller placement or when loaded with {@code formed = false}.
     */
    void initialSweep();

    /**
     * Called when a block is placed and discovers this controller through neighbour scanning.
     * The controller checks whether {@code pos} is in the expected structure and whether
     * {@code block} is an allowed type there. If yes, adds it to actual members.
     *
     * @param pos   absolute position of the newly placed block
     * @param block the block type placed there
     */
    void tryAcceptMember(BlockPos pos, Block block);

    /**
     * Called by a member's {@code setRemoved()} when it is destroyed.
     * Removes the position from actual members and, if the structure was formed,
     * invalidates it (calls {@link #onInvalidated()} and notifies all remaining members).
     *
     * @param pos position of the destroyed member
     */
    void onMemberDestroyed(BlockPos pos);

    /**
     * Checks whether all expected positions are present in actual members.
     * If all chunks containing expected positions are loaded and everything matches,
     * sets {@code formed = true} and fires {@link #onFormed()}.
     * <p>
     * Does NOT form if any expected position is in an unloaded chunk — the state
     * of unloaded areas is unknown.
     */
    void checkFormation();

    /**
     * Iterates actual members in loaded chunks and verifies each still has the correct block.
     * One mismatch triggers {@link #onMemberDestroyed} for that position.
     * Skips positions in unloaded chunks silently.
     * <p>
     * Should be called periodically (~every 200 ticks) while formed.
     */
    default void healthCheck() {
        Level level = getLevel();
        if (level == null || !isFormed()) return;

        for (BlockPos pos : Set.copyOf(getActualMembers())) {
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof IMultiblockMember)) {
                onMemberDestroyed(pos);
                return;
            }
        }
    }

    /**
     * Notifies all currently loaded actual members that the structure is invalidated,
     * calls {@link IMultiblockMember#onLeaveStructure()} on each, and clears the set.
     */
    default void notifyAllMembersLeave() {
        Level level = getLevel();
        if (level == null) return;
        for (BlockPos pos : getActualMembers()) {
            if (!level.isLoaded(pos)) continue;
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof IMultiblockMember m) m.onLeaveStructure();
        }
        getActualMembers().clear();
    }

    /**
     * Called when the structure becomes fully formed.
     * Override in the concrete controller to start producing, enable AE2 grid, etc.
     */
    void onFormed();

    /**
     * Called when the structure breaks (any member removed or health-check fails).
     * Override in the concrete controller to stop producing, disable grid, etc.
     */
    void onInvalidated();
}
