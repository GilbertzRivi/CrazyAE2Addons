package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.crafting.PatternDetailsHelper;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.*;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.helpers.MachineSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IAutoBuilderLogicHost;
import net.oktawia.crazyae2addons.logic.AutoBuilderLogic;
import net.oktawia.crazyae2addons.menus.AutoBuilderMenu;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewInfo;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;

public class AutoBuilderBE extends AENetworkedBlockEntity implements
        IGridTickable, MenuProvider, InternalInventoryHost, IUpgradeableObject,
        ICraftingRequester, PatternProviderLogicHost, IAutoBuilderLogicHost {

    public IUpgradeInventory upgrades;
    public Integer delay = 20;
    public BlockPos offset = new BlockPos(0, 2, 0);
    private BlockPos ghostRenderPos;
    private List<String> code = new ArrayList<>();
    private int currentInstruction = 0;
    private int tickDelayLeft = 0;
    private boolean isRunning = false;
    public AppEngInternalInventory inventory = new AppEngInternalInventory(this, 2);
    public int redstonePulseTicks = 0;
    private boolean isPulsing = false;

    private boolean isCrafting = false;

    public GenericStack missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
    public boolean skipEmpty = false;
    private boolean previewEnabled = false;
    private final List<BlockPos> previewPositions = new ArrayList<>();
    private final List<String> previewPalette = new ArrayList<>();
    private int[] previewIndices = new int[0];
    private final int PREVIEW_LIMIT = CrazyConfig.COMMON.AutobuilderPreviewLimit.get();
    private double requiredEnergyAE = 0.0D;
    private boolean energyPrepaid = false;
    private Direction sourceFacing = Direction.NORTH;

    public static final List<AutoBuilderBE> CLIENT_INSTANCES = new java.util.concurrent.CopyOnWriteArrayList<>();

    @OnlyIn(Dist.CLIENT)
    private PreviewInfo previewInfo;

    private boolean previewDirty = true;
    private boolean previewSyncDirty = false;
    private AutoBuilderMenu menu;
    private ICraftingLink craftingLink;
    private final AutoBuilderLogic logic;
    private GenericStack target;
    private Future<ICraftingPlan> toCraftPlan;

    private static final String NBT_BUILD_BUFFER = "BuildBuffer";

    private final Object2LongOpenHashMap<AEItemKey> buildBuffer = new Object2LongOpenHashMap<>();
    private boolean flushPending = false;
    private int flushTickAcc = 0;

    @OnlyIn(Dist.CLIENT)
    public PreviewInfo getPreviewInfo() {
        return previewInfo;
    }

    @OnlyIn(Dist.CLIENT)
    public void setPreviewInfo(PreviewInfo info) {
        this.previewInfo = info;
    }

    public boolean isPreviewEnabled() {
        return previewEnabled;
    }

    public List<BlockPos> getPreviewPositions() {
        return previewPositions;
    }

    public List<String> getPreviewPalette() {
        return previewPalette;
    }

    public int[] getPreviewIndices() {
        return previewIndices;
    }

    public boolean isPulsing() {
        return isPulsing;
    }

    public int getRedstonePulseTicks() {
        return redstonePulseTicks;
    }

    public AutoBuilderBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.AUTO_BUILDER_BE.get(), pos, state);

        this.upgrades = UpgradeInventories.forMachine(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 7, this::setChanged);
        this.ghostRenderPos = pos.above().above();
        this.logic = new AutoBuilderLogic(getMainNode(), this, this);

        buildBuffer.defaultReturnValue(0L);

        getMainNode()
                .addService(IGridTickable.class, this)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get().asItem())
                );

        this.inventory.setFilter(new appeng.util.inv.filter.IAEItemFilter() {
            @Override
            public boolean allowInsert(appeng.api.inventories.InternalInventory inv, int slot, ItemStack stack) {
                return slot == 0 && stack.getItem().equals(CrazyItemRegistrar.BUILDER_PATTERN.get().asItem());
            }
        });
    }

    // ─── PatternProviderLogicHost ──────────────────────────────────────────

    @Override
    public PatternProviderLogic getLogic() {
        return logic;
    }

    @Override
    public BlockEntity getBlockEntity() {
        return this;
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.allOf(Direction.class);
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get());
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(), player, locator);
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get().asItem().getDefaultInstance();
    }

    // ─── InternalInventoryHost ─────────────────────────────────────────────

    @Override
    public void saveChangedInventory(AppEngInternalInventory inv) {
        setChanged();
    }

    @Override
    public boolean isClientSide() {
        return level == null || level.isClientSide();
    }

    @Override
    public void onChangeInventory(AppEngInternalInventory inv, int slot) {
        if (this.isPreviewEnabled()) {
            this.togglePreview();
            if (this.menu != null) {
                this.menu.preview = false;
            }
        }
        this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
        this.setChanged();
        loadCode();
        recalculateRequiredEnergy();
        if (menu != null) {
            menu.pushEnergyDisplay();
        }

        if (inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty()) {
            isRunning = false;
            isCrafting = false;
            code = new ArrayList<>();
            currentInstruction = 0;
            tickDelayLeft = 0;
            energyPrepaid = false;
            requiredEnergyAE = 0.0D;
            resetGhostToHome();
        }

        if (!isRunning && !isCrafting && !buildBuffer.isEmpty()) {
            beginFlushBuffer();
        }
    }

    // ─── ISegmentedInventory ───────────────────────────────────────────────

    @Override
    public @Nullable InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.inventory;
        } else if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }
        return super.getSubInventory(id);
    }

    // ─── IItemHandler capability ────────────────────────────────────────────

    public IItemHandler getPatternHandler() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 2;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return inventory.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                if (slot == 0 && inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty()
                        && stack.getItem() == CrazyItemRegistrar.BUILDER_PATTERN.get()) {
                    if (!simulate) inventory.addItems(stack);
                    return ItemStack.EMPTY;
                }
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                if (slot == 1 && !isRunning) {
                    return inventory.extractItem(slot, amount, simulate);
                }
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return slot == 0 && stack.getItem() == CrazyItemRegistrar.BUILDER_PATTERN.get();
            }
        };
    }

    // ─── Lifecycle ─────────────────────────────────────────────────────────

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_INSTANCES.remove(this);
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        for (var stack : upgrades) {
            var genericStack = GenericStack.unwrapItemStack(stack);
            if (genericStack != null) {
                genericStack.what().addDrops(genericStack.amount(), drops, level, pos);
            } else {
                drops.add(stack);
            }
        }
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack s = inventory.getStackInSlot(i);
            if (!s.isEmpty()) drops.add(s);
        }
    }

    // ─── Client streaming (ghost cursor sync) ─────────────────────────────

    @Override
    protected void writeToStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeLong(ghostRenderPos != null ? ghostRenderPos.asLong() : worldPosition.asLong());
        data.writeBoolean(previewEnabled);
        boolean sendPreviewData = previewSyncDirty || previewEnabled;
        data.writeBoolean(sendPreviewData);
        if (sendPreviewData) {
            data.writeInt(previewPalette.size());
            for (String s : previewPalette) data.writeUtf(s);
            data.writeInt(previewPositions.size());
            for (BlockPos p : previewPositions) data.writeLong(p.asLong());
            data.writeInt(previewIndices.length);
            for (int idx : previewIndices) data.writeInt(idx);
            previewSyncDirty = false;
        }
    }

    @Override
    protected boolean readFromStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        boolean ret = super.readFromStream(data);
        ghostRenderPos = BlockPos.of(data.readLong());
        previewEnabled = data.readBoolean();
        boolean hasData = data.readBoolean();
        if (hasData) {
            previewPalette.clear();
            int palSize = data.readInt();
            for (int i = 0; i < palSize; i++) previewPalette.add(data.readUtf());
            previewPositions.clear();
            int posSize = data.readInt();
            for (int i = 0; i < posSize; i++) previewPositions.add(BlockPos.of(data.readLong()));
            int idxSize = data.readInt();
            previewIndices = new int[idxSize];
            for (int i = 0; i < idxSize; i++) previewIndices[i] = data.readInt();
            previewDirty = true;
        }
        return ret || true;
    }

    // ─── NBT ───────────────────────────────────────────────────────────────

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        this.upgrades.readFromNBT(tag, "upgrades", registries);
        this.inventory.readFromNBT(tag, "inv", registries);
        this.logic.readFromNBT(tag, registries);

        this.currentInstruction = tag.getInt("currentInstruction");
        this.tickDelayLeft = tag.getInt("tickDelayLeft");
        this.isRunning = tag.getBoolean("isRunning");
        if (tag.contains("GhostPos")) {
            this.ghostRenderPos = BlockPos.of(tag.getLong("GhostPos"));
        }
        if (tag.contains("offset")) {
            this.offset = BlockPos.of(tag.getLong("offset"));
        }
        this.previewEnabled = tag.getBoolean("previewEnabled");
        this.skipEmpty = tag.getBoolean("skipEmpty");
        this.energyPrepaid = tag.getBoolean("energyPrepaid");
        this.isCrafting = tag.getBoolean("isCrafting");
        this.flushPending = tag.getBoolean("flushPending");
        this.flushTickAcc = tag.getInt("flushTickAcc");

        previewPositions.clear();
        if (tag.contains("previewPositions", Tag.TAG_LIST)) {
            ListTag list = tag.getList("previewPositions", Tag.TAG_LONG);
            for (int i = 0; i < Math.min(list.size(), PREVIEW_LIMIT); i++) {
                previewPositions.add(BlockPos.of(((LongTag) list.get(i)).getAsLong()));
            }
        }

        previewPalette.clear();
        if (tag.contains("previewPalette", Tag.TAG_LIST)) {
            ListTag list = tag.getList("previewPalette", Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                previewPalette.add(list.getString(i));
            }
        }

        if (tag.contains("previewIndices", Tag.TAG_INT_ARRAY)) {
            previewIndices = tag.getIntArray("previewIndices");
        } else {
            previewIndices = new int[0];
        }

        buildBuffer.clear();
        if (tag.contains(NBT_BUILD_BUFFER, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_BUILD_BUFFER, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                if (!e.contains("Stack", Tag.TAG_COMPOUND)) continue;

                var s = ItemStack.parseOptional(registries, e.getCompound("Stack"));
                if (s.isEmpty()) continue;

                long amt = e.getLong("Amount");
                if (amt <= 0) continue;

                AEItemKey key = AEItemKey.of(s);
                if (key == null) continue;

                buildBuffer.put(key, amt);
            }
        }

        if (!buildBuffer.isEmpty() && !isRunning && !isCrafting) {
            flushPending = true;
            flushTickAcc = 0;
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.upgrades.writeToNBT(tag, "upgrades", registries);
        this.inventory.writeToNBT(tag, "inv", registries);
        this.logic.writeToNBT(tag, registries);

        tag.putInt("currentInstruction", this.currentInstruction);
        tag.putInt("tickDelayLeft", this.tickDelayLeft);
        tag.putBoolean("isRunning", this.isRunning);
        tag.putLong("GhostPos", ghostRenderPos != null ? ghostRenderPos.asLong() : worldPosition.above().above().asLong());
        tag.putLong("offset", this.offset.asLong());
        tag.putBoolean("previewEnabled", previewEnabled);
        tag.putBoolean("skipEmpty", skipEmpty);
        tag.putBoolean("energyPrepaid", energyPrepaid);
        tag.putBoolean("isCrafting", isCrafting);
        tag.putBoolean("flushPending", flushPending);
        tag.putInt("flushTickAcc", flushTickAcc);

        ListTag posList = new ListTag();
        for (int i = 0; i < Math.min(previewPositions.size(), PREVIEW_LIMIT); i++) {
            posList.add(LongTag.valueOf(previewPositions.get(i).asLong()));
        }
        tag.put("previewPositions", posList);

        ListTag palList = new ListTag();
        for (String s : previewPalette) palList.add(StringTag.valueOf(s));
        tag.put("previewPalette", palList);

        tag.put("previewIndices", new IntArrayTag(previewIndices));

        ListTag buf = new ListTag();
        for (Object2LongMap.Entry<AEItemKey> e : buildBuffer.object2LongEntrySet()) {
            if (e.getLongValue() <= 0) continue;
            CompoundTag t = new CompoundTag();
            ItemStack s = e.getKey().toStack(1);
            t.put("Stack", s.save(registries));
            t.putLong("Amount", e.getLongValue());
            buf.add(t);
        }
        tag.put(NBT_BUILD_BUFFER, buf);
    }

    // ─── Redstone pulse ────────────────────────────────────────────────────

    private void triggerRedstonePulse() {
        if (level == null || level.isClientSide) return;
        isPulsing = true;
        redstonePulseTicks = 2;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    // ─── IUpgradeableObject ────────────────────────────────────────────────

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    // ─── Creative supply detection ─────────────────────────────────────────

    private boolean hasCreativeSupply() {
        var grid = getMainNode().getGrid();
        if (grid == null) return false;
        return !grid.getMachines(AutoBuilderCreativeSupplyBE.class).isEmpty();
    }

    // ─── IGridTickable ─────────────────────────────────────────────────────

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {

        // Redstone pulse countdown
        if (redstonePulseTicks > 0) {
            redstonePulseTicks -= ticksSinceLastCall;
            if (redstonePulseTicks <= 0) {
                redstonePulseTicks = 0;
                isPulsing = false;
                if (level != null && !level.isClientSide) {
                    level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                }
            }
        }

        if (!isRunning && !isCrafting && !buildBuffer.isEmpty() && !flushPending) {
            beginFlushBuffer();
        }

        if (flushPending) {
            flushTickAcc += ticksSinceLastCall;
            if (flushTickAcc >= 20) {
                flushTickAcc = 0;
                boolean done = flushBufferOnce();
                if (done) {
                    flushPending = false;
                }
            }
            return TickRateModulation.URGENT;
        }

        if (this.toCraftPlan != null && this.toCraftPlan.isDone()) {
            try {
                var plan = this.toCraftPlan.get();
                this.toCraftPlan = null;

                var grid = getMainNode().getGrid();
                if (grid == null) {
                    this.isCrafting = false;
                    beginFlushBuffer();
                    return TickRateModulation.URGENT;
                }

                var result = grid.getCraftingService().submitJob(
                        plan, this, null, true, IActionSource.ofMachine(this)
                );

                if (result.successful() && result.link() != null) {
                    this.craftingLink = result.link();
                } else {
                    this.logic.getPatternInv().setItemDirect(0, ItemStack.EMPTY);
                    this.logic.updatePatterns();

                    try {
                        KeyCounter missing = plan.missingItems();
                        if (missing != null && !missing.isEmpty()) {
                            var firstMissing = missing.iterator().next();
                            this.missingItems = new GenericStack(firstMissing.getKey(), firstMissing.getLongValue());
                        } else {
                            this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
                        }
                    } catch (Throwable ignored) {
                        this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
                    }

                    this.isCrafting = false;
                    if (this.craftingLink != null) {
                        try { this.craftingLink.cancel(); } catch (Throwable ignored) {}
                        this.craftingLink = null;
                    }
                    beginFlushBuffer();
                }
            } catch (Exception ignored) {
                this.toCraftPlan = null;
                this.isCrafting = false;
                beginFlushBuffer();
            }
        }

        if (!isRunning || code.isEmpty() || isCrafting) {
            return TickRateModulation.URGENT;
        }

        if (!energyPrepaid) {
            isRunning = false;
            beginFlushBuffer();
            return TickRateModulation.URGENT;
        }

        if (inventory.getStackInSlot(0).isEmpty()) {
            isRunning = false;
            resetGhostToHome();
            beginFlushBuffer();
            return TickRateModulation.URGENT;
        }

        if (tickDelayLeft > 0) {
            tickDelayLeft -= ticksSinceLastCall;
            return TickRateModulation.URGENT;
        }

        boolean didWork = false;

        for (int steps = 0; steps < calcStepsPerTick() && currentInstruction < code.size(); steps++) {
            String inst = code.get(currentInstruction);

            if (inst.startsWith("Z|")) {
                tickDelayLeft = Integer.parseInt(inst.substring(2));
                currentInstruction++;
                return TickRateModulation.URGENT;
            }

            didWork = true;

            switch (inst) {
                case "F", "B", "L", "R", "U", "D" ->
                        setGhostRenderPos(stepRelative(getGhostRenderPos(), inst.charAt(0)));

                case "H" -> resetGhostToHome();

                case "X" -> {
                    var grid = getMainNode().getGrid();
                    boolean didDestroy = false;
                    BlockPos pos = getGhostRenderPos();

                    if (grid != null) {
                        BlockState state = level.getBlockState(pos);
                        if (!state.isAir() && isBreakable(state, level, pos)) {
                            var drops = getSilkTouchDrops(state, (ServerLevel) level, pos);

                            long inserted = 0;
                            for (var drop : drops) {
                                inserted += StorageHelper.poweredInsert(
                                        grid.getEnergyService(),
                                        grid.getStorageService().getInventory(),
                                        AEItemKey.of(drop.getItem()),
                                        1,
                                        IActionSource.ofMachine(this),
                                        Actionable.MODULATE
                                );
                            }

                            if (inserted > 0 || drops.isEmpty()) {
                                if (level.destroyBlock(pos, false)) {
                                    didDestroy = true;
                                }
                            }
                        }

                        var fs = level.getFluidState(pos);
                        if (!fs.isEmpty()) {
                            if (fs.isSource()) {
                                StorageHelper.poweredInsert(
                                        grid.getEnergyService(),
                                        grid.getStorageService().getInventory(),
                                        AEFluidKey.of(fs.getType()),
                                        1000,
                                        IActionSource.ofMachine(this),
                                        Actionable.MODULATE
                                );
                            }
                            if (level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
                                didDestroy = true;
                            }
                        }
                    }

                    if (didDestroy) {
                        currentInstruction++;
                        tickDelayLeft = Math.max(tickDelayLeft, CrazyConfig.COMMON.AutobuilderMineDelay.get());
                        return TickRateModulation.URGENT;
                    }
                }
                default -> {
                    if (inst.startsWith("P|")) {
                        String blockIdRaw = inst.substring(2);

                        try {
                            String blockIdClean;
                            Map<String, String> props = new HashMap<>();
                            int idx = blockIdRaw.indexOf('[');
                            if (idx > 0 && blockIdRaw.endsWith("]")) {
                                blockIdClean = blockIdRaw.substring(0, idx);
                                String propString = blockIdRaw.substring(idx + 1, blockIdRaw.length() - 1);
                                for (String pair : propString.split(",")) {
                                    String[] kv = pair.split("=", 2);
                                    if (kv.length == 2) {
                                        props.put(kv[0], kv[1]);
                                    }
                                }
                            } else {
                                blockIdClean = blockIdRaw;
                            }

                            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockIdClean))
                                    .orElse(Blocks.AIR);
                            if (block != Blocks.AIR) {
                                var grid = getMainNode().getGrid();
                                if (grid != null) {
                                    BlockPos target = getGhostRenderPos();

                                    if (level.getBlockState(target).getBlock() == block) {
                                        break;
                                    }

                                    if (isBreakable(level.getBlockState(target), level, target)) {
                                        var drops = getSilkTouchDrops(level.getBlockState(target), (ServerLevel) level, target);
                                        long inserted = 0;
                                        for (var drop : drops) {
                                            inserted += StorageHelper.poweredInsert(
                                                    grid.getEnergyService(),
                                                    grid.getStorageService().getInventory(),
                                                    AEItemKey.of(drop.getItem()),
                                                    1,
                                                    IActionSource.ofMachine(this),
                                                    Actionable.MODULATE
                                            );
                                        }
                                        if (inserted <= 0 && !drops.isEmpty()) {
                                            currentInstruction++;
                                            return TickRateModulation.URGENT;
                                        }
                                    }

                                    boolean creative = hasCreativeSupply();

                                    long extracted = 0;
                                    if (!creative) {
                                        AEItemKey key = AEItemKey.of(block.asItem());
                                        extracted = bufferExtract(key, 1);
                                        if (extracted <= 0) {
                                            this.missingItems = new GenericStack(key, 1);
                                            if (skipEmpty) {
                                                currentInstruction++;
                                                return TickRateModulation.URGENT;
                                            }
                                            this.isRunning = false;
                                            this.energyPrepaid = false;
                                            beginFlushBuffer();
                                            return TickRateModulation.URGENT;
                                        }
                                    }

                                    if (extracted > 0 || creative) {
                                        BlockState state = block.defaultBlockState();
                                        if (!props.isEmpty()) {
                                            for (Map.Entry<String, String> entry : props.entrySet()) {
                                                Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                                                if (property != null) {
                                                    state = applyProperty(state, property, entry.getValue());
                                                }
                                            }
                                        }

                                        int delta = Math.floorMod(
                                                yawStepsFromNorth(getFacing()) - yawStepsFromNorth(this.sourceFacing), 4);
                                        state = rotateStateByDelta(state, delta);

                                        level.setBlock(target, state, 3);
                                    }
                                }
                            }
                        } catch (Exception ignored) {
                        }
                    }
                }
            }

            currentInstruction++;
        }

        if (currentInstruction >= code.size()) {
            isRunning = false;
            energyPrepaid = false;

            ItemStack pattern = inventory.getStackInSlot(0);
            if (!pattern.isEmpty()) {
                inventory.setItemDirect(0, ItemStack.EMPTY);
                inventory.setItemDirect(1, pattern.copyWithCount(1));
            }

            resetGhostToHome();
            triggerRedstonePulse();

            if (!buildBuffer.isEmpty()) {
                beginFlushBuffer();
            }

        } else if (didWork) {
            tickDelayLeft = this.delay;
        }

        return TickRateModulation.URGENT;
    }

    // ─── ICraftingRequester ────────────────────────────────────────────────

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.craftingLink != null ? ImmutableSet.of(this.craftingLink) : ImmutableSet.of();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
    }

    // ─── IAutoBuilderPPLogicHost ───────────────────────────────────────────

    @Override
    public void addToBuildBuffer(AEItemKey key, long amount) {
        if (amount <= 0) return;
        if (flushPending && getMainNode().getGrid() != null) {
            var grid = getMainNode().getGrid();
            long inserted = StorageHelper.poweredInsert(
                    grid.getEnergyService(),
                    grid.getStorageService().getInventory(),
                    key, amount,
                    IActionSource.ofMachine(this),
                    Actionable.MODULATE
            );
            long left = amount - inserted;
            if (left > 0) bufferAdd(key, left);
            return;
        }
        bufferAdd(key, amount);
    }

    @Override
    public void cancelCraftNoFlush() {
        if (this.craftingLink != null) {
            try { this.craftingLink.cancel(); } catch (Throwable ignored) {}
        }
        this.logic.getPatternInv().clear();
        this.logic.updatePatterns();
        this.craftingLink = null;
        this.toCraftPlan = null;
        this.isCrafting = false;
    }

    // ─── Redstone activation (called from block) ───────────────────────────

    @Override
    public void onRedstoneActivate() {
        if (getLevel() == null) return;
        if (flushPending) return;
        if (this.craftingLink != null) return;

        if (inventory.getStackInSlot(0).isEmpty() && !inventory.getStackInSlot(1).isEmpty()) {
            inventory.setItemDirect(0, inventory.getStackInSlot(1).copyWithCount(1));
            inventory.setItemDirect(1, ItemStack.EMPTY);
        }

        loadCode();
        if (this.code.isEmpty()) {
            if (!buildBuffer.isEmpty()) beginFlushBuffer();
            return;
        }
        var required = ProgramExpander.countUsedBlocks(String.join("/", this.code));
        if (!hasCreativeSupply()) {
            reserveFromNetwork(required);
            var missing = computeMissingAfterReserve(required);

            if (!missing.isEmpty() && upgrades.isInstalled(AEItems.CRAFTING_CARD) && !isClientSide()) {
                var tag = new CompoundTag();
                tag.putString("id", UUID.randomUUID().toString());
                var targetStack = new ItemStack(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get());
                targetStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                        net.minecraft.world.item.component.CustomData.of(tag));
                this.target = new GenericStack(AEItemKey.of(targetStack), 1);

                var pattern = PatternDetailsHelper.encodeProcessingPattern(
                        missing,
                        java.util.List.of(target)
                );

                this.logic.getPatternInv().setItemDirect(0, pattern);
                this.logic.updatePatterns();

                var grid = getMainNode().getGrid();
                if (grid != null) {
                    this.toCraftPlan = grid.getCraftingService().beginCraftingCalculation(
                            getLevel(),
                            () -> new MachineSource(this),
                            target.what(),
                            target.amount(),
                            CalculationStrategy.REPORT_MISSING_ITEMS
                    );
                }

                this.isCrafting = true;
                this.isRunning = false;
                this.energyPrepaid = false;
                return;
            }

            if (!missing.isEmpty()) {
                this.missingItems = missing.get(0);
                if (!skipEmpty) {
                    beginFlushBuffer();
                    return;
                }
            }
        } else {
            this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
            this.isCrafting = false;
        }

        recalculateRequiredEnergy();

        var node = getMainNode();
        if (node.getGrid() == null) {
            this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
            this.isCrafting = false;
            this.isRunning = false;
            this.energyPrepaid = false;
            beginFlushBuffer();
            return;
        }

        var es = node.getGrid().getEnergyService();
        double can = es.extractAEPower(requiredEnergyAE, Actionable.SIMULATE, PowerMultiplier.ONE);
        if (can < requiredEnergyAE) {
            this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
            this.isCrafting = false;
            this.isRunning = false;
            this.energyPrepaid = false;
            beginFlushBuffer();
            return;
        }

        es.extractAEPower(requiredEnergyAE, Actionable.MODULATE, PowerMultiplier.ONE);
        this.energyPrepaid = true;

        this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
        this.isCrafting = false;
        this.isRunning = true;
        this.currentInstruction = 0;
        this.tickDelayLeft = 0;
    }

    // ─── Buffer management ─────────────────────────────────────────────────

    private long bufferGet(AEItemKey key) {
        return buildBuffer.getOrDefault(key, 0L);
    }

    private void bufferAdd(AEItemKey key, long amount) {
        if (amount <= 0) return;
        buildBuffer.put(key, bufferGet(key) + amount);
        setChanged();
    }

    private long bufferExtract(AEItemKey key, long amount) {
        if (amount <= 0) return 0;
        long have = bufferGet(key);
        long take = Math.min(have, amount);
        if (take <= 0) return 0;
        long left = have - take;
        if (left <= 0) buildBuffer.removeLong(key);
        else buildBuffer.put(key, left);
        setChanged();
        return take;
    }

    private void beginFlushBuffer() {
        if (buildBuffer.isEmpty()) {
            flushPending = false;
            return;
        }
        this.isRunning = false;
        this.isCrafting = false;
        this.energyPrepaid = false;
        this.tickDelayLeft = 0;
        if (this.toCraftPlan != null) this.toCraftPlan = null;
        if (this.craftingLink != null) {
            try { this.craftingLink.cancel(); } catch (Throwable ignored) {}
            this.craftingLink = null;
        }
        flushPending = true;
        flushTickAcc = 0;
        setChanged();
    }

    private boolean flushBufferOnce() {
        var grid = getMainNode().getGrid();
        if (grid == null) return false;

        var inv = grid.getStorageService().getInventory();
        var es = grid.getEnergyService();
        var src = IActionSource.ofMachine(this);

        var it = buildBuffer.object2LongEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            var key = e.getKey();
            long amt = e.getLongValue();
            if (amt <= 0) { it.remove(); continue; }

            long inserted = StorageHelper.poweredInsert(es, inv, key, amt, src, Actionable.MODULATE);
            if (inserted >= amt) {
                it.remove();
            } else {
                e.setValue(amt - inserted);
            }
        }

        return buildBuffer.isEmpty();
    }

    // ─── Network reservation ───────────────────────────────────────────────

    private void reserveFromNetwork(Map<String, Integer> requiredBlocks) {
        var grid = getMainNode().getGrid();
        if (grid == null) return;
        if (hasCreativeSupply()) return;

        var storage = grid.getStorageService().getInventory();
        var es = grid.getEnergyService();
        var src = IActionSource.ofMachine(this);

        for (var entry : requiredBlocks.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(entry.getKey().split("\\[")[0]))
                    .orElse(Blocks.AIR);
            if (block == Blocks.AIR) continue;

            var key = AEItemKey.of(block.asItem());
            long need = (long) entry.getValue() - bufferGet(key);
            if (need <= 0) continue;

            long pulled = StorageHelper.poweredExtraction(es, storage, key, need, src, Actionable.MODULATE);
            if (pulled > 0) bufferAdd(key, pulled);
        }
    }

    private List<GenericStack> computeMissingAfterReserve(Map<String, Integer> requiredBlocks) {
        if (hasCreativeSupply()) return new ArrayList<>();

        List<GenericStack> missing = new ArrayList<>();
        for (var entry : requiredBlocks.entrySet()) {
            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(entry.getKey().split("\\[")[0]))
                    .orElse(Blocks.AIR);
            if (block == Blocks.AIR) continue;

            var key = AEItemKey.of(block.asItem());
            long need = (long) entry.getValue() - bufferGet(key);
            if (need > 0) missing.add(new GenericStack(key, need));
        }
        return missing;
    }

    // ─── Energy calculation ────────────────────────────────────────────────

    public double getRequiredEnergyAE() {
        recalculateRequiredEnergy();
        return requiredEnergyAE;
    }

    public void recalculateRequiredEnergy() {
        requiredEnergyAE = 0.0D;
        if (level == null || code == null || code.isEmpty()) return;

        BlockPos cursor = homePos();
        for (String inst : code) {
            if (inst == null || inst.isEmpty()) continue;
            if (inst.startsWith("Z|")) continue;
            if (inst.equals("H")) { cursor = homePos(); continue; }
            if (inst.length() == 1) {
                char c = inst.charAt(0);
                if (c == 'X') {
                    requiredEnergyAE += calcStepCostAE(cursor);
                } else if ("FBLRUD".indexOf(c) >= 0) {
                    cursor = stepRelative(cursor, c);
                }
                continue;
            }
            if (inst.startsWith("P|") || (inst.startsWith("P(") && inst.endsWith(")"))) {
                requiredEnergyAE += calcStepCostAE(cursor);
            }
        }
    }

    private double calcStepCostAE(BlockPos target) {
        double dx = target.getX() - worldPosition.getX();
        double dy = target.getY() - worldPosition.getY();
        double dz = target.getZ() - worldPosition.getZ();
        return Math.sqrt(dx * dx + dy * dy + dz * dz) * CrazyConfig.COMMON.AutobuilderCostMult.get();
    }

    private static int stepsFromCards(int cards, int configMax) {
        int c = Mth.clamp(cards, 0, 6);
        int max = Math.max(1, configMax);
        return 1 + ((max - 1) * c) / 6;
    }

    private int calcStepsPerTick() {
        int cards = upgrades.getInstalledUpgrades(AEItems.SPEED_CARD);
        int maxFromConfig = CrazyConfig.COMMON.AutobuilderSpeed.get();
        return stepsFromCards(cards, maxFromConfig);
    }

    // ─── Program loading ───────────────────────────────────────────────────

    public void loadCode() {
        ItemStack s = this.inventory.getStackInSlot(0);
        if (s.isEmpty()) s = this.inventory.getStackInSlot(1);

        if (s.isEmpty()) {
            this.code.clear();
            return;
        }

        String programId = net.oktawia.crazyae2addons.items.BuilderPatternItem.getProgramId(s);
        if (programId == null) {
            this.code.clear();
            return;
        }

        var program = ProgramExpander.expand(loadProgramFromFile(s, getLevel() != null ? getLevel().getServer() : null));
        if (program.success) {
            this.code = program.program;
        } else {
            this.code.clear();
        }

        Direction d = net.oktawia.crazyae2addons.items.BuilderPatternItem.getSourceFacing(s);
        this.sourceFacing = d != null ? d : Direction.NORTH;
        this.delay = net.oktawia.crazyae2addons.items.BuilderPatternItem.getDelay(s);
    }

    public static String loadProgramFromFile(ItemStack stack, @Nullable MinecraftServer server) {
        if (server == null) return "";
        String programId = net.oktawia.crazyae2addons.items.BuilderPatternItem.getProgramId(stack);
        if (programId == null) return "";

        Path file = server.getWorldPath(new LevelResource("serverdata"))
                .resolve("autobuilder")
                .resolve(programId);

        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogUtils.getLogger().info("AutoBuilder: could not load program file: {}", e.toString());
            return "";
        }
    }

    // ─── Preview ──────────────────────────────────────────────────────────

    public void togglePreview() {
        this.previewEnabled = !this.previewEnabled;

        if (this.previewEnabled) {
            if (this.code.isEmpty()) {
                ItemStack s = this.inventory.getStackInSlot(0);
                if (s.isEmpty()) s = this.inventory.getStackInSlot(1);
                if (!s.isEmpty()) {
                    var res = ProgramExpander.expand(loadProgramFromFile(s, getLevel() != null ? getLevel().getServer() : null));
                    if (res.success) this.code = res.program;
                }
            }
            rebuildPreviewFromCode();
        } else {
            previewPositions.clear();
            previewPalette.clear();
            previewIndices = new int[0];
        }

        this.previewSyncDirty = true;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        this.setPreviewDirty(true);
        if (level != null && level.isClientSide) this.setPreviewInfo(null);
    }

    private void rebuildPreviewFromCode() {
        previewPositions.clear();
        var indices = new it.unimi.dsi.fastutil.ints.IntArrayList();

        if (this.code == null || this.code.isEmpty()) {
            previewIndices = new int[0];
            return;
        }

        BlockPos localCursor = BlockPos.ZERO;

        for (String inst : this.code) {
            if (previewPositions.size() >= PREVIEW_LIMIT) break;
            if (inst == null || inst.isEmpty()) continue;
            if (inst.startsWith("Z|")) continue;
            if (inst.equals("H")) { localCursor = BlockPos.ZERO; continue; }

            if (inst.length() == 1) {
                char c = inst.charAt(0);
                if ("FBLRUD".indexOf(c) >= 0) localCursor = stepLocal(localCursor, c);
                continue;
            }

            int paletteIndex = -1;
            if (inst.startsWith("P|")) {
                String blockKey = inst.substring(2);
                int idx = previewPalette.indexOf(blockKey);
                if (idx < 0) { previewPalette.add(blockKey); idx = previewPalette.size() - 1; }
                paletteIndex = idx;
            } else {
                continue;
            }

            BlockPos localTotal = new BlockPos(
                    offset.getX() + localCursor.getX(),
                    offset.getY() + localCursor.getY(),
                    offset.getZ() + localCursor.getZ()
            );

            previewPositions.add(transformRelative(localTotal));
            indices.add(paletteIndex);
        }

        previewIndices = indices.toIntArray();
        this.previewSyncDirty = true;
        this.setPreviewDirty(true);
        if (level != null && level.isClientSide) this.setPreviewInfo(null);
    }

    public boolean isPreviewDirty() { return previewDirty; }
    public void setPreviewDirty(boolean dirty) { this.previewDirty = dirty; }

    // ─── Ghost / cursor ────────────────────────────────────────────────────

    public void setGhostRenderPos(BlockPos pos) {
        this.ghostRenderPos = pos;
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public BlockPos getGhostRenderPos() {
        return ghostRenderPos;
    }

    public void onOffsetChanged() {
        resetGhostToHome();
        if (previewEnabled) {
            rebuildPreviewFromCode();
        }
        previewSyncDirty = true;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void resetGhostToHome() {
        setGhostRenderPos(homePos());
    }

    private BlockPos homePos() {
        return transformRelative(offset);
    }

    private BlockPos transformRelative(BlockPos local) {
        return getBlockPos().offset(localToWorldOffset(local));
    }

    private Direction getFacing() {
        BlockState state = getBlockState();
        if (state.hasProperty(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        }
        return Direction.NORTH;
    }

    private BlockPos localToWorldOffset(BlockPos local) {
        Direction f = getFacing();
        int fx, fz, rx, rz;
        switch (f) {
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

    private BlockPos stepRelative(BlockPos pos, char cmd) {
        Direction f = getFacing();
        int fx, fz, rx, rz;
        switch (f) {
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

    private static BlockPos stepLocal(BlockPos pos, char cmd) {
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

    // ─── Silk touch mining ─────────────────────────────────────────────────

    public static List<ItemStack> getSilkTouchDrops(BlockState state, ServerLevel level, BlockPos pos) {
        ItemStack silkTool = new ItemStack(Items.DIAMOND_PICKAXE);
        var enchReg = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        silkTool.enchant(enchReg.getHolderOrThrow(Enchantments.SILK_TOUCH), 1);

        var lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.TOOL, silkTool)
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withParameter(LootContextParams.BLOCK_STATE, state);

        return state.getDrops(lootParams);
    }

    // ─── Block state helpers ───────────────────────────────────────────────

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String valueStr) {
        try {
            if (property instanceof BooleanProperty) {
                return state.setValue((BooleanProperty) property, Boolean.parseBoolean(valueStr));
            }
            if (property instanceof IntegerProperty) {
                return state.setValue((IntegerProperty) property, Integer.parseInt(valueStr));
            }
            Optional<T> value = property.getValue(valueStr);
            if (value.isPresent()) return state.setValue(property, value.get());
        } catch (Exception ignored) {}
        return state;
    }

    public static boolean isBreakable(BlockState state, Level level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) >= 0;
    }

    private static int yawStepsFromNorth(Direction d) {
        return switch (d) {
            case NORTH -> 0;
            case EAST  -> 1;
            case SOUTH -> 2;
            case WEST  -> 3;
            default    -> 0;
        };
    }

    private static Direction rotateHorizontal(Direction dir, int steps) {
        steps = Math.floorMod(steps, 4);
        Direction out = dir;
        for (int i = 0; i < steps; i++) out = out.getClockWise();
        return out;
    }

    private static boolean isHorizontal(Direction d) {
        return d.getAxis().isHorizontal();
    }

    private BlockState rotateStateByDelta(BlockState state, int steps) {
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

    // ─── Menu ─────────────────────────────────────────────────────────────

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AutoBuilderMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public void setMenu(AutoBuilderMenu menu) {
        this.menu = menu;
    }

    public AutoBuilderMenu getMenu() {
        return this.menu;
    }
}
