package net.oktawia.crazyae2addonslite.parts;

import appeng.api.networking.IGridNode;
import appeng.api.networking.storage.IStorageService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.util.AECableType;
import appeng.items.parts.PartModels;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.automation.PlaneModels;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addonslite.MathParser;
import net.oktawia.crazyae2addonslite.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addonslite.menus.DisplayMenu;
import net.oktawia.crazyae2addonslite.network.DisplayValuePacket;
import net.oktawia.crazyae2addonslite.network.NetworkHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DisplayPart extends AEBasePart implements MenuProvider, IGridTickable {

    private static final PlaneModels MODELS = new PlaneModels("part/display_mon_off", "part/display_mon_on");

    private static final Pattern CLIENT_VAR_TOKEN = Pattern.compile(
            "&(d\\^[a-z0-9_\\.:]+(?:%\\d+[tsm])?@\\d+[tsm]|" +
                    "s\\^[a-z0-9_\\.:]+(?:%\\d+)?|" +
                    "i\\^[a-z0-9_.\\-]+:[a-z0-9_./\\-]+|" +
                    "[A-Za-z0-9_]+)",
            Pattern.CASE_INSENSITIVE
    );
    private static final Pattern CLIENT_STOCK_TOKEN = Pattern.compile("&s\\^([a-z0-9_\\.:]+)(?:%(\\d+))?", Pattern.CASE_INSENSITIVE);
    private static final Pattern ICON_TOKEN = Pattern.compile("&i\\^([a-z0-9_.\\-]+:[a-z0-9_./\\-]+)", Pattern.CASE_INSENSITIVE);

    private static final Pattern SERVER_STOCK_TOKEN = Pattern.compile("&(s\\^[\\w:]+(?:%\\d+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern SERVER_DELTA_TOKEN = Pattern.compile("&(d\\^[a-z0-9_\\.:]+(?:%\\d+[tsm])?@\\d+[tsm])", Pattern.CASE_INSENSITIVE);

    private static final Pattern DELTA_PARSE = Pattern.compile(
            "^d\\^([a-z0-9_\\.:]+)(?:%(\\d+)([tsm]))?@([0-9]+)([tsm])$",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern LINE_SPLIT = Pattern.compile("(?:&nl|\\r\\n|\\r|\\n)");

    // kolor/tylko do inline parsowania (&cRRGGBB / &bRRGGBB)
    private static final Pattern COLOR_TOKEN = Pattern.compile("(&[cb])([0-9A-Fa-f]{6})");

    public byte spin = 0; // 0-3
    public String textValue = "";
    public HashMap<String, String> variables = new HashMap<>();
    public boolean reRegister = true;
    public String identifier = randomHexId();
    public boolean mode = true;
    public int fontSize;
    public boolean margin = false;
    public boolean center = false;

    // ====== RATE / DELTA HISTORY (server-side) ======
    private static final int MAX_RATE_SAMPLES = 2048;
    private static final long MAX_RATE_WINDOW_TICKS = 20L * 60L * 30L; // 30 minut
    private final Map<String, SampleRing> rateHistory = new HashMap<>();
    private final Map<String, Long> deltaLastUpdateTick = new HashMap<>(); // (zostawione, może się przydać)

    private static final class SampleRing {
        private final long[] ticks = new long[MAX_RATE_SAMPLES];
        private final long[] values = new long[MAX_RATE_SAMPLES];
        private int head = 0; // oldest
        private int size = 0;

        private int idx(int i) { // i=0..size-1 (0=oldest)
            int x = head + i;
            int m = MAX_RATE_SAMPLES;
            return x >= m ? x - m : x;
        }

        void add(long tick, long value) {
            if (size > 0) {
                long lastTick = ticks[idx(size - 1)];
                if (tick <= lastTick) return;
            }
            if (size < MAX_RATE_SAMPLES) {
                int tail = idx(size);
                ticks[tail] = tick;
                values[tail] = value;
                size++;
            } else {
                head = (head + 1) % MAX_RATE_SAMPLES; // drop oldest
                int tail = idx(size - 1);
                ticks[tail] = tick;
                values[tail] = value;
            }
        }

        void trimOlderThan(long minTick) {
            while (size > 0 && ticks[head] < minTick) {
                head = (head + 1) % MAX_RATE_SAMPLES;
                size--;
            }
        }

        @Nullable Sample getAtOrBefore(long targetTick) {
            for (int i = size - 1; i >= 0; i--) {
                int p = idx(i);
                if (ticks[p] <= targetTick) return new Sample(ticks[p], values[p]);
            }
            return null;
        }

        @Nullable Sample oldest() {
            if (size <= 0) return null;
            return new Sample(ticks[head], values[head]);
        }
    }

    private record Sample(long tick, long value) { }

    private record DeltaSpec(String id, long perTicksForDisplay, long updateEveryTicks, long windowTicks) { }
    private record DeltaToken(String tokenKey, DeltaSpec spec) { }

    private static long unitToTicks(long n, char unit) {
        n = Math.max(1L, n);
        return switch (Character.toLowerCase(unit)) {
            case 't' -> n;
            case 's' -> n * 20L;
            case 'm' -> n * 20L * 60L;
            default -> n;
        };
    }

    private static String formatSignedNumber(double v) {
        if (Math.abs(v) < 0.0005) return "0";

        long r = Math.round(v);
        if (Math.abs(v - r) < 1e-9) {
            return (r > 0 ? "+" : "") + r;
        }

        String s = String.format(Locale.ROOT, "%+.2f", v);
        if (s.indexOf('.') >= 0) {
            while (s.endsWith("0")) s = s.substring(0, s.length() - 1);
            if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        }
        return s;
    }

    @Nullable
    private static DeltaSpec parseDeltaSpec(String token) {
        Matcher m = DELTA_PARSE.matcher(token);
        if (!m.matches()) return null;

        String id = m.group(1);

        long perN = 20L;
        char perU = 't';

        if (m.group(2) != null && m.group(3) != null) {
            try {
                perN = Long.parseLong(m.group(2));
                perU = m.group(3).charAt(0);
            } catch (Throwable ignored) { }
        }

        long winN = 5L;
        char winU = 's';
        try {
            winN = Long.parseLong(m.group(4));
            winU = m.group(5).charAt(0);
        } catch (Throwable ignored) { }

        long perTicks = unitToTicks(perN, perU);
        long windowTicks = unitToTicks(winN, winU);

        // min okno = 1s
        if (windowTicks < 20L) windowTicks = 20L;
        if (windowTicks > MAX_RATE_WINDOW_TICKS) windowTicks = MAX_RATE_WINDOW_TICKS;

        // i tak liczysz/syncujesz co 1s, ale zostawiamy semantykę % jako "na ile ticków/sekund/minut"
        long updateEvery = Math.max(20L, perTicks);
        long perTicksForDisplay = Math.max(1L, perTicks);

        return new DeltaSpec(id, perTicksForDisplay, updateEvery, windowTicks);
    }

    @PartModels
    public static List<IPartModel> getModels() {
        return MODELS.getModels();
    }

    public DisplayPart(IPartItem<?> partItem) {
        super(partItem);
        this.getMainNode()
                .setIdlePowerUsage(1)
                .addService(IGridTickable.class, this);
    }

    public static String randomHexId() {
        SecureRandom rand = new SecureRandom();
        StringBuilder sb = new StringBuilder(4);
        for (int i = 0; i < 4; i++) sb.append(Integer.toHexString(rand.nextInt(16)).toUpperCase());
        return sb.toString();
    }

    @Override
    public void getBoxes(IPartCollisionHelper bch) {
        bch.addBox(0, 0, 15.5, 16, 16, 16);
    }

    @Override
    public float getCableConnectionLength(AECableType cable) {
        return 1;
    }

    @Override
    public IPartModel getStaticModels() {
        return MODELS.getModel(this.isPowered(), this.isActive());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new DisplayMenu(containerId, playerInventory, this);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public boolean onPartActivate(Player p, InteractionHand hand, Vec3 pos) {
        if (!p.getCommandSenderWorld().isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.DISPLAY_MENU.get(), p, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void readFromNBT(CompoundTag extra) {
        super.readFromNBT(extra);
        if (extra.contains("textvalue")) this.textValue = extra.getString("textvalue");
        if (extra.contains("spin")) this.spin = extra.getByte("spin");
        if (extra.contains("ident")) this.identifier = extra.getString("ident");
        if (extra.contains("mode")) this.mode = extra.getBoolean("mode");
        if (extra.contains("margin")) this.margin = extra.getBoolean("margin");
        if (extra.contains("center")) this.center = extra.getBoolean("center");

        if (!isClientSide()) {
            String packed = this.variables.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("|"));
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new DisplayValuePacket(this.getBlockEntity().getBlockPos(), this.textValue, this.getSide(), this.spin, packed, fontSize, mode, margin, center));
        }
    }

    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        extra.putString("textvalue", this.textValue);
        extra.putByte("spin", this.spin);
        extra.putString("ident", this.identifier);
        extra.putBoolean("mode", this.mode);
        extra.putBoolean("margin", this.margin);
        extra.putBoolean("center", this.center);
    }

    @Override
    public boolean requireDynamicRender() {
        return true;
    }

    @Override
    public final void onPlacement(Player player) {
        super.onPlacement(player);
        final byte rotation = (byte) (Mth.floor(player.getYRot() * 4F / 360F + 2.5D) & 3);
        if (getSide() == Direction.UP || getSide() == Direction.DOWN) {
            this.spin = rotation;
        }
    }

    // ====== ME query (one scan per recompute) ======

    private @Nullable IStorageService getStorageService() {
        try {
            var node = this.getGridNode();
            if (node == null) return null;
            var grid = node.getGrid();
            if (grid == null) return null;
            return grid.getService(IStorageService.class);
        } catch (Throwable t) {
            return null;
        }
    }

    private Map<String, Long> getAmountsInME(Set<String> ids) {
        Map<String, Long> out = new HashMap<>();
        if (ids == null || ids.isEmpty()) return out;

        try {
            var storage = getStorageService();
            if (storage == null) {
                for (String id : ids) out.put(id, 0L);
                return out;
            }

            var avail = storage.getInventory().getAvailableStacks();

            Map<Object, Long> byKey = new HashMap<>();
            for (var gs : avail) {
                byKey.merge(gs.getKey(), gs.getLongValue(), Long::sum);
            }

            for (String id : ids) {
                long amount = 0L;
                try {
                    ResourceLocation rl = new ResourceLocation(id);

                    var item = ForgeRegistries.ITEMS.getValue(rl);
                    if (item != null && item != Items.AIR) {
                        var key = AEItemKey.of(new ItemStack(item));
                        if (key != null) amount = byKey.getOrDefault(key, 0L);
                    } else {
                        var fluid = ForgeRegistries.FLUIDS.getValue(rl);
                        if (fluid != null) {
                            var fKey = AEFluidKey.of(new FluidStack(fluid, 1));
                            if (fKey != null) amount = byKey.getOrDefault(fKey, 0L);
                        }
                    }
                } catch (Throwable ignored) { }
                out.put(id, amount);
            }

            return out;
        } catch (Throwable t) {
            for (String id : ids) out.put(id, 0L);
            return out;
        }
    }

    private void recomputeVariablesAndNotify() {
        if (this.getLevel() == null || this.getLevel().isClientSide()) return;

        String txt = this.textValue == null ? "" : this.textValue;

        List<String> stockTokens = new ArrayList<>();
        List<DeltaToken> deltaTokens = new ArrayList<>();
        Set<String> neededIds = new HashSet<>();
        Set<String> liveKeys = new HashSet<>();

        Matcher sm = SERVER_STOCK_TOKEN.matcher(txt);
        while (sm.find()) {
            String token = sm.group(1); // bez &
            stockTokens.add(token);
            liveKeys.add(token);

            String core = token;
            int pct = token.indexOf('%');
            if (pct >= 0) core = token.substring(0, pct);
            if (core.startsWith("s^")) neededIds.add(core.substring(2));
        }

        Matcher dm = SERVER_DELTA_TOKEN.matcher(txt);
        while (dm.find()) {
            String token = dm.group(1); // bez &
            DeltaSpec spec = parseDeltaSpec(token);
            if (spec != null) {
                deltaTokens.add(new DeltaToken(token, spec));
                liveKeys.add(token);
                neededIds.add(spec.id());
            }
        }

        if (stockTokens.size() > 512) stockTokens = stockTokens.subList(0, 512);
        if (deltaTokens.size() > 512) deltaTokens = deltaTokens.subList(0, 512);

        Map<String, Long> amounts = neededIds.isEmpty() ? Collections.emptyMap() : getAmountsInME(neededIds);
        long nowTick = this.getLevel().getGameTime();

        // próbka co 1s
        if (!deltaTokens.isEmpty()) {
            Set<String> deltaIds = deltaTokens.stream().map(dt -> dt.spec().id()).collect(Collectors.toSet());

            rateHistory.keySet().removeIf(id -> !deltaIds.contains(id));

            for (String id : deltaIds) {
                SampleRing ring = rateHistory.computeIfAbsent(id, k -> new SampleRing());
                ring.add(nowTick, amounts.getOrDefault(id, 0L));
                ring.trimOlderThan(nowTick - MAX_RATE_WINDOW_TICKS);
            }
        } else {
            rateHistory.clear();
            deltaLastUpdateTick.clear();
        }

        // stock
        for (String token : stockTokens) {
            String core = token;
            long divisor = 1L;

            int pct = token.indexOf('%');
            if (pct >= 0) {
                core = token.substring(0, pct);
                try {
                    int pow = Integer.parseInt(token.substring(pct + 1));
                    if (pow > 0) divisor = (long) Math.pow(10, pow);
                } catch (NumberFormatException ignored) {
                    divisor = 1L;
                }
            }

            if (!core.startsWith("s^")) continue;

            String itemId = core.substring(2);
            long amount = amounts.getOrDefault(itemId, 0L);
            long display = Math.round((double) amount / (double) divisor);

            this.variables.put(token, String.valueOf(display));
        }

        // delta/rate (liczone zawsze, sync zawsze)
        for (DeltaToken dt : deltaTokens) {
            String tokenKey = dt.tokenKey();
            DeltaSpec spec = dt.spec();

            long cur = amounts.getOrDefault(spec.id(), 0L);
            SampleRing ring = rateHistory.get(spec.id());

            String out = "0";
            if (ring != null) {
                long targetTick = nowTick - spec.windowTicks();
                Sample past = ring.getAtOrBefore(targetTick);
                if (past == null) past = ring.oldest();

                if (past != null) {
                    long dticks = nowTick - past.tick();
                    if (dticks > 0) {
                        long delta = cur - past.value();
                        double scaled = (double) delta * (double) spec.perTicksForDisplay() / (double) dticks;
                        out = formatSignedNumber(scaled);
                    }
                }
            }

            this.variables.put(tokenKey, out);
        }

        // prune
        if (!liveKeys.isEmpty()) {
            List<String> toRemove = new ArrayList<>();
            for (String k : this.variables.keySet()) {
                if ((k.startsWith("s^") || k.startsWith("d^")) && !liveKeys.contains(k)) {
                    toRemove.add(k);
                }
            }
            for (String k : toRemove) this.variables.remove(k);

            deltaLastUpdateTick.keySet().removeIf(k -> k.startsWith("d^") && !liveKeys.contains(k));
        }

        // SYNC zawsze co sekundę (monitor render + ikonki)
        String packed = this.variables.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining("|"));

        NetworkHandler.INSTANCE.send(
                PacketDistributor.ALL.noArg(),
                new DisplayValuePacket(
                        this.getBlockEntity().getBlockPos(),
                        this.textValue,
                        this.getSide(),
                        this.spin,
                        packed,
                        this.fontSize,
                        this.mode,
                        this.margin,
                        this.center
                )
        );
    }

    public void updateController(String value) {
        this.textValue = value;
        try {
            var node = this.getGridNode();
            if (node == null) { this.reRegister = true; return; }
            var grid = node.getGrid();
            if (grid == null) { this.reRegister = true; return; }

            if (!isClientSide()) {
                recomputeVariablesAndNotify();
            }
        } catch (Throwable t) {
            this.reRegister = true;
        }
    }

    private record Transformation(float tx, float ty, float tz, float yRotation, float xRotation) { }

    public boolean isStructureComplete(Set<DisplayPart> group) {
        if (group == null || group.isEmpty()) return false;

        Direction side = this.getSide();

        Set<Pair<Integer, Integer>> coords = new HashSet<>();

        int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;

        for (DisplayPart part : group) {
            BlockPos pos = part.getBlockEntity().getBlockPos();
            int col = 0, row = 0;

            switch (side) {
                case NORTH, SOUTH -> { col = pos.getX(); row = pos.getY(); }
                case EAST, WEST -> { col = pos.getZ(); row = pos.getY(); }
                case UP, DOWN -> { col = pos.getX(); row = pos.getZ(); }
            }

            coords.add(Pair.of(col, row));
            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        for (int col = minCol; col <= maxCol; col++) {
            for (int row = minRow; row <= maxRow; row++) {
                if (!coords.contains(Pair.of(col, row))) return false;
            }
        }

        return true;
    }

    private void applyFacingTransform(PoseStack poseStack) {
        Transformation t = getFacingTransformation(getSide());
        poseStack.translate(t.tx, t.ty, t.tz);
        poseStack.mulPose(Axis.YP.rotationDegrees(t.yRotation));
        poseStack.mulPose(Axis.XP.rotationDegrees(t.xRotation));
        if (t.xRotation != 0) applySpinTransformation(poseStack, t.xRotation);
    }

    private Transformation getFacingTransformation(Direction facing) {
        float tx = 0, ty = 0, tz = 0, yRot = 0, xRot = 0;
        switch (facing) {
            case SOUTH -> { tx = 0;   ty = 1;   tz = 0.5F; }
            case WEST  -> { tx = 0.5F;ty = 1;   tz = 0;    yRot = -90F; }
            case EAST  -> { tx = 0.5F;ty = 1;   tz = 1;    yRot = 90F; }
            case NORTH -> { tx = 1;   ty = 1;   tz = 0.5F; yRot = 180F; }
            case UP    -> { tx = 0;   ty = 0.5F;tz = 0;    xRot = -90F; }
            case DOWN  -> { tx = 1;   ty = 0.5F;tz = 0;    xRot = 90F; }
        }
        return new Transformation(tx, ty, tz, yRot, xRot);
    }

    private void applySpinTransformation(PoseStack poseStack, float upRotation) {
        float theSpin = 0.0F;
        if (upRotation == 90F) {
            switch (this.spin) {
                case 0 -> { theSpin = 0.0F;   poseStack.translate(-1, 1, 0); }
                case 1 -> { theSpin = 90.0F;  poseStack.translate(-1, 0, 0); }
                case 2 -> { theSpin = 180.0F; poseStack.translate(0, 0, 0); }
                case 3 -> { theSpin = -90.0F; poseStack.translate(0, 1, 0); }
            }
        } else {
            switch (this.spin) {
                case 0 -> { theSpin = 0.0F;   poseStack.translate(0, 0, 0); }
                case 1 -> { theSpin = -90.0F; poseStack.translate(1, 0, 0); }
                case 2 -> { theSpin = 180.0F; poseStack.translate(1, -1, 0); }
                case 3 -> { theSpin = 90.0F;  poseStack.translate(0, -1, 0); }
            }
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(theSpin));
    }

    public Set<DisplayPart> getConnectedGrid() {
        Set<BlockPos> visited = new HashSet<>();
        Set<DisplayPart> result = new LinkedHashSet<>();
        Direction side = getSide();

        if (side == Direction.UP || side == Direction.DOWN) {
            result.add(this);
            return result;
        }

        DisplayPart base = this;
        while (true) {
            DisplayPart down = base.getNeighbor(Direction.DOWN);
            if (down != null && down.getSide() == side && down.isPowered() && down.mode) {
                base = down;
                continue;
            }
            DisplayPart left = base.getNeighbor(side.getCounterClockWise());
            if (left != null && left.getSide() == side && left.isPowered() && left.mode) {
                base = left;
                continue;
            }
            break;
        }

        DisplayPart rowStart = base;
        do {
            DisplayPart current = rowStart;
            do {
                BlockPos pos = current.getBlockEntity().getBlockPos();
                if (visited.add(pos)) {
                    result.add(current);
                }

                current = current.getNeighbor(side.getClockWise());
            } while (current != null && current.getSide() == side && current.isPowered() && current.mode);

            rowStart = rowStart.getNeighbor(Direction.UP);
        } while (rowStart != null && rowStart.getSide() == side && rowStart.isPowered() && rowStart.mode);

        if (isStructureComplete(result)) {
            return result;
        } else {
            return Set.of(this);
        }
    }

    @Nullable
    public DisplayPart getNeighbor(Direction dir) {
        BlockPos neighborPos = getBlockEntity().getBlockPos().relative(dir);
        BlockEntity be = getLevel().getBlockEntity(neighborPos);
        if (be instanceof appeng.blockentity.networking.CableBusBlockEntity cbbe &&
                cbbe.getPart(getSide()) instanceof DisplayPart neighbor &&
                neighbor.getSide() == this.getSide() &&
                neighbor.isPowered()) {
            return neighbor;
        }
        return null;
    }

    public boolean isRenderOrigin(Set<DisplayPart> group) {
        return this == group.stream().toList().get(group.size() - 1);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        // 1/s
        return new TickingRequest(20, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (!isClientSide()) {
            recomputeVariablesAndNotify();
        }
        return TickRateModulation.IDLE;
    }

    public static Pair<Integer, Integer> getGridSize(List<DisplayPart> sorted, Direction side) {
        int minCol = Integer.MAX_VALUE, maxCol = Integer.MIN_VALUE;
        int minRow = Integer.MAX_VALUE, maxRow = Integer.MIN_VALUE;

        for (DisplayPart part : sorted) {
            BlockPos pos = part.getBlockEntity().getBlockPos();
            int col = 0, row = 0;

            switch (side) {
                case NORTH, SOUTH -> { col = pos.getX(); row = pos.getY(); }
                case EAST, WEST -> { col = pos.getZ(); row = pos.getY(); }
                case UP, DOWN -> { col = pos.getX(); row = pos.getZ(); }
            }

            minCol = Math.min(minCol, col);
            maxCol = Math.max(maxCol, col);
            minRow = Math.min(minRow, row);
            maxRow = Math.max(maxRow, row);
        }

        int width = maxCol - minCol + 1;
        int height = maxRow - minRow + 1;

        return Pair.of(width, height);
    }

    private Component parseMarkdownSegment(String text, Style baseStyle) {
        Pattern pattern = Pattern.compile("(\\*\\*|\\*|__|~~|`)(.+?)\\1");
        Matcher matcher = pattern.matcher(text);

        MutableComponent result = Component.empty();
        int last = 0;

        while (matcher.find()) {
            if (matcher.start() > last) {
                String plain = text.substring(last, matcher.start());
                result.append(Component.literal(plain).withStyle(baseStyle));
            }

            String tag = matcher.group(1);
            String content = matcher.group(2);

            Style newStyle = baseStyle;
            switch (tag) {
                case "**" -> newStyle = baseStyle.withBold(true);
                case "*"  -> newStyle = baseStyle.withItalic(true);
                case "__" -> newStyle = baseStyle.withUnderlined(true);
                case "~~" -> newStyle = baseStyle.withStrikethrough(true);
            }

            result.append(parseMarkdownSegment(content, newStyle));
            last = matcher.end();
        }

        if (last < text.length()) {
            String tail = text.substring(last);
            result.append(Component.literal(tail).withStyle(baseStyle));
        }

        return result;
    }

    private String resolveTokensClientSide(String input) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        Matcher m = CLIENT_VAR_TOKEN.matcher(input);

        while (m.find()) {
            String key = m.group(1);
            String withAmp = "&" + key;

            String repl = this.variables.get(key);

            if (repl == null) {
                Matcher sm = CLIENT_STOCK_TOKEN.matcher(withAmp);
                if (sm.matches()) {
                    String itemId = sm.group(1);
                    String powStr = sm.group(2);
                    String baseVal = this.variables.get("s^" + itemId);
                    if (baseVal != null) {
                        try {
                            long amount = Long.parseLong(baseVal);
                            long divisor = 1L;
                            if (powStr != null) {
                                int pow = Integer.parseInt(powStr);
                                if (pow > 0) divisor = (long) Math.pow(10, pow);
                            }
                            long display = Math.round((double) amount / (double) divisor);
                            repl = String.valueOf(display);
                        } catch (NumberFormatException ignored) { }
                    }
                }
            }

            if (repl == null) {
                repl = this.variables.getOrDefault(key, withAmp);
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);

        return evalMathExpressions(sb.toString());
    }


    // ====== RICH TEXT (tekst + ikonki inline) ======

    private interface LineSeg { }
    private record TextSeg(Component c) implements LineSeg { }
    private record ItemIconSeg(ItemStack stack) implements LineSeg { }
    private record FluidIconSeg(FluidStack stack) implements LineSeg { }

    // ====== RENDERABLE LINES (plain lines + markdown tables) ======
    private interface RenderLine {
        float scaleMul();
    }

    private record StyledLine(List<LineSeg> segs, float scaleMul) implements RenderLine { }

    private record TableRow(List<List<LineSeg>> cells) { }

    private record TableBlock(List<TableRow> rows, int indentLevel, int[] align, float scaleMul) implements RenderLine { }

    private record RichTextWithColors(List<RenderLine> lines, @Nullable Integer backgroundColor) { }

    private record DrawEntry(RenderLine line, int tableRowsToDraw) { }

    private static final class BgBox { @Nullable Integer v; }

    private static float headingScaleMul(int level) {
        return switch (level) {
            case 1 -> 1.60f; // # title
            case 2 -> 1.35f; // ## subtitle
            case 3 -> 1.20f; // ### subsubtitle
            case 4 -> 1.10f;
            case 5 -> 1.00f;
            default -> 0.95f;
        };
    }

    @OnlyIn(Dist.CLIENT)
    private @Nullable LineSeg makeIconSeg(String id) {
        try {
            ResourceLocation rl = new ResourceLocation(id);

            var item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != null && item != Items.AIR) {
                return new ItemIconSeg(new ItemStack(item));
            }

            Block block = ForgeRegistries.BLOCKS.getValue(rl);
            if (block != null && block != Blocks.AIR && block.asItem() != Items.AIR) {
                return new ItemIconSeg(new ItemStack(block));
            }

            var fluid = ForgeRegistries.FLUIDS.getValue(rl);
            if (fluid != null && fluid != Fluids.EMPTY) {
                return new FluidIconSeg(new FluidStack(fluid, 1000));
            }

            return null;
        } catch (Throwable t) {
            return null;
        }
    }

    @OnlyIn(Dist.CLIENT)
    private void appendTextAndIcons(String text, Style baseStyle, List<LineSeg> out) {
        if (text == null || text.isEmpty()) return;

        Matcher im = ICON_TOKEN.matcher(text);
        int last = 0;

        while (im.find()) {
            if (im.start() > last) {
                String chunk = text.substring(last, im.start());
                if (!chunk.isEmpty()) out.add(new TextSeg(parseMarkdownSegment(chunk, baseStyle)));
            }

            String id = im.group(1);
            LineSeg seg = makeIconSeg(id);

            if (seg != null) {
                out.add(seg);
            } else {
                out.add(new TextSeg(Component.literal(text.substring(im.start(), im.end())).withStyle(baseStyle)));
            }

            last = im.end();
        }

        if (last < text.length()) {
            String tail = text.substring(last);
            if (!tail.isEmpty()) out.add(new TextSeg(parseMarkdownSegment(tail, baseStyle)));
        }
    }

    // ===== markdown tables helpers =====

    private static int alignFromSepCell(String cell) {
        String t = (cell == null) ? "" : cell.trim();
        boolean left = t.startsWith(":");
        boolean right = t.endsWith(":");

        if (left && right) return 1;
        if (right) return 2;
        if (left) return 0;
        return 1;
    }

    private static int[] parseSepAlign(String sepLine, int cols) {
        int[] out = new int[Math.max(0, cols)];
        Arrays.fill(out, 1); // default center

        List<String> sepCells = splitMdTableCells(sepLine).cells();
        for (int i = 0; i < Math.min(out.length, sepCells.size()); i++) {
            out[i] = alignFromSepCell(sepCells.get(i));
        }
        return out;
    }

    private static boolean isMdTableRowCore(String s) {
        if (s == null) return false;
        String t = s.trim();
        int pipes = 0;
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) == '|') pipes++;
        return pipes >= 2;
    }

    private static boolean isMdTableSepCore(String s) {
        if (s == null) return false;
        String t = s.trim();
        if (t.isEmpty()) return false;

        boolean hasDash = false;
        int pipes = 0;

        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '|') pipes++;
            else if (c == '-') hasDash = true;
            else if (c == ':' || c == ' ' || c == '\t') { /* ok */ }
            else return false;
        }
        return pipes >= 1 && hasDash;
    }

    private static TableCells splitMdTableCells(String line) {
        String t = (line == null) ? "" : line.trim();

        int firstPipe = t.indexOf('|');
        if (firstPipe < 0) {
            return new TableCells("", List.of(t));
        }

        String before = t.substring(0, firstPipe);
        String prefix = "";
        Matcher pm = LEADING_TABLE_PREFIX.matcher(before);
        if (pm.find()) prefix = pm.group(0).trim();

        t = t.substring(firstPipe);

        if (t.startsWith("|")) t = t.substring(1);
        if (t.endsWith("|")) t = t.substring(0, t.length() - 1);

        String[] parts = t.split("\\|", -1);
        List<String> out = new ArrayList<>(parts.length);
        for (String p : parts) out.add(p.trim());

        return new TableCells(prefix, out);
    }


    private static int countIndentMarkers(String s) {
        int ind = 0;
        String t = s == null ? "" : s;
        while (t.startsWith(">>")) {
            ind++;
            t = t.substring(2);
        }
        return ind;
    }

    private static @Nullable String stripIndentMarkers(String s, int indentLevel) {
        String t = s == null ? "" : s;
        for (int i = 0; i < indentLevel; i++) {
            if (!t.startsWith(">>")) return null;
            t = t.substring(2);
        }
        return t.stripLeading();
    }

    @OnlyIn(Dist.CLIENT)
    private void parseInlineWithColors(String raw, Style initialStyle, BgBox bg, List<LineSeg> out) {
        if (raw == null || raw.isEmpty()) return;

        Style currentStyle = initialStyle;

        Matcher colorMatcher = COLOR_TOKEN.matcher(raw);
        int last = 0;

        while (colorMatcher.find()) {
            if (colorMatcher.start() > last) {
                String between = raw.substring(last, colorMatcher.start());
                appendTextAndIcons(between, currentStyle, out);
            }

            String type = colorMatcher.group(1);
            String hex = colorMatcher.group(2);
            int color = Integer.parseInt(hex, 16);

            if (type.equalsIgnoreCase("&c")) {
                currentStyle = currentStyle.withColor(color);
            } else if (type.equalsIgnoreCase("&b")) {
                bg.v = color;
            }

            last = colorMatcher.end();
        }

        if (last < raw.length()) {
            appendTextAndIcons(raw.substring(last), currentStyle, out);
        }
    }

    private record TableParseResult(TableBlock block, int endIndex) { }

    @OnlyIn(Dist.CLIENT)
    private @Nullable TableParseResult tryParseTableBlock(String[] rawLines, int startIdx, BgBox bg) {
        if (startIdx + 1 >= rawLines.length) return null;

        String headerRaw = rawLines[startIdx];
        String sepRaw = rawLines[startIdx + 1];

        int indent = countIndentMarkers(headerRaw);
        int indentSep = countIndentMarkers(sepRaw);
        if (indentSep != indent) return null;

        String header = stripIndentMarkers(headerRaw, indent);
        String sep = stripIndentMarkers(sepRaw, indent);
        if (header == null || sep == null) return null;

        if (!isMdTableRowCore(header) || !isMdTableSepCore(sep)) return null;

        int cols = Math.max(
                splitMdTableCells(header).cells().size(),
                splitMdTableCells(sep).cells().size()
        );

        int[] align = parseSepAlign(sep, cols);

        List<TableRow> rows = new ArrayList<>();
        rows.add(parseOneTableRow(header, bg));

        int end = startIdx + 1;

        for (int i = startIdx + 2; i < rawLines.length; i++) {
            String rowRaw = rawLines[i];
            int indRow = countIndentMarkers(rowRaw);
            if (indRow != indent) break;

            String row = stripIndentMarkers(rowRaw, indent);
            if (row == null || !isMdTableRowCore(row)) break;

            rows.add(parseOneTableRow(row, bg));
            end = i;
        }

        return new TableParseResult(new TableBlock(rows, indent, align, 1.0f), end);
    }

    @OnlyIn(Dist.CLIENT)
    private void drawSolidRect(PoseStack poseStack, MultiBufferSource buffers,
                               int packedLight, int argb,
                               float x0, float y0, float x1, float y1,
                               float z) {

        var buffer = buffers.getBuffer(RenderType.textBackground());
        Matrix4f matrix = poseStack.last().pose();

        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>>  8) & 0xFF) / 255f;
        float b = ((argb       ) & 0xFF) / 255f;

        float u = 0f, v = 0f;

        buffer.vertex(matrix, x0, y1, z).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x1, y1, z).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x1, y0, z).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x0, y0, z).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    private TableRow parseOneTableRow(String rowLine, BgBox bg) {
        TableCells tc = splitMdTableCells(rowLine);

        List<List<LineSeg>> cellSegs = new ArrayList<>(tc.cells().size());
        for (String cell : tc.cells()) {
            List<LineSeg> segs = new ArrayList<>();

            String txt = tc.rowPrefix().isEmpty() ? cell : (tc.rowPrefix() + cell);
            parseInlineWithColors(txt, Style.EMPTY, bg, segs);

            cellSegs.add(segs);
        }
        return new TableRow(cellSegs);
    }


    @OnlyIn(Dist.CLIENT)
    private RichTextWithColors parseStyledTextWithIcons(String rawText) {
        List<RenderLine> lines = new ArrayList<>();
        BgBox bg = new BgBox();

        String[] rawLines = LINE_SPLIT.split(rawText == null ? "" : rawText, -1);

        for (int i = 0; i < rawLines.length; i++) {
            String rawLine0 = rawLines[i];

            // ===== markdown tables =====
            TableParseResult tbl = tryParseTableBlock(rawLines, i, bg);
            if (tbl != null) {
                lines.add(tbl.block());
                i = tbl.endIndex();
                continue;
            }
            // ===========================

            String rawLine = rawLine0;
            List<LineSeg> segs = new ArrayList<>();

            int indentLevel = 0;
            while (rawLine.startsWith(">>")) {
                indentLevel++;
                rawLine = rawLine.substring(2);
            }

            if (indentLevel > 0) {
                String indentVisual = "|>".repeat(indentLevel) + " ";
                Style indentStyle = Style.EMPTY.withColor(0x888888);
                segs.add(new TextSeg(Component.literal(indentVisual).withStyle(indentStyle)));
            }

            if (rawLine.matches("^[*-] .*")) {
                char bulletChar = '•';
                Style bulletStyle = Style.EMPTY.withColor(0xAAAAAA);
                segs.add(new TextSeg(Component.literal(" " + bulletChar + " ").withStyle(bulletStyle)));
                rawLine = rawLine.substring(2);
            }

            // ===== markdown nagłówki (#, ##, ###...) =====
            float lineScaleMul = 1.0f;
            int h = 0;
            while (h < rawLine.length() && rawLine.charAt(h) == '#') h++;
            if (h > 0) {
                int level = Math.min(6, h);
                int cut = h;
                if (cut < rawLine.length() && rawLine.charAt(cut) == ' ') cut++; // opcjonalna spacja po ###
                rawLine = rawLine.substring(cut);
                lineScaleMul = headingScaleMul(level);
            }
            // ============================================

            parseInlineWithColors(rawLine, Style.EMPTY, bg, segs);

            lines.add(new StyledLine(segs, lineScaleMul));
        }

        return new RichTextWithColors(lines, bg.v);
    }

    @OnlyIn(Dist.CLIENT)
    private int iconAdvancePx(Font font) {
        return font.lineHeight + 1;
    }

    @OnlyIn(Dist.CLIENT)
    private int segsWidthPx(Font font, List<LineSeg> segs) {
        int w = 0;
        int iconW = iconAdvancePx(font);
        for (LineSeg s : segs) {
            if (s instanceof TextSeg ts) w += font.width(ts.c());
            else if (s instanceof ItemIconSeg || s instanceof FluidIconSeg) w += iconW;
        }
        return Math.max(1, w);
    }

    @OnlyIn(Dist.CLIENT)
    private float lineWidthPx(Font font, StyledLine line) {
        return segsWidthPx(font, line.segs()) * line.scaleMul();
    }

    // ===== table layout + rendering =====

    private record TableCells(String rowPrefix, List<String> cells) { }

    private static final Pattern LEADING_TABLE_PREFIX =
            Pattern.compile("^\\s*(?:&[cb][0-9A-Fa-f]{6}\\s*)+");

    private record TableLayout(int cols, int[] colContentW, int padPx, int barW, int prefixW, String indentText, int totalW) { }

    @OnlyIn(Dist.CLIENT)
    private TableLayout computeTableLayout(Font font, TableBlock tb) {
        int cols = 0;
        for (TableRow r : tb.rows()) cols = Math.max(cols, r.cells().size());

        int[] colW = new int[Math.max(0, cols)];
        for (TableRow r : tb.rows()) {
            for (int c = 0; c < r.cells().size(); c++) {
                List<LineSeg> cell = r.cells().get(c);
                int w = (cell == null || cell.isEmpty()) ? 0 : segsWidthPx(font, cell);
                colW[c] = Math.max(colW[c], w);
            }
        }

        int pad = 4;
        int barW = font.width("|");

        String indentText = tb.indentLevel() > 0 ? "|>".repeat(tb.indentLevel()) + " " : "";
        int prefixW = indentText.isEmpty() ? 0 : font.width(indentText);

        int sumCols = 0;
        for (int w : colW) sumCols += w;

        int total = prefixW + (barW * (cols + 1)) + sumCols + (pad * 2 * cols);
        return new TableLayout(cols, colW, pad, barW, prefixW, indentText, total);
    }

    @OnlyIn(Dist.CLIENT)
    private float renderLineWidthPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) return lineWidthPx(font, sl);
        if (ln instanceof TableBlock tb) {
            TableLayout layout = computeTableLayout(font, tb);
            return layout.totalW() * tb.scaleMul();
        }
        return 1f;
    }

    @OnlyIn(Dist.CLIENT)
    private float renderLineHeightPx(Font font, RenderLine ln) {
        if (ln instanceof StyledLine sl) return font.lineHeight * sl.scaleMul();
        if (ln instanceof TableBlock tb) return (font.lineHeight * tb.rows().size()) * tb.scaleMul();
        return font.lineHeight;
    }

    @OnlyIn(Dist.CLIENT)
    private void drawTableBlock(Font font, TableBlock tb, int rowsToDraw,
                                PoseStack poseStack, MultiBufferSource buffers, int light) {

        TableLayout layout = computeTableLayout(font, tb);

        Component bar = Component.literal("|").withStyle(Style.EMPTY.withColor(0xAAAAAA));
        @Nullable Component indent = layout.indentText().isEmpty()
                ? null
                : Component.literal(layout.indentText()).withStyle(Style.EMPTY.withColor(0x888888));

        int cols = layout.cols();
        int pad = layout.padPx();
        int barW = layout.barW();
        int[] colW = layout.colContentW();
        float rowH = font.lineHeight;

        int drawRows = Math.min(rowsToDraw, tb.rows().size());
        if (drawRows <= 0) return;

        // ===== poziome linie / ramki =====
        int lineColor = 0x66AAAAAA;
        float z = -0.01f;

        float xLine0 = layout.prefixW();
        float xLine1 = layout.totalW();

        drawSolidRect(poseStack, buffers, light, lineColor, xLine0, -1f, xLine1, 0f, z);

        drawSolidRect(poseStack, buffers, light, lineColor,
                xLine0, drawRows * rowH - 1f, xLine1, drawRows * rowH, z);

        if (drawRows > 1) {
            drawSolidRect(poseStack, buffers, light, lineColor,
                    xLine0, rowH - 1f, xLine1, rowH, z);
        }

        for (int r = 0; r < drawRows; r++) {
            TableRow row = tb.rows().get(r);
            float y = r * rowH;

            float x = 0f;

            if (indent != null) {
                font.drawInBatch(indent, 0f, y, 0xFFFFFF, false, poseStack.last().pose(), buffers,
                        Font.DisplayMode.NORMAL, 0, light);
                x += layout.prefixW();
            }

            // leading bar
            font.drawInBatch(bar, x, y, 0xFFFFFF, false, poseStack.last().pose(), buffers,
                    Font.DisplayMode.NORMAL, 0, light);
            x += barW;

            for (int c = 0; c < cols; c++) {
                List<LineSeg> cell = (c < row.cells().size()) ? row.cells().get(c) : List.of();

                int cellW = (cell == null || cell.isEmpty()) ? 0 : segsWidthPx(font, cell);
                int innerW = colW[c];

                int a = (tb.align() != null && c < tb.align().length) ? tb.align()[c] : 1;

                float contentX;
                if (a == 0) {
                    // LEFT
                    contentX = x + pad;
                } else if (a == 2) {
                    // RIGHT
                    contentX = x + pad + Math.max(0f, (innerW - cellW));
                } else {
                    // CENTER
                    contentX = x + pad + Math.max(0f, (innerW - cellW) / 2f);
                }

                drawLineSegments(font, cell, poseStack, buffers, light, contentX, y);

                x += innerW + pad * 2;

                // trailing bar
                font.drawInBatch(bar, x, y, 0xFFFFFF, false, poseStack.last().pose(), buffers,
                        Font.DisplayMode.NORMAL, 0, light);
                x += barW;
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private net.minecraft.client.renderer.texture.TextureAtlasSprite getFluidSprite(FluidStack fs) {
        var ext = IClientFluidTypeExtensions.of(fs.getFluid());
        ResourceLocation still = ext.getStillTexture();
        return Minecraft.getInstance().getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(still);
    }

    @OnlyIn(Dist.CLIENT)
    private int getFluidTint(FluidStack fs) {
        var ext = IClientFluidTypeExtensions.of(fs.getFluid());
        return ext.getTintColor();
    }

    @OnlyIn(Dist.CLIENT)
    private void renderItemLikeInventoryFlattened(ItemStack stack, PoseStack poseStack, MultiBufferSource buffers,
                                                  int light, float x, float y, int iconPx) {
        var mc = Minecraft.getInstance();
        var itemRenderer = mc.getItemRenderer();

        poseStack.pushPose();

        poseStack.translate(x, y, 0.001f);
        poseStack.translate(iconPx / 2f, iconPx / 2f, 0f);

        // korekta na to, że parent robi scale(scale, -scale, scale)
        poseStack.scale(1f, -1f, 1f);

        float zFlatten = 1f / 256f;
        poseStack.scale(iconPx, iconPx, iconPx * zFlatten);

        RenderSystem.disableCull();

        int guiLight = 0xF000F0; // jak inventory

        itemRenderer.renderStatic(
                stack,
                ItemDisplayContext.GUI,
                guiLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffers,
                mc.level,
                0
        );

        RenderSystem.enableCull();
        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawSpriteQuad(PoseStack poseStack, MultiBufferSource buffers,
                                int packedLight, int argb,
                                float x, float y, float sizePx,
                                net.minecraft.client.renderer.texture.TextureAtlasSprite sprite) {

        var buffer = buffers.getBuffer(RenderType.text(InventoryMenu.BLOCK_ATLAS));
        Matrix4f matrix = poseStack.last().pose();

        float a = ((argb >>> 24) & 0xFF) / 255f;
        float r = ((argb >>> 16) & 0xFF) / 255f;
        float g = ((argb >>>  8) & 0xFF) / 255f;
        float b = ((argb       ) & 0xFF) / 255f;

        float x0 = x;
        float y0 = y;
        float x1 = x + sizePx;
        float y1 = y + sizePx;
        float z = 0.001f;

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // dwustronnie
        buffer.vertex(matrix, x0, y0, z).color(r, g, b, a).uv(u0, v0).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x1, y0, z).color(r, g, b, a).uv(u1, v0).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x1, y1, z).color(r, g, b, a).uv(u1, v1).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x0, y1, z).color(r, g, b, a).uv(u0, v1).uv2(packedLight).endVertex();

        buffer.vertex(matrix, x0, y0, z).color(r, g, b, a).uv(u0, v0).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x0, y1, z).color(r, g, b, a).uv(u0, v1).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x1, y1, z).color(r, g, b, a).uv(u1, v1).uv2(packedLight).endVertex();
        buffer.vertex(matrix, x1, y0, z).color(r, g, b, a).uv(u1, v0).uv2(packedLight).endVertex();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawLineSegments(Font font, List<LineSeg> segs, PoseStack poseStack, MultiBufferSource buffers,
                                  int light, float x, float y) {

        float cursor = x;
        float iconSize = font.lineHeight;
        int iconAdv = iconAdvancePx(font);

        for (LineSeg s : segs) {
            if (s instanceof TextSeg ts) {
                Component c = ts.c();
                font.drawInBatch(c, cursor, y, 0xFFFFFF, false,
                        poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);
                cursor += font.width(c);
            } else if (s instanceof ItemIconSeg is) {
                renderItemLikeInventoryFlattened(is.stack(), poseStack, buffers, light, cursor, y, (int) iconSize);
                cursor += iconAdv;
            } else if (s instanceof FluidIconSeg fs) {
                FluidStack st = fs.stack();
                var sprite = getFluidSprite(st);
                int tint = getFluidTint(st);
                drawSpriteQuad(poseStack, buffers, light, tint, cursor, y, iconSize, sprite);
                cursor += iconAdv;
            }
        }
    }

    // ====== RENDER ======

    @OnlyIn(Dist.CLIENT)
    @Override
    public void renderDynamic(float partialTicks, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        if (!isPowered() || textValue.isEmpty()) return;

        Set<DisplayPart> group = getConnectedGrid();
        if (group.isEmpty() || !isRenderOrigin(group)) return;

        List<DisplayPart> sorted = new ArrayList<>(group);
        sorted.sort(Comparator.comparingInt((DisplayPart dp) -> dp.getBlockEntity().getBlockPos().getY())
                .thenComparingInt(dp -> dp.getBlockEntity().getBlockPos().getX()));

        var dims = getGridSize(sorted, getSide());
        int widthBlocks = dims.getFirst();
        int heightBlocks = dims.getSecond();

        Font font = Minecraft.getInstance().font;

        String resolved = resolveTokensClientSide(this.textValue);

        RichTextWithColors parsed = parseStyledTextWithIcons(resolved);
        List<RenderLine> renderLines = parsed.lines();
        Integer bgColor = parsed.backgroundColor();

        float maxLineWidth = 1f;
        float totalTextHeight = 0f;
        for (RenderLine ln : renderLines) {
            maxLineWidth = Math.max(maxLineWidth, renderLineWidthPx(font, ln));
            totalTextHeight += renderLineHeightPx(font, ln);
        }

        float pxW = 64f * widthBlocks;
        float pxH = 64f * heightBlocks;

        float marginFrac = this.margin ? 0.03f : 0.0f;
        float marginX = pxW * marginFrac, marginY = pxH * marginFrac;
        float usableW = pxW - 2f * marginX, usableH = pxH - 2f * marginY;

        float scale;
        if (fontSize <= 0) {
            float fitX = usableW / Math.max(1f, maxLineWidth);
            float fitY = usableH / Math.max(1f, totalTextHeight);
            scale = (1f / 64f) * Math.min(fitX, fitY);
        } else {
            scale = fontSize / (64f * 8f);
        }

        // tło
        poseStack.pushPose();
        applyFacingTransform(poseStack);
        poseStack.translate(0, 0, 0.51f);
        if (bgColor != null) {
            drawBackground(poseStack, buffers, widthBlocks, heightBlocks, light, 0xFF000000 | bgColor);
        }
        poseStack.popPose();

        // tekst + ikonki
        poseStack.pushPose();
        applyFacingTransform(poseStack);
        poseStack.translate(0, 0, 0.52f);

        poseStack.translate(marginX / 64f, -marginY / 64f, 0);
        poseStack.scale(scale, -scale, scale);

        float availTextW = (usableW / 64f) / scale;
        float availTextH = (usableH / 64f) / scale;

        // plan rysowania (tabela może się uciąć do N wierszy)
        List<DrawEntry> drawPlan = new ArrayList<>();

        if (fontSize <= 0) {
            for (RenderLine ln : renderLines) {
                int rows = (ln instanceof TableBlock tb) ? tb.rows().size() : 1;
                drawPlan.add(new DrawEntry(ln, rows));
            }
        } else {
            float remainingH = availTextH;

            for (RenderLine ln : renderLines) {
                if (remainingH <= 0f) break;

                if (ln instanceof StyledLine sl) {
                    float lh = font.lineHeight * sl.scaleMul();
                    if (lh > remainingH) break;
                    drawPlan.add(new DrawEntry(ln, 1));
                    remainingH -= lh;
                } else if (ln instanceof TableBlock tb) {
                    float rowH = font.lineHeight * tb.scaleMul();
                    int rowsFit = (int) Math.floor(remainingH / rowH);
                    rowsFit = Math.min(rowsFit, tb.rows().size());
                    if (rowsFit <= 0) break;
                    drawPlan.add(new DrawEntry(ln, rowsFit));
                    remainingH -= rowsFit * rowH;
                }
            }
        }

        // centrowanie pionowe całości
        if (this.center) {
            float drawnH = 0f;
            for (DrawEntry de : drawPlan) {
                if (de.line() instanceof StyledLine sl) {
                    drawnH += font.lineHeight * sl.scaleMul();
                } else if (de.line() instanceof TableBlock tb) {
                    drawnH += de.tableRowsToDraw() * font.lineHeight * tb.scaleMul();
                }
            }
            float extraY = Math.max(0f, (availTextH - drawnH) / 2f);
            poseStack.translate(0, +extraY, 0);
        }

        float yCursor = 0f;

        for (DrawEntry de : drawPlan) {
            RenderLine ln = de.line();

            if (ln instanceof StyledLine sl) {
                float lineScale = sl.scaleMul();
                float lineW = renderLineWidthPx(font, sl);

                float xOffset = 0f;
                if (this.center) xOffset = Math.max(0f, (availTextW - lineW) / 2f);

                poseStack.pushPose();
                poseStack.translate(xOffset, yCursor, 0);
                poseStack.scale(lineScale, lineScale, 1f);
                drawLineSegments(font, sl.segs(), poseStack, buffers, light, 0f, 0f);
                poseStack.popPose();

                yCursor += font.lineHeight * lineScale;
            } else if (ln instanceof TableBlock tb) {
                float blockW = renderLineWidthPx(font, tb);

                float xOffset = 0f;
                if (this.center) xOffset = Math.max(0f, (availTextW - blockW) / 2f);

                poseStack.pushPose();
                poseStack.translate(xOffset, yCursor, 0);
                poseStack.scale(tb.scaleMul(), tb.scaleMul(), 1f);
                drawTableBlock(font, tb, de.tableRowsToDraw(), poseStack, buffers, light);
                poseStack.popPose();

                yCursor += de.tableRowsToDraw() * font.lineHeight * tb.scaleMul();
            }
        }

        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawBackground(PoseStack poseStack, MultiBufferSource buffers,
                                int blocksWide, int blocksHigh, int packedLight, int color) {

        var buffer = buffers.getBuffer(RenderType.textBackground());
        Matrix4f matrix = poseStack.last().pose();

        float a = ((color >>> 24) & 0xFF) / 255f;
        float r = ((color >>> 16) & 0xFF) / 255f;
        float g = ((color >>>  8) & 0xFF) / 255f;
        float b = ((color       ) & 0xFF) / 255f;

        float u = 0f, v = 0f;

        buffer.vertex(matrix, 0f, -(float) blocksHigh, 0f).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
        buffer.vertex(matrix, (float) blocksWide, -(float) blocksHigh, 0f).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
        buffer.vertex(matrix, (float) blocksWide, 0f, 0f).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
        buffer.vertex(matrix, 0f, 0f, 0f).color(r, g, b, a).uv(u, v).uv2(packedLight).endVertex();
    }

    // ====== MATH EXPRESSIONS: &(...) ======

    private static String formatMathResult(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "ERR";
        BigDecimal bd = BigDecimal.valueOf(v).stripTrailingZeros();
        if (bd.compareTo(BigDecimal.ZERO) == 0) return "0";
        return bd.toPlainString();
    }


    private static String evalMathExpressions(String s) {
        if (s == null || s.isEmpty() || s.indexOf("&(") < 0) return s;

        StringBuilder out = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); ) {
            if (s.charAt(i) == '&' && i + 1 < s.length() && s.charAt(i + 1) == '(') {
                int start = i + 2;
                int depth = 0;
                int j = start;
                boolean found = false;

                for (; j < s.length(); j++) {
                    char c = s.charAt(j);
                    if (c == '(') depth++;
                    else if (c == ')') {
                        if (depth == 0) { found = true; break; }
                        depth--;
                    }
                }

                if (!found) {
                    out.append(s.charAt(i));
                    i++;
                    continue;
                }

                String inner = s.substring(start, j);
                inner = evalMathExpressions(inner);

                String repl;
                try {
                    double val = MathParser.parse(inner);
                    repl = formatMathResult(val);
                } catch (Throwable t) {
                    repl = "ERR";
                }

                out.append(repl);
                i = j + 1;
            } else {
                out.append(s.charAt(i));
                i++;
            }
        }
        return out.toString();
    }
}
