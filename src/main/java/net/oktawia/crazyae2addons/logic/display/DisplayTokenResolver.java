package net.oktawia.crazyae2addons.logic.display;

import appeng.api.networking.storage.IStorageService;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.logic.display.keytypes.DisplayKeyCompatRegistry;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.packets.DisplaySyncPacket;
import net.oktawia.crazyae2addons.parts.Display;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class DisplayTokenResolver {

    private static final long MAX_RATE_WINDOW_TICKS = 20L * 60L * 30L;

    private static final Pattern SERVER_STOCK_TOKEN = Pattern.compile("&(s\\^[\\w:]+(?:%\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SERVER_DELTA_TOKEN = Pattern.compile("&(d\\^[a-z0-9_\\.:]+(?:%\\d+[tsm])?@\\d+[tsm])", Pattern.CASE_INSENSITIVE);
    private static final Pattern DELTA_PARSE = Pattern.compile(
            "^d\\^([a-z0-9_\\.:]+)(?:%(\\d+)([tsm]))?@([0-9]+)([tsm])$",
            Pattern.CASE_INSENSITIVE
    );

    private DisplayTokenResolver() {
    }

    public record DeltaSpec(String id, long perTicksForDisplay, long updateEveryTicks, long windowTicks) {
    }

    private record DeltaToken(String tokenKey, DeltaSpec spec) {
    }

    public static void recomputeVariablesAndNotify(Display part) {
        var level = part.getLevel();
        if (level == null || level.isClientSide()) {
            return;
        }

        String txt = part.getTextValue() == null ? "" : part.getTextValue();

        List<String> stockTokens = new ArrayList<>();
        List<DeltaToken> deltaTokens = new ArrayList<>();
        Set<String> neededIds = new HashSet<>();
        Set<String> liveKeys = new HashSet<>();

        Matcher sm = SERVER_STOCK_TOKEN.matcher(txt);
        while (sm.find()) {
            String token = sm.group(1);
            stockTokens.add(token);
            liveKeys.add(token);
            String core = token;
            int pct = token.indexOf('%');
            if (pct >= 0) {
                core = token.substring(0, pct);
            }
            if (core.startsWith("s^")) {
                neededIds.add(core.substring(2));
            }
        }

        Matcher dm = SERVER_DELTA_TOKEN.matcher(txt);
        while (dm.find()) {
            String token = dm.group(1);
            DeltaSpec spec = parseDeltaSpec(token);
            if (spec != null) {
                deltaTokens.add(new DeltaToken(token, spec));
                liveKeys.add(token);
                neededIds.add(spec.id());
            }
        }

        if (stockTokens.size() > 512) {
            stockTokens = stockTokens.subList(0, 512);
        }
        if (deltaTokens.size() > 512) {
            deltaTokens = deltaTokens.subList(0, 512);
        }

        Map<String, Long> amounts = neededIds.isEmpty() ? Collections.emptyMap() : getAmountsInME(part, neededIds);
        long nowTick = level.getGameTime();

        if (!deltaTokens.isEmpty()) {
            Set<String> deltaIds = deltaTokens.stream().map(dt -> dt.spec().id()).collect(Collectors.toSet());
            part.rateHistory.keySet().removeIf(id -> !deltaIds.contains(id));

            if (amounts.isEmpty()) {
                part.gridWarmupRemaining = 2;
            } else if (part.gridWarmupRemaining > 0) {
                part.gridWarmupRemaining--;
            }

            for (String id : deltaIds) {
                if (!amounts.containsKey(id)) {
                    continue;
                }
                if (part.gridWarmupRemaining > 0) {
                    continue;
                }
                SampleRing ring = part.rateHistory.computeIfAbsent(id, k -> new SampleRing());
                ring.add(nowTick, amounts.get(id));
                ring.trimOlderThan(nowTick - MAX_RATE_WINDOW_TICKS);
            }
        } else {
            part.rateHistory.clear();
            part.gridWarmupRemaining = 2;
        }

        for (String token : stockTokens) {
            String core = token;
            long divisor = 1L;
            int pct = token.indexOf('%');
            if (pct >= 0) {
                core = token.substring(0, pct);
                try {
                    int pow = Integer.parseInt(token.substring(pct + 1));
                    if (pow > 0) {
                        divisor = (long) Math.pow(10, pow);
                    }
                } catch (NumberFormatException e) {
                    CrazyAddons.LOGGER.debug("invalid divisor format in display token", e);
                }
            }
            if (!core.startsWith("s^")) {
                continue;
            }
            String itemId = core.substring(2);
            if (!amounts.containsKey(itemId)) {
                continue;
            }
            long amount = amounts.get(itemId);
            long display = Math.round((double) amount / (double) divisor);
            part.resolvedTokens.put(token, String.valueOf(display));
        }

        for (DeltaToken dt : deltaTokens) {
            String tokenKey = dt.tokenKey();
            DeltaSpec spec = dt.spec();
            long cur = amounts.getOrDefault(spec.id(), 0L);
            SampleRing ring = part.rateHistory.get(spec.id());
            String out = "0";
            if (ring != null) {
                long targetTick = nowTick - spec.windowTicks();
                SampleRing.Sample past = ring.getAtOrBefore(targetTick);
                if (past == null) {
                    past = ring.oldest();
                }
                if (past != null) {
                    long dticks = nowTick - past.tick();
                    if (dticks > 0) {
                        long delta = cur - past.value();
                        double scaled = (double) delta * (double) spec.perTicksForDisplay() / (double) dticks;
                        out = formatSignedNumber(scaled);
                    }
                }
            }
            part.resolvedTokens.put(tokenKey, out);
        }

        if (!liveKeys.isEmpty()) {
            part.resolvedTokens.keySet().removeIf(k ->
                    (k.startsWith("s^") || k.startsWith("d^")) && !liveKeys.contains(k));
        }

        String packed = part.resolvedTokens.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("|"));

        var be = part.getBlockEntity();
        NetworkHandler.sendToTrackingChunk(
                level.getChunkAt(be.getBlockPos()),
                new DisplaySyncPacket(be.getBlockPos(), part.getSide(), packed)
        );
    }

    private static Map<String, Long> getAmountsInME(Display part, Set<String> ids) {
        Map<String, Long> out = new HashMap<>();
        try {
            var node = part.getGridNode();
            if (node == null) {
                return out;
            }
            var grid = node.getGrid();
            if (grid == null) {
                return out;
            }
            IStorageService storage = grid.getService(IStorageService.class);
            if (storage == null) {
                return out;
            }

            var avail = storage.getInventory().getAvailableStacks();
            Map<Object, Long> byKey = new HashMap<>();
            for (var gs : avail) {
                byKey.merge(gs.getKey(), gs.getLongValue(), Long::sum);
            }

            for (String id : ids) {
                try {
                    AEKey key = resolveKey(id);
                    if (key != null) {
                        out.put(id, byKey.getOrDefault(key, 0L));
                    }
                } catch (Throwable e) {
                    CrazyAddons.LOGGER.debug("failed to resolve display key: {}", id, e);
                }
            }
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("failed to resolve display storage amounts", e);
        }
        return out;
    }

    @Nullable
    public static AEKey resolveKey(String rawId) {
        int colon = rawId.indexOf(':');
        if (colon > 0) {
            String prefix = rawId.substring(0, colon);
            String rest = rawId.substring(colon + 1);
            if (prefix.equals("item")) {
                var item = BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(rest)).orElse(null);
                return (item != null && item != net.minecraft.world.item.Items.AIR) ? AEItemKey.of(item) : null;
            }
            if (prefix.equals("fluid")) {
                var fluid = BuiltInRegistries.FLUID.getOptional(ResourceLocation.parse(rest)).orElse(null);
                return (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) ? AEFluidKey.of(fluid) : null;
            }
            if (DisplayKeyCompatRegistry.hasPrefix(prefix)) {
                return DisplayKeyCompatRegistry.resolve(prefix, rest);
            }
        }
        ResourceLocation rl = ResourceLocation.parse(rawId);
        var item = BuiltInRegistries.ITEM.getOptional(rl).orElse(null);
        if (item != null && item != net.minecraft.world.item.Items.AIR) {
            return AEItemKey.of(item);
        }
        var fluid = BuiltInRegistries.FLUID.getOptional(rl).orElse(null);
        return (fluid != null && fluid != net.minecraft.world.level.material.Fluids.EMPTY) ? AEFluidKey.of(fluid) : null;
    }

    @Nullable
    public static DeltaSpec parseDeltaSpec(String token) {
        Matcher m = DELTA_PARSE.matcher(token);
        if (!m.matches()) {
            return null;
        }

        String id = m.group(1);
        long perN = 20L;
        char perU = 't';

        if (m.group(2) != null && m.group(3) != null) {
            try {
                perN = Long.parseLong(m.group(2));
                perU = m.group(3).charAt(0);
            } catch (Throwable e) {
                CrazyAddons.LOGGER.debug("invalid rate format in display token", e);
            }
        }

        long winN = 5L;
        char winU = 's';
        try {
            winN = Long.parseLong(m.group(4));
            winU = m.group(5).charAt(0);
        } catch (Throwable e) {
            CrazyAddons.LOGGER.debug("invalid window format in display token", e);
        }

        long perTicks = unitToTicks(perN, perU);
        long windowTicks = unitToTicks(winN, winU);
        if (windowTicks < 20L) {
            windowTicks = 20L;
        }
        if (windowTicks > MAX_RATE_WINDOW_TICKS) {
            windowTicks = MAX_RATE_WINDOW_TICKS;
        }

        return new DeltaSpec(id, Math.max(1L, perTicks), Math.max(20L, perTicks), windowTicks);
    }

    public static long unitToTicks(long n, char unit) {
        n = Math.max(1L, n);
        return switch (Character.toLowerCase(unit)) {
            case 't' -> n;
            case 's' -> n * 20L;
            case 'm' -> n * 20L * 60L;
            default -> n;
        };
    }

    public static String formatSignedNumber(double v) {
        if (Math.abs(v) < 0.0005) {
            return "0";
        }
        long r = Math.round(v);
        if (Math.abs(v - r) < 1e-9) {
            return (r > 0 ? "+" : "") + r;
        }
        String s = String.format(Locale.ROOT, "%+.2f", v);
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
}