package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.stacks.AmountFormat;
import appeng.api.stacks.GenericStack;
import appeng.api.storage.StorageHelper;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.ConfigInventory;
import appeng.util.SettingsFrom;
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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.blocks.EjectorBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;
import net.oktawia.crazyae2addons.menus.block.EjectorMenu;
import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import net.oktawia.crazyae2addons.util.IMenuOpeningBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class EjectorBE extends AENetworkBlockEntity implements
        MenuProvider,
        IGridTickable,
        PatternProviderLogicHost,
        ICraftingRequester,
        InternalInventoryHost,
        IManagedBEHelper,
        IMenuOpeningBlockEntity {

    private static final String NBT_EJECTOR_CONFIG = "ejectorConfig";
    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(EjectorBE.class);

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @LazyManaged
    public final ConfigInventory config = ConfigInventory.configStacks(null, 36, this::setChanged, true);

    @Persisted
    @LazyManaged
    public final AppEngInternalInventory pattern = new AppEngInternalInventory(this, 1);

    @Persisted
    @DescSynced
    @Getter
    private boolean isCrafting = false;

    @DescSynced
    @Getter
    private ItemStack cantCraftStack = ItemStack.EMPTY;

    @DescSynced
    @Getter
    private String cantCraftCountText = "";

    @DescSynced
    @Getter
    private String cantCraftText = "nothing";

    @Persisted
    @LazyManaged
    public final ManagedBuffer buffer;

    public EjectorBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.EJECTOR_BE.get(), pos, blockState);

        this.pattern.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return slot == 0 && stack.getItem().equals(AEItems.PROCESSING_PATTERN.asItem());
            }
        });

        this.buffer = new ManagedBuffer(
                getMainNode(),
                this,
                this,
                this::setChanged,
                this::ejectFromBuffer,
                this::isCrafting
        );

        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .addService(ICraftingRequester.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.EJECTOR_BLOCK.get().asItem()));
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

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            buffer.onLoad();
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
        return EnumSet.of(getBlockState().getValue(EjectorBlock.FACING));
    }

    @Override
    public void saveChanges() {
        setChanged();
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

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        var missing = buffer.tick(ticksSinceLastCall);
        if (missing != null) {
            setCantCraft(missing);
        }

        boolean crafting = buffer.hasActiveCrafting();
        if (this.isCrafting != crafting) {
            setCraftingState(crafting);
        }

        return (crafting || buffer.isFlushPending()) ? TickRateModulation.URGENT : TickRateModulation.IDLE;
    }

    public void doWork() {
        if (getMainNode() == null
                || getMainNode().getGrid() == null
                || !getMainNode().isActive()
                || isCrafting) {
            return;
        }

        clearCantCraft();

        List<GenericStack> required = new ArrayList<>();
        for (int slot = 0; slot < config.size(); slot++) {
            var gs = config.getStack(slot);
            if (gs == null || gs.amount() <= 0) {
                continue;
            }
            required.add(gs);
        }

        if (required.isEmpty()) {
            return;
        }

        if (buffer.request(required.toArray(new GenericStack[0]))) {
            setCraftingState(true);
        }
    }

    private void ejectFromBuffer() {
        setCraftingState(false);
        clearCantCraft();

        var level = getLevel();
        var grid = getMainNode().getGrid();
        if (level == null || grid == null) {
            if (!buffer.isEmpty()) {
                buffer.beginFlush();
            }
            return;
        }

        var direction = getBlockState().getValue(EjectorBlock.FACING);
        var src = IActionSource.ofMachine(this);
        var energy = grid.getEnergyService();
        var storageInv = grid.getStorageService().getInventory();

        List<GenericStack> toEject = new ArrayList<>();
        for (int slot = 0; slot < config.size(); slot++) {
            var gs = config.getStack(slot);
            if (gs == null || gs.amount() <= 0 || gs.what() == null) {
                continue;
            }
            toEject.add(gs);
        }

        var targetPos = getBlockPos().relative(direction);
        var target = PatternProviderTarget.get(
                level,
                targetPos,
                level.getBlockEntity(targetPos),
                direction.getOpposite(),
                src
        );

        if (target == null) {
            for (var gs : toEject) {
                long amount = buffer.extract(gs.what(), gs.amount());
                if (amount > 0) {
                    StorageHelper.poweredInsert(energy, storageInv, gs.what(), amount, src, Actionable.MODULATE);
                }
            }
            if (!buffer.isEmpty()) {
                buffer.beginFlush();
            }
            return;
        }

        for (var gs : toEject) {
            if (target.insert(gs.what(), gs.amount(), Actionable.SIMULATE) < gs.amount()) {
                if (!buffer.isEmpty()) {
                    buffer.beginFlush();
                }
                return;
            }
        }

        for (var gs : toEject) {
            long amount = buffer.extract(gs.what(), gs.amount());
            if (amount <= 0) {
                continue;
            }

            long inserted = target.insert(gs.what(), amount, Actionable.MODULATE);
            long leftover = amount - inserted;
            if (leftover > 0) {
                StorageHelper.poweredInsert(energy, storageInv, gs.what(), leftover, src, Actionable.MODULATE);
            }
        }

        if (!buffer.isEmpty()) {
            buffer.beginFlush();
        }
    }

    private void setCraftingState(boolean crafting) {
        this.isCrafting = crafting;
        setChanged();

        if (level != null) {
            var state = getBlockState();
            if (state.hasProperty(EjectorBlock.ISCRAFTING) && state.getValue(EjectorBlock.ISCRAFTING) != crafting) {
                level.setBlockAndUpdate(getBlockPos(), state.setValue(EjectorBlock.ISCRAFTING, crafting));
            }
        }

        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void clearCantCraft() {
        this.cantCraftStack = ItemStack.EMPTY;
        this.cantCraftCountText = "";
        this.cantCraftText = "nothing";
        setChanged();

        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void setCantCraft(@Nullable GenericStack missing) {
        if (missing == null || missing.what() == null) {
            clearCantCraft();
            return;
        }

        this.cantCraftText = String.format(
                "%sx %s",
                missing.what().formatAmount(missing.amount(), AmountFormat.SLOT),
                missing.what()
        );
        this.cantCraftCountText = missing.what().formatAmount(missing.amount(), AmountFormat.SLOT);

        if (missing.what() instanceof AEItemKey itemKey) {
            this.cantCraftStack = itemKey.toStack(1);
        } else {
            this.cantCraftStack = ItemStack.EMPTY;
        }

        setChanged();
        if (!isClientSide()) {
            syncManaged();
        }
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);

        ItemStack patternStack = pattern.getStackInSlot(0);
        if (!patternStack.isEmpty()) {
            drops.add(patternStack);
        }

        var bufferTag = buffer.toTag();
        ListTag entries = bufferTag.getList("entries", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            ItemStack wrapped = ItemStack.of(entries.getCompound(i));
            GenericStack gs = GenericStack.fromItemStack(wrapped);
            if (gs != null && gs.what() instanceof AEItemKey key) {
                drops.add(key.toStack((int) Math.min(gs.amount(), Integer.MAX_VALUE)));
            }
        }
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        setChanged();
        if (!isClientSide()) {
            syncManaged();
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new EjectorMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        if (!player.level().isClientSide) {
            forceSyncManaged();
        }
        MenuOpener.open(CrazyMenuRegistrar.EJECTOR_MENU.get(), player, locator);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getBlockState().getBlock().asItem());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getBlockState().getBlock().asItem().getDefaultInstance();
    }

    public boolean isClientSide() {
        return level == null || level.isClientSide;
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM || mode == SettingsFrom.MEMORY_CARD) {
            this.config.writeToChildTag(output, NBT_EJECTOR_CONFIG);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM || mode == SettingsFrom.MEMORY_CARD) {
            if (input.contains(NBT_EJECTOR_CONFIG, Tag.TAG_LIST)) {
                for (int i = 0; i < this.config.size(); i++) {
                    this.config.setStack(i, null);
                }

                this.config.readFromChildTag(input, NBT_EJECTOR_CONFIG);
            }

            clearCantCraft();
            setChanged();

            if (!isClientSide()) {
                syncManaged();
            }
        }
    }
}