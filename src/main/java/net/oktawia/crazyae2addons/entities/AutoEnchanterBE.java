package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageHelper;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.oktawia.crazyae2addons.Utils;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.items.XpShardItem;
import net.oktawia.crazyae2addons.menus.AutoEnchanterMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class AutoEnchanterBE extends AENetworkInvBlockEntity implements MenuProvider, IUpgradeableObject, IGridTickable {

    public AppEngInternalInventory inventory = new AppEngInternalInventory(this, 3);
    public IUpgradeInventory upgrades = UpgradeInventories.forMachine(CrazyBlockRegistrar.DATA_PROCESSOR_BLOCK.get(), 0, this::saveChanges);
    public InternalInventory inputInv = inventory.getSubInventory(0, 1);
    public InternalInventory lapisInv = inventory.getSubInventory(1, 2);
    public InternalInventory outputInv = inventory.getSubInventory(2, 3);
    public AutoEnchanterMenu menu;
    public int xp = 0;
    public int option = 0;
    public Boolean autoSupplyLapis = false;
    public Boolean autoSupplyBooks = false;
    public String levelCost = "";

    public AutoEnchanterBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.AUTO_ENCHANTER_BE.get(), pos, blockState);
        this.getMainNode()
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setIdlePowerUsage(4)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.AUTO_ENCHANTER_BLOCK.get().asItem())
                );
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
                    return AutoEnchanterBE.this.outputInv.getStackInSlot(0);
                }

                @Override
                public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (stack.getItem() == Items.LAPIS_LAZULI){
                        return AutoEnchanterBE.this.lapisInv.insertItem(0, stack, simulate);
                    } else if (stack.isEnchantable()){
                        return AutoEnchanterBE.this.inputInv.insertItem(0, stack, simulate);
                    } else {
                        return stack;
                    }
                }

                @Override
                public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return AutoEnchanterBE.this.outputInv.extractItem(0, amount, simulate);
                }

                @Override
                public int getSlotLimit(int slot) {
                    return 64;
                }

                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    return stack.getItem() == Items.LAPIS_LAZULI || stack.isEnchantable() || stack.getItem() == Items.BOOK;
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
        this.setChanged();
        this.markForUpdate();
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.STORAGE)) {
            return this.getInternalInventory();
        } else if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("option")){
            this.option = data.getInt("option");
        }
        if (data.contains("aslapis")){
            this.autoSupplyLapis = data.getBoolean("aslapis");
        }
        if (data.contains("asbooks")){
            this.autoSupplyBooks = data.getBoolean("asbooks");
        }
    }

    @Override
    public void saveAdditional(CompoundTag data){
        super.saveAdditional(data);
        data.putInt("option", this.option);
        data.putBoolean("aslapis", this.autoSupplyLapis);
        data.putBoolean("asbooks", this.autoSupplyBooks);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new AutoEnchanterMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.AUTO_ENCHANTER_MENU.get(), player, locator);
    }

    private int countBookshelves(BlockPos tablePos) {
        int count = 0;

        int[][] positions = {
                {-1, 0, -2}, {0, 0, -2}, {1, 0, -2},
                {-2, 0, -1}, {-2, 0, 0}, {-2, 0, 1},
                {-1, 0, 2}, {0, 0, 2}, {1, 0, 2},
                {2, 0, -1}, {2, 0, 0}, {2, 0, 1},
                {-1, 1, -2}, {0, 1, -2}, {1, 1, -2},
                {-2, 1, -1}, {-2, 1, 0}, {-2, 1, 1},
                {-1, 1, 2}, {0, 1, 2}, {1, 1, 2},
                {2, 1, -1}, {2, 1, 0}, {2, 1, 1}
        };

        for (int[] offset : positions) {
            BlockPos pos = tablePos.offset(offset[0], offset[1], offset[2]);
            if (level.getBlockState(pos).is(Blocks.BOOKSHELF)) {
                count++;
            }
        }

        return count;
    }

    public static int levelToXp(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        } else {
            return (int) (4.5 * level * level - 162.5 * level + 2220);
        }
    }

    public ItemStack performEnchant(ItemStack input, int option) {
        ItemStack lapis = lapisInv.getStackInSlot(0);

        if (input.isEmpty() || (!input.isEnchantable() && input.getItem() != Items.BOOK) || lapis.getCount() <= 0 || lapis.getItem() != Items.LAPIS_LAZULI) {
            return input;
        }

        int bookshelfCount = countBookshelves(this.getBlockPos().above().above());
        RandomSource random = RandomSource.create();
        int enchantLevel = EnchantmentHelper.getEnchantmentCost(random, option, bookshelfCount, input);

        if (enchantLevel <= 0) {
            return input;
        }

        int wantExtract = Math.round((float) levelToXp(enchantLevel) / XpShardItem.XP_VAL);
        long toExtract = StorageHelper.poweredExtraction(
                getGridNode().getGrid().getEnergyService(),
                getGridNode().getGrid().getStorageService().getInventory(),
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                wantExtract,
                IActionSource.ofMachine(this),
                Actionable.SIMULATE
        );
        if (toExtract < wantExtract){
            return input;
        }
        StorageHelper.poweredExtraction(
                getGridNode().getGrid().getEnergyService(),
                getGridNode().getGrid().getStorageService().getInventory(),
                AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()),
                wantExtract,
                IActionSource.ofMachine(this),
                Actionable.MODULATE
        );
        if (this.menu != null){
            this.menu.xp = Math.toIntExact(getGridNode().getGrid().getStorageService().getInventory().extract(AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()), Integer.MAX_VALUE, Actionable.SIMULATE, IActionSource.ofMachine(this)));
        }
        List<EnchantmentInstance> enchantments = EnchantmentHelper.selectEnchantment(random, input, enchantLevel, false);
        if (enchantments.isEmpty()) {
            return input;
        }

        ItemStack result;

        if (input.getItem() == Items.BOOK) {
            result = new ItemStack(Items.ENCHANTED_BOOK);
            for (EnchantmentInstance inst : enchantments) {
                EnchantedBookItem.addEnchantment(result, inst);
            }
        } else {
            result = input.copy();
            for (EnchantmentInstance inst : enchantments) {
                result.enchant(inst.enchantment, inst.level);
            }
        }

        lapis.shrink(option);
        return result;
    }


    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(40, 40, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.getLevel() == null || this.getLevel().getBlockState(this.getBlockPos().above().above()).getBlock() != Blocks.ENCHANTING_TABLE || this.option == 0) return TickRateModulation.IDLE;
        if (this.menu != null && this.getGridNode() != null){
            this.xp = Math.toIntExact(getGridNode().getGrid().getStorageService().getInventory().extract(AEItemKey.of(CrazyItemRegistrar.XP_SHARD_ITEM.get()), Integer.MAX_VALUE, Actionable.SIMULATE, IActionSource.ofMachine(this)));
            this.menu.xp = this.xp;
        }
        var outStack = this.outputInv.getStackInSlot(0);
        if (this.option != 0 && outStack == ItemStack.EMPTY || outStack.getItem() == Items.AIR){
            ItemStack enchanted = performEnchant(this.inputInv.getStackInSlot(0), this.option);
            if (enchanted != inputInv.getStackInSlot(0)){
                this.inputInv.getSlotInv(0).extractItem(0, 1, false);
                this.outputInv.setItemDirect(0, enchanted);
            }
        }
        if (this.autoSupplyLapis && getGridNode() != null){
            int toSupply = this.lapisInv.getSlotLimit(0) - this.lapisInv.getStackInSlot(0).getCount();
            int extracted = Math.toIntExact(StorageHelper.poweredExtraction(
                    getGridNode().getGrid().getEnergyService(),
                    getGridNode().getGrid().getStorageService().getInventory(),
                    AEItemKey.of(Items.LAPIS_LAZULI),
                    toSupply,
                    IActionSource.ofMachine(this),
                    Actionable.MODULATE
            ));
            ItemStack is = Items.LAPIS_LAZULI.getDefaultInstance();
            is.setCount(extracted);
            this.lapisInv.addItems(is);
        }
        if (this.autoSupplyBooks && getGridNode() != null &&
               (this.inputInv.getStackInSlot(0).getItem() == Items.BOOK
                       || this.inputInv.getStackInSlot(0).isEmpty()
                       || this.inputInv.getStackInSlot(0).getItem() == Items.AIR)){
            int toSupply = this.inputInv.getSlotLimit(0) - this.inputInv.getStackInSlot(0).getCount();
            int extracted = Math.toIntExact(StorageHelper.poweredExtraction(
                    getGridNode().getGrid().getEnergyService(),
                    getGridNode().getGrid().getStorageService().getInventory(),
                    AEItemKey.of(Items.BOOK),
                    toSupply,
                    IActionSource.ofMachine(this),
                    Actionable.MODULATE
            ));
            ItemStack is = Items.BOOK.getDefaultInstance();
            is.setCount(extracted);
            this.inputInv.addItems(is);
        }
        this.levelCost = Utils.shortenNumber(levelToXp(
                EnchantmentHelper.getEnchantmentCost(
                        RandomSource.create(),
                        this.option,
                        countBookshelves(this.getBlockPos().relative(Direction.UP)),
                        this.inputInv.getStackInSlot(0)
                )
        ));
        if (this.menu != null){
            this.menu.levelCost = this.levelCost;
        }
        return TickRateModulation.IDLE;
    }

    @Override
    public Component getDisplayName() {
        return super.getDisplayName();
    }

    public void setMenu(AutoEnchanterMenu autoEnchanterMenu) {
        this.menu = autoEnchanterMenu;
    }
}
