package net.oktawia.crazyae2addons.logic.display;

import appeng.blockentity.networking.CableBusBlockEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.oktawia.crazyae2addons.parts.Display;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class DisplayGrid {

    public record RenderGroup(Set<Display> parts, Display renderOrigin, AABB aabb) {}

    private static final Map<Display, RenderGroup> CLIENT_RENDER_GROUP_CACHE = new IdentityHashMap<>();

    private DisplayGrid() {
    }

    public static synchronized void invalidateClientCache() {
        CLIENT_RENDER_GROUP_CACHE.clear();
    }

    public static synchronized RenderGroup getRenderGroup(Display origin) {
        RenderGroup cached = CLIENT_RENDER_GROUP_CACHE.get(origin);
        if (cached != null) {
            return cached;
        }

        rebuildClientCache();

        cached = CLIENT_RENDER_GROUP_CACHE.get(origin);
        if (cached != null) {
            return cached;
        }

        RenderGroup fallback = buildStandaloneRenderGroup(origin);
        CLIENT_RENDER_GROUP_CACHE.put(origin, fallback);
        return fallback;
    }

    private static void rebuildClientCache() {
        CLIENT_RENDER_GROUP_CACHE.clear();

        List<Display> snapshot = new ArrayList<>();
        for (Display part : Display.CLIENT_INSTANCES) {
            if (isUsablePart(part)) {
                snapshot.add(part);
            }
        }

        Set<Display> assigned = Collections.newSetFromMap(new IdentityHashMap<>());

        for (Display part : snapshot) {
            if (assigned.contains(part)) {
                continue;
            }

            Direction side = part.getSide();

            if (side == Direction.UP || side == Direction.DOWN || !part.isPowered() || !part.isMergeMode()) {
                RenderGroup singleton = buildSingletonRenderGroup(part);
                CLIENT_RENDER_GROUP_CACHE.put(part, singleton);
                assigned.add(part);
                continue;
            }

            Set<Display> component = getActiveConnectedComponent(part, side);
            if (component.isEmpty()) {
                RenderGroup singleton = buildSingletonRenderGroup(part);
                CLIENT_RENDER_GROUP_CACHE.put(part, singleton);
                assigned.add(part);
                continue;
            }

            RenderGroup group = buildRenderGroupForActiveComponent(component, side);
            for (Display member : component) {
                CLIENT_RENDER_GROUP_CACHE.put(member, group);
                assigned.add(member);
            }
        }
    }

    private static boolean isUsablePart(Display part) {
        if (part == null) {
            return false;
        }

        BlockEntity be = part.getBlockEntity();
        return be != null && !be.isRemoved() && part.getLevel() != null;
    }

    private static RenderGroup buildStandaloneRenderGroup(Display origin) {
        if (!isUsablePart(origin)) {
            return new RenderGroup(Collections.emptySet(), origin, new AABB(0, 0, 0, 0, 0, 0));
        }

        Direction side = origin.getSide();

        if (side == Direction.UP || side == Direction.DOWN || !origin.isPowered() || !origin.isMergeMode()) {
            return buildSingletonRenderGroup(origin);
        }

        Set<Display> component = getActiveConnectedComponent(origin, side);
        if (component.isEmpty()) {
            return buildSingletonRenderGroup(origin);
        }

        return buildRenderGroupForActiveComponent(component, side);
    }

    private static RenderGroup buildSingletonRenderGroup(Display part) {
        Set<Display> singleton = Set.of(part);
        return new RenderGroup(singleton, part, getMatrixAABB(singleton));
    }

    private static RenderGroup buildRenderGroupForActiveComponent(Set<Display> component, Direction side) {
        Display renderOrigin = getRenderOrigin(component, side);

        Set<Display> renderParts;
        if (isStructureComplete(component, side)) {
            renderParts = component;
        } else {
            renderParts = findLargestAnchoredRectangle(component, side, renderOrigin);
            if (renderParts.isEmpty()) {
                renderParts = Set.of(renderOrigin);
            }
        }

        Set<Display> immutableParts = Collections.unmodifiableSet(new LinkedHashSet<>(renderParts));
        return new RenderGroup(immutableParts, renderOrigin, getMatrixAABB(immutableParts));
    }

    private static Set<Display> getActiveConnectedComponent(Display origin, Direction side) {
        Set<Display> all = new LinkedHashSet<>();
        Deque<Display> queue = new ArrayDeque<>();

        all.add(origin);
        queue.add(origin);

        Direction[] dirs = {
                Direction.UP,
                Direction.DOWN,
                side.getClockWise(),
                side.getCounterClockWise()
        };

        while (!queue.isEmpty()) {
            Display cur = queue.poll();

            for (Direction dir : dirs) {
                Display nb = getNeighbor(cur, dir);
                if (nb != null && nb.getSide() == side && nb.isPowered() && nb.isMergeMode() && all.add(nb)) {
                    queue.add(nb);
                }
            }
        }

        return all;
    }

    public static Display getRenderOrigin(Set<Display> parts, Direction side) {
        Display any = parts.iterator().next();

        return parts.stream()
                .max(Comparator
                        .comparingInt((Display dp) -> partRow(dp, side))
                        .thenComparingInt(dp -> partCol(dp, side)))
                .orElse(any);
    }

    private static int partCol(Display part, Direction side) {
        BlockPos pos = part.getBlockEntity().getBlockPos();
        return switch (side) {
            case NORTH -> pos.getX();
            case SOUTH -> -pos.getX();
            case EAST -> pos.getZ();
            case WEST -> -pos.getZ();
            default -> pos.getX();
        };
    }

    private static int partRow(Display part, Direction side) {
        BlockPos pos = part.getBlockEntity().getBlockPos();
        return switch (side) {
            case NORTH, SOUTH, EAST, WEST -> pos.getY();
            default -> pos.getZ();
        };
    }

    private static Set<Display> findLargestAnchoredRectangle(Set<Display> parts,
                                                             Direction side,
                                                             Display anchor) {
        if (parts.isEmpty()) {
            return Collections.emptySet();
        }

        Map<Pair<Integer, Integer>, Display> grid = new HashMap<>();
        int minCol = Integer.MAX_VALUE;
        int minRow = Integer.MAX_VALUE;

        for (Display p : parts) {
            int col = partCol(p, side);
            int row = partRow(p, side);

            grid.put(Pair.of(col, row), p);
            minCol = Math.min(minCol, col);
            minRow = Math.min(minRow, row);
        }

        int anchorCol = partCol(anchor, side);
        int anchorRow = partRow(anchor, side);

        int bestArea = 0;
        Set<Display> best = Collections.emptySet();

        for (int r0 = anchorRow; r0 >= minRow; r0--) {
            for (int c0 = anchorCol; c0 >= minCol; c0--) {
                int area = (anchorCol - c0 + 1) * (anchorRow - r0 + 1);
                if (area <= bestArea) {
                    continue;
                }

                Set<Display> candidate = new LinkedHashSet<>();
                boolean valid = true;

                outer:
                for (int r = r0; r <= anchorRow; r++) {
                    for (int c = c0; c <= anchorCol; c++) {
                        Display p = grid.get(Pair.of(c, r));
                        if (p == null) {
                            valid = false;
                            break outer;
                        }
                        candidate.add(p);
                    }
                }

                if (valid) {
                    bestArea = area;
                    best = candidate;
                }
            }
        }

        return best;
    }

    @Nullable
    public static Display getNeighbor(Display part, Direction dir) {
        BlockPos neighborPos = part.getBlockEntity().getBlockPos().relative(dir);
        BlockEntity be = part.getLevel().getBlockEntity(neighborPos);

        if (be instanceof CableBusBlockEntity cbbe) {
            if (cbbe.getPart(part.getSide()) instanceof Display neighbor
                    && neighbor.getSide() == part.getSide()
                    && neighbor.isPowered()) {
                return neighbor;
            }
        }

        return null;
    }

    public static boolean isStructureComplete(Set<Display> group, Direction side) {
        if (group == null || group.isEmpty()) {
            return false;
        }

        Set<Pair<Integer, Integer>> coords = new HashSet<>();
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;

        for (Display part : group) {
            int col = partCol(part, side);
            int row = partRow(part, side);

            coords.add(Pair.of(col, row));
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        for (int col = minCol; col <= maxCol; col++) {
            for (int row = minRow; row <= maxRow; row++) {
                if (!coords.contains(Pair.of(col, row))) {
                    return false;
                }
            }
        }

        return true;
    }

    public static Pair<Integer, Integer> getGridSize(List<Display> sorted, Direction side) {
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (Display part : sorted) {
            int col = partCol(part, side);
            int row = partRow(part, side);

            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        return Pair.of(maxCol - minCol + 1, maxRow - minRow + 1);
    }

    public static AABB getMatrixAABB(Set<Display> group) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;
        double maxZ = -Double.MAX_VALUE;

        for (Display part : group) {
            BlockPos pos = part.getBlockEntity().getBlockPos();
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX() + 1);
            maxY = Math.max(maxY, pos.getY() + 1);
            maxZ = Math.max(maxZ, pos.getZ() + 1);
        }

        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static Pair<Integer, Integer> computePreviewGridSize(Display origin) {
        if (origin == null || !origin.isPowered()) {
            return Pair.of(1, 1);
        }

        Direction side = origin.getSide();
        if (side == Direction.UP || side == Direction.DOWN) {
            return Pair.of(1, 1);
        }

        Set<Display> component = getActiveConnectedComponent(origin, side);
        if (component.isEmpty()) {
            return Pair.of(1, 1);
        }

        RenderGroup group = buildRenderGroupForActiveComponent(component, side);
        if (group.parts().isEmpty()) {
            return Pair.of(1, 1);
        }

        Pair<Integer, Integer> dims = getGridSize(new ArrayList<>(group.parts()), group.renderOrigin().getSide());
        return Pair.of(Math.max(1, dims.getFirst()), Math.max(1, dims.getSecond()));
    }

    public static Display resolveMenuOrigin(Display clicked) {
        if (clicked == null) {
            return null;
        }

        RenderGroup group = buildStandaloneRenderGroup(clicked);
        if (group.parts().isEmpty()) {
            return clicked;
        }

        if (group.parts().contains(clicked)) {
            return group.renderOrigin();
        }

        return clicked;
    }
}