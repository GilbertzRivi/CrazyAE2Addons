package net.oktawia.insaneae2addons.entities.penrose;

import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import lombok.Getter;

import appeng.api.inventories.BaseInternalInventory;
import appeng.api.inventories.InternalInventory;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.CombinedInternalInventory;
import appeng.util.inv.FilteredInternalInventory;
import appeng.util.inv.filter.AEItemFilters;
import appeng.util.inv.filter.IAEItemFilter;

import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import net.oktawia.crazyae2addons.util.IMenuOpeningBlockEntity;
import net.oktawia.insaneae2addons.defs.regs.InsaneBlockEntityRegistrar;
import net.oktawia.insaneae2addons.defs.regs.InsaneItemRegistrar;
import net.oktawia.insaneae2addons.defs.regs.InsaneMenuRegistrar;
import net.oktawia.insaneae2addons.menus.block.ReinforcedMatterCondenserMenu;

public class ReinforcedMatterCondenserBE extends AEBaseInvBlockEntity
        implements MenuProvider, IManagedBEHelper, IMenuOpeningBlockEntity {

    public static final int SINGULARITIES_PER_SUPER = 8192;
    public static final int REQUIRED_CELL_COMPONENTS = 64;

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(
            ReinforcedMatterCondenserBE.class);

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @DescSynced
    @Getter
    private int storedSingularities = 0;

    @Getter
    private final AppEngInternalInventory outputInventory = new AppEngInternalInventory(this, 1, 64,
            onlyItem(InsaneItemRegistrar.SUPER_SINGULARITY.get()));

    @Getter
    private final AppEngInternalInventory componentInventory = new AppEngInternalInventory(this, 1,
            REQUIRED_CELL_COMPONENTS,
            onlyItem(AEItems.CELL_COMPONENT_256K.asItem()));

    @Getter
    private final InternalInventory inputInventory = new CondenseInventory();

    private final InternalInventory exposedInventory = new CombinedInternalInventory(this.inputInventory,
            new FilteredInternalInventory(this.outputInventory, AEItemFilters.EXTRACT_ONLY));

    public ReinforcedMatterCondenserBE(BlockPos pos, BlockState blockState) {
        super(InsaneBlockEntityRegistrar.REINFORCED_MATTER_CONDENSER_BE.get(), pos, blockState);
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
    public InternalInventory getInternalInventory() {
        return new CombinedInternalInventory(this.outputInventory, this.componentInventory);
    }

    @Override
    protected InternalInventory getExposedInventoryForSide(Direction side) {
        return this.exposedInventory;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        setChanged();
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ReinforcedMatterCondenserMenu(id, inventory, this);
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(InsaneMenuRegistrar.REINFORCED_MATTER_CONDENSER_MENU.get(), player, locator);
    }

    public boolean hasCompressionMatrix() {
        return getInstalledCellComponents() >= REQUIRED_CELL_COMPONENTS;
    }

    public int getInstalledCellComponents() {
        return this.componentInventory.getStackInSlot(0).getCount();
    }

    private boolean canPushOutput() {
        return this.outputInventory
                .insertItem(0, InsaneItemRegistrar.SUPER_SINGULARITY.get().getDefaultInstance(), true).isEmpty();
    }

    private int freeCapacity() {
        return SINGULARITIES_PER_SUPER - this.storedSingularities;
    }

    private void compress(int amount) {
        this.storedSingularities += amount;

        if (this.storedSingularities >= SINGULARITIES_PER_SUPER
                && this.outputInventory
                        .insertItem(0, InsaneItemRegistrar.SUPER_SINGULARITY.get().getDefaultInstance(), false)
                        .isEmpty()) {
            this.storedSingularities -= SINGULARITIES_PER_SUPER;
        }

        setChanged();
        syncManaged();
    }

    private static IAEItemFilter onlyItem(Item allowed) {
        return new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
                return stack.getItem() == allowed;
            }
        };
    }

    private final class CondenseInventory extends BaseInternalInventory {

        @Override
        public int size() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return SINGULARITIES_PER_SUPER;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() == AEItems.SINGULARITY.asItem() && hasCompressionMatrix();
        }

        @Override
        public void setItemDirect(int slotIndex, ItemStack stack) {
            insertItem(slotIndex, stack, false);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            if (stack.isEmpty() || !isItemValid(slot, stack) || !canPushOutput()) {
                return stack;
            }

            int accepted = Math.min(stack.getCount(), freeCapacity());
            if (accepted <= 0) {
                return stack;
            }

            if (!simulate) {
                compress(accepted);
            }

            int leftover = stack.getCount() - accepted;
            return leftover <= 0 ? ItemStack.EMPTY : new ItemStack(stack.getItem(), leftover);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    }
}
