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
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.google.common.collect.ImmutableSet;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.client.renderer.preview.builder.PreviewInfo;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.BuilderPatternItem;
import net.oktawia.crazyae2addons.logic.autobuilder.AutoBuilderPreviewOps;
import net.oktawia.crazyae2addons.logic.autobuilder.AutoBuilderWorldOps;
import net.oktawia.crazyae2addons.logic.autobuilder.BuilderPatternHost;
import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;
import net.oktawia.crazyae2addons.menus.block.AutoBuilderMenu;
import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import net.oktawia.crazyae2addons.util.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.util.ProgramExpander;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class AutoBuilderBE extends AENetworkBlockEntity implements
        IGridTickable,
        MenuProvider,
        InternalInventoryHost,
        IUpgradeableObject,
        ICraftingRequester,
        PatternProviderLogicHost,
        IManagedBEHelper,
        IMenuOpeningBlockEntity {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(AutoBuilderBE.class);

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Getter
    @Persisted
    @LazyManaged
    public final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get(), 7, this::setChanged
    );

    @Persisted
    @LazyManaged
    public final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 2);

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

    @DescSynced
    @Getter
    public boolean previewEnabled = false;

    @Persisted
    @DescSynced
    @Getter
    public Direction sourceFacing = Direction.NORTH;

    public static final List<AutoBuilderBE> CLIENT_INSTANCES = new CopyOnWriteArrayList<>();

    @Setter
    @Getter
    @OnlyIn(Dist.CLIENT)
    public PreviewInfo previewInfo;

    @Getter
    @Setter
    @DescSynced
    public boolean previewDirty = true;

    @Persisted
    @LazyManaged
    public final ManagedBuffer buffer;

    public AutoBuilderBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.AUTO_BUILDER_BE.get(), pos, state);

        this.ghostRenderPos = pos.above().above();

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
                .addService(ICraftingRequester.class, this)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get().asItem())
                );

        this.inventory.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return slot == 0 && stack.getItem().equals(CrazyItemRegistrar.BUILDER_PATTERN.get().asItem());
            }
        });
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        saveManagedData(tag);
    }

    @Override
    public void loadTag(net.minecraft.nbt.CompoundTag tag) {
        loadManagedData(tag);
        super.loadTag(tag);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag();
        saveManagedData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(net.minecraft.nbt.CompoundTag tag) {
        loadManagedData(tag);
        super.handleUpdateTag(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        var tag = pkt.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    public void clearMissingItem() {
        this.missingItems = GenericStack.fromItemStack(ItemStack.EMPTY);
        this.missingItemStack = ItemStack.EMPTY;
        this.missingItemAmount = 0;
        setChanged();
        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void setMissingItem(GenericStack stack) {
        this.missingItems = stack != null ? stack : GenericStack.fromItemStack(ItemStack.EMPTY);

        if (stack == null || stack.what() == null) {
            this.missingItemStack = ItemStack.EMPTY;
            this.missingItemAmount = 0;
            setChanged();
            if (!isClientSide()) {
                syncManaged();
            }
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
        if (!isClientSide()) {
            syncManaged();
        }
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
    public void openMenu(Player player, MenuLocator locator) {
        if (!player.level().isClientSide) {
            forceSyncManaged();
        }
        MenuOpener.open(CrazyMenuRegistrar.AUTO_BUILDER_MENU.get(), player, locator);
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.AUTO_BUILDER_BLOCK.get().asItem().getDefaultInstance();
    }

    @Override
    public boolean isClientSide() {
        return level == null || level.isClientSide;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        clearMissingItem();
        this.setChanged();

        loadCode();
        updateSkipEmptyFromCode();
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

        if (!isClientSide()) {
            forceSyncManaged();
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
            buffer.onLoad();

            if (!buffer.isFlushPending() && !buffer.isEmpty() && !isRunning && !isCrafting) {
                buffer.beginFlush();
            }

            if (previewEnabled && (!inventory.getStackInSlot(0).isEmpty() || !inventory.getStackInSlot(1).isEmpty())) {
                AutoBuilderPreviewOps.rebuildPreviewFromCode(this);
            }

            syncManaged();
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
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
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
        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void recalculateRequiredEnergy() {
        AutoBuilderWorldOps.recalculateRequiredEnergy(this);
        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void loadCode() {
        ItemStack s = this.inventory.getStackInSlot(0);
        if (s.isEmpty()) {
            s = this.inventory.getStackInSlot(1);
        }

        if (s.isEmpty()) {
            this.code.clear();
            if (!isClientSide()) {
                syncManaged();
            }
            return;
        }

        String programId = BuilderPatternItem.getProgramId(s);
        if (programId == null) {
            this.code.clear();
            if (!isClientSide()) {
                syncManaged();
            }
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

        Direction d = BuilderPatternItem.getSourceFacing(s);
        this.sourceFacing = d != null ? d : Direction.NORTH;
        setChanged();

        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void togglePreview() {
        AutoBuilderPreviewOps.togglePreview(this);
        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void updateSkipEmptyFromCode() {
        if (!this.code.isEmpty() && ProgramExpander.hasConditionalInstructions(String.join("/", this.code))) {
            this.skipEmpty = true;
        }
        setChanged();
        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void setGhostRenderPos(BlockPos pos) {
        AutoBuilderPreviewOps.setGhostRenderPos(this, pos);
        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void onOffsetChanged(BlockPos oldOffset) {
        AutoBuilderPreviewOps.onOffsetChanged(this, oldOffset);
        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void resetGhostToHome() {
        AutoBuilderPreviewOps.resetGhostToHome(this);
        if (!isClientSide()) {
            syncManaged();
        }
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