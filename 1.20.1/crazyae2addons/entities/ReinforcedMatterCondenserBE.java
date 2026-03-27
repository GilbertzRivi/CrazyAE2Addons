package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.ReinforcedMatterCondenserMenu;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;

public class ReinforcedMatterCondenserBE extends AEBaseInvBlockEntity implements MenuProvider {

    private static final long   MAX_POWER = 8192;
    private static final int    MAX_CELLS = 64;

    public BaseInternalInventory inputInv    = new CondenseHandler();
    public AppEngInternalInventory inv = new AppEngInternalInventory(this, 2, 64, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.getItem() == AEItems.CELL_COMPONENT_256K.asItem() || stack.getItem() == CrazyItemRegistrar.SUPER_SINGULARITY.get().asItem();
        }
    });
    public InternalInventory outputInv = inv.getSubInventory(0, 1);
    public InternalInventory componentInv= inv.getSubInventory(1, 2);
    public long   storedPower = 0;

    public ReinforcedMatterCondenserBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.REINFORCED_MATTER_CONDENSER_BE.get(), pos, blockState);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            return LazyOptional.of(() -> new IItemHandler() {
                @Override public int getSlots()                       { return 1; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) {
                    return outputInv.getStackInSlot(0);
                }
                @Override
                public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (stack.getItem() != AEItems.SINGULARITY.asItem() || !isComponentFull() || outputInv.getStackInSlot(0).getCount() == 64) {
                        return stack;
                    }
                    int count = stack.getCount();
                    long overflow = canAddPower(count);
                    int accepted = count - (int) overflow;
                    if (accepted <= 0) {
                        return stack;
                    }
                    if (simulate) {
                        return overflow > 0
                                ? new ItemStack(AEItems.SINGULARITY.asItem(), (int) overflow)
                                : ItemStack.EMPTY;
                    }
                    addPower(accepted);
                    setChanged();
                    return overflow > 0
                            ? new ItemStack(AEItems.SINGULARITY.asItem(), (int) overflow)
                            : ItemStack.EMPTY;
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return outputInv.extractItem(0, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return Integer.MAX_VALUE; }
                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    return stack.getItem() == AEItems.SINGULARITY.asItem()
                            && isComponentFull();
                }
            }).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public InternalInventory getInternalInventory() {
        return inv;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        this.setChanged();
    }

    public boolean isComponentFull() {
        return this.componentInv.getStackInSlot(0).getCount() >= MAX_CELLS;
    }

    public long canAddPower(long attempt) {
        long available = MAX_POWER - this.storedPower;
        if (attempt <= available) {
            return 0L;
        }
        return attempt - available;
    }

    public boolean canAddOutput() {
        ItemStack out = outputInv.getStackInSlot(0);
        return out.isEmpty() ||
                (out.getItem() == AEItems.SINGULARITY.asItem() && out.getCount() < outputInv.getSlotLimit(0));
    }

    public long addPower(long amount) {
        long overflow = canAddPower(amount);
        long accepted = amount - overflow;
        this.storedPower += accepted;

        if (this.storedPower >= MAX_POWER){
            outputInv.insertItem(
                    0,
                    CrazyItemRegistrar.SUPER_SINGULARITY.get().getDefaultInstance(),
                    false
            );
            this.storedPower = 0;
            setChanged();
        }

        return overflow;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new ReinforcedMatterCondenserMenu(i, inventory, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.REINFORCED_MATTER_CONDENSER_MENU.get(), player, locator);
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.reinforced_matter_condenser");
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("storedpower")) {
            this.storedPower = data.getLong("storedpower");
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putLong("storedpower", this.storedPower);
    }

    private class CondenseHandler extends BaseInternalInventory {
        @Override public int size() { return 1; }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == AEItems.SINGULARITY.asItem()
                    && isComponentFull();
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            if (!stack.isEmpty()
                    && stack.getItem() == AEItems.SINGULARITY.asItem()
                    && isComponentFull()
                    && canAddOutput()) {
                addPower(stack.getCount());
            }
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (!isComponentFull()) {
                return stack;
            }
            if (!canAddOutput()) {
                return stack;
            }
            if (!simulate && !stack.isEmpty() && canAddPower(stack.getCount()) < stack.getCount()) {
                addPower(canAddPower(stack.getCount()));
            }
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }
}
