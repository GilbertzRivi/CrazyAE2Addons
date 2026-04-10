package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.storage.StorageHelper;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.*;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.NbtOps;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.items.IItemHandler;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.components.AEItemBufferData;
import net.oktawia.crazyae2addons.defs.components.AutoBuilderPreviewData;
import net.oktawia.crazyae2addons.defs.components.AutoBuilderStateData;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.builder.BuilderPatternHost;
import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;
import net.oktawia.crazyae2addons.logic.builder.BuilderCoordMath;
import net.oktawia.crazyae2addons.menus.AutoBuilderMenu;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewInfo;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AutoBuilderBE extends AENetworkedBlockEntity implements
        IGridTickable, MenuProvider, InternalInventoryHost, IUpgradeableObject,
        ICraftingRequester, PatternProviderLogicHost {

    public IUpgradeInventory upgrades = UpgradeInventories.forMachine(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 7, this::setChanged);
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
    private final ManagedBuffer buffer;

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

        this.ghostRenderPos = pos.above().above();
        this.buffer = new ManagedBuffer(getMainNode(), this, this, this::setChanged, this::onRedstoneActivate, () -> isRunning || isCrafting);

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

    @Override
    public PatternProviderLogic getLogic() {
        return buffer.getLogic();
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

        if (!isRunning && !isCrafting && !buffer.isEmpty()) {
            beginFlushBuffer();
        }
    }

    @Override
    public @Nullable InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.inventory;
        } else if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }
        return super.getSubInventory(id);
    }

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

    @Override
    protected void writeToStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        var preview = new AutoBuilderPreviewData(
                Optional.ofNullable(ghostRenderPos), previewEnabled,
                List.copyOf(previewPositions), List.copyOf(previewPalette),
                previewIndices.clone());
        AutoBuilderPreviewData.STREAM_CODEC.encode(data, preview);
        previewSyncDirty = false;
    }

    @Override
    protected boolean readFromStream(net.minecraft.network.RegistryFriendlyByteBuf data) {
        boolean ret = super.readFromStream(data);
        AutoBuilderPreviewData preview = AutoBuilderPreviewData.STREAM_CODEC.decode(data);
        ghostRenderPos = preview.ghostRenderPos().orElse(null);
        previewEnabled = preview.previewEnabled();
        previewPositions.clear();
        previewPositions.addAll(preview.previewPositions());
        previewPalette.clear();
        previewPalette.addAll(preview.previewPalette());
        previewIndices = preview.previewIndices().clone();
        previewDirty = true;
        return ret || true;
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);
        this.upgrades.readFromNBT(tag, "myupgrades", registries);
        this.inventory.readFromNBT(tag, "inv", registries);
        buffer.getLogic().readFromNBT(tag, registries);

        if (tag.contains("state", Tag.TAG_COMPOUND)) {
            AutoBuilderStateData s = AutoBuilderStateData.CODEC
                    .parse(NbtOps.INSTANCE, tag.getCompound("state"))
                    .getOrThrow();
            this.currentInstruction = s.currentInstruction();
            this.tickDelayLeft = s.tickDelayLeft();
            this.isRunning = s.isRunning();
            this.offset = s.offset();
            this.skipEmpty = s.skipEmpty();
            this.energyPrepaid = s.energyPrepaid();
            this.isCrafting = s.isCrafting();
        }
        if (tag.contains("preview", Tag.TAG_COMPOUND)) {
            AutoBuilderPreviewData p = AutoBuilderPreviewData.CODEC
                    .parse(NbtOps.INSTANCE, tag.getCompound("preview"))
                    .getOrThrow();
            this.ghostRenderPos = p.ghostRenderPos().orElse(null);
            this.previewEnabled = p.previewEnabled();
            previewPositions.clear();
            previewPositions.addAll(p.previewPositions().subList(0, Math.min(p.previewPositions().size(), PREVIEW_LIMIT)));
            previewPalette.clear();
            previewPalette.addAll(p.previewPalette());
            this.previewIndices = p.previewIndices().clone();
        }
        if (tag.contains("buffer", Tag.TAG_COMPOUND)) {
            AEItemBufferData b = AEItemBufferData.CODEC
                    .parse(NbtOps.INSTANCE, tag.getCompound("buffer"))
                    .getOrThrow();
            buffer.fromData(b);
            if (!b.flushPending() && !buffer.isEmpty() && !isRunning && !isCrafting) {
                buffer.beginFlush();
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        this.upgrades.writeToNBT(tag, "myupgrades", registries);
        this.inventory.writeToNBT(tag, "inv", registries);
        buffer.getLogic().writeToNBT(tag, registries);

        tag.put("state", AutoBuilderStateData.CODEC
                .encodeStart(NbtOps.INSTANCE, new AutoBuilderStateData(
                        currentInstruction, tickDelayLeft, isRunning,
                        offset, skipEmpty, energyPrepaid, isCrafting))
                .getOrThrow());
        tag.put("preview", AutoBuilderPreviewData.CODEC
                .encodeStart(NbtOps.INSTANCE, new AutoBuilderPreviewData(
                        Optional.ofNullable(ghostRenderPos), previewEnabled,
                        List.copyOf(previewPositions.subList(0, Math.min(previewPositions.size(), PREVIEW_LIMIT))),
                        List.copyOf(previewPalette),
                        previewIndices.clone()))
                .getOrThrow());
        tag.put("buffer", AEItemBufferData.CODEC
                .encodeStart(NbtOps.INSTANCE, buffer.toData())
                .getOrThrow());
    }

    private void triggerRedstonePulse() {
        if (level == null || level.isClientSide) return;
        isPulsing = true;
        redstonePulseTicks = 2;
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    private boolean hasCreativeSupply() {
        var grid = getMainNode().getGrid();
        if (grid == null) return false;
        return !grid.getMachines(AutoBuilderCreativeSupplyBE.class).isEmpty();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        tickRedstonePulse(ticksSinceLastCall);
        var missing = buffer.tick(ticksSinceLastCall);
        if (missing != null) {
            this.missingItems = missing;
            this.isCrafting = false;
        }
        if (buffer.isFlushPending()) return TickRateModulation.URGENT;
        if (!isRunning || code.isEmpty() || isCrafting) return TickRateModulation.URGENT;
        if (tickPreRunChecks(ticksSinceLastCall)) return TickRateModulation.URGENT;
        tickDispatchInstructions();
        return TickRateModulation.URGENT;
    }

    private void tickRedstonePulse(int ticksSinceLastCall) {
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
    }

    private boolean tickPreRunChecks(int ticksSinceLastCall) {
        if (!energyPrepaid) {
            isRunning = false;
            beginFlushBuffer();
            return true;
        }
        if (inventory.getStackInSlot(0).isEmpty()) {
            isRunning = false;
            resetGhostToHome();
            beginFlushBuffer();
            return true;
        }
        if (tickDelayLeft > 0) {
            tickDelayLeft -= ticksSinceLastCall;
            return true;
        }
        return false;
    }

    private void tickDispatchInstructions() {
        boolean didWork = false;

        for (int steps = 0; steps < calcStepsPerTick() && currentInstruction < code.size(); steps++) {
            String inst = code.get(currentInstruction);

            if (inst.startsWith("Z|")) {
                tickDelayLeft = Integer.parseInt(inst.substring(2));
                currentInstruction++;
                return;
            }

            didWork = true;

            switch (inst) {
                case "F", "B", "L", "R", "U", "D" ->
                        setGhostRenderPos(stepRelative(getGhostRenderPos(), inst.charAt(0)));
                case "H" -> resetGhostToHome();
                case "X" -> {
                    if (executeBreak()) {
                        currentInstruction++;
                        return;
                    }
                }
                default -> {
                    if (inst.startsWith("P|")) {
                        if (tickInstructionPlace(inst.substring(2))) return;
                    } else if (inst.startsWith("PEQ|") || inst.startsWith("PNE|")) {
                        String[] parts = inst.substring(4).split("\\|", 2);
                        if (parts.length == 2) {
                            String checkId = parts[1].contains("[") ? parts[1].substring(0, parts[1].indexOf('[')) : parts[1];
                            Block checkBlock = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(checkId)).orElse(Blocks.AIR);
                            boolean matches = level.getBlockState(getGhostRenderPos()).getBlock() == checkBlock;
                            if ((inst.startsWith("PEQ|") ? matches : !matches) && tickInstructionPlace(parts[0])) return;
                        }
                    } else if (inst.startsWith("XEQ|") || inst.startsWith("XNE|")) {
                        String checkId = inst.substring(4);
                        if (checkId.contains("[")) checkId = checkId.substring(0, checkId.indexOf('['));
                        Block checkBlock = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(checkId)).orElse(Blocks.AIR);
                        boolean matches = level.getBlockState(getGhostRenderPos()).getBlock() == checkBlock;
                        if (inst.startsWith("XEQ|") ? matches : !matches) {
                            if (executeBreak()) { currentInstruction++; return; }
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

            if (!buffer.isEmpty()) {
                beginFlushBuffer();
            }
        } else if (didWork) {
            tickDelayLeft = this.delay;
        }
    }

    private boolean tickInstructionPlace(String blockIdRaw) {
        try {
            String blockIdClean;
            Map<String, String> props = new HashMap<>();
            int idx = blockIdRaw.indexOf('[');
            if (idx > 0 && blockIdRaw.endsWith("]")) {
                blockIdClean = blockIdRaw.substring(0, idx);
                String propString = blockIdRaw.substring(idx + 1, blockIdRaw.length() - 1);
                for (String pair : propString.split(",")) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) props.put(kv[0], kv[1]);
                }
            } else {
                blockIdClean = blockIdRaw;
            }

            Block block = BuiltInRegistries.BLOCK.getOptional(ResourceLocation.parse(blockIdClean))
                    .orElse(Blocks.AIR);
            if (block == Blocks.AIR) return false;

            var grid = getMainNode().getGrid();
            if (grid == null) return false;

            BlockPos target = getGhostRenderPos();
            if (level.getBlockState(target).getBlock() == block) return false;

            if (BuilderCoordMath.isBreakable(level.getBlockState(target), level, target)) {
                var drops = getSilkTouchDrops(level.getBlockState(target), (ServerLevel) level, target);
                long inserted = 0;
                for (var drop : drops) {
                    inserted += StorageHelper.poweredInsert(
                            grid.getEnergyService(), grid.getStorageService().getInventory(),
                            AEItemKey.of(drop.getItem()), 1,
                            IActionSource.ofMachine(this), Actionable.MODULATE
                    );
                }
                if (inserted <= 0 && !drops.isEmpty()) {
                    currentInstruction++;
                    return true;
                }
            }

            boolean creative = hasCreativeSupply();
            long extracted = 0;
            if (!creative) {
                AEItemKey key = AEItemKey.of(block.asItem());
                extracted = buffer.extract(key, 1);
                if (extracted <= 0) {
                    this.missingItems = new GenericStack(key, 1);
                    if (skipEmpty) {
                        currentInstruction++;
                        return true;
                    }
                    this.isRunning = false;
                    this.energyPrepaid = false;
                    beginFlushBuffer();
                    return true;
                }
            }

            if (extracted > 0 || creative) {
                BlockState state = block.defaultBlockState();
                for (Map.Entry<String, String> entry : props.entrySet()) {
                    Property<?> property = state.getBlock().getStateDefinition().getProperty(entry.getKey());
                    if (property != null) {
                        state = BuilderCoordMath.applyProperty(state, property, entry.getValue());
                    }
                }
                int delta = Math.floorMod(
                        BuilderCoordMath.yawStepsFromNorth(getFacing()) - BuilderCoordMath.yawStepsFromNorth(this.sourceFacing), 4);
                state = BuilderCoordMath.rotateStateByDelta(state, delta);
                level.setBlock(target, state, 3);
            }
        } catch (Exception ignored) {}
        return false;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return buffer.getRequestedJobs();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return buffer.insertCraftedItems(what, amount, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        buffer.jobStateChange(link);
    }

    private List<GenericStack> toGenericStackList(Map<String, Integer> required) {
        List<GenericStack> result = new ArrayList<>();
        for (var entry : required.entrySet()) {
            var block = BuiltInRegistries.BLOCK
                    .getOptional(ResourceLocation.parse(entry.getKey().split("\\[")[0]))
                    .orElse(null);
            if (block == null || block == net.minecraft.world.level.block.Blocks.AIR) continue;
            var key = AEItemKey.of(block.asItem());
            result.add(new GenericStack(key, (long) entry.getValue()));
        }
        return result;
    }

    public void onRedstoneActivate() {
        if (getLevel() == null) return;
        if (buffer.isFlushPending()) return;
        if (!buffer.getRequestedJobs().isEmpty()) return;

        if (inventory.getStackInSlot(0).isEmpty() && !inventory.getStackInSlot(1).isEmpty()) {
            inventory.setItemDirect(0, inventory.getStackInSlot(1).copyWithCount(1));
            inventory.setItemDirect(1, ItemStack.EMPTY);
        }

        loadCode();
        if (this.code.isEmpty()) {
            if (!buffer.isEmpty()) beginFlushBuffer();
            return;
        }
        var rawRequired = ProgramExpander.countUsedBlocks(String.join("/", this.code));
        var required = toGenericStackList(rawRequired);
        if (!hasCreativeSupply()) {
            buffer.setCanCraft(upgrades.isInstalled(AEItems.CRAFTING_CARD) && !isClientSide());
            buffer.collectFromNetwork(required, this::hasCreativeSupply);
            var missing = buffer.computeMissing(required, this::hasCreativeSupply);

            if (!missing.isEmpty() && buffer.requestCrafting(missing)) {
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

    private void beginFlushBuffer() {
        if (buffer.isEmpty()) return;
        this.isRunning = false;
        this.isCrafting = false;
        this.energyPrepaid = false;
        this.tickDelayLeft = 0;
        buffer.beginFlush();
        setChanged();
    }

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
            else if (inst.startsWith("PEQ|") || inst.startsWith("PNE|")) {
                requiredEnergyAE += calcStepCostAE(cursor);
            }
            else if (inst.startsWith("XEQ|") || inst.startsWith("XNE|")) {
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

        var program = ProgramExpander.expand(BuilderPatternHost.loadProgramFromFile(s, getLevel() != null ? getLevel().getServer() : null));
        if (program.success) {
            this.code = program.program;
        } else {
            this.code.clear();
        }

        Direction d = net.oktawia.crazyae2addons.items.BuilderPatternItem.getSourceFacing(s);
        this.sourceFacing = d != null ? d : Direction.NORTH;
        this.delay = net.oktawia.crazyae2addons.items.BuilderPatternItem.getDelay(s);
    }

    public void togglePreview() {
        this.previewEnabled = !this.previewEnabled;

        if (this.previewEnabled) {
            if (this.code.isEmpty()) {
                ItemStack s = this.inventory.getStackInSlot(0);
                if (s.isEmpty()) s = this.inventory.getStackInSlot(1);
                if (!s.isEmpty()) {
                    var res = ProgramExpander.expand(BuilderPatternHost.loadProgramFromFile(s, getLevel() != null ? getLevel().getServer() : null));
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

    public void updateSkipEmptyFromCode() {
        if (this.code.isEmpty()) {
            this.skipEmpty = false;
            return;
        }
        boolean hasConditionals = ProgramExpander.hasConditionalInstructions(String.join("/", this.code));
        this.skipEmpty = hasConditionals;
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
                if ("FBLRUD".indexOf(c) >= 0) localCursor = BuilderCoordMath.stepLocal(localCursor, c);
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
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_FACING)) {
            return state.getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        }
        return Direction.NORTH;
    }

    private BlockPos localToWorldOffset(BlockPos local) {
        return BuilderCoordMath.localToWorldOffset(local, getFacing());
    }

    private BlockPos stepRelative(BlockPos pos, char cmd) {
        return BuilderCoordMath.stepRelative(pos, cmd, getFacing());
    }

    private boolean executeBreak() {
        var grid = getMainNode().getGrid();
        boolean didDestroy = false;
        BlockPos pos = getGhostRenderPos();

        if (grid != null) {
            BlockState state = level.getBlockState(pos);
            if (!state.isAir() && BuilderCoordMath.isBreakable(state, level, pos)) {
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
                    if (getLevel().destroyBlock(pos, false)) {
                        didDestroy = true;
                    }
                }
            }

            var fs = getLevel().getFluidState(pos);
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
                if (getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3)) {
                    didDestroy = true;
                }
            }
        }

        if (didDestroy) {
            tickDelayLeft = Math.max(tickDelayLeft, CrazyConfig.COMMON.AutobuilderMineDelay.get());
        }
        return didDestroy;
    }

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
