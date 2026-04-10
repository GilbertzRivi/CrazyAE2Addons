package net.oktawia.crazyae2addons.logic.builder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;

import java.util.Optional;

public final class BuilderCoordMath {

    private BuilderCoordMath() {}

    public static BlockPos localToWorldOffset(BlockPos local, Direction facing) {
        int fx, fz, rx, rz;
        switch (facing) {
            case SOUTH -> { fx = 0; fz = 1; rx = -1; rz = 0; }
            case EAST  -> { fx = 1; fz = 0; rx = 0;  rz = 1; }
            case WEST  -> { fx = -1; fz = 0; rx = 0; rz = -1; }
            default    -> { fx = 0; fz = -1; rx = 1; rz = 0; }
        }
        int wx = local.getX() * rx + local.getZ() * fx;
        int wy = local.getY();
        int wz = local.getX() * rz + local.getZ() * fz;
        return new BlockPos(wx, wy, wz);
    }

    public static BlockPos stepRelative(BlockPos pos, char cmd, Direction facing) {
        int fx, fz, rx, rz;
        switch (facing) {
            case SOUTH -> { fx = 0; fz = 1; rx = -1; rz = 0; }
            case EAST  -> { fx = 1; fz = 0; rx = 0;  rz = 1; }
            case WEST  -> { fx = -1; fz = 0; rx = 0; rz = -1; }
            default    -> { fx = 0; fz = -1; rx = 1; rz = 0; }
        }
        return switch (cmd) {
            case 'F' -> pos.offset(fx, 0, fz);
            case 'B' -> pos.offset(-fx, 0, -fz);
            case 'R' -> pos.offset(rx, 0, rz);
            case 'L' -> pos.offset(-rx, 0, -rz);
            case 'U' -> pos.offset(0, 1, 0);
            case 'D' -> pos.offset(0, -1, 0);
            default  -> pos;
        };
    }

    public static BlockPos stepLocal(BlockPos pos, char cmd) {
        return switch (cmd) {
            case 'F' -> pos.offset(0, 0, 1);
            case 'B' -> pos.offset(0, 0, -1);
            case 'R' -> pos.offset(1, 0, 0);
            case 'L' -> pos.offset(-1, 0, 0);
            case 'U' -> pos.offset(0, 1, 0);
            case 'D' -> pos.offset(0, -1, 0);
            default  -> pos;
        };
    }

    public static int yawStepsFromNorth(Direction d) {
        return switch (d) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default    -> 0;
        };
    }

    public static Direction rotateHorizontal(Direction dir, int steps) {
        steps = Math.floorMod(steps, 4);
        Direction out = dir;
        for (int i = 0; i < steps; i++) out = out.getClockWise();
        return out;
    }

    public static boolean isHorizontal(Direction d) {
        return d.getAxis().isHorizontal();
    }

    public static BlockState rotateStateByDelta(BlockState state, int steps) {
        for (var p : state.getProperties()) {
            if (p instanceof DirectionProperty dp) {
                Direction d = state.getValue(dp);
                if (isHorizontal(d)) state = state.setValue(dp, rotateHorizontal(d, steps));
            }
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            var ax = state.getValue(BlockStateProperties.AXIS);
            if (ax.isHorizontal() && (steps % 2 != 0)) {
                state = state.setValue(BlockStateProperties.AXIS,
                        ax == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            }
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            var ax = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            if (ax.isHorizontal() && (steps % 2 != 0)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_AXIS,
                        ax == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            }
        }
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int val = state.getValue(BlockStateProperties.ROTATION_16);
            val = (val + steps * 4) & 15;
            state = state.setValue(BlockStateProperties.ROTATION_16, val);
        }
        return state;
    }

    public static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String valueStr) {
        try {
            if (property instanceof BooleanProperty bp) {
                return state.setValue(bp, Boolean.parseBoolean(valueStr));
            }
            if (property instanceof IntegerProperty ip) {
                return state.setValue(ip, Integer.parseInt(valueStr));
            }
            Optional<T> value = property.getValue(valueStr);
            if (value.isPresent()) return state.setValue(property, value.get());
        } catch (Exception ignored) {}
        return state;
    }

    public static boolean isBreakable(BlockState state, Level level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) >= 0;
    }
}
