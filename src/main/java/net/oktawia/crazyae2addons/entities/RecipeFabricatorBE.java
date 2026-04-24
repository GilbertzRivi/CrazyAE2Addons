package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.menus.block.RecipeFabricatorMenu;
import net.oktawia.crazyae2addons.recipes.FabricationRecipe;
import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import net.oktawia.crazyae2addons.util.IMenuOpeningBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecipeFabricatorBE extends AENetworkInvBlockEntity
        implements MenuProvider, IGridTickable, IUpgradeableObject, IMenuOpeningBlockEntity, IManagedBEHelper {

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(RecipeFabricatorBE.class);

    private static final int FIXED_DURATION = 10;

    private static final int INPUT_SLOTS = 6;
    private static final int OUTPUT_SLOT_INDEX = 6;
    private static final int TOTAL_SLOTS = INPUT_SLOTS + 1;

    private static final double IDLE_POWER_USAGE = 2.0;
    private static final double ACTIVE_POWER_USAGE = 16.0;

    private static final int TANK_CAPACITY = 8 * 1000;

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @LazyManaged
    public final AppEngInternalInventory inv = new AppEngInternalInventory(this, TOTAL_SLOTS);

    public final InternalInventory input = inv.getSubInventory(0, INPUT_SLOTS);
    public final InternalInventory output = inv.getSubInventory(OUTPUT_SLOT_INDEX, OUTPUT_SLOT_INDEX + 1);

    @Persisted
    @LazyManaged
    private final FluidTank fluidIn = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            markForUpdate();
            if (!isClientSide()) {
                syncManaged();
            }
        }
    };

    @Persisted
    @LazyManaged
    private final FluidTank fluidOut = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            markForUpdate();
            if (!isClientSide()) {
                syncManaged();
            }
        }
    };

    @Persisted
    @DescSynced
    private int progress = 0;

    @Nullable
    private FabricationRecipe active = null;

    @Nullable
    private Direction lastInputSide = null;

    private final IFluidHandler menuInputHandler = new SingleTankHandler(fluidIn, true, true);
    private final IFluidHandler menuOutputHandler = new SingleTankHandler(fluidOut, false, true);

    public RecipeFabricatorBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.RECIPE_FABRICATOR_BE.get(), pos, state);

        this.inv.setFilter(new IAEItemFilter() {
            @Override
            public boolean allowInsert(InternalInventory inventory, int slot, ItemStack stack) {
                return slot >= 0 && slot < INPUT_SLOTS;
            }
        });

        this.getMainNode()
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get().asItem()));

        this.getMainNode().setIdlePowerUsage(IDLE_POWER_USAGE);
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveManagedData(tag);

        if (lastInputSide != null) {
            tag.putInt("lastInputSide", lastInputSide.get3DDataValue());
        }
    }

    @Override
    public void loadTag(CompoundTag tag) {
        loadManagedData(tag);

        if (tag.contains("lastInputSide", Tag.TAG_INT)) {
            this.lastInputSide = Direction.from3DDataValue(tag.getInt("lastInputSide"));
        } else {
            this.lastInputSide = null;
        }

        super.loadTag(tag);
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory invPlayer, Player player) {
        return new RecipeFabricatorMenu(id, invPlayer, this);
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        if (!player.level().isClientSide) {
            forceSyncManaged();
        }
        MenuOpener.open(CrazyMenuRegistrar.RECIPE_FABRICATOR_MENU.get(), player, locator);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            final Direction capSide = side;

            return LazyOptional.of(() -> new IItemHandler() {
                @Override
                public int getSlots() {
                    return 1;
                }

                @Override
                public @NotNull ItemStack getStackInSlot(int slot) {
                    return output.getStackInSlot(0);
                }

                @Override
                public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (stack.isEmpty()) {
                        return ItemStack.EMPTY;
                    }

                    if (!simulate) {
                        lastInputSide = capSide;
                    }

                    return insertIntoInputs(stack, simulate);
                }

                @Override
                public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return output.extractItem(0, amount, simulate);
                }

                @Override
                public int getSlotLimit(int slot) {
                    return 64;
                }

                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) {
                    return true;
                }
            }).cast();
        }

        if (cap == ForgeCapabilities.FLUID_HANDLER) {
            final Direction capSide = side;

            return LazyOptional.of(() -> new IFluidHandler() {
                @Override
                public int getTanks() {
                    return 2;
                }

                @Override
                public @NotNull FluidStack getFluidInTank(int tank) {
                    return tank == 0 ? fluidIn.getFluid() : fluidOut.getFluid();
                }

                @Override
                public int getTankCapacity(int tank) {
                    return tank == 0 ? fluidIn.getCapacity() : fluidOut.getCapacity();
                }

                @Override
                public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
                    return tank == 0;
                }

                @Override
                public int fill(FluidStack resource, FluidAction action) {
                    if (resource.isEmpty()) {
                        return 0;
                    }

                    if (capSide != null && action.execute()) {
                        lastInputSide = capSide;
                    }

                    return fluidIn.fill(resource, action);
                }

                @Override
                public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                    if (resource.isEmpty()) {
                        return FluidStack.EMPTY;
                    }

                    return fluidOut.drain(resource, action);
                }

                @Override
                public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                    if (maxDrain <= 0) {
                        return FluidStack.EMPTY;
                    }

                    return fluidOut.drain(maxDrain, action);
                }
            }).cast();
        }

        return super.getCapability(cap, side);
    }

    private @NotNull ItemStack insertIntoInputs(@NotNull ItemStack stack, boolean simulate) {
        ItemStack remaining = stack;
        for (int i = 0; i < input.size() && !remaining.isEmpty(); i++) {
            remaining = input.insertItem(i, remaining, simulate);
        }
        return remaining;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return inv;
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (Objects.equals(id, ISegmentedInventory.STORAGE)) {
            return getInternalInventory();
        }
        return super.getSubInventory(id);
    }

    @Override
    public void onChangeInventory(InternalInventory changed, int slot) {
        setChanged();
        if (!isClientSide()) {
            syncManaged();
        }
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 5, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide) {
            return TickRateModulation.IDLE;
        }

        IGrid grid = node.getGrid();
        if (grid == null) {
            setIdlePowerUsage(IDLE_POWER_USAGE);
            cancelWork();
            return TickRateModulation.IDLE;
        }

        IEnergyService energy = grid.getEnergyService();
        if (!energy.isNetworkPowered()) {
            setIdlePowerUsage(IDLE_POWER_USAGE);
            if (active != null) {
                return TickRateModulation.SLEEP;
            }
            return TickRateModulation.IDLE;
        }

        if (active == null) {
            active = findStartableRecipe();
            progress = 0;
            syncManaged();

            if (active == null) {
                setIdlePowerUsage(IDLE_POWER_USAGE);
                return TickRateModulation.IDLE;
            }
        }

        if (!stillValid(active)) {
            cancelWork();
            setIdlePowerUsage(IDLE_POWER_USAGE);
            return TickRateModulation.IDLE;
        }

        setIdlePowerUsage(ACTIVE_POWER_USAGE);

        progress++;
        syncManaged();

        if (progress >= FIXED_DURATION) {
            finish(active);
            cancelWork();
            return TickRateModulation.IDLE;
        }

        setChanged();
        return TickRateModulation.URGENT;
    }

    private void setIdlePowerUsage(double amount) {
        this.getMainNode().setIdlePowerUsage(amount);
    }

    private void cancelWork() {
        active = null;
        progress = 0;
        setIdlePowerUsage(IDLE_POWER_USAGE);
        setChanged();
        if (!isClientSide()) {
            syncManaged();
        }
    }

    private void finish(FabricationRecipe recipe) {
        for (FabricationRecipe.Entry entry : recipe.getInputs()) {
            int toConsume = entry.count();

            for (int slot = 0; slot < input.size() && toConsume > 0; slot++) {
                ItemStack stack = input.getStackInSlot(slot).copy();
                if (stack.isEmpty()) {
                    continue;
                }
                if (!entry.ingredient().test(stack)) {
                    continue;
                }

                int remove = Math.min(toConsume, stack.getCount());
                stack.shrink(remove);
                toConsume -= remove;

                input.setItemDirect(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }

        FluidStack requiredFluid = recipe.getFluidInput();
        if (!requiredFluid.isEmpty()) {
            fluidIn.drain(requiredFluid.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        ItemStack resultItem = recipe.getOutput().copy();
        if (!resultItem.isEmpty()) {
            ItemStack currentOut = output.getStackInSlot(0);
            if (currentOut.isEmpty()) {
                output.setItemDirect(0, resultItem);
            } else {
                currentOut.grow(resultItem.getCount());
                output.setItemDirect(0, currentOut);
            }
        }

        FluidStack resultFluid = recipe.getFluidOutput();
        if (!resultFluid.isEmpty()) {
            fluidOut.fill(resultFluid.copy(), IFluidHandler.FluidAction.EXECUTE);
        }

        tryAutoEjectOutputs();

        setChanged();
        markForUpdate();

        if (!isClientSide()) {
            syncManaged();
        }
    }

    private List<Direction> getEjectOrder() {
        ArrayList<Direction> dirs = new ArrayList<>(6);

        if (lastInputSide != null) {
            dirs.add(lastInputSide);
        }

        for (Direction dir : Direction.values()) {
            if (dir != lastInputSide) {
                dirs.add(dir);
            }
        }

        return dirs;
    }

    private void tryAutoEjectOutputs() {
        if (level == null || level.isClientSide) {
            return;
        }

        ItemStack itemOut = output.getStackInSlot(0);
        FluidStack fluid = fluidOut.getFluid();

        if (itemOut.isEmpty() && fluid.isEmpty()) {
            return;
        }

        for (Direction dir : getEjectOrder()) {
            BlockPos targetPos = worldPosition.relative(dir);
            BlockEntity blockEntity = level.getBlockEntity(targetPos);
            if (blockEntity == null) {
                continue;
            }

            IItemHandler itemHandler = null;
            if (!itemOut.isEmpty()) {
                itemHandler = blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
                if (itemHandler == null) {
                    continue;
                }
            }

            IFluidHandler fluidHandler = null;
            if (!fluid.isEmpty()) {
                fluidHandler = blockEntity.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
                if (fluidHandler == null) {
                    continue;
                }
            }

            boolean itemOk = itemOut.isEmpty()
                    || ItemHandlerHelper.insertItem(itemHandler, itemOut.copy(), true).isEmpty();

            boolean fluidOk = fluid.isEmpty()
                    || fluidHandler.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE) >= fluid.getAmount();

            if (!itemOk || !fluidOk) {
                continue;
            }

            if (!itemOut.isEmpty()) {
                ItemStack remainder = ItemHandlerHelper.insertItem(itemHandler, itemOut.copy(), false);
                output.setItemDirect(0, remainder);
            }

            if (!fluid.isEmpty()) {
                int filled = fluidHandler.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    fluidOut.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
            }

            setChanged();
            markForUpdate();
            syncManaged();
            return;
        }
    }

    @Nullable
    private FabricationRecipe findStartableRecipe() {
        if (level == null) {
            return null;
        }

        SimpleContainer container = buildInputContainer();
        var recipes = level.getRecipeManager().getAllRecipesFor(CrazyRecipes.FABRICATION_TYPE.get());

        for (FabricationRecipe recipe : recipes) {
            if (!recipe.matches(container, level)) {
                continue;
            }

            if (!hasAllOutputsSpaceFor(recipe)) {
                continue;
            }

            if (!hasAllFluidInputsFor(recipe)) {
                continue;
            }

            return recipe;
        }

        return null;
    }

    private boolean stillValid(FabricationRecipe recipe) {
        if (level == null) {
            return false;
        }

        SimpleContainer container = buildInputContainer();
        return recipe.matches(container, level)
                && hasAllOutputsSpaceFor(recipe)
                && hasAllFluidInputsFor(recipe);
    }

    private boolean hasAllFluidInputsFor(FabricationRecipe recipe) {
        FluidStack required = recipe.getFluidInput();
        if (required.isEmpty()) {
            return true;
        }

        FluidStack current = fluidIn.getFluid();
        if (current.isEmpty()) {
            return false;
        }
        if (!current.isFluidEqual(required)) {
            return false;
        }

        return current.getAmount() >= required.getAmount();
    }

    private boolean hasAllOutputsSpaceFor(FabricationRecipe recipe) {
        return hasItemOutputSpaceFor(recipe) && hasFluidOutputSpaceFor(recipe);
    }

    private boolean hasItemOutputSpaceFor(FabricationRecipe recipe) {
        ItemStack want = recipe.getOutput();
        if (want.isEmpty()) {
            return true;
        }

        ItemStack out = output.getStackInSlot(0);
        if (out.isEmpty()) {
            return want.getCount() <= want.getMaxStackSize();
        }
        if (!ItemStack.isSameItemSameTags(out, want)) {
            return false;
        }

        return out.getCount() + want.getCount() <= out.getMaxStackSize();
    }

    private boolean hasFluidOutputSpaceFor(FabricationRecipe recipe) {
        FluidStack want = recipe.getFluidOutput();
        if (want.isEmpty()) {
            return true;
        }

        FluidStack out = fluidOut.getFluid();
        if (out.isEmpty()) {
            return want.getAmount() <= fluidOut.getCapacity();
        }
        if (!out.isFluidEqual(want)) {
            return false;
        }

        return out.getAmount() + want.getAmount() <= fluidOut.getCapacity();
    }

    private SimpleContainer buildInputContainer() {
        ItemStack[] stacks = new ItemStack[input.size()];
        for (int i = 0; i < input.size(); i++) {
            stacks[i] = input.getStackInSlot(i);
        }
        return new SimpleContainer(stacks);
    }

    public int getProgress() {
        return progress;
    }

    public int getDuration() {
        return FIXED_DURATION;
    }

    public FluidStack getFluidIn() {
        return fluidIn.getFluid().copy();
    }

    public FluidStack getFluidOut() {
        return fluidOut.getFluid().copy();
    }

    public int getFluidCapacity() {
        return TANK_CAPACITY;
    }

    public IFluidHandler getMenuInputHandler() {
        return menuInputHandler;
    }

    public IFluidHandler getMenuOutputHandler() {
        return menuOutputHandler;
    }

    private record SingleTankHandler(FluidTank tank, boolean allowFill, boolean allowDrain) implements IFluidHandler {

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public @NotNull FluidStack getFluidInTank(int tankIdx) {
            return tank.getFluid();
        }

        @Override
        public int getTankCapacity(int tankIdx) {
            return tank.getCapacity();
        }

        @Override
        public boolean isFluidValid(int tankIdx, @NotNull FluidStack stack) {
            return allowFill && this.tank.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!allowFill || resource.isEmpty()) {
                return 0;
            }
            return tank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (!allowDrain || resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return tank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowDrain || maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            return tank.drain(maxDrain, action);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public boolean isClientSide() {
        return level == null || level.isClientSide;
    }
}