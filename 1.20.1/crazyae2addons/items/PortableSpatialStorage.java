package net.oktawia.crazyae2addons.items;

import appeng.api.config.FuzzyMode;
import appeng.api.implementations.menuobjects.IMenuItem;
import appeng.api.implementations.menuobjects.ItemMenuHost;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.ISubMenuHost;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.definitions.AEItems;
import appeng.core.localization.Tooltips;
import appeng.items.tools.powered.WirelessTerminalItem;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.BuildScheduler;
import net.oktawia.crazyae2addons.logic.GadgetHost;
import net.oktawia.crazyae2addons.misc.TemplateUtil;
import net.oktawia.crazyae2addons.recipes.StructureSnapshot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class PortableSpatialStorage extends WirelessTerminalItem implements IMenuItem, IUpgradeableObject, ISubMenuHost {

    private BlockPos cornerA = null;
    private BlockPos cornerB = null;
    private BlockPos origin = null;
    private Direction originFacing = Direction.NORTH;


    public PortableSpatialStorage(Properties props) {
        super(() -> 200000, props.stacksTo(1));
    }

    public static boolean hasStoredStructure(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return false;
        CompoundTag tag = stack.getTag();
        if (tag == null) return false;
        if (!tag.getBoolean("code")) return false;
        if (!tag.contains("program_id")) return false;
        String id = tag.getString("program_id");
        return !id.isEmpty();
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines,
                                TooltipFlag advancedTooltips) {
        final CompoundTag tag = stack.getTag();
        double internalCurrentPower = 0;
        final double internalMaxPower = this.getAEMaxPower(stack);

        if (tag != null) {
            internalCurrentPower = tag.getDouble("internalCurrentPower");
        }

        lines.add(Tooltips.energyStorageComponent(internalCurrentPower, internalMaxPower));
    }

    public static @Nullable StructureSnapshot loadSnapshot(ItemStack stack, @Nullable Level level) {
        if (stack == null || stack.isEmpty() || !stack.hasTag()) return null;
        CompoundTag tag = stack.getTag();
        if (tag == null) return null;

        if (!tag.contains("preview_palette") || !tag.contains("preview_positions") || !tag.contains("preview_indices"))
            return null;

        var palList = tag.getList("preview_palette", Tag.TAG_STRING);
        if (palList.isEmpty()) return null;

        List<BlockState> palette = new ArrayList<>(palList.size());
        for (int i = 0; i < palList.size(); i++) {
            String spec = palList.getString(i);
            BlockState st = parseBlockStateSpecForSnapshot(spec);
            if (st == null) return null;
            palette.add(st);
        }

        int[] posArr = tag.getIntArray("preview_positions");
        int[] idxArr = tag.getIntArray("preview_indices");
        if (posArr.length == 0 || posArr.length % 3 != 0) return null;
        int blocksN = posArr.length / 3;
        if (idxArr.length != blocksN) return null;

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (int i = 0; i < blocksN; i++) {
            int x = posArr[i * 3];
            int y = posArr[i * 3 + 1];
            int z = posArr[i * 3 + 2];
            if (x < minX) minX = x; if (x > maxX) maxX = x;
            if (y < minY) minY = y; if (y > maxY) maxY = y;
            if (z < minZ) minZ = z; if (z > maxZ) maxZ = z;
        }

        int sizeX = (maxX - minX) + 1;
        int sizeY = (maxY - minY) + 1;
        int sizeZ = (maxZ - minZ) + 1;

        Map<BlockPos, BlockState> map = new HashMap<>(blocksN * 2);
        for (int i = 0; i < blocksN; i++) {
            int lx = posArr[i * 3]     - minX;
            int ly = posArr[i * 3 + 1] - minY;
            int lz = posArr[i * 3 + 2] - minZ;
            int palIndex = idxArr[i];
            if (palIndex < 0 || palIndex >= palette.size()) continue;
            map.put(new BlockPos(lx, ly, lz), palette.get(palIndex));
        }

        return new StructureSnapshot(sizeX, sizeY, sizeZ, map);
    }

    private static @Nullable BlockState parseBlockStateSpecForSnapshot(String spec) {
        String name = spec;
        String props = null;
        int br = spec.indexOf('[');
        if (br >= 0 && spec.endsWith("]")) {
            name = spec.substring(0, br);
            props = spec.substring(br + 1, spec.length() - 1);
        }
        var rl = net.minecraft.resources.ResourceLocation.tryParse(name);
        if (rl == null) return null;

        var block = ForgeRegistries.BLOCKS.getValue(rl);
        if (block == null) return null;

        BlockState state = block.defaultBlockState();
        if (props == null || props.isEmpty()) return state;

        StateDefinition<?, ?> def = block.getStateDefinition();
        String[] pairs = props.split(",");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length != 2) continue;
            String key = kv[0].trim();
            String val = kv[1].trim();
            Property<?> prop = def.getProperty(key);
            if (prop == null) continue;
            Optional<?> parsed = ((Property) prop).getValue(val);
            if (parsed.isPresent()) {
                state = setUnchecked(state, prop, (Comparable) parsed.get());
            }
        }
        return state;
    }

    @Override
    public @NotNull InteractionResultHolder<ItemStack> use(@NotNull Level level, @NotNull Player p, @NotNull InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);

        if (!level.isClientSide() && p.isShiftKeyDown()) {
            MenuOpener.open(CrazyMenuRegistrar.GADGET_MENU.get(), p, MenuLocators.forHand(p, hand));
            return new InteractionResultHolder<>(InteractionResult.SUCCESS, stack);
        }

        if (stack.hasTag() && stack.getTag().getBoolean("code") && !p.isShiftKeyDown()) {
            if (!level.isClientSide() && level instanceof net.minecraft.server.level.ServerLevel sl) {
                BlockHitResult bhr = rayTraceFromPlayer(sl, p, 50.0D);
                if (bhr.getType() == HitResult.Type.BLOCK) {
                    BlockPos placeOrigin = bhr.getBlockPos().relative(bhr.getDirection());
                    pasteNow(level, p, stack, placeOrigin);
                } else {
                    p.displayClientMessage(Component.literal("No block in range (max 50 blocks)."), true);
                }
            }
            return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), stack);
        }

        if (!level.isClientSide() && cornerA != null && cornerB != null && origin != null) {
            generateProgramAndCut(level, p, stack);
            return InteractionResultHolder.success(stack);
        }

        return new InteractionResultHolder<>(InteractionResult.sidedSuccess(level.isClientSide()), stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos();
        Player player = ctx.getPlayer();
        if (player == null || player.isLocalPlayer()) return InteractionResult.SUCCESS;

        ItemStack stack = ctx.getItemInHand();

        if (!player.isShiftKeyDown()) {
            if (!level.isClientSide()) {
                BlockPos placeOrigin = clicked.relative(ctx.getClickedFace());
                pasteNow(level, player, stack, placeOrigin);
            }
            return InteractionResult.SUCCESS;
        } else {
            if (stack.hasTag() && stack.getTag().getBoolean("code")) {
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.cut_paste_first"), true);
            } else if (cornerA == null) {
                cornerA = clicked.immutable();
                CompoundTag tag = stack.getOrCreateTag();
                tag.putIntArray("selA", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.builder_corner_1"), true);
            } else if (cornerB == null) {
                cornerB = clicked.immutable();
                origin = clicked.immutable();
                originFacing = player.getDirection();
                stack.getOrCreateTag().remove("selA");
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.builder_corner_2"), true);
            } else {
                cornerA = clicked.immutable();
                cornerB = null;
                origin = null;
                CompoundTag tag = stack.getOrCreateTag();
                tag.putIntArray("selA", new int[]{cornerA.getX(), cornerA.getY(), cornerA.getZ()});
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.builder_corner_3"), true);
            }
            return InteractionResult.SUCCESS;
        }
    }

    private static BlockHitResult rayTraceFromPlayer(net.minecraft.server.level.ServerLevel level, Player player, double maxDistance) {
        Vec3 eye = player.getEyePosition(1.0F);
        Vec3 look = player.getViewVector(1.0F);
        Vec3 end  = eye.add(look.x * maxDistance, look.y * maxDistance, look.z * maxDistance);
        ClipContext ctx = new ClipContext(eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        HitResult res = level.clip(ctx);
        if (res instanceof BlockHitResult bhr && res.getType() == HitResult.Type.BLOCK) {
            return bhr;
        }
        return BlockHitResult.miss(end, Direction.getNearest(look.x, look.y, look.z), BlockPos.containing(end));
    }

    @Override
    public @Nullable ItemMenuHost getMenuHost(Player player, int inventorySlot, ItemStack stack, @Nullable BlockPos pos) {
        return new GadgetHost(player, inventorySlot, stack);
    }

    public static double calcStepCostFE(BlockPos from, BlockPos target) {
        double dx = target.getX() - from.getX();
        double dy = target.getY() - from.getY();
        double dz = target.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance * CrazyConfig.COMMON.PortableSpatialStorageCostMult.get();
    }

    public static int computeCutCostFE(Level level, BlockPos cornerA, BlockPos cornerB, BlockPos origin) {
        if (level == null || cornerA == null || cornerB == null || origin == null) return 0;

        BlockPos min = new BlockPos(
                Math.min(cornerA.getX(), cornerB.getX()),
                Math.min(cornerA.getY(), cornerB.getY()),
                Math.min(cornerA.getZ(), cornerB.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(cornerA.getX(), cornerB.getX()),
                Math.max(cornerA.getY(), cornerB.getY()),
                Math.max(cornerA.getZ(), cornerB.getZ())
        );

        double required = 0.0;
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos wp = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(wp);
                    if (state.isAir()) continue;
                    var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
                    if (id == null) continue;
                    var itemKey = AEItemKey.of(state.getBlock().asItem());
                    if (itemKey.fuzzyEquals(AEItemKey.of(Blocks.AIR.asItem()), FuzzyMode.IGNORE_ALL)) continue;
                    required += calcStepCostFE(origin, wp);
                }
            }
        }
        return (int) Math.ceil(required);
    }

    @Override
    public IUpgradeInventory getUpgrades(ItemStack stack) {
        return UpgradeInventories.forItem(stack, 4, this::onUpgradesChanged);
    }

    private void onUpgradesChanged(ItemStack stack, IUpgradeInventory upgrades) {
        setAEMaxPowerMultiplier(stack, 1 + upgrades.getInstalledUpgrades(AEItems.ENERGY_CARD));
    }

    private void generateProgramAndCut(Level level, Player p, ItemStack stack) {
        if (stack.hasTag() && stack.getTag().getBoolean("code")) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.cut_paste_first"), true);
            return;
        }
        if (cornerA == null || cornerB == null || origin == null) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.copy_paste_select_corners"), true);
            return;
        }

        BlockPos min = new BlockPos(
                Math.min(cornerA.getX(), cornerB.getX()),
                Math.min(cornerA.getY(), cornerB.getY()),
                Math.min(cornerA.getZ(), cornerB.getZ())
        );
        BlockPos max = new BlockPos(
                Math.max(cornerA.getX(), cornerB.getX()),
                Math.max(cornerA.getY(), cornerB.getY()),
                Math.max(cornerA.getZ(), cornerB.getZ())
        );

        double required = 0.0;
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos wp = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(wp);
                    if (state.isAir()) continue;
                    if (ForgeRegistries.BLOCKS.getKey(state.getBlock()) == null) continue;
                    var itemKey = AEItemKey.of(state.getBlock().asItem());
                    if (itemKey.fuzzyEquals(AEItemKey.of(Blocks.AIR.asItem()), FuzzyMode.IGNORE_ALL)) continue;
                    required += calcStepCostFE(origin, wp);
                }
            }
        }
        int needFE = (int) Math.ceil(required);
        if (!this.usePower(p, needFE / 2.d, stack)) {
            p.displayClientMessage(Component.literal("Not enough energy (" + needFE + " FE) to cut."), true);
            return;
        }

        Basis basis = Basis.forFacing(originFacing);
        List<TemplateUtil.BlockInfo> blockInfos = new ArrayList<>();
        List<Runnable> cutOps = new ArrayList<>();

        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    BlockPos wp = new BlockPos(x, y, z);
                    BlockState state = level.getBlockState(wp);
                    if (state.isAir()) continue;
                    if (state.canBeReplaced()) continue; // skip grass, snow, short plants, etc.
                    if (ForgeRegistries.BLOCKS.getKey(state.getBlock()) == null) continue;
                    var itemKey = AEItemKey.of(state.getBlock().asItem());
                    if (itemKey.fuzzyEquals(AEItemKey.of(Blocks.AIR.asItem()), FuzzyMode.IGNORE_ALL)) continue;

                    BlockPos localPos = worldToLocal(wp, origin, basis);
                    blockInfos.add(new TemplateUtil.BlockInfo(localPos, state));

                    final BlockPos wpc = wp;
                    cutOps.add(() -> {
                        if (level.hasChunkAt(wpc)) {
                            level.removeBlock(wpc, false);
                        }
                    });
                }
            }
        }

        int cutCount = blockInfos.size();
        p.displayClientMessage(Component.literal("Starting CUT: " + cutCount + " blocks"), true);

        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            BuildScheduler.enqueue(
                    sl,
                    p.getUUID(),
                    4,
                    cutOps,
                    () -> {
                        try {
                            String programId = java.util.UUID.randomUUID().toString();
                            var tag = stack.getOrCreateTag();
                            tag.putBoolean("code", true);
                            tag.putString("program_id", programId);
                            tag.putInt("delay", 0);
                            tag.putString("src_facing", originFacing.getName());

                            net.minecraft.nbt.CompoundTag templateTag = TemplateUtil.buildTemplateTag(blockInfos);
                            byte[] bytes = TemplateUtil.compressNbt(templateTag);
                            TemplateUtil.saveBytesToFile(programId, bytes, p.getServer());
                            TemplateUtil.rebuildPreviewFromTag(stack, templateTag);

                            p.displayClientMessage(Component.literal("CUT complete (" + cutCount + " blocks)"), true);
                            cornerA = cornerB = origin = null;
                        } catch (Exception e) {
                            p.displayClientMessage(Component.translatable("gui.crazyae2addons.error_saving"), true);
                        }
                    }
            );
        }
    }

    private void pasteNow(Level level, Player p, ItemStack stack, BlockPos originWorld) {
        if (!stack.hasTag() || !stack.getTag().getBoolean("code")) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.copy_paste_no_structure"), true);
            return;
        }

        byte[] bytes = TemplateUtil.loadBytesFromFile(stack, p.getServer());
        if (bytes == null || bytes.length == 0) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.copy_paste_no_structure"), true);
            return;
        }

        net.minecraft.nbt.CompoundTag templateTag;
        try {
            templateTag = TemplateUtil.decompressNbt(bytes);
        } catch (Exception e) {
            p.displayClientMessage(Component.translatable("gui.crazyae2addons.copy_paste_no_structure"), true);
            return;
        }

        List<TemplateUtil.BlockInfo> blockInfos = TemplateUtil.parseBlocksFromTag(templateTag);
        if (blockInfos.isEmpty()) return;

        Direction structureFacing = readSrcFacingFromNbt(stack);
        Basis basis = Basis.forFacing(structureFacing);

        // Collision check — skip positions where the stored block is itself replaceable
        for (TemplateUtil.BlockInfo info : blockInfos) {
            if (info.state().canBeReplaced()) continue;
            BlockPos wp = localToWorld(info.pos(), originWorld, basis);
            BlockState cur = level.getBlockState(wp);
            if (!cur.canBeReplaced() && !cur.equals(info.state())) {
                p.displayClientMessage(Component.literal(
                        "Cant paste: collision on " + wp.getX() + "," + wp.getY() + "," + wp.getZ()), true);
                return;
            }
        }

        // Energy cost
        double required = 0.0;
        for (TemplateUtil.BlockInfo info : blockInfos) {
            BlockPos wp = localToWorld(info.pos(), originWorld, basis);
            required += calcStepCostFE(originWorld, wp);
        }
        int needFE = (int) Math.ceil(required);
        if (!this.usePower(p, needFE / 2.d, stack)) {
            p.displayClientMessage(Component.literal("Not enough energy (" + needFE + " FE) to paste."), true);
            return;
        }

        TemplateUtil.clearStoredStructure(stack, p.getServer());

        List<Runnable> pasteOps = new ArrayList<>();
        final int[] placedCounter = {0};
        for (TemplateUtil.BlockInfo info : blockInfos) {
            BlockPos wp = localToWorld(info.pos(), originWorld, basis);
            BlockState st = info.state();
            pasteOps.add(() -> {
                if (level.hasChunkAt(wp)) {
                    level.setBlock(wp, st, 3);
                    placedCounter[0]++;
                }
            });
        }

        if (level instanceof net.minecraft.server.level.ServerLevel sl) {
            BuildScheduler.enqueue(sl, p.getUUID(), 4, pasteOps, () -> {});
        }
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.open(CrazyMenuRegistrar.GADGET_MENU.get(), player, MenuLocators.forHand(player, player.swingingArm));
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyItemRegistrar.PORTABLE_SPATIAL_IO.get().asItem().getDefaultInstance();
    }

    public static class Basis {
        final int fx, fz;
        final int rx, rz;

        private Basis(int fx, int fz, int rx, int rz) {
            this.fx = fx;
            this.fz = fz;
            this.rx = rx;
            this.rz = rz;
        }

        public static Basis forFacing(Direction f) {
            return switch (f) {
                case SOUTH -> new Basis(0, 1, -1, 0);
                case EAST  -> new Basis(1, 0, 0, 1);
                case WEST  -> new Basis(-1, 0, 0, -1);
                default    -> new Basis(0, -1, 1, 0); // NORTH
            };
        }
    }

    private static BlockPos worldToLocal(BlockPos worldPos, BlockPos origin, Basis b) {
        int dx = worldPos.getX() - origin.getX();
        int dy = worldPos.getY() - origin.getY();
        int dz = worldPos.getZ() - origin.getZ();
        int right   = dx * b.rx + dz * b.rz;
        int up      = dy;
        int forward = dx * b.fx + dz * b.fz;
        return new BlockPos(right, up, forward);
    }

    public static BlockPos localToWorld(BlockPos local, BlockPos origin, Basis b) {
        int dx = local.getX() * b.rx + local.getZ() * b.fx;
        int dz = local.getX() * b.rz + local.getZ() * b.fz;
        int dy = local.getY();
        return origin.offset(dx, dy, dz);
    }

    public static Direction readSrcFacingFromNbt(ItemStack stack) {
        if (stack.hasTag() && stack.getTag().contains("src_facing")) {
            String s = stack.getTag().getString("src_facing");
            Direction d = Direction.byName(s);
            if (d != null && d.getAxis().isHorizontal()) return d;
        }
        return Direction.NORTH;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static BlockState setUnchecked(BlockState state, Property prop, Comparable value) {
        return state.setValue(prop, value);
    }
}
