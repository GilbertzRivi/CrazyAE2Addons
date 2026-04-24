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

    private record PlaneAxes(Direction right, Direction up) {}

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

            if (!part.isPowered() || !part.isMergeMode()) {
                RenderGroup singleton = buildSingletonRenderGroup(part);
                CLIENT_RENDER_GROUP_CACHE.put(part, singleton);
                assigned.add(part);
                continue;
            }

            Set<Display> component = getActiveConnectedComponent(part, side, assigned);
            if (component.isEmpty()) {
                RenderGroup singleton = buildSingletonRenderGroup(part);
                CLIENT_RENDER_GROUP_CACHE.put(part, singleton);
                assigned.add(part);
                continue;
            }

            RenderGroup group = buildRenderGroupForActiveComponent(component);
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

        if (!origin.isPowered() || !origin.isMergeMode()) {
            return buildSingletonRenderGroup(origin);
        }

        Set<Display> component = getActiveConnectedComponent(origin, side);
        if (component.isEmpty()) {
            return buildSingletonRenderGroup(origin);
        }

        return buildRenderGroupForActiveComponent(component);
    }

    private static RenderGroup buildSingletonRenderGroup(Display part) {
        Set<Display> singleton = Set.of(part);
        return new RenderGroup(singleton, part, getMatrixAABB(singleton));
    }

    private static RenderGroup buildRenderGroupForActiveComponent(Set<Display> component) {
        Display renderOrigin = getRenderOrigin(component);

        Set<Display> renderParts;
        if (isStructureComplete(component)) {
            renderParts = component;
        } else {
            renderParts = findLargestAnchoredRectangle(component, renderOrigin);
            if (renderParts.isEmpty()) {
                renderParts = Set.of(renderOrigin);
            }
        }

        Set<Display> immutableParts = Collections.unmodifiableSet(new LinkedHashSet<>(renderParts));
        return new RenderGroup(immutableParts, renderOrigin, getMatrixAABB(immutableParts));
    }

    private static Set<Display> getActiveConnectedComponent(Display origin, Direction side) {
        return getActiveConnectedComponent(origin, side, Collections.emptySet());
    }

    private static Set<Display> getActiveConnectedComponent(Display origin, Direction side, Set<Display> excluded) {
        Set<Display> all = new LinkedHashSet<>();
        Deque<Display> queue = new ArrayDeque<>();

        all.add(origin);
        queue.add(origin);

        Direction[] dirs = (side == Direction.UP || side == Direction.DOWN)
                ? new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}
                : new Direction[]{Direction.UP, Direction.DOWN, side.getClockWise(), side.getCounterClockWise()};

        while (!queue.isEmpty()) {
            Display cur = queue.poll();

            for (Direction dir : dirs) {
                Display nb = getNeighbor(cur, dir);
                if (nb != null
                        && !excluded.contains(nb)
                        && nb.getSide() == side
                        && nb.getSpin() == origin.getSpin()
                        && nb.isPowered()
                        && nb.isMergeMode()
                        && all.add(nb)) {
                    queue.add(nb);
                }
            }
        }

        return all;
    }

    public static Display getRenderOrigin(Set<Display> parts) {
        Display any = parts.iterator().next();
        Direction side = any.getSide();

        if (side == Direction.UP) {
            return parts.stream()
                    .min(Comparator
                            .comparingInt(DisplayGrid::partRow)
                            .thenComparingInt(DisplayGrid::partCol))
                    .orElse(any);
        }

        if (side == Direction.DOWN) {
            return parts.stream()
                    .min(Comparator
                            .comparingInt(DisplayGrid::partCol)
                            .thenComparing(Comparator.comparingInt(DisplayGrid::partRow).reversed()))
                    .orElse(any);
        }

        return parts.stream()
                .max(Comparator
                        .comparingInt(DisplayGrid::partRow)
                        .thenComparingInt(DisplayGrid::partCol))
                .orElse(any);
    }

    private static PlaneAxes axesForWall(Direction side, int spin) {
        PlaneAxes base = switch (side) {
            case NORTH -> new PlaneAxes(Direction.EAST, Direction.UP);
            case SOUTH -> new PlaneAxes(Direction.WEST, Direction.UP);
            case EAST -> new PlaneAxes(Direction.SOUTH, Direction.UP);
            case WEST -> new PlaneAxes(Direction.NORTH, Direction.UP);
            default -> throw new IllegalStateException("Unexpected side for wall axes: " + side);
        };

        int normalizedSpin = Math.floorMod(spin, 4);
        PlaneAxes result = base;

        for (int i = 0; i < normalizedSpin; i++) {
            result = rotateClockwise(result);
        }

        return result;
    }

    private static PlaneAxes rotateClockwise(PlaneAxes axes) {
        return new PlaneAxes(
                axes.up(),
                axes.right().getOpposite()
        );
    }

    private static int project(BlockPos pos, Direction dir) {
        return switch (dir) {
            case EAST -> pos.getX();
            case WEST -> -pos.getX();
            case UP -> pos.getY();
            case DOWN -> -pos.getY();
            case SOUTH -> pos.getZ();
            case NORTH -> -pos.getZ();
        };
    }

    private static int partCol(Display part) {
        BlockPos pos = part.getBlockEntity().getBlockPos();
        Direction side = part.getSide();

        if (side == Direction.UP || side == Direction.DOWN) {
            return switch (Math.floorMod(part.getSpin(), 4)) {
                case 0 -> pos.getX();   // right = east
                case 1 -> pos.getZ();   // right = south
                case 2 -> -pos.getX();  // right = west
                default -> -pos.getZ(); // right = north
            };
        }

        PlaneAxes axes = axesForWall(side, part.getSpin());
        return project(pos, axes.right());
    }

    private static int partRow(Display part) {
        BlockPos pos = part.getBlockEntity().getBlockPos();
        Direction side = part.getSide();

        if (side == Direction.UP || side == Direction.DOWN) {
            return switch (Math.floorMod(part.getSpin(), 4)) {
                case 0 -> pos.getZ();   // down = south
                case 1 -> -pos.getX();  // down = west
                case 2 -> -pos.getZ();  // down = north
                default -> pos.getX();  // down = east
            };
        }

        PlaneAxes axes = axesForWall(side, part.getSpin());
        return project(pos, axes.up());
    }

    private static Set<Display> findLargestAnchoredRectangle(Set<Display> parts, Display anchor) {
        if (parts.isEmpty()) {
            return Collections.emptySet();
        }

        Direction side = anchor.getSide();
        boolean floorOrCeiling = side == Direction.UP || side == Direction.DOWN;
        byte anchorSpin = anchor.getSpin();

        Map<Pair<Integer, Integer>, Display> grid = new HashMap<>();
        int minCol = Integer.MAX_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (Display part : parts) {
            if (floorOrCeiling && part.getSpin() != anchorSpin) {
                continue;
            }
            int col = partCol(part);
            int row = partRow(part);

            grid.put(Pair.of(col, row), part);
            minCol = Math.min(minCol, col);
            minRow = Math.min(minRow, row);
            maxCol = Math.max(maxCol, col);
            maxRow = Math.max(maxRow, row);
        }

        int anchorCol = partCol(anchor);
        int anchorRow = partRow(anchor);

        int bestArea = 0;
        Set<Display> best = Collections.emptySet();

        if (side == Direction.UP) {
            for (int r1 = anchorRow; r1 <= maxRow; r1++) {
                for (int c1 = anchorCol; c1 <= maxCol; c1++) {
                    int area = (c1 - anchorCol + 1) * (r1 - anchorRow + 1);
                    if (area <= bestArea) {
                        continue;
                    }

                    Set<Display> candidate = new LinkedHashSet<>();
                    boolean valid = true;

                    outer:
                    for (int r = anchorRow; r <= r1; r++) {
                        for (int c = anchorCol; c <= c1; c++) {
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

        if (side == Direction.DOWN) {
            for (int r0 = anchorRow; r0 >= minRow; r0--) {
                for (int c1 = anchorCol; c1 <= maxCol; c1++) {
                    int area = (c1 - anchorCol + 1) * (anchorRow - r0 + 1);
                    if (area <= bestArea) {
                        continue;
                    }

                    Set<Display> candidate = new LinkedHashSet<>();
                    boolean valid = true;

                    outer:
                    for (int r = r0; r <= anchorRow; r++) {
                        for (int c = anchorCol; c <= c1; c++) {
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

    public static boolean isStructureComplete(Set<Display> group) {
        if (group == null || group.isEmpty()) {
            return false;
        }

        Set<Pair<Integer, Integer>> coords = new HashSet<>();
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;

        for (Display part : group) {
            int col = partCol(part);
            int row = partRow(part);

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

    public static Pair<Integer, Integer> getGridSize(List<Display> sorted) {
        int minCol = Integer.MAX_VALUE;
        int maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE;
        int maxRow = Integer.MIN_VALUE;

        for (Display part : sorted) {
            int col = partCol(part);
            int row = partRow(part);

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
        Set<Display> component = getActiveConnectedComponent(origin, side);
        if (component.isEmpty()) {
            return Pair.of(1, 1);
        }

        RenderGroup group = buildRenderGroupForActiveComponent(component);
        if (group.parts().isEmpty()) {
            return Pair.of(1, 1);
        }

        Pair<Integer, Integer> dims = getGridSize(new ArrayList<>(group.parts()));
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