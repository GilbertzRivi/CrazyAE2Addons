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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
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
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.*;
import net.oktawia.crazyae2addons.menus.RecipeFabricatorMenu;
import net.oktawia.crazyae2addons.recipes.FabricationRecipe;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class RecipeFabricatorBE extends AENetworkInvBlockEntity
        implements MenuProvider, IGridTickable, IUpgradeableObject {

    private static final String NBT_KEYS = "keys";
    private static final int FIXED_DURATION = 10;

    private static final int INPUT_SLOTS = 6;
    private static final int DRIVE_SLOT_INDEX = 6;
    private static final int OUTPUT_SLOT_INDEX = 7;
    private static final int TOTAL_SLOTS = INPUT_SLOTS + 2;

    private static final double IDLE_POWER_USAGE = 2.0;
    private static final double ACTIVE_POWER_USAGE = 16.0;

    private static final int TANK_CAPACITY = 8 * 1000;

    public final AppEngInternalInventory inv =
            new AppEngInternalInventory(this, TOTAL_SLOTS);

    public final InternalInventory input =
            inv.getSubInventory(0, INPUT_SLOTS);
    public final InternalInventory drive =
            inv.getSubInventory(DRIVE_SLOT_INDEX, DRIVE_SLOT_INDEX + 1);
    public final InternalInventory output =
            inv.getSubInventory(OUTPUT_SLOT_INDEX, OUTPUT_SLOT_INDEX + 1);

    // FLUID IO
    private final FluidTank fluidIn = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            markForUpdate();
        }
    };

    private final FluidTank fluidOut = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            markForUpdate();
        }
    };

    private int progress = 0;
    @Nullable
    private FabricationRecipe active = null;

    private int diskProgress = 0;
    private int diskInputSlot = -1;

    @Nullable
    private Direction lastInputSide = null;

    public RecipeFabricatorBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.RECIPE_FABRICATOR_BE.get(), pos, state);
        this.getMainNode()
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(
                                CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get()
                                        .asItem()));
        this.getMainNode().setIdlePowerUsage(IDLE_POWER_USAGE);
    }

    private boolean researchEnabled() {
        return CrazyConfig.COMMON.ResearchRequired.get();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory invPlayer, Player player) {
        return new RecipeFabricatorMenu(id, invPlayer, this);
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.RECIPE_FABRICATOR_MENU.get(), player, locator);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER && side != null) {
            final Direction capSide = side; // capture
            return LazyOptional.of(() -> new IItemHandler() {
                @Override
                public int getSlots() { return 1; }

                @Override
                public @NotNull ItemStack getStackInSlot(int slot) {
                    return output.getStackInSlot(0);
                }

                @Override
                public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    if (stack.isEmpty()) return ItemStack.EMPTY;

                    lastInputSide = capSide;

                    if (researchEnabled() && isDiskItem(stack)) {
                        return drive.insertItem(0, stack, simulate);
                    } else {
                        return insertIntoInputs(stack, simulate);
                    }
                }

                @Override
                public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return output.extractItem(0, amount, simulate);
                }

                @Override
                public int getSlotLimit(int slot) { return 64; }

                @Override
                public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
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
                    if (resource.isEmpty()) return 0;

                    if (capSide != null) {
                        lastInputSide = capSide;
                    }

                    return fluidIn.fill(resource, action);
                }

                @Override
                public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
                    if (resource.isEmpty()) return FluidStack.EMPTY;
                    return fluidOut.drain(resource, action);
                }

                @Override
                public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
                    if (maxDrain <= 0) return FluidStack.EMPTY;
                    return fluidOut.drain(maxDrain, action);
                }
            }).cast();
        }

        return super.getCapability(cap, side);
    }

    private static boolean isDiskItem(ItemStack st) {
        return st.is(CrazyItemRegistrar.DATA_DRIVE.get());
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
        if (Objects.equals(id, ISegmentedInventory.STORAGE)) return getInternalInventory();
        return super.getSubInventory(id);
    }

    @Override
    public void onChangeInventory(InternalInventory changed, int slot) {
        setChanged();
        markForUpdate();
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);

        if (tag.contains("inv")) {
            this.inv.readFromNBT(tag, "inv");
        }

        if (tag.contains("fluidIn", Tag.TAG_COMPOUND)) {
            fluidIn.readFromNBT(tag.getCompound("fluidIn"));
        }
        if (tag.contains("fluidOut", Tag.TAG_COMPOUND)) {
            fluidOut.readFromNBT(tag.getCompound("fluidOut"));
        }

        if (tag.contains("lastInputSide", Tag.TAG_INT)) {
            this.lastInputSide = Direction.from3DDataValue(tag.getInt("lastInputSide"));
        } else {
            this.lastInputSide = null;
        }
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);

        this.inv.writeToNBT(tag, "inv");

        tag.put("fluidIn", fluidIn.writeToNBT(new CompoundTag()));
        tag.put("fluidOut", fluidOut.writeToNBT(new CompoundTag()));

        if (lastInputSide != null) {
            tag.putInt("lastInputSide", lastInputSide.get3DDataValue());
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
            cancelDiskWork();
            return TickRateModulation.IDLE;
        }

        IEnergyService energy = grid.getEnergyService();
        if (!energy.isNetworkPowered()) {
            setIdlePowerUsage(IDLE_POWER_USAGE);
            if (active != null || diskInputSlot != -1) {
                return TickRateModulation.SLEEP;
            }
            return TickRateModulation.IDLE;
        }

        if (!researchEnabled()) {
            cancelDiskWork();
        }

        if (researchEnabled() && handleDiskMergeTick()) {
            return TickRateModulation.URGENT;
        }

        if (active == null) {
            active = findStartableRecipe();
            progress = 0;
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
        if (progress >= FIXED_DURATION) {
            finish(active);
            cancelWork();
            return TickRateModulation.IDLE;
        }

        setChanged();
        return TickRateModulation.URGENT;
    }

    private boolean handleDiskMergeTick() {
        if (level == null) return false;

        ItemStack src = drive.getStackInSlot(0);
        if (src.isEmpty() || !isDiskItem(src)) {
            cancelDiskWork();
            return false;
        }

        if (diskInputSlot == -1) {
            int slot = findDiskInInputs(src);
            if (slot == -1) return false;

            ItemStack preview = buildMergedDiskPreview(src, input.getStackInSlot(slot));
            if (preview.isEmpty() || !hasItemOutputSpaceForStack(preview)) return false;

            diskInputSlot = slot;
            diskProgress = 0;

            cancelWork();
        } else {
            ItemStack in = input.getStackInSlot(diskInputSlot);
            if (in.isEmpty() || !ItemStack.isSameItem(in, src)) {
                cancelDiskWork();
                setIdlePowerUsage(IDLE_POWER_USAGE);
                return false;
            }

            ItemStack preview = buildMergedDiskPreview(src, in);
            if (preview.isEmpty() || !hasItemOutputSpaceForStack(preview)) {
                cancelDiskWork();
                setIdlePowerUsage(IDLE_POWER_USAGE);
                return false;
            }
        }

        setIdlePowerUsage(ACTIVE_POWER_USAGE);

        diskProgress++;
        if (diskProgress >= FIXED_DURATION) {
            doDiskMerge(diskInputSlot);
            cancelDiskWork();
            setIdlePowerUsage(IDLE_POWER_USAGE);
            return true;
        }

        setChanged();
        return true;
    }

    private int findDiskInInputs(ItemStack srcDisk) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack st = input.getStackInSlot(i);
            if (st.isEmpty()) continue;
            if (ItemStack.isSameItem(st, srcDisk)) {
                return i;
            }
        }
        return -1;
    }

    private boolean hasItemOutputSpaceForStack(ItemStack want) {
        ItemStack out = output.getStackInSlot(0);
        if (out.isEmpty()) {
            return want.getCount() <= want.getMaxStackSize();
        }
        if (!ItemStack.isSameItemSameTags(out, want)) {
            return false;
        }
        return out.getCount() + want.getCount() <= out.getMaxStackSize();
    }

    private ItemStack buildMergedDiskPreview(ItemStack srcDisk, ItemStack inputDisk) {
        if (srcDisk.isEmpty() || inputDisk.isEmpty()) return ItemStack.EMPTY;
        if (!ItemStack.isSameItem(srcDisk, inputDisk)) return ItemStack.EMPTY;

        ItemStack result = inputDisk.copy();
        result.setCount(1);

        CompoundTag resTag = result.getOrCreateTag();
        ListTag resKeys = getOrCreateKeys(resTag);

        CompoundTag srcTag = srcDisk.getTag();
        if (srcTag != null && srcTag.contains(NBT_KEYS, Tag.TAG_LIST)) {
            ListTag srcKeys = srcTag.getList(NBT_KEYS, Tag.TAG_STRING);
            unionKeys(resKeys, srcKeys);
        }

        return result;
    }

    private void doDiskMerge(int slot) {
        if (level == null) return;

        ItemStack srcDisk = drive.getStackInSlot(0);
        ItemStack inDisk  = input.getStackInSlot(slot);

        if (srcDisk.isEmpty() || inDisk.isEmpty()) return;
        if (!isDiskItem(srcDisk) || !ItemStack.isSameItem(srcDisk, inDisk)) return;

        ItemStack result = buildMergedDiskPreview(srcDisk, inDisk);
        if (result.isEmpty() || !hasItemOutputSpaceForStack(result)) return;

        ItemStack inCopy = inDisk.copy();
        inCopy.shrink(1);
        input.setItemDirect(slot, inCopy.isEmpty() ? ItemStack.EMPTY : inCopy);

        ItemStack outCur = output.getStackInSlot(0);
        if (outCur.isEmpty()) {
            output.setItemDirect(0, result);
        } else {
            outCur.grow(result.getCount());
            output.setItemDirect(0, outCur);
        }

        setChanged();
        markForUpdate();
    }

    private static ListTag getOrCreateKeys(CompoundTag tag) {
        if (tag.contains(NBT_KEYS, Tag.TAG_LIST)) {
            return tag.getList(NBT_KEYS, Tag.TAG_STRING);
        }
        ListTag list = new ListTag();
        tag.put(NBT_KEYS, list);
        return list;
    }

    private static void unionKeys(ListTag target, ListTag src) {
        for (int i = 0; i < src.size(); i++) {
            String k = src.getString(i);
            if (!containsString(target, k)) {
                target.add(StringTag.valueOf(k));
            }
        }
    }

    private static boolean containsString(ListTag list, String s) {
        for (int i = 0; i < list.size(); i++) {
            if (s.equals(list.getString(i))) return true;
        }
        return false;
    }

    private void setIdlePowerUsage(double amount) {
        this.getMainNode().setIdlePowerUsage(amount);
    }

    private void cancelWork() {
        active = null;
        progress = 0;
        setIdlePowerUsage(IDLE_POWER_USAGE);
        setChanged();
    }

    private void cancelDiskWork() {
        diskInputSlot = -1;
        diskProgress = 0;
        setChanged();
    }

    private void finish(FabricationRecipe r) {
        for (FabricationRecipe.Entry entry : r.getInputs()) {
            int toConsume = entry.count();

            for (int slot = 0; slot < input.size() && toConsume > 0; slot++) {
                ItemStack stack = input.getStackInSlot(slot).copy();
                if (stack.isEmpty()) continue;
                if (!entry.ingredient().test(stack)) continue;

                int remove = Math.min(toConsume, stack.getCount());
                stack.shrink(remove);
                toConsume -= remove;

                input.setItemDirect(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
            }
        }

        FluidStack fin = r.getFluidInput();
        if (!fin.isEmpty()) {
            fluidIn.drain(fin.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        }

        ItemStack outWant = r.getOutput().copy();
        if (!outWant.isEmpty()) {
            ItemStack outCur = output.getStackInSlot(0);
            if (outCur.isEmpty()) {
                output.setItemDirect(0, outWant);
            } else {
                outCur.grow(outWant.getCount());
                output.setItemDirect(0, outCur);
            }
        }

        FluidStack fout = r.getFluidOutput();
        if (!fout.isEmpty()) {
            fluidOut.fill(fout.copy(), IFluidHandler.FluidAction.EXECUTE);
        }

        tryAutoEjectOutputs();

        setChanged();
        markForUpdate();
    }

    private List<Direction> getEjectOrder() {
        ArrayList<Direction> dirs = new ArrayList<>(6);
        if (lastInputSide != null) {
            dirs.add(lastInputSide);
        }
        for (Direction d : Direction.values()) {
            if (d != lastInputSide) dirs.add(d);
        }
        return dirs;
    }


    private void tryAutoEjectOutputs() {
        if (level == null || level.isClientSide) return;

        ItemStack itemOut = output.getStackInSlot(0);
        FluidStack fluid = fluidOut.getFluid();

        if (itemOut.isEmpty() && fluid.isEmpty()) return;

        for (Direction dir : getEjectOrder()) {
            BlockPos p = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(p);
            if (be == null) continue;

            IItemHandler itemHandler = null;
            if (!itemOut.isEmpty()) {
                itemHandler = be.getCapability(ForgeCapabilities.ITEM_HANDLER, dir.getOpposite()).orElse(null);
                if (itemHandler == null) continue;
            }

            IFluidHandler fluidHandler = null;
            if (!fluid.isEmpty()) {
                fluidHandler = be.getCapability(ForgeCapabilities.FLUID_HANDLER, dir.getOpposite()).orElse(null);
                if (fluidHandler == null) continue;
            }

            boolean itemOk = itemOut.isEmpty()
                    || ItemHandlerHelper.insertItem(itemHandler, itemOut.copy(), true).isEmpty();

            boolean fluidOk = fluid.isEmpty()
                    || fluidHandler.fill(fluid.copy(), IFluidHandler.FluidAction.SIMULATE) >= fluid.getAmount();

            if (!itemOk || !fluidOk) continue;

            if (!itemOut.isEmpty()) {
                ItemStack rem = ItemHandlerHelper.insertItem(itemHandler, itemOut.copy(), false);
                output.setItemDirect(0, rem);
                itemOut = rem;
            }

            if (!fluid.isEmpty()) {
                int filled = fluidHandler.fill(fluid.copy(), IFluidHandler.FluidAction.EXECUTE);
                if (filled > 0) {
                    fluidOut.drain(filled, IFluidHandler.FluidAction.EXECUTE);
                }
                fluid = fluidOut.getFluid();
            }

            setChanged();
            markForUpdate();
            return;
        }
    }

    @Nullable
    private FabricationRecipe findStartableRecipe() {
        if (level == null) return null;

        SimpleContainer container = buildInputContainer();
        var list = level.getRecipeManager().getAllRecipesFor(CrazyRecipes.FABRICATION_TYPE.get());

        for (FabricationRecipe r : list) {
            if (!r.matches(container, level)) continue;

            if (researchEnabled()
                    && r.getRequiredKey() != null
                    && !driveHasKey(r.getRequiredKey())) {
                continue;
            }

            if (!hasAllOutputsSpaceFor(r)) continue;
            if (!hasAllFluidInputsFor(r)) continue;

            return r;
        }
        return null;
    }

    private boolean stillValid(FabricationRecipe r) {
        if (level == null) return false;

        SimpleContainer container = buildInputContainer();
        if (!r.matches(container, level)) return false;

        if (researchEnabled()
                && r.getRequiredKey() != null
                && !driveHasKey(r.getRequiredKey())) {
            return false;
        }

        return hasAllOutputsSpaceFor(r) && hasAllFluidInputsFor(r);
    }

    private boolean hasAllFluidInputsFor(FabricationRecipe r) {
        FluidStack req = r.getFluidInput();
        if (req.isEmpty()) return true;

        FluidStack have = fluidIn.getFluid();
        if (have.isEmpty()) return false;
        if (!have.isFluidEqual(req)) return false;

        return have.getAmount() >= req.getAmount();
    }

    private boolean hasAllOutputsSpaceFor(FabricationRecipe r) {
        return hasItemOutputSpaceFor(r) && hasFluidOutputSpaceFor(r);
    }

    private boolean hasItemOutputSpaceFor(FabricationRecipe r) {
        ItemStack want = r.getOutput();
        if (want.isEmpty()) return true;

        ItemStack out = output.getStackInSlot(0);
        if (out.isEmpty()) {
            return want.getCount() <= want.getMaxStackSize();
        }
        if (!ItemStack.isSameItemSameTags(out, want)) {
            return false;
        }
        return out.getCount() + want.getCount() <= out.getMaxStackSize();
    }

    private boolean hasFluidOutputSpaceFor(FabricationRecipe r) {
        FluidStack want = r.getFluidOutput();
        if (want.isEmpty()) return true;

        FluidStack out = fluidOut.getFluid();
        if (out.isEmpty()) {
            return want.getAmount() <= fluidOut.getCapacity();
        }
        if (!out.isFluidEqual(want)) {
            return false;
        }
        return out.getAmount() + want.getAmount() <= fluidOut.getCapacity();
    }

    private boolean driveHasKey(@NotNull String key) {
        if (!researchEnabled()) return true;

        ItemStack d = drive.getStackInSlot(0);
        if (d.isEmpty()) return false;
        CompoundTag tag = d.getTag();
        if (tag == null || !tag.contains(NBT_KEYS, Tag.TAG_LIST)) return false;
        ListTag list = tag.getList(NBT_KEYS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (key.equals(list.getString(i))) return true;
        }
        return false;
    }

    private SimpleContainer buildInputContainer() {
        ItemStack[] stacks = new ItemStack[input.size()];
        for (int i = 0; i < input.size(); i++) {
            stacks[i] = input.getStackInSlot(i);
        }
        return new SimpleContainer(stacks);
    }

    // ===================== GUI getters =====================

    public int getProgress() {
        return diskInputSlot != -1 ? diskProgress : progress;
    }

    public int getDuration() { return FIXED_DURATION; }

    public FluidStack getFluidIn() { return fluidIn.getFluid().copy(); }
    public FluidStack getFluidOut() { return fluidOut.getFluid().copy(); }
    public int getFluidCapacity() { return TANK_CAPACITY; }

    // --- GUI handlers (do menu) ---
    private final IFluidHandler menuInputHandler = new SingleTankHandler(fluidIn, true, true);   // fill + drain
    private final IFluidHandler menuOutputHandler = new SingleTankHandler(fluidOut, false, true); // tylko drain

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
            if (!allowFill || resource.isEmpty()) return 0;
            return tank.fill(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
            if (!allowDrain || resource.isEmpty()) return FluidStack.EMPTY;
            return tank.drain(resource, action);
        }

        @Override
        public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
            if (!allowDrain || maxDrain <= 0) return FluidStack.EMPTY;
            return tank.drain(maxDrain, action);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.crazyae2addons.recipe_fabricator");
    }
}
