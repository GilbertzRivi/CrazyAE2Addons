package net.oktawia.crazyae2addons.util;

import java.util.*;
import java.util.concurrent.*;

public class Utils {

    private static final ScheduledExecutorService SCHEDULER = Executors.newSingleThreadScheduledExecutor();

    public static void asyncDelay(Runnable function, float delay) {
        long delayInMillis = (long) (delay * 1000);
        SCHEDULER.schedule(function, delayInMillis, TimeUnit.MILLISECONDS);
    }

    private static final Map<Double, String> SHORTEN_THRESHOLDS;
    static {
        SHORTEN_THRESHOLDS = new LinkedHashMap<>();
        SHORTEN_THRESHOLDS.put(1e18, "E");
        SHORTEN_THRESHOLDS.put(1e15, "P");
        SHORTEN_THRESHOLDS.put(1e12, "T");
        SHORTEN_THRESHOLDS.put(1e9,  "G");
        SHORTEN_THRESHOLDS.put(1e6,  "M");
        SHORTEN_THRESHOLDS.put(1e3,  "K");
    }

    public static String shortenNumber(double number) {
        return shortenNumber(number, 2);
    }

    public static String shortenNumber(double number, int decimals) {
        double abs = Math.abs(number);

        for (Map.Entry<Double, String> entry : SHORTEN_THRESHOLDS.entrySet()) {
            double threshold = entry.getKey();
            String suffix = entry.getValue();

            if (abs >= threshold) {
                return formatDecimal(number / threshold, decimals) + " " + suffix;
            }
        }

        return formatDecimal(number, decimals);
    }

    private static String formatDecimal(double value, int decimals) {
        String s = String.format(Locale.ROOT, "%." + decimals + "f", value);

        if (s.indexOf('.') >= 0) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }

        return s;
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