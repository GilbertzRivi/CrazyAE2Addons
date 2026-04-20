package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEFluidKey;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.items.IItemHandler;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.block.AutoEnchanterMenu;
import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import net.oktawia.crazyae2addons.util.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.util.Utils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AutoEnchanterBE extends AENetworkBlockEntity
        implements MenuProvider, IGridTickable, InternalInventoryHost, IManagedBEHelper, IMenuOpeningBlockEntity {

    private static final int[][] BOOKSHELF_OFFSETS = {
            {-1, 0, -2}, {0, 0, -2}, {1, 0, -2},
            {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1},
            {-1, 0, 2}, {0, 0, 2}, {1, 0, 2},
            {2, 0, -1}, {2, 0, 0}, {2, 0, 1},
            {-1, 1, -2}, {0, 1, -2}, {1, 1, -2},
            {-2, 1, -1}, {-2, 1, 0}, {-2, 1, 1},
            {-1, 1, 2}, {0, 1, 2}, {1, 1, 2},
            {2, 1, -1}, {2, 1, 0}, {2, 1, 1}
    };

    public static final Set<TagKey<Fluid>> XP_FLUID_TAGS = Set.of(
            TagKey.create(Registries.FLUID, new ResourceLocation("forge", "experience")),
            TagKey.create(Registries.FLUID, new ResourceLocation("forge", "xpjuice")),
            TagKey.create(Registries.FLUID, new ResourceLocation("neoforge", "experience")),
            TagKey.create(Registries.FLUID, new ResourceLocation("neoforge", "xpjuice"))
    );

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(AutoEnchanterBE.class);

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Getter
    @Persisted
    @LazyManaged
    public final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 3);

    @Getter
    public final InternalInventory inputInv = inventory.getSubInventory(0, 1);

    @Getter
    public final InternalInventory lapisInv = inventory.getSubInventory(1, 2);

    @Getter
    public final InternalInventory outputInv = inventory.getSubInventory(2, 3);

    private final IItemHandler sideItemHandler = new SideItemHandler();

    @Getter
    @DescSynced
    private int xp = 0;

    @Getter
    @Persisted
    @DescSynced
    private int option = 0;

    @Getter
    @Persisted
    @DescSynced
    private boolean autoSupplyLapis = false;

    @Getter
    @Persisted
    @DescSynced
    private boolean autoSupplyBooks = false;

    @Getter
    @DescSynced
    private String levelCost = "";

    public AutoEnchanterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.AUTO_ENCHANTER_BE.get(), pos, blockState);

        this.inventory.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return switch (slot) {
                    case 0 -> stack.isEnchantable() || stack.getItem() == Items.BOOK;
                    case 1 -> stack.getItem() == Items.LAPIS_LAZULI;
                    case 2 -> false;
                    default -> false;
                };
            }
        });

        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(getBlockState().getBlock()));
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveManagedData(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        loadManagedData(tag);
        super.loadTag(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = super.getUpdateTag();
        saveManagedData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
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
            refreshRuntimeState(false);
            syncManaged();
        }
    }

    @Override
    public void saveChanges() {
        setChanged();
    }

    private void handleInventoryChanged() {
        setChanged();
        refreshRuntimeState(true);
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        handleInventoryChanged();
    }

    public void setOption(int option) {
        int clamped = Mth.clamp(option, 0, 3);
        if (this.option == clamped) {
            return;
        }

        this.option = clamped;
        setChanged();
        refreshRuntimeState(true);
    }

    public void setAutoSupplyLapis(boolean autoSupplyLapis) {
        if (this.autoSupplyLapis == autoSupplyLapis) {
            return;
        }

        this.autoSupplyLapis = autoSupplyLapis;
        setChanged();

        if (!isClientSide()) {
            syncManaged();
        }
    }

    public void setAutoSupplyBooks(boolean autoSupplyBooks) {
        if (this.autoSupplyBooks == autoSupplyBooks) {
            return;
        }

        this.autoSupplyBooks = autoSupplyBooks;
        setChanged();

        if (!isClientSide()) {
            syncManaged();
        }
    }

    @Nullable
    public IItemHandler getItemHandler(@Nullable Direction side) {
        return side == null ? null : sideItemHandler;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AutoEnchanterMenu(id, inventory, this);
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        if (!player.level().isClientSide) {
            forceSyncManaged();
        }
        MenuOpener.open(CrazyMenuRegistrar.AUTO_ENCHANTER_MENU.get(), player, locator);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(40, 40, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (node == null || !node.isActive()) {
            return TickRateModulation.IDLE;
        }

        refreshRuntimeState(true);

        if (!hasEnchantingTable()) {
            return TickRateModulation.IDLE;
        }

        tryEnchantOne();
        tryAutoSupplyLapis();
        tryAutoSupplyBooks();

        return TickRateModulation.IDLE;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    protected void refreshRuntimeState(boolean syncNow) {
        boolean changed = false;
        changed |= updateXpValue();
        changed |= updateLevelCostValue();

        if (changed) {
            setChanged();
            if (syncNow && !isClientSide()) {
                syncManaged();
            }
        }
    }

    protected boolean updateXpValue() {
        long totalXp = getAvailableXpTotal();
        int newXp = (int) Math.min(totalXp, Integer.MAX_VALUE);

        if (this.xp == newXp) {
            return false;
        }

        this.xp = newXp;
        return true;
    }

    protected boolean updateLevelCostValue() {
        int enchantLevel = computeEnchantLevel(inputInv.getStackInSlot(0), option);
        long display = computeDisplayedLevelCost(enchantLevel);
        String newLevelCost = Utils.shortenNumber(display);

        if (java.util.Objects.equals(this.levelCost, newLevelCost)) {
            return false;
        }

        this.levelCost = newLevelCost;
        return true;
    }

    protected long computeDisplayedLevelCost(int enchantLevel) {
        return safeMul(levelToXpLong(enchantLevel), CrazyConfig.COMMON.AutoEnchanterCost.get());
    }

    protected long getAvailableXpTotal() {
        IGridNode node = getGridNode();
        if (node == null || node.getGrid() == null) {
            return 0;
        }

        var storage = node.getGrid().getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);

        long totalXp = 0;

        long shardCount = storage.extract(
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                Long.MAX_VALUE,
                Actionable.SIMULATE,
                source
        );
        totalXp += safeMul(shardCount, XpShardItem.XP_VAL);

        for (AEFluidKey fluid : getAvailableXpFluids()) {
            long fluidAmount = storage.extract(fluid, Long.MAX_VALUE, Actionable.SIMULATE, source);
            totalXp += fluidAmount / 20L;
        }

        return totalXp;
    }

    protected void tryEnchantOne() {
        if (option <= 0) {
            return;
        }

        ItemStack outStack = outputInv.getStackInSlot(0);
        if (!outStack.isEmpty() && outStack.getItem() != Items.AIR) {
            return;
        }

        ItemStack rawInput = inputInv.getStackInSlot(0);
        if (rawInput.isEmpty()) {
            return;
        }

        ItemStack input = rawInput.copyWithCount(1);
        ItemStack enchanted = performEnchant(input, option);

        if (enchanted != input) {
            inputInv.getSlotInv(0).extractItem(0, 1, false);
            outputInv.setItemDirect(0, enchanted);
        }
    }

    protected void tryAutoSupplyLapis() {
        if (!autoSupplyLapis) {
            return;
        }

        supplyItemToSlot(lapisInv, Items.LAPIS_LAZULI);
    }

    protected void tryAutoSupplyBooks() {
        if (!autoSupplyBooks) {
            return;
        }

        ItemStack current = inputInv.getStackInSlot(0);
        if (!current.isEmpty() && current.getItem() != Items.BOOK && current.getItem() != Items.AIR) {
            return;
        }

        supplyItemToSlot(inputInv, Items.BOOK);
    }

    protected void supplyItemToSlot(InternalInventory target, Item item) {
        IGridNode node = getGridNode();
        if (node == null || node.getGrid() == null) {
            return;
        }

        int toSupply = target.getSlotLimit(0) - target.getStackInSlot(0).getCount();
        if (toSupply <= 0) {
            return;
        }

        int extracted = (int) Math.min(
                Integer.MAX_VALUE,
                StorageHelper.poweredExtraction(
                        node.getGrid().getEnergyService(),
                        node.getGrid().getStorageService().getInventory(),
                        AEItemKey.of(item),
                        toSupply,
                        IActionSource.ofMachine(this),
                        Actionable.MODULATE
                )
        );

        if (extracted <= 0) {
            return;
        }

        ItemStack stack = item.getDefaultInstance();
        stack.setCount(extracted);
        target.addItems(stack);
    }

    protected boolean hasEnchantingTable() {
        return this.getLevel() != null
                && this.getLevel().getBlockState(getEnchantingTablePos()).getBlock() == Blocks.ENCHANTING_TABLE;
    }

    protected BlockPos getEnchantingTablePos() {
        return this.getBlockPos().above().above();
    }

    protected int countBookshelves(BlockPos tablePos) {
        int count = 0;
        for (int[] offset : BOOKSHELF_OFFSETS) {
            BlockPos pos = tablePos.offset(offset[0], offset[1], offset[2]);
            if (level.getBlockState(pos).is(Blocks.BOOKSHELF)) {
                count++;
            }
        }
        return count;
    }

    public static long levelToXpLong(int level) {
        if (level <= 16) {
            return (long) level * level + 6L * level;
        } else if (level <= 31) {
            return (long) (2.5d * level * level - 40.5d * level + 360d);
        } else {
            return (long) (4.5d * level * level - 162.5d * level + 2220d);
        }
    }

    public static int levelToXp(int level) {
        return (int) Math.min(Integer.MAX_VALUE, levelToXpLong(level));
    }

    protected static long safeMul(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    protected boolean isValidEnchantingInput(ItemStack stack) {
        return !stack.isEmpty() && (stack.isEnchantable() || stack.getItem() == Items.BOOK);
    }

    protected int computeEnchantLevel(ItemStack input, int option) {
        if (!isValidEnchantingInput(input) || !hasEnchantingTable()) {
            return 0;
        }

        int slotIndex = option - 1;
        if (slotIndex < 0) {
            return 0;
        }

        int bookshelfCount = countBookshelves(getEnchantingTablePos());
        return Math.max(
                EnchantmentHelper.getEnchantmentCost(RandomSource.create(), slotIndex, bookshelfCount, input),
                0
        );
    }

    protected List<EnchantmentInstance> selectEnchantments(ItemStack input, int option, int enchantLevel) {
        return EnchantmentHelper.selectEnchantment(
                RandomSource.create(),
                input,
                enchantLevel,
                this.getLevel().registryAccess().registryOrThrow(Registries.ENCHANTMENT).holders().map(h -> h)
        );
    }

    protected long computeXpToConsume(int enchantLevel) {
        return safeMul(levelToXpLong(enchantLevel), CrazyConfig.COMMON.AutoEnchanterCost.get());
    }

    protected ItemStack buildEnchantResult(ItemStack input, List<EnchantmentInstance> enchantments) {
        ItemStack result = input.getItem() == Items.BOOK
                ? new ItemStack(Items.ENCHANTED_BOOK)
                : input.copyWithCount(1);

        for (EnchantmentInstance inst : enchantments) {
            result.enchant(inst.enchantment, inst.level);
        }

        return result;
    }

    public ItemStack performEnchant(ItemStack input, int option) {
        ItemStack lapis = lapisInv.getStackInSlot(0);

        if (!isValidEnchantingInput(input)
                || lapis.isEmpty()
                || lapis.getItem() != Items.LAPIS_LAZULI
                || lapis.getCount() < option) {
            return input;
        }

        IGridNode node = getGridNode();
        if (node == null || !node.isActive() || node.getGrid() == null) {
            return input;
        }

        int enchantLevel = computeEnchantLevel(input, option);
        if (enchantLevel <= 0) {
            return input;
        }

        List<EnchantmentInstance> enchantments = selectEnchantments(input, option, enchantLevel);
        if (enchantments.isEmpty()) {
            return input;
        }

        long xpToConsume = computeXpToConsume(enchantLevel);
        if (!consumeXpFromNetworkAtomically(xpToConsume)) {
            return input;
        }

        ItemStack result = buildEnchantResult(input, enchantments);
        lapis.shrink(option);

        refreshRuntimeState(true);

        return result;
    }

    public Set<AEFluidKey> getAvailableXpFluids() {
        IGridNode node = getGridNode();
        if (node == null || node.getGrid() == null) {
            return Set.of();
        }

        Set<Fluid> validXpFluids = new HashSet<>();
        for (TagKey<Fluid> tag : XP_FLUID_TAGS) {
            BuiltInRegistries.FLUID.getTag(tag).ifPresent(
                    holders -> holders.forEach(h -> validXpFluids.add(h.value()))
            );
        }

        Set<AEFluidKey> availableFluids = new HashSet<>();
        node.getGrid().getStorageService().getInventory().getAvailableStacks().forEach(key -> {
            if (key.getKey() instanceof AEFluidKey fluidKey && validXpFluids.contains(fluidKey.getFluid())) {
                availableFluids.add(fluidKey);
            }
        });

        return availableFluids;
    }

    protected boolean consumeXpFromNetworkAtomically(long xpToConsume) {
        IGridNode node = getGridNode();
        if (node == null || !node.isActive() || node.getGrid() == null) {
            return false;
        }

        var grid = node.getGrid();
        var energy = grid.getEnergyService();
        var storage = grid.getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);

        long xpLeft = xpToConsume;

        List<AEFluidKey> fluids = new ArrayList<>(getAvailableXpFluids());
        fluids.sort(Comparator.comparing(f -> {
            var key = BuiltInRegistries.FLUID.getKey(f.getFluid());
            return key == null ? "" : key.toString();
        }));

        long shardAvail = storage.extract(
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                Long.MAX_VALUE,
                Actionable.SIMULATE,
                source
        );

        long shardsPlanned = Mth.clamp(xpLeft / XpShardItem.XP_VAL, 1L, shardAvail) + 1L;
        long shardsSim = StorageHelper.poweredExtraction(
                energy,
                storage,
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                shardsPlanned,
                source,
                Actionable.SIMULATE
        );
        if (shardsSim < shardsPlanned) {
            return false;
        }

        xpLeft -= shardsPlanned * XpShardItem.XP_VAL;

        Map<AEFluidKey, Long> fluidsPlanned = new LinkedHashMap<>();
        for (AEFluidKey fluid : fluids) {
            if (xpLeft <= 0) {
                break;
            }

            long availableMb = storage.extract(fluid, Long.MAX_VALUE, Actionable.SIMULATE, source);
            long needMb = safeMul(xpLeft, 20L);
            long toExtractMb = Math.min(needMb, availableMb);
            toExtractMb = (toExtractMb / 20L) * 20L;

            if (toExtractMb <= 0) {
                continue;
            }

            long simMb = StorageHelper.poweredExtraction(
                    energy, storage, fluid, toExtractMb, source, Actionable.SIMULATE
            );
            if (simMb < toExtractMb) {
                return false;
            }

            fluidsPlanned.put(fluid, toExtractMb);
            xpLeft -= (toExtractMb / 20L);
        }

        if (xpLeft > 0) {
            return false;
        }

        long shardsDone = StorageHelper.poweredExtraction(
                energy,
                storage,
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                shardsPlanned,
                source,
                Actionable.MODULATE
        );
        if (shardsDone < shardsPlanned) {
            return false;
        }

        for (var entry : fluidsPlanned.entrySet()) {
            long doneMb = StorageHelper.poweredExtraction(
                    energy,
                    storage,
                    entry.getKey(),
                    entry.getValue(),
                    source,
                    Actionable.MODULATE
            );
            if (doneMb < entry.getValue()) {
                return false;
            }
        }

        return true;
    }

    private final class SideItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot) {
            return outputInv.getStackInSlot(0);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
            if (stack.getItem() == Items.LAPIS_LAZULI) {
                return lapisInv.insertItem(0, stack, simulate);
            }
            if (stack.isEnchantable() || stack.getItem() == Items.BOOK) {
                return inputInv.insertItem(0, stack, simulate);
            }
            return stack;
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
            return outputInv.extractItem(0, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return stack.getItem() == Items.LAPIS_LAZULI
                    || stack.isEnchantable()
                    || stack.getItem() == Items.BOOK;
        }
    }

    public boolean isClientSide() {
        return level == null || level.isClientSide;
    }
}