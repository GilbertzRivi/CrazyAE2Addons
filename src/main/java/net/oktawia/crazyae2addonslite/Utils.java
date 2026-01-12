package net.oktawia.crazyae2addonslite;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.*;
import java.util.concurrent.*;

public class Utils {

    public static NavigableMap<Long, String> voltagesMap = new TreeMap<>(Map.ofEntries(
            Map.entry((long) Math.pow(2, 3), "ULV"),
            Map.entry((long) Math.pow(2, 5), "LV"),
            Map.entry((long) Math.pow(2, 7), "MV"),
            Map.entry((long) Math.pow(2, 9), "HV"),
            Map.entry((long) Math.pow(2, 11), "EV"),
            Map.entry((long) Math.pow(2, 13), "IV"),
            Map.entry((long) Math.pow(2, 15), "LuV"),
            Map.entry((long) Math.pow(2, 17), "ZPM"),
            Map.entry((long) Math.pow(2, 19), "UV"),
            Map.entry((long) Math.pow(2, 21), "UHV"),
            Map.entry((long) Math.pow(2, 23), "UEV"),
            Map.entry((long) Math.pow(2, 25), "UIV"),
            Map.entry((long) Math.pow(2, 27), "UXV"),
            Map.entry((long) Math.pow(2, 29), "OpV"),
            Map.entry((long) Math.pow(2, 31), "MAX")
    ));

    public static <T> List<T> rotate(List<T> inputList, int offset) {
        if (inputList.isEmpty()) {
            return new ArrayList<>(inputList);
        }

        int effectiveOffset = offset % inputList.size();
        if (effectiveOffset < 0) {
            effectiveOffset += inputList.size();
        }

        List<T> rotatedList = new ArrayList<>();
        rotatedList.addAll(inputList.subList(inputList.size() - effectiveOffset, inputList.size()));
        rotatedList.addAll(inputList.subList(0, inputList.size() - effectiveOffset));

        return rotatedList;
    }

    public static void asyncDelay(Runnable function, float delay) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        long delayInMillis = (long) (delay * 1000);
        scheduler.schedule(() -> {
            try {
                function.run();
            } finally {
                scheduler.shutdown();
            }
        }, delayInMillis, TimeUnit.MILLISECONDS);
    }

    public static String shortenNumber(double number) {
        Map<Double, String> thresholds = new LinkedHashMap<>();
        thresholds.put(1e18, "E");
        thresholds.put(1e15, "P");
        thresholds.put(1e12, "T");
        thresholds.put(1e9, "G");
        thresholds.put(1e6, "M");
        thresholds.put(1e3, "K");
        for (Map.Entry<Double, String> entry : thresholds.entrySet()) {
            double threshold = entry.getKey();
            String name = entry.getValue();
            if (number >= threshold) {
                return String.format("%.2f %s", number / threshold, name);
            }
        }
        return String.valueOf(number);
    }

    public static Direction getRightDirection(BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return facing.getClockWise(Direction.Axis.Y);
    }

    public static Direction getLeftDirection(BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        return facing.getCounterClockWise(Direction.Axis.Y);
    }

    public static String toTitle(String id) {
        StringBuilder out = new StringBuilder();

        for (String part : id.split("_")) {
            if (part.isEmpty()) continue;

            if (part.chars().anyMatch(Character::isDigit)) {
                out.append(part.toUpperCase());
            } else {
                out.append(Character.toUpperCase(part.charAt(0)))
                        .append(part.substring(1).toLowerCase());
            }
            out.append(' ');
        }
        return out.toString().trim();
    }
}