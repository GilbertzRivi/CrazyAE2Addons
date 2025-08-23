package net.oktawia.crazyae2addons.parts;

import java.security.SecureRandom;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.AEBasePart;
import appeng.parts.automation.PlaneModels;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.entities.MEDataControllerBE;
import net.oktawia.crazyae2addons.interfaces.VariableMachine;
import net.oktawia.crazyae2addons.menus.DisplayMenu;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.DisplayValuePacket;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.minecraft.world.entity.player.Player;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.parts.IPartCollisionHelper;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.util.AECableType;
import appeng.items.parts.PartModels;
import org.joml.Matrix4f;

public class DisplayPart extends AEBasePart implements MenuProvider, IGridTickable, VariableMachine {

    private static final PlaneModels MODELS = new PlaneModels("part/display_mon_off",
            "part/display_mon_on");
    private static final Pattern CLIENT_VAR_TOKEN =
            Pattern.compile("&(s\\^[a-z0-9_\\.:]+(?:%\\d+)?|[A-Za-z0-9_]+)");
    private static final Pattern CLIENT_STOCK_TOKEN =
            Pattern.compile("&s\\^([a-z0-9_\\.:]+)(?:%(\\d+))?");
    private static final Pattern CLIENT_ICON_TOKEN =
            Pattern.compile("(?i)&ii?\\^([a-z0-9_\\.:]+)");

    public byte spin = 0; // 0-3
    public String textValue = "";
    public HashMap<String, String> variables = new HashMap<>();
    public boolean reRegister = true;
    public String identifier = randomHexId();
    public boolean mode = true;
    public int fontSize;

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
    public String getId() {
        return this.identifier;
    }

    @Override
    public void notifyVariable(String name, String value, MEDataControllerBE db) {
        this.variables.put(name, value);
        if (!getLevel().isClientSide()) {
            String packed;
            if (this.getGridNode() != null && !this.getGridNode().getGrid().getMachines(MEDataControllerBE.class).isEmpty()){
                packed = this.variables.entrySet().stream()
                        .map(e -> e.getKey() + "=" + e.getValue())
                        .collect(java.util.stream.Collectors.joining("|"));
            } else {
                packed = "";
            }
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new DisplayValuePacket(this.getBlockEntity().getBlockPos(), this.textValue, this.getSide(), this.spin, packed, this.fontSize, this.mode));
        }
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
        if(extra.contains("textvalue")){
            this.textValue = extra.getString("textvalue");
        }
        if(extra.contains("spin")){
            this.spin = extra.getByte("spin");
        }
        if(extra.contains("ident")){
            this.identifier = extra.getString("ident");
        }
        if(extra.contains("mode")){
            this.mode = extra.getBoolean("mode");
        }
        if(extra.contains("font")){
            this.fontSize = extra.getInt("font");
        }
        if(!isClientSide()){
            String packed = this.variables.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(java.util.stream.Collectors.joining("|"));
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new DisplayValuePacket(this.getBlockEntity().getBlockPos(), this.textValue, this.getSide(), this.spin, packed, fontSize, mode));
        }
    }



    @Override
    public void writeToNBT(CompoundTag extra) {
        super.writeToNBT(extra);
        extra.putString("textvalue", this.textValue);
        extra.putByte("spin", this.spin);
        extra.putString("ident", this.identifier);
        extra.putBoolean("mode", this.mode);
        extra.putInt("font", this.fontSize);
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

    private void recomputeStockVariablesAndNotify() {
        if (this.getLevel() == null || this.getLevel().isClientSide()) return;

        String txt = this.textValue == null ? "" : this.textValue;
        Pattern p = Pattern.compile("&(s\\^[\\w:]+(?:%\\d+)?)");
        Matcher m = p.matcher(txt);

        int seen = 0;

        while (m.find()) {
            String token = m.group(1);
            String core = token;
            long divisor = 1L;

            int pct = token.indexOf('%');
            if (pct >= 0) {
                core = token.substring(0, pct);
                try {
                    int pow = Integer.parseInt(token.substring(pct + 1));
                    if (pow > 0) divisor = (long) Math.pow(10, pow);
                } catch (NumberFormatException ignored) { divisor = 1L; }
            }

            if (!core.startsWith("s^")) continue;
            seen++;

            String itemId = core.substring(2);
            long amount = getItemAmountInME(itemId);
            long display = Math.round((double) amount / (double) divisor);

            String old = this.variables.get(token);
            String now = String.valueOf(display);
            if (!Objects.equals(old, now)) {
                this.variables.put(token, now);
            }
        }

        if (seen > 0) {
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
                            this.mode
                    )
            );
        }
    }


    private long getItemAmountInME(String id) {
        try {
            var node = this.getGridNode();
            if (node == null) return 0;
            var grid = node.getGrid();
            if (grid == null) return 0;

            var storage = grid.getService(appeng.api.networking.storage.IStorageService.class);
            if (storage == null) return 0;

            var rl = new ResourceLocation(id);

            var item = ForgeRegistries.ITEMS.getValue(rl);
            if (item != Items.AIR) {
                var itemKey = AEItemKey.of(new ItemStack(item));
                if (itemKey == null) return 0;

                long total = 0L;
                var avail = storage.getInventory().getAvailableStacks();
                for (var gs : avail) {
                    if (gs.getKey().equals(itemKey)) {
                        total += gs.getLongValue();
                    }
                }
                if (total > 0) return total;
            }

            var fluid = ForgeRegistries.FLUIDS.getValue(rl);
            if (fluid != null) {
                var fluidKey = AEFluidKey.of(new FluidStack(fluid, 1));
                if (fluidKey == null) return 0;

                long total = 0L;
                var avail = storage.getInventory().getAvailableStacks();
                for (var gs : avail) {
                    if (gs.getKey().equals(fluidKey)) {
                        total += gs.getLongValue();
                    }
                }
                return total;
            }

            return 0;
        } catch (Throwable t) {
            return 0;
        }
    }



    public void updateController(String value) {
        this.textValue = value;

        try {
            var node = this.getGridNode();
            if (node == null) {
                this.reRegister = true;
                return;
            }
            var grid = node.getGrid();
            if (grid == null) {
                this.reRegister = true;
                return;
            }

            var machines = grid.getMachines(MEDataControllerBE.class);
            if (machines == null || machines.isEmpty()) {
                this.reRegister = true;
                return;
            }
            var controller = machines.stream().findFirst().orElse(null);
            if (controller == null) {
                this.reRegister = true;
                return;
            }

            int maxVars = controller.getMaxVariables();
            if (maxVars <= 0) {
                this.reRegister = true;
                return;
            }

            controller.removeNotification(this.identifier);

            Pattern pattern = Pattern.compile("&\\w+");
            Matcher matcher = pattern.matcher(value);
            while (matcher.find()) {
                String word = matcher.group();
                String name = word.substring(1);
                controller.registerNotification(this.identifier, name, this.identifier, this.getClass());
            }

            this.reRegister = false;

            if (!isClientSide()) {
                recomputeStockVariablesAndNotify();
            }
        } catch (Throwable t) {
            this.reRegister = true;
        }
    }

    private record Transformation(float tx, float ty, float tz, float yRotation, float xRotation) {
    }

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
                case NORTH, SOUTH -> {
                    col = pos.getX();
                    row = pos.getY();
                }
                case EAST, WEST -> {
                    col = pos.getZ();
                    row = pos.getY();
                }
                case UP, DOWN -> {
                    col = pos.getX();
                    row = pos.getZ();
                }
            }

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
            case SOUTH -> { tx = 0; ty = 1; tz = 0.5F; }
            case WEST  -> { tx = 0.5F; ty = 1; tz = 0; yRot = -90F; }
            case EAST  -> { tx = 0.5F; ty = 1; tz = 1; yRot = 90F; }
            case NORTH -> { tx = 1; ty = 1; tz = 0.5F; yRot = 180F; }
            case UP    -> { tx = 0; ty = 0.5F; tz = 0; xRot = -90F; }
            case DOWN  -> { tx = 1; ty = 0.5F; tz = 0; xRot = 90F; }
        }
        return new Transformation(tx, ty, tz, yRot, xRot);
    }

    private void applySpinTransformation(PoseStack poseStack, float upRotation) {
        float theSpin = 0.0F;
        if (upRotation == 90F) {
            switch (this.spin) {
                case 0 -> { theSpin = 0.0F; poseStack.translate(-1, 1, 0); }
                case 1 -> { theSpin = 90.0F; poseStack.translate(-1, 0, 0); }
                case 2 -> { theSpin = 180.0F; poseStack.translate(0, 0, 0); }
                case 3 -> { theSpin = -90.0F; poseStack.translate(0, 1, 0); }
            }
        } else {
            switch (this.spin) {
                case 0 -> { theSpin = 0.0F; poseStack.translate(0, 0, 0); }
                case 1 -> { theSpin = -90.0F; poseStack.translate(1, 0, 0); }
                case 2 -> { theSpin = 180.0F; poseStack.translate(1, -1, 0); }
                case 3 -> { theSpin = 90.0F; poseStack.translate(0, -1, 0); }
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

        if (isStructureComplete(result)){
            return result;
        } else {
            return Set.of(this);
        }
    }

    @Nullable
    public DisplayPart getNeighbor(Direction dir) {
        BlockPos neighborPos = getBlockEntity().getBlockPos().relative(dir);
        BlockEntity be = getLevel().getBlockEntity(neighborPos);
        if (be instanceof CableBusBlockEntity cbbe &&
                cbbe.getPart(getSide()) instanceof DisplayPart neighbor &&
                neighbor.getSide() == this.getSide() &&
                neighbor.isPowered()) {
            return neighbor;
        }
        return null;
    }

    public boolean isRenderOrigin(Set<DisplayPart> group) {
        return this == group.stream().toList().get(group.size()-1);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.getGridNode() == null || this.getGridNode().getGrid() == null || this.getGridNode().getGrid().getMachines(MEDataControllerBE.class).isEmpty()){
            this.reRegister = true;
        } else {
            MEDataControllerBE controller = getMainNode().getGrid().getMachines(MEDataControllerBE.class).stream().toList().get(0);
            if (controller.getMaxVariables() <= 0){
                this.reRegister = true;
            } else if (this.reRegister){
                this.reRegister = false;
                updateController(this.textValue);
            }
        }

        if(!isClientSide()){
            recomputeStockVariablesAndNotify();
            String packed = this.variables.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("|"));
            NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                    new DisplayValuePacket(this.getBlockEntity().getBlockPos(), this.textValue, this.getSide(), this.spin, packed, this.fontSize, this.mode));
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
                case NORTH, SOUTH -> {
                    col = pos.getX();
                    row = pos.getY();
                }
                case EAST, WEST -> {
                    col = pos.getZ();
                    row = pos.getY();
                }
                case UP, DOWN -> {
                    col = pos.getX();
                    row = pos.getZ();
                }
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

    private record IconSpan(int columnIndex, String itemId) {}
    private record TokenizedLine(String textWithoutIcons, List<IconSpan> icons) {}

    private TokenizedLine stripIconTokens(String rawLine) {
        List<IconSpan> icons = new ArrayList<>();
        StringBuilder out = new StringBuilder();

        Matcher m = CLIENT_ICON_TOKEN.matcher(rawLine);
        int last = 0;
        while (m.find()) {
            out.append(rawLine, last, m.start());
            int column = out.length();

            String itemId = m.group(1);
            icons.add(new IconSpan(column, itemId));

            out.append(' ');
            last = m.end();
        }
        if (last < rawLine.length()) {
            out.append(rawLine.substring(last));
        }
        return new TokenizedLine(out.toString(), icons);
    }

    private String replaceStockTokensForMeasure(String line) {
        if (line == null || line.isEmpty()) return "";
        StringBuffer out = new StringBuffer();
        Matcher m = CLIENT_STOCK_TOKEN.matcher(line);
        while (m.find()) {
            String itemId = m.group(1);
            String powStr = m.group(2);
            String baseVal = this.variables.get("s^" + itemId);

            String repl = "0";
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
                } catch (NumberFormatException ignored) {}
            }

            m.appendReplacement(out, Matcher.quoteReplacement(repl));
        }
        m.appendTail(out);
        return out.toString();
    }


    private String resolveTokensClientSide(String input) {
        if (input == null || input.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        Matcher m = CLIENT_VAR_TOKEN.matcher(input);
        while (m.find()) {
            String key = m.group(1);         // np. s^minecraft:oak_log%1  |  foo
            String withAmp = "&" + key;

            String repl = this.variables.get(key); // dokładne trafienie
            if (repl == null) {
                Matcher sm = CLIENT_STOCK_TOKEN.matcher(withAmp);
                if (sm.matches()) {
                    String itemId = sm.group(1);                   // minecraft:oak_log
                    String powStr = sm.group(2);                   // N lub null
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
                        } catch (NumberFormatException ignored) {
                        }
                    }
                }
            }

            if (repl == null) {
                repl = this.variables.getOrDefault(key, withAmp);
            }

            m.appendReplacement(sb, Matcher.quoteReplacement(repl));
        }
        m.appendTail(sb);
        return sb.toString();
    }


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

        String[] rawLines = resolved.split("&nl");
        List<TokenizedLine> tokenized = new ArrayList<>(rawLines.length);
        for (String rl : rawLines) {
            tokenized.add(stripIconTokens(rl));
        }

        StringBuilder withoutIconsAll = new StringBuilder();
        for (int i = 0; i < tokenized.size(); i++) {
            if (i > 0) withoutIconsAll.append("&nl");
            withoutIconsAll.append(tokenized.get(i).textWithoutIcons());
        }
        TextWithColors parsed = parseStyledText(withoutIconsAll.toString());
        List<Component> styledLines = parsed.lines();
        Integer bgColor = parsed.backgroundColor();

        int maxLineWidth = 1;
        for (int i = 0; i < tokenized.size(); i++) {
            TokenizedLine lineTok = tokenized.get(i);
            String measuredText = replaceStockTokensForMeasure(lineTok.textWithoutIcons());

            int widthPx = font.width(measuredText) + lineTok.icons().size() * font.lineHeight;
            if (i < parsed.lines().size()) {
                widthPx = Math.max(widthPx,
                        font.width(parsed.lines().get(i)) + lineTok.icons().size() * font.lineHeight);
            }
            maxLineWidth = Math.max(maxLineWidth, widthPx);
        }
        int totalTextHeight = styledLines.size() * font.lineHeight;

        float scale;
        if (fontSize <= 0) {
            float pxW = 64f * widthBlocks;
            float pxH = 64f * heightBlocks;
            float fitX = pxW / Math.max(1, maxLineWidth);
            float fitY = pxH / Math.max(1, totalTextHeight);
            scale = (1f / 64f) * Math.min(fitX, fitY);
        } else {
            scale = fontSize / (64f * 8f);
        }

        poseStack.pushPose();
        applyFacingTransform(poseStack);
        poseStack.translate(0, 0, 0.51f);
        if (bgColor != null) {
            drawBackground(poseStack, buffers, widthBlocks, heightBlocks, 0xFF000000 | bgColor);
        }
        poseStack.popPose();

        poseStack.pushPose();
        applyFacingTransform(poseStack);
        poseStack.translate(0, 0, 0.52f);
        poseStack.scale(scale, -scale, scale);

        for (int i = 0; i < styledLines.size(); i++) {
            if (i >= tokenized.size()) break;

            TokenizedLine lineTok = tokenized.get(i);
            Component styled = styledLines.get(i);

            float y = i * font.lineHeight;

            font.drawInBatch(styled, 0, y, 0xFFFFFF, false,
                    poseStack.last().pose(), buffers, Font.DisplayMode.NORMAL, 0, light);

            if (!lineTok.icons().isEmpty()) {
                String printable = lineTok.textWithoutIcons();
                for (IconSpan is : lineTok.icons()) {
                    int col = Math.min(is.columnIndex(), printable.length());
                    int xBefore = font.width(printable.substring(0, col));
                    int charH = font.lineHeight;
                    drawInlineIcon(poseStack, buffers, (float) xBefore, y, charH, charH, is.itemId(), overlay);
                }
            }
        }

        poseStack.popPose();
    }

    @OnlyIn(Dist.CLIENT)
    private void drawInlineIcon(PoseStack poseStack, MultiBufferSource buffers, float x, float y, int w, int h, String itemId, int overlay) {
        var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemId));
        if (item == null || item == Items.AIR) return;

        ItemStack stack = new ItemStack(item);

        poseStack.pushPose();
        poseStack.translate(x + w * 0.5f, y + h * 0.5f, 0.0f);
        poseStack.scale(w * 0.9f, -h * 0.9f, 0.1f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                net.minecraft.world.item.ItemDisplayContext.GUI,
                0xF000F0,
                overlay,
                poseStack,
                buffers,
                Minecraft.getInstance().level,
                0
        );

        poseStack.popPose();
    }



    @OnlyIn(Dist.CLIENT)
    private void drawBackground(PoseStack poseStack, MultiBufferSource buffers, int blocksWide, int blocksHigh, int color) {
        var buffer = buffers.getBuffer(net.minecraft.client.renderer.RenderType.gui());
        Matrix4f matrix = poseStack.last().pose();

        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        float w = blocksWide;
        float h = blocksHigh;

        buffer.vertex(matrix, 0,   -h, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, w,   -h, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, w,    0, 0).color(r, g, b, a).endVertex();
        buffer.vertex(matrix, 0,    0, 0).color(r, g, b, a).endVertex();
    }

    private record TextWithColors(List<Component> lines, @Nullable Integer backgroundColor) {}

    private TextWithColors parseStyledText(String rawText) {
        List<Component> lines = new ArrayList<>();
        Integer bgColor = null;

        String[] rawLines = rawText.split("&nl");
        Pattern colorPattern = Pattern.compile("(&[cb])([0-9A-Fa-f]{6})");

        for (String rawLine : rawLines) {
            MutableComponent lineComponent = Component.empty();
            Style currentStyle = Style.EMPTY;

            int indentLevel = 0;
            while (rawLine.startsWith(">>")) {
                indentLevel++;
                rawLine = rawLine.substring(2);
            }

            if (indentLevel > 0) {
                String indentVisual = "|>".repeat(indentLevel) + " ";
                Style indentStyle = Style.EMPTY.withColor(0x888888);
                lineComponent.append(Component.literal(indentVisual).withStyle(indentStyle));
            }

            if (rawLine.matches("^[*-] .*")) {
                char bulletChar = '•';
                Style bulletStyle = Style.EMPTY.withColor(0xAAAAAA);
                lineComponent.append(Component.literal(" " + bulletChar + " ").withStyle(bulletStyle));
                rawLine = rawLine.substring(2); // usuń "* " lub "- "
            }

            Matcher colorMatcher = colorPattern.matcher(rawLine);
            int lastColorEnd = 0;

            while (colorMatcher.find()) {
                if (colorMatcher.start() > lastColorEnd) {
                    String between = rawLine.substring(lastColorEnd, colorMatcher.start());
                    lineComponent.append(parseMarkdownSegment(between, currentStyle));
                }

                String type = colorMatcher.group(1);
                String hex = colorMatcher.group(2);
                int color = Integer.parseInt(hex, 16);

                if (type.equals("&c")) {
                    currentStyle = currentStyle.withColor(color);
                } else if (type.equals("&b")) {
                    bgColor = color;
                }

                lastColorEnd = colorMatcher.end();
            }

            if (lastColorEnd < rawLine.length()) {
                String tail = rawLine.substring(lastColorEnd);
                lineComponent.append(parseMarkdownSegment(tail, currentStyle));
            }

            lines.add(lineComponent);
        }

        return new TextWithColors(lines, bgColor);
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

}

