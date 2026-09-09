package net.oktawia.insaneae2addons.entities;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import lombok.Getter;

import appeng.api.config.Actionable;
import appeng.api.inventories.ISegmentedInventory;
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
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;

import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import net.oktawia.crazyae2addons.util.IMenuOpeningBlockEntity;
import net.oktawia.crazyae2addons.util.Utils;
import net.oktawia.insaneae2addons.InsaneConfig;
import net.oktawia.insaneae2addons.IsModLoaded;
import net.oktawia.insaneae2addons.defs.regs.InsaneBlockEntityRegistrar;
import net.oktawia.insaneae2addons.defs.regs.InsaneItemRegistrar;
import net.oktawia.insaneae2addons.defs.regs.InsaneMenuRegistrar;
import net.oktawia.insaneae2addons.items.XpShardItem;
import net.oktawia.insaneae2addons.logic.enchanter.ApotheosisEnchantStrategy;
import net.oktawia.insaneae2addons.logic.enchanter.EnchantStrategy;
import net.oktawia.insaneae2addons.logic.enchanter.VanillaEnchantStrategy;
import net.oktawia.insaneae2addons.menus.block.AutoEnchanterMenu;

public class AutoEnchanterBE extends AENetworkInvBlockEntity
        implements MenuProvider, IManagedBEHelper, IMenuOpeningBlockEntity, IGridTickable {

    private static final String NBT_OPTION = "option";
    private static final String NBT_AUTO_SUPPLY_LAPIS = "auto_supply_lapis";
    private static final String NBT_AUTO_SUPPLY_BOOKS = "auto_supply_books";

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(AutoEnchanterBE.class);

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @Getter
    private int option = 0;

    @Persisted
    @DescSynced
    @Getter
    private boolean autoSupplyLapis = false;

    @Persisted
    @DescSynced
    @Getter
    private boolean autoSupplyBooks = false;

    @DescSynced
    @Getter
    private int xp = 0;

    @DescSynced
    @Getter
    private String levelCost = "0";

    private static final Set<TagKey<Fluid>> XP_FLUID_TAGS = Set.of(
            TagKey.create(Registries.FLUID, new ResourceLocation("forge", "experience")),
            TagKey.create(Registries.FLUID, new ResourceLocation("forge", "xpjuice")));

    private final AppEngInternalInventory inventory = new AppEngInternalInventory(this, 3, 64, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.isEnchantable() || stack.is(Items.BOOK);
                case 1 -> stack.is(Items.LAPIS_LAZULI);
                default -> false;
            };
        }
    });

    private final InternalInventory inputInv = inventory.getSubInventory(0, 1);
    private final InternalInventory lapisInv = inventory.getSubInventory(1, 2);
    private final InternalInventory outputInv = inventory.getSubInventory(2, 3);

    private final EnchantStrategy strategy;

    public AutoEnchanterBE(BlockPos pos, BlockState blockState) {
        super(InsaneBlockEntityRegistrar.AUTO_ENCHANTER_BE.get(), pos, blockState);
        this.strategy = IsModLoaded.APOTHEOSIS ? new ApotheosisEnchantStrategy() : new VanillaEnchantStrategy();
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .addService(IGridTickable.class, this);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            return LazyOptional.of(() -> new IItemHandler() {
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
                    if (stack.is(Items.LAPIS_LAZULI)) {
                        return lapisInv.insertItem(0, stack, simulate);
                    }
                    if (stack.isEnchantable() || stack.is(Items.BOOK)) {
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
                    return stack.is(Items.LAPIS_LAZULI) || stack.isEnchantable() || stack.is(Items.BOOK);
                }
            }).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return this.inventory;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        setChanged();
        markForUpdate();
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.inventory;
        }
        return super.getSubInventory(id);
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
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AutoEnchanterMenu(id, inventory, this);
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        if (getLevel() != null && !getLevel().isClientSide()) {
            forceSyncManaged();
        }
        MenuOpener.open(InsaneMenuRegistrar.AUTO_ENCHANTER_MENU.get(), player, locator);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);

        if (mode == SettingsFrom.MEMORY_CARD) {
            output.putInt(NBT_OPTION, this.option);
            output.putBoolean(NBT_AUTO_SUPPLY_LAPIS, this.autoSupplyLapis);
            output.putBoolean(NBT_AUTO_SUPPLY_BOOKS, this.autoSupplyBooks);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.MEMORY_CARD && input.contains(NBT_OPTION, Tag.TAG_INT)) {
            setOption(input.getInt(NBT_OPTION));
            setAutoSupplyLapis(input.getBoolean(NBT_AUTO_SUPPLY_LAPIS));
            setAutoSupplyBooks(input.getBoolean(NBT_AUTO_SUPPLY_BOOKS));
        }
    }

    public void setOption(int option) {
        this.option = option;
        setChanged();
        syncManaged();
    }

    public void setAutoSupplyLapis(boolean value) {
        this.autoSupplyLapis = value;
        setChanged();
        syncManaged();
    }

    public void setAutoSupplyBooks(boolean value) {
        this.autoSupplyBooks = value;
        setChanged();
        syncManaged();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(40, 40, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (node == null || !node.isActive() || node.getGrid() == null) {
            return TickRateModulation.IDLE;
        }
        if (!InsaneConfig.COMMON.AUTO_ENCHANTER_ENABLED.get()) {
            return TickRateModulation.IDLE;
        }
        if (getLevel() == null
                || getLevel().getBlockState(getBlockPos().above().above()).getBlock() != Blocks.ENCHANTING_TABLE
                || this.option == 0) {
            return TickRateModulation.IDLE;
        }

        refreshDisplay();

        ItemStack outStack = outputInv.getStackInSlot(0);
        if (outStack.isEmpty()) {
            ItemStack input = inputInv.getStackInSlot(0);
            ItemStack enchanted = performEnchant(input, this.option);
            if (enchanted != input) {
                inputInv.getSlotInv(0).extractItem(0, 1, false);
                outputInv.setItemDirect(0, enchanted);
            }
        }

        if (this.autoSupplyLapis) {
            supplyFromNetwork(lapisInv, Items.LAPIS_LAZULI);
        }
        if (this.autoSupplyBooks) {
            ItemStack in = inputInv.getStackInSlot(0);
            if (in.isEmpty() || in.is(Items.BOOK)) {
                supplyFromNetwork(inputInv, Items.BOOK);
            }
        }
        return TickRateModulation.IDLE;
    }

    private void supplyFromNetwork(InternalInventory inv, Item item) {
        int toSupply = inv.getSlotLimit(0) - inv.getStackInSlot(0).getCount();
        if (toSupply <= 0) {
            return;
        }
        int extracted = (int) StorageHelper.poweredExtraction(
                getGridNode().getGrid().getEnergyService(),
                getGridNode().getGrid().getStorageService().getInventory(),
                AEItemKey.of(item),
                toSupply,
                IActionSource.ofMachine(this),
                Actionable.MODULATE);
        if (extracted > 0) {
            ItemStack stack = item.getDefaultInstance();
            stack.setCount(extracted);
            inv.addItems(stack);
        }
    }

    public ItemStack performEnchant(ItemStack input, int option) {
        ItemStack lapis = lapisInv.getStackInSlot(0);
        if (input.isEmpty()
                || (!input.isEnchantable() && !input.is(Items.BOOK))
                || lapis.isEmpty()
                || !lapis.is(Items.LAPIS_LAZULI)
                || lapis.getCount() < option) {
            return input;
        }

        IGridNode node = getGridNode();
        if (node == null || !node.isActive() || node.getGrid() == null) {
            return input;
        }

        RandomSource random = RandomSource.create();
        EnchantStrategy.EnchantRoll roll = strategy.roll(random, getLevel(), getBlockPos().above().above(), input,
                option);
        if (roll.isEmpty()) {
            return input;
        }

        if (!consumeXpFromNetworkAtomically(xpCost(roll.xpLevel()))) {
            return input;
        }

        ItemStack result;
        if (input.is(Items.BOOK)) {
            result = new ItemStack(Items.ENCHANTED_BOOK);
            for (EnchantmentInstance inst : roll.enchantments()) {
                EnchantedBookItem.addEnchantment(result, inst);
            }
        } else {
            result = input.copy();
            for (EnchantmentInstance inst : roll.enchantments()) {
                result.enchant(inst.enchantment, inst.level);
            }
        }

        lapis.shrink(option);
        return result;
    }

    private static long xpCost(int enchantLevel) {
        long fullXp = levelToXp(enchantLevel);
        long base = Math.max(1L, fullXp / 100L);
        return safeMul(base, InsaneConfig.COMMON.AUTO_ENCHANTER_COST.get());
    }

    private static long levelToXp(int level) {
        if (level <= 16) {
            return (long) level * level + 6L * level;
        } else if (level <= 31) {
            return (long) (2.5d * level * level - 40.5d * level + 360d);
        }
        return (long) (4.5d * level * level - 162.5d * level + 2220d);
    }

    private static long safeMul(long a, long b) {
        if (a == 0 || b == 0) {
            return 0;
        }
        if (a > Long.MAX_VALUE / b) {
            return Long.MAX_VALUE;
        }
        return a * b;
    }

    private Set<AEFluidKey> getAvailableXpFluids() {
        IGridNode node = getGridNode();
        if (node == null || node.getGrid() == null) {
            return Set.of();
        }
        Set<Fluid> validXpFluids = new HashSet<>();
        for (TagKey<Fluid> tag : XP_FLUID_TAGS) {
            ForgeRegistries.FLUIDS.tags().getTag(tag).forEach(validXpFluids::add);
        }

        Set<AEFluidKey> available = new HashSet<>();
        node.getGrid().getStorageService().getCachedInventory().forEach(key -> {
            if (key.getKey() instanceof AEFluidKey fkey && validXpFluids.contains(fkey.getFluid())) {
                available.add(fkey);
            }
        });
        return available;
    }

    private boolean consumeXpFromNetworkAtomically(long xpToConsume) {
        IGridNode node = getGridNode();
        if (node == null || !node.isActive() || node.getGrid() == null) {
            return false;
        }

        var energy = node.getGrid().getEnergyService();
        var storage = node.getGrid().getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);

        long xpLeft = xpToConsume;
        AEItemKey shardKey = AEItemKey.of(InsaneItemRegistrar.XP_SHARD.get());

        long shardAvail = storage.extract(shardKey, Long.MAX_VALUE, Actionable.SIMULATE, source);
        long shardsPlanned = Math.min(xpLeft / XpShardItem.XP_VAL, shardAvail);
        if (StorageHelper.poweredExtraction(energy, storage, shardKey, shardsPlanned, source,
                Actionable.SIMULATE) < shardsPlanned) {
            return false;
        }
        xpLeft -= shardsPlanned * (long) XpShardItem.XP_VAL;

        Map<AEFluidKey, Long> fluidsPlanned = new LinkedHashMap<>();
        for (AEFluidKey fluid : getAvailableXpFluids()) {
            if (xpLeft <= 0) {
                break;
            }
            long availableMb = storage.extract(fluid, Long.MAX_VALUE, Actionable.SIMULATE, source);
            long toExtractMb = Math.min(safeMul(xpLeft, 20L), availableMb);
            toExtractMb = (toExtractMb / 20L) * 20L;
            if (toExtractMb <= 0) {
                continue;
            }
            if (StorageHelper.poweredExtraction(energy, storage, fluid, toExtractMb, source,
                    Actionable.SIMULATE) < toExtractMb) {
                return false;
            }
            fluidsPlanned.put(fluid, toExtractMb);
            xpLeft -= toExtractMb / 20L;
        }

        if (xpLeft > 0) {
            return false;
        }

        if (StorageHelper.poweredExtraction(energy, storage, shardKey, shardsPlanned, source,
                Actionable.MODULATE) < shardsPlanned) {
            return false;
        }
        for (Map.Entry<AEFluidKey, Long> e : fluidsPlanned.entrySet()) {
            if (StorageHelper.poweredExtraction(energy, storage, e.getKey(), e.getValue(), source,
                    Actionable.MODULATE) < e.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void refreshDisplay() {
        IGridNode node = getGridNode();
        if (node == null || node.getGrid() == null) {
            return;
        }
        var storage = node.getGrid().getStorageService().getInventory();
        var source = IActionSource.ofMachine(this);

        long totalXp = safeMul(
                storage.extract(AEItemKey.of(InsaneItemRegistrar.XP_SHARD.get()), Long.MAX_VALUE, Actionable.SIMULATE,
                        source),
                XpShardItem.XP_VAL);
        for (AEFluidKey fluid : getAvailableXpFluids()) {
            long newTotal = totalXp + storage.extract(fluid, Long.MAX_VALUE, Actionable.SIMULATE, source) / 20L;
            totalXp = newTotal < totalXp ? Long.MAX_VALUE : newTotal;
        }
        this.xp = (int) Math.min(totalXp, Integer.MAX_VALUE);

        ItemStack input = inputInv.getStackInSlot(0);
        if (input.isEmpty() || (!input.isEnchantable() && !input.is(Items.BOOK))) {
            this.levelCost = "0";
        } else {
            int enchLevel = strategy.costLevel(RandomSource.create(), getLevel(), getBlockPos().above().above(), input,
                    this.option);
            this.levelCost = Utils
                    .shortenNumber(safeMul(levelToXp(enchLevel), InsaneConfig.COMMON.AUTO_ENCHANTER_COST.get()));
        }
        syncManaged();
    }
}
