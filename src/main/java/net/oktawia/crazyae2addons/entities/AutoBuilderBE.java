package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.GenericStack;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import com.google.common.collect.ImmutableSet;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.client.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.defs.components.AEItemBufferData;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;
import net.oktawia.crazyae2addons.logic.builder.AutoBuilderPreviewOps;
import net.oktawia.crazyae2addons.logic.builder.AutoBuilderWorldOps;
import net.oktawia.crazyae2addons.logic.builder.BuilderPatternHost;
import net.oktawia.crazyae2addons.menus.AutoBuilderMenu;
import net.oktawia.crazyae2addons.misc.ProgramExpander;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class AutoBuilderBE extends AENetworkedBlockEntity implements
        IGridTickable, MenuProvider, InternalInventoryHost, IUpgradeableObject,
        ICraftingRequester, PatternProviderLogicHost, ISyncPersistRPCBlockEntity {

    @Getter
    private final FieldManagedStorage syncStorage;

    @Getter
    @Persisted
    public final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 7, this::setChanged
    );

    @Persisted
    public final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 2);

    @Persisted
    @DescSynced
    public int delay = 20;

    @Persisted
    @DescSynced
    public BlockPos offset = new BlockPos(0, 2, 0);

    @Getter
    @Persisted
    @DescSynced
    public BlockPos ghostRenderPos;

    public List<String> code = new ArrayList<>();

    @Persisted
    @DescSynced
    public int currentInstruction = 0;

    @Persisted
    @DescSynced
    public int tickDelayLeft = 0;

    @Persisted
    @DescSynced
    public boolean isRunning = false;

    @Getter
    @DescSynced
    public int redstonePulseTicks = 0;

    @Getter
    @DescSynced
    public boolean isPulsing = false;

    @Persisted
    @DescSynced
    public boolean isCrafting = false;

    public GenericStack missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);

    @Getter
    @DescSynced
    private ItemStack missingItemStack = ItemStack.EMPTY;

    @Getter
    @DescSynced
    private long missingItemAmount = 0;

    @Persisted
    @DescSynced
    public boolean skipEmpty = false;

    public final int PREVIEW_LIMIT = CrazyConfig.COMMON.AutobuilderPreviewLimit.get();

    @DescSynced
    public double requiredEnergyAE = 0.0D;

    @Persisted
    public boolean energyPrepaid = false;

    @DescSynced
    @Getter
    public BlockPos[] previewPositions = new BlockPos[0];

    @DescSynced
    @Getter
    public String[] previewPalette = new String[0];

    @DescSynced
    @Getter
    public int[] previewIndices = new int[0];

    @Persisted
    @DescSynced
    @Getter
    public boolean previewEnabled = false;

    @Persisted
    @DescSynced
    @Getter
    public Direction sourceFacing = Direction.NORTH;

    public static final List<AutoBuilderBE> CLIENT_INSTANCES = new java.util.concurrent.CopyOnWriteArrayList<>();

    @Setter
    @Getter
    @OnlyIn(Dist.CLIENT)
    public PreviewInfo previewInfo;

    @Getter
    @Setter
    @DescSynced
    public boolean previewDirty = true;

    @Persisted
    public final ManagedBuffer buffer;

    public AutoBuilderBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.AUTO_BUILDER_BE.get(), pos, state);

        this.ghostRenderPos = pos.above().above();
        this.syncStorage = new FieldManagedStorage(this);

        this.buffer = new ManagedBuffer(
                getMainNode(),
                this,
                this,
                this::setChanged,
                this::onRedstoneActivate,
                () -> isRunning || isCrafting
        );

        getMainNode()
                .addService(IGridTickable.class, this)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get().asItem())
                );

        this.inventory.setFilter(new appeng.util.inv.filter.IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return slot == 0 && stack.getItem().equals(CrazyItemRegistrar.BUILDER_PATTERN.get().asItem());
            }
        });
    }

    public void clearMissingItem() {
        this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
        this.missingItemStack = ItemStack.EMPTY;
        this.missingItemAmount = 0;
        setChanged();
    }

    public void setMissingItem(GenericStack stack) {
        this.missingItems = stack != null ? stack : GenericStack.fromItemStack(ItemStack.EMPTY);

        if (stack == null || stack.what() == null) {
            this.missingItemStack = ItemStack.EMPTY;
            this.missingItemAmount = 0;
            setChanged();
            return;
        }

        if (stack.what() instanceof AEItemKey itemKey) {
            this.missingItemStack = itemKey.toStack(1);
            this.missingItemAmount = stack.amount();
        } else {
            this.missingItemStack = ItemStack.EMPTY;
            this.missingItemAmount = 0;
        }

        setChanged();
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
        clearMissingItem();
        this.previewEnabled = false;
        this.setChanged();

        loadCode();
        recalculateRequiredEnergy();

        if (inventory.getStackInSlot(0).isEmpty() && inventory.getStackInSlot(1).isEmpty()) {
            isRunning = false;
            isCrafting = false;
            code = new ArrayList<>();
            currentInstruction = 0;
            tickDelayLeft = 0;
            energyPrepaid = false;
            requiredEnergyAE = 0.0D;
            resetGhostToHome();

            previewPositions = new BlockPos[0];
            previewPalette = new String[0];
            previewIndices = new int[0];
            previewDirty = true;

            if (level != null && level.isClientSide) {
                previewInfo = null;
            }
        } else if (previewEnabled) {
            AutoBuilderPreviewOps.rebuildPreviewFromCode(this);
        }

        if (!isRunning && !isCrafting && !buffer.isEmpty()) {
            AutoBuilderWorldOps.beginFlushBuffer(this);
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

    @Override
    public void onLoad() {
        super.onLoad();

        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.add(this);
            this.previewDirty = true;
            this.previewInfo = null;
            return;
        }

        if (level != null && !level.isClientSide) {
            loadCode();
            recalculateRequiredEnergy();

            if (previewEnabled && (!inventory.getStackInSlot(0).isEmpty() || !inventory.getStackInSlot(1).isEmpty())) {
                AutoBuilderPreviewOps.rebuildPreviewFromCode(this);
            }
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
            if (!s.isEmpty()) {
                drops.add(s);
            }
        }
    }

    @Override
    public void loadTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadTag(tag, registries);

        this.upgrades.readFromNBT(tag, "myupgrades", registries);
        this.inventory.readFromNBT(tag, "inv", registries);
        buffer.getLogic().readFromNBT(tag, registries);

        if (tag.contains("buffer", Tag.TAG_COMPOUND)) {
            AEItemBufferData b = AEItemBufferData.CODEC
                    .parse(NbtOps.INSTANCE, tag.getCompound("buffer"))
                    .getOrThrow();
            buffer.fromData(b);
            if (!b.flushPending() && !buffer.isEmpty() && !isRunning && !isCrafting) {
                buffer.beginFlush();
            }
        }

        this.previewDirty = true;
        if (level != null && level.isClientSide) {
            this.previewInfo = null;
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        this.upgrades.writeToNBT(tag, "myupgrades", registries);
        this.inventory.writeToNBT(tag, "inv", registries);
        buffer.getLogic().writeToNBT(tag, registries);

        tag.put("buffer", AEItemBufferData.CODEC
                .encodeStart(NbtOps.INSTANCE, buffer.toData())
                .getOrThrow());
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        return AutoBuilderWorldOps.tickingRequest(this, node, ticksSinceLastCall);
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

    public void onRedstoneActivate() {
        AutoBuilderWorldOps.onRedstoneActivate(this);
    }

    public void recalculateRequiredEnergy() {
        AutoBuilderWorldOps.recalculateRequiredEnergy(this);
    }

    public void loadCode() {
        ItemStack s = this.inventory.getStackInSlot(0);
        if (s.isEmpty()) {
            s = this.inventory.getStackInSlot(1);
        }

        if (s.isEmpty()) {
            this.code.clear();
            return;
        }

        String programId = net.oktawia.crazyae2addons.items.BuilderPatternItem.getProgramId(s);
        if (programId == null) {
            this.code.clear();
            return;
        }

        var program = ProgramExpander.expand(
                BuilderPatternHost.loadProgramFromFile(
                        s,
                        getLevel() != null ? getLevel().getServer() : null
                )
        );

        if (program.success && program.program != null) {
            this.code = new ArrayList<>(program.program);
        } else {
            this.code.clear();
        }

        Direction d = net.oktawia.crazyae2addons.items.BuilderPatternItem.getSourceFacing(s);
        this.sourceFacing = d != null ? d : Direction.NORTH;
        this.delay = net.oktawia.crazyae2addons.items.BuilderPatternItem.getDelay(s);
        setChanged();
    }

    public void togglePreview() {
        AutoBuilderPreviewOps.togglePreview(this);
    }

    public void updateSkipEmptyFromCode() {
        if (this.code.isEmpty()) {
            this.skipEmpty = false;
            setChanged();
            return;
        }
        this.skipEmpty = ProgramExpander.hasConditionalInstructions(String.join("/", this.code));
        setChanged();
    }

    public void setGhostRenderPos(BlockPos pos) {
        AutoBuilderPreviewOps.setGhostRenderPos(this, pos);
    }

    public void onOffsetChanged(BlockPos oldOffset) {
        AutoBuilderPreviewOps.onOffsetChanged(this, oldOffset);
    }

    public void resetGhostToHome() {
        AutoBuilderPreviewOps.resetGhostToHome(this);
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AutoBuilderMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }
}