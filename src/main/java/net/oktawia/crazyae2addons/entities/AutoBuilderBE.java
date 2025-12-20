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
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.me.helpers.MachineSource;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.google.common.collect.ImmutableSet;
import com.mojang.logging.LogUtils;
import it.unimi.dsi.fastutil.objects.Object2LongMap;
import it.unimi.dsi.fastutil.objects.Object2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.*;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.blocks.EjectorBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IHackedProvider;
import net.oktawia.crazyae2addons.logic.HackedPatternProviderLogic;
import net.oktawia.crazyae2addons.menus.AutoBuilderMenu;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Future;

public class AutoBuilderBE extends AENetworkInvBlockEntity implements
        IGridTickable, MenuProvider, InternalInventoryHost, IUpgradeableObject, ICraftingRequester, PatternProviderLogicHost, IHackedProvider {

    public IUpgradeInventory upgrades = UpgradeInventories.forMachine(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 7, this::setChanged);
    public Integer delay = 20;
    public BlockPos offset = new BlockPos(0, 2, 0);
    private BlockPos ghostRenderPos = worldPosition.above().above();
    private List<String> code = new ArrayList<>();
    private int currentInstruction = 0;
    private int tickDelayLeft = 0;
    private boolean isRunning = false;
    public AppEngInternalInventory inventory = new AppEngInternalInventory(this, 2);
    public int redstonePulseTicks = 0;

    // true = czekamy na craft (pushPattern dostarczy inputy)
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

    public static final java.util.List<AutoBuilderBE> CLIENT_INSTANCES = new java.util.concurrent.CopyOnWriteArrayList<>();
    private PreviewInfo previewInfo;

    private boolean previewDirty = true;
    private AutoBuilderMenu menu;
    private ICraftingLink craftingLink;
    private PatternProviderLogic logic;
    private GenericStack target;
    private Future<ICraftingPlan> toCraftPlan;

    // ------------------------------
    // Infinite temp buffer (reserved build storage) + flush state
    // ------------------------------
    private static final String NBT_BUILD_BUFFER = "BuildBuffer";

    private final Object2LongOpenHashMap<AEItemKey> buildBuffer = new Object2LongOpenHashMap<>();

    // Jeśli true -> próbujemy opróżnić bufor do ME. Dopóki nie opróżnione: nie wolno requestować/pobierać.
    private boolean flushPending = false;
    private int flushTickAcc = 0; // retry co sekundę (20 ticków)

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

    public AutoBuilderBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.AUTO_BUILDER_BE.get(), pos, state);
        this.logic = new HackedPatternProviderLogic(getMainNode(), this);

        buildBuffer.defaultReturnValue(0L);

        getMainNode()
                .addService(IGridTickable.class, this)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get().asItem())
                );

        this.inventory.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return stack.getItem().equals(CrazyItemRegistrar.BUILDER_PATTERN.get().asItem());
            }
        });
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
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.remove(this);
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        var inv = getUpgrades();
        for (var stack : inv) {
            var genericStack = GenericStack.unwrapItemStack(stack);
            if (genericStack != null) {
                genericStack.what().addDrops(
                        genericStack.amount(),
                        drops,
                        level,
                        pos);
            } else {
                drops.add(stack);
            }
        }

        // (opcjonalnie) bufor też mógłby dropić, ale to akurat "tymczasowy" magazyn – ja go ZAWSZE flushuję
        // więc nie dodaję dropsów z bufora.
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            return LazyOptional.of(() -> new IItemHandler() {
                @Override
                public int getSlots() {
                    return 2;
                }

                @Override
                public @NotNull ItemStack getStackInSlot(int slot) {
                    return inventory.getStackInSlot(slot);
                }

                @Override
                public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (slot == 0 && inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty() && stack.getItem() == CrazyItemRegistrar.BUILDER_PATTERN.get()) {
                        return inventory.insertItem(0, stack, simulate);
                    }
                    return stack;
                }

                @Override
                public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    if (slot == 1 && !isRunning) {
                        return inventory.extractItem(1, amount, simulate).copy();
                    }
                    return ItemStack.EMPTY;
                }

                @Override
                public int getSlotLimit(int slot) {
                    return 1;
                }

                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    return slot == 0 && stack.getItem() == CrazyItemRegistrar.BUILDER_PATTERN.get();
                }
            }).cast();
        }
        return super.getCapability(cap, side);
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.inventory;
        } else if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        this.upgrades.readFromNBT(tag, "upgrades");
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
            previewIndices = tag.getIntArray(tag.contains("previewIndices") ? "previewIndices" : "");
        } else {
            previewIndices = new int[0];
        }

        buildBuffer.clear();
        if (tag.contains(NBT_BUILD_BUFFER, Tag.TAG_LIST)) {
            ListTag list = tag.getList(NBT_BUILD_BUFFER, Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                CompoundTag e = list.getCompound(i);
                if (!e.contains("Stack", Tag.TAG_COMPOUND)) continue;

                ItemStack s = ItemStack.of(e.getCompound("Stack"));
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
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.upgrades.writeToNBT(tag, "upgrades");

        tag.putInt("currentInstruction", this.currentInstruction);
        tag.putInt("tickDelayLeft", this.tickDelayLeft);
        tag.putBoolean("isRunning", this.isRunning);
        tag.putLong("GhostPos", ghostRenderPos.asLong());
        tag.putLong("offset", this.offset.asLong());
        tag.putBoolean("previewEnabled", previewEnabled);
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

        // Save build buffer
        ListTag buf = new ListTag();
        for (Object2LongMap.Entry<AEItemKey> e : buildBuffer.object2LongEntrySet()) {
            if (e.getLongValue() <= 0) continue;

            CompoundTag t = new CompoundTag();
            ItemStack s = e.getKey().toStack(1);
            t.put("Stack", s.save(new CompoundTag()));
            t.putLong("Amount", e.getLongValue());
            buf.add(t);
        }
        tag.put(NBT_BUILD_BUFFER, buf);
    }

    private void triggerRedstonePulse() {
        redstonePulseTicks = 1;
        if (!level.isClientSide) {
            level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        }
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    private boolean hasCreativeSupply() {
        var node = getMainNode();
        if (node == null) return false;
        var grid = node.getGrid();
        if (grid == null) return false;
        return !grid.getMachines(AutoBuilderCreativeSupplyBE.class).isEmpty();
    }


    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();

        tag.remove(NBT_BUILD_BUFFER);

        tag.putLong("GhostPos", getGhostRenderPos() != null ? getGhostRenderPos().asLong() : BlockPos.ZERO.asLong());
        tag.putBoolean("previewEnabled", previewEnabled);

        ListTag posList = new ListTag();
        for (int i = 0; i < Math.min(previewPositions.size(), PREVIEW_LIMIT); i++) {
            posList.add(LongTag.valueOf(previewPositions.get(i).asLong()));
        }
        tag.put("previewPositions", posList);

        ListTag palList = new ListTag();
        for (String s : previewPalette) palList.add(StringTag.valueOf(s));
        tag.put("previewPalette", palList);

        tag.put("previewIndices", new IntArrayTag(previewIndices));

        tag.put("Inv", writeInventoryToTag());

        return tag;
    }


    @Override
    public void handleUpdateTag(CompoundTag tag) {
        super.handleUpdateTag(tag);

        if (tag.contains("GhostPos")) {
            this.ghostRenderPos = BlockPos.of(tag.getLong("GhostPos"));
        }
        this.previewEnabled = tag.getBoolean("previewEnabled");

        previewPositions.clear();
        if (tag.contains("previewPositions", Tag.TAG_LIST)) {
            ListTag list = tag.getList("previewPositions", Tag.TAG_LONG);
            for (int i = 0; i < list.size(); i++) {
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

        if (tag.contains("Inv", Tag.TAG_COMPOUND)) {
            readInventoryFromTag(tag.getCompound("Inv"));
        }

        this.setPreviewDirty(true);
        if (level != null && level.isClientSide) this.setPreviewInfo(null);
    }

    @Override
    public @Nullable ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        CompoundTag tag = pkt.getTag();
        handleUpdateTag(tag);
    }

    private static BlockPos stepLocal(BlockPos pos, char cmd) {
        return switch (cmd) {
            case 'F' -> pos.offset(0, 0, 1);
            case 'B' -> pos.offset(0, 0, -1);
            case 'R' -> pos.offset(1, 0, 0);
            case 'L' -> pos.offset(-1, 0, 0);
            case 'U' -> pos.offset(0, 1, 0);
            case 'D' -> pos.offset(0, -1, 0);
            default -> pos;
        };
    }

    public boolean isPreviewDirty() {
        return previewDirty;
    }

    public void setPreviewDirty(boolean dirty) {
        this.previewDirty = dirty;
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

            if (inst.equals("H")) {
                localCursor = BlockPos.ZERO;
                continue;
            }

            if (inst.length() == 1) {
                char c = inst.charAt(0);
                if ("FBLRUD".indexOf(c) >= 0) {
                    localCursor = stepLocal(localCursor, c);
                }
                continue;
            }

            int paletteIndex = -1;

            if (inst.startsWith("P|")) {
                String blockKey = inst.substring(2);
                int idx = previewPalette.indexOf(blockKey);
                if (idx < 0) {
                    previewPalette.add(blockKey);
                    idx = previewPalette.size() - 1;
                }
                paletteIndex = idx;
            } else if (inst.startsWith("P(") && inst.endsWith(")")) {
                try {
                    int id = Integer.parseInt(inst.substring(2, inst.length() - 1));
                    if (id >= 1 && id <= previewPalette.size()) {
                        paletteIndex = id - 1;
                    } else {
                        continue;
                    }
                } catch (NumberFormatException ignored) {
                    continue;
                }
            } else {
                continue;
            }

            BlockPos localTotal = new BlockPos(
                    offset.getX() + localCursor.getX(),
                    offset.getY() + localCursor.getY(),
                    offset.getZ() + localCursor.getZ()
            );

            BlockPos worldPos = transformRelative(localTotal);

            previewPositions.add(worldPos);
            indices.add(paletteIndex);
        }

        previewIndices = indices.toIntArray();
        this.setPreviewDirty(true);
        if (level != null && level.isClientSide) this.setPreviewInfo(null);
    }

    public void togglePreview() {
        this.previewEnabled = !this.previewEnabled;

        if (this.previewEnabled) {
            if (this.code.isEmpty()) {
                ItemStack s = this.inventory.getStackInSlot(0);
                if (s.isEmpty()) s = this.inventory.getStackInSlot(1);
                if (!s.isEmpty()) {
                    var res = ProgramExpander.expand(loadProgramFromFile(s, getLevel().getServer()));
                    if (res.success) this.code = res.program;
                }
            }
            rebuildPreviewFromCode();
        } else {
            previewPositions.clear();
            previewPalette.clear();
            previewIndices = new int[0];
        }

        setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        this.setPreviewDirty(true);
        if (level != null && level.isClientSide) this.setPreviewInfo(null);
    }

    private CompoundTag writeInventoryToTag() {
        CompoundTag root = new CompoundTag();
        ListTag list = new ListTag();
        for (int i = 0; i < this.inventory.size(); i++) {
            ItemStack stack = this.inventory.getStackInSlot(i);
            if (!stack.isEmpty()) {
                CompoundTag entry = new CompoundTag();
                entry.putByte("Slot", (byte) i);
                entry.put("Stack", stack.save(new CompoundTag()));
                list.add(entry);
            }
        }
        root.put("Items", list);
        return root;
    }

    private void readInventoryFromTag(CompoundTag root) {
        if (root == null || !root.contains("Items")) return;

        for (int i = 0; i < this.inventory.size(); i++) {
            this.inventory.setItemDirect(i, ItemStack.EMPTY);
        }

        ListTag list = root.getList("Items", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            int slot = entry.getByte("Slot") & 0xFF;
            ItemStack stack = ItemStack.of(entry.getCompound("Stack"));
            if (slot >= 0 && slot < this.inventory.size()) {
                this.inventory.setItemDirect(slot, stack);
            }
        }
    }

    public void setGhostRenderPos(BlockPos pos) {
        this.ghostRenderPos = pos;
        if (!level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Nullable
    public BlockPos getGhostRenderPos() {
        return ghostRenderPos;
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    private double calcStepCostAE(BlockPos target) {
        double dx = target.getX() - worldPosition.getX();
        double dy = target.getY() - worldPosition.getY();
        double dz = target.getZ() - worldPosition.getZ();
        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        return distance * CrazyConfig.COMMON.AutobuilderCostMult.get();
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

            if (inst.equals("H")) {
                cursor = homePos();
                continue;
            }

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

    // ------------------------------
    // Buffer helpers + flush logic
    // ------------------------------
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

        // Stop wszystkiego
        this.isRunning = false;
        this.isCrafting = false;
        this.energyPrepaid = false;
        this.tickDelayLeft = 0;

        // Anuluj craft kalkulacje/linki
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
        var node = getMainNode();
        if (node == null || node.getGrid() == null) return false;

        var grid = node.getGrid();
        var inv = grid.getStorageService().getInventory();
        var es = grid.getEnergyService();
        var src = IActionSource.ofMachine(this);

        boolean allMoved = true;

        var it = buildBuffer.object2LongEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            var key = e.getKey();
            long amt = e.getLongValue();
            if (amt <= 0) {
                it.remove();
                continue;
            }

            long inserted = StorageHelper.poweredInsert(es, inv, key, amt, src, Actionable.MODULATE);
            if (inserted >= amt) {
                it.remove();
            } else {
                e.setValue(amt - inserted);
                allMoved = false;
            }
        }

        return allMoved || buildBuffer.isEmpty();
    }

    private void reserveFromNetwork(Map<String, Integer> requiredBlocks) {
        if (getGridNode() == null || getGridNode().getGrid() == null) return;

        if (hasCreativeSupply()) return;

        var grid = getGridNode().getGrid();
        var storage = grid.getStorageService().getInventory();
        var es = grid.getEnergyService();
        var src = IActionSource.ofMachine(this);

        for (var entry : requiredBlocks.entrySet()) {
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getKey().split("\\[")[0]));
            if (block == null || block == Blocks.AIR) continue;

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
            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(entry.getKey().split("\\[")[0]));
            if (block == null || block == Blocks.AIR) continue;

            var key = AEItemKey.of(block.asItem());
            long need = (long) entry.getValue() - bufferGet(key);
            if (need > 0) missing.add(new GenericStack(key, need));
        }
        return missing;
    }


    public void addToBuildBuffer(AEItemKey key, long amount) {
        if (amount <= 0) return;
        if (flushPending && getMainNode() != null && getMainNode().getGrid() != null) {
            var grid = getMainNode().getGrid();
            long inserted = StorageHelper.poweredInsert(
                    grid.getEnergyService(),
                    grid.getStorageService().getInventory(),
                    key,
                    amount,
                    IActionSource.ofMachine(this),
                    Actionable.MODULATE
            );
            long left = amount - inserted;
            if (left > 0) bufferAdd(key, left);
            return;
        }

        bufferAdd(key, amount);
    }

    public void cancelCraftNoFlush() {
        if (this.craftingLink != null) {
            try { this.craftingLink.cancel(); } catch (Throwable ignored) {}
        }
        this.getLogic().getPatternInv().clear();
        this.getLogic().updatePatterns();
        this.craftingLink = null;
        this.toCraftPlan = null;
        this.isCrafting = false;
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {

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

        // 3) Obsługa ukończonej kalkulacji crafta (submitJob)
        if (this.toCraftPlan != null && this.toCraftPlan.isDone()) {
            try {
                var plan = this.toCraftPlan.get();
                this.toCraftPlan = null;

                var grid = getGridNode() != null ? getGridNode().getGrid() : null;
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
                    this.getLogic().getPatternInv().setItemDirect(0, ItemStack.EMPTY);
                    this.getLogic().updatePatterns();

                    Object2LongMap.Entry<AEKey> firstMissing = null;
                    try {
                        KeyCounter missing = plan.missingItems();
                        if (missing != null && !missing.isEmpty()) {
                            firstMissing = missing.iterator().next();
                        }
                        if (firstMissing != null) {
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

                            Block block = ForgeRegistries.BLOCKS.getValue(new ResourceLocation(blockIdClean));
                            if (block != null && block != Blocks.AIR) {
                                var grid = getMainNode().getGrid();
                                if (grid != null) {
                                    BlockPos target = getGhostRenderPos();

                                    if (isBreakable(level.getBlockState(getGhostRenderPos()), level, getGhostRenderPos())) {
                                        var drops = getSilkTouchDrops(level.getBlockState(getGhostRenderPos()), (ServerLevel) level, getGhostRenderPos());
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

                                    boolean creative = !getMainNode().getGrid().getMachines(AutoBuilderCreativeSupplyBE.class).isEmpty();

                                    long extracted = 0;
                                    if (!creative) {
                                        AEItemKey key = AEItemKey.of(block.asItem());

                                        // BUILD STRICT: tylko bufor. Jeśli brak -> stop + flush.
                                        extracted = bufferExtract(key, 1);
                                        if (extracted <= 0) {
                                            this.missingItems = new GenericStack(key, 1);
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

                                        int delta = Math.floorMod(yawStepsFromNorth(getFacing()) - yawStepsFromNorth(this.sourceFacing), 4);
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

            // Jak coś zostało w buforze (np craft “wyszedł więcej”) -> flush
            if (!buildBuffer.isEmpty()) {
                beginFlushBuffer();
            }

        } else if (didWork) {
            tickDelayLeft = this.delay;
        }

        if (redstonePulseTicks > 0) {
            redstonePulseTicks -= ticksSinceLastCall;
            if (redstonePulseTicks <= 0) {
                if (!level.isClientSide) {
                    level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
                }
            }
        }
        return TickRateModulation.URGENT;
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
        Direction f = getFacing();
        int fx, fz, rx, rz;
        switch (f) {
            case SOUTH -> {
                fx = 0;
                fz = 1;
                rx = -1;
                rz = 0;
            }
            case EAST -> {
                fx = 1;
                fz = 0;
                rx = 0;
                rz = 1;
            }
            case WEST -> {
                fx = -1;
                fz = 0;
                rx = 0;
                rz = -1;
            }
            default -> {
                fx = 0;
                fz = -1;
                rx = 1;
                rz = 0;
            }
        }
        int wx = local.getX() * rx + local.getZ() * fx;
        int wy = local.getY();
        int wz = local.getX() * rz + local.getZ() * fz;
        return new BlockPos(wx, wy, wz);
    }

    private BlockPos homePos() {
        return transformRelative(offset);
    }

    private BlockPos stepRelative(BlockPos pos, char cmd) {
        Direction f = getFacing();
        int fx, fz, rx, rz;
        switch (f) {
            case SOUTH -> {
                fx = 0;
                fz = 1;
                rx = -1;
                rz = 0;
            }
            case EAST -> {
                fx = 1;
                fz = 0;
                rx = 0;
                rz = 1;
            }
            case WEST -> {
                fx = -1;
                fz = 0;
                rx = 0;
                rz = -1;
            }
            default -> {
                fx = 0;
                fz = -1;
                rx = 1;
                rz = 0;
            }
        }
        return switch (cmd) {
            case 'F' -> pos.offset(fx, 0, fz);
            case 'B' -> pos.offset(-fx, 0, -fz);
            case 'R' -> pos.offset(rx, 0, rz);
            case 'L' -> pos.offset(-rx, 0, -rz);
            case 'U' -> pos.offset(0, 1, 0);
            case 'D' -> pos.offset(0, -1, 0);
            default -> pos;
        };
    }

    public void resetGhostToHome() {
        setGhostRenderPos(homePos());
    }

    public static List<ItemStack> getSilkTouchDrops(BlockState state, ServerLevel level, BlockPos pos) {
        ItemStack silkTool = new ItemStack(Items.DIAMOND_PICKAXE);
        silkTool.enchant(Enchantments.SILK_TOUCH, 1);

        var lootParams = new LootParams.Builder(level)
                .withParameter(LootContextParams.TOOL, silkTool)
                .withParameter(LootContextParams.ORIGIN, pos.getCenter())
                .withParameter(LootContextParams.BLOCK_STATE, state);

        return state.getDrops(lootParams);
    }

    private static <T extends Comparable<T>> BlockState applyProperty(BlockState state, Property<T> property, String valueStr) {
        try {
            if (property instanceof BooleanProperty) {
                Boolean boolVal = Boolean.parseBoolean(valueStr);
                return state.setValue((BooleanProperty) property, boolVal);
            }
            if (property instanceof IntegerProperty) {
                Integer intVal = Integer.parseInt(valueStr);
                return state.setValue((IntegerProperty) property, intVal);
            }
            Optional<T> value = property.getValue(valueStr);
            if (value.isPresent()) {
                return state.setValue(property, value.get());
            }
        } catch (Exception ignored) {
        }
        return state;
    }

    public static boolean isBreakable(BlockState state, Level level, BlockPos pos) {
        return state.getDestroySpeed(level, pos) >= 0;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new AutoBuilderMenu(i, inventory, this);
    }

    @Override
    public PatternProviderLogic getLogic() {
        return this.logic;
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.allOf(Direction.class);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(), player, locator);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get());
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    public static String loadProgramFromFile(ItemStack stack, MinecraftServer server) {
        if (!stack.hasTag() || !stack.getTag().contains("program_id") || server == null) return "";

        String id = stack.getTag().getString("program_id");
        Path file = server.getWorldPath(new LevelResource("serverdata"))
                .resolve("autobuilder")
                .resolve(id);

        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LogUtils.getLogger().info(e.toString());
            return "";
        }
    }

    public void loadCode() {
        ItemStack s = this.inventory.getStackInSlot(0);
        if (s.isEmpty()) s = this.inventory.getStackInSlot(1);

        if (s.isEmpty()) {
            this.code.clear();
            return;
        }

        var tag = s.getOrCreateTag();
        if (tag.contains("code") && tag.getBoolean("code")) {
            var program = ProgramExpander.expand(loadProgramFromFile(s, getLevel().getServer()));
            if (program.success) {
                this.code = program.program;
            }
        }
        if (tag.contains("delay")) {
            this.delay = tag.getInt("delay");
        }

        if (tag.contains("srcFacing")) {
            Direction d = Direction.byName(tag.getString("srcFacing"));
            if (d != null) this.sourceFacing = d;
            else this.sourceFacing = Direction.NORTH;
        } else {
            this.sourceFacing = Direction.NORTH;
        }
    }

    public void onRedstoneActivate() {
        if (getLevel() == null) return;
        if (flushPending) return;
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

            if (!missing.isEmpty() && isUpgradedWith(AEItems.CRAFTING_CARD) && !isClientSide()) {
                var tag = new CompoundTag();
                tag.putString("id", UUID.randomUUID().toString());
                this.target = new GenericStack(AEItemKey.of(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), tag), 1);

                var pattern = PatternDetailsHelper.encodeProcessingPattern(
                        missing.toArray(new GenericStack[0]),
                        new GenericStack[]{target}
                );

                this.getLogic().getPatternInv().setItemDirect(0, pattern);
                this.getLogic().updatePatterns();

                this.toCraftPlan = getGridNode().getGrid().getCraftingService().beginCraftingCalculation(
                        getLevel(),
                        () -> new MachineSource(this),
                        target.what(),
                        target.amount(),
                        CalculationStrategy.REPORT_MISSING_ITEMS
                );

                this.isCrafting = true;
                this.isRunning = false;
                this.energyPrepaid = false;
                return;
            }

            if (!missing.isEmpty()) {
                this.missingItems = missing.get(0);
                beginFlushBuffer();
                return;
            }
        } else {
            this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
            this.isCrafting = false;
        }

        recalculateRequiredEnergy();

        var node = getMainNode();
        if (node == null || node.getGrid() == null) {
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

    @Override
    public InternalInventory getInternalInventory() {
        return this.inventory;
    }

    public void setMenu(AutoBuilderMenu menu) {
        this.menu = menu;
    }

    public AutoBuilderMenu getMenu() {
        return this.menu;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        if (this.isPreviewEnabled()) {
            this.togglePreview();
            if (this.getMenu() != null) {
                this.getMenu().preview = false;
            }
        }
        this.setChanged();
        loadCode();
        recalculateRequiredEnergy();
        if (getMenu() != null) {
            getMenu().pushEnergyDisplay();
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

    private static int yawStepsFromNorth(Direction d) {
        return switch (d) {
            case NORTH -> 0;
            case EAST -> 1;
            case SOUTH -> 2;
            case WEST -> 3;
            default -> 0;
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
                if (isHorizontal(d)) {
                    state = state.setValue(dp, rotateHorizontal(d, steps));
                }
            }
        }
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            var ax = state.getValue(BlockStateProperties.AXIS);
            if (ax.isHorizontal() && (steps % 2 != 0)) {
                state = state.setValue(BlockStateProperties.AXIS, ax == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            }
        }
        if (state.hasProperty(BlockStateProperties.HORIZONTAL_AXIS)) {
            var ax = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
            if (ax.isHorizontal() && (steps % 2 != 0)) {
                state = state.setValue(BlockStateProperties.HORIZONTAL_AXIS, ax == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            }
        }
        if (state.hasProperty(BlockStateProperties.ROTATION_16)) {
            int val = state.getValue(BlockStateProperties.ROTATION_16);
            val = (val + steps * 4) & 15;
            state = state.setValue(BlockStateProperties.ROTATION_16, val);
        }
        return state;
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return ImmutableSet.of(this.craftingLink);
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return 0;
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get().asItem().getDefaultInstance();
    }

    @Override
    public void cancelCraft() {
        cancelCraftNoFlush();
        beginFlushBuffer();
    }
}
