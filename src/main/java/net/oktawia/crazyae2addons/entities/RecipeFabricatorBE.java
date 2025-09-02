package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.IGridNode;
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
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyRecipes;
import net.oktawia.crazyae2addons.menus.RecipeFabricatorMenu;
import net.oktawia.crazyae2addons.recipes.FabricationRecipe;
import net.oktawia.crazyae2addons.recipes.FabricationRecipeType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class RecipeFabricatorBE extends AENetworkInvBlockEntity implements MenuProvider, IGridTickable, IUpgradeableObject {

    private static final String NBT_KEYS = "keys";
    private static final int FIXED_DURATION = 10;

    public final AppEngInternalInventory inv = new AppEngInternalInventory(this, 3);
    public final InternalInventory input  = inv.getSubInventory(0, 1);
    public final InternalInventory drive  = inv.getSubInventory(1, 2);
    public final InternalInventory output = inv.getSubInventory(2, 3);

    private int progress = 0;
    @Nullable private FabricationRecipe active = null;

    public RecipeFabricatorBE(BlockPos pos, BlockState state) {
        super(CrazyBlockEntityRegistrar.RECIPE_FABRICATOR_BE.get(), pos, state);
        this.getMainNode()
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.RECIPE_FABRICATOR_BLOCK.get().asItem()));
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
            return LazyOptional.of(() -> new IItemHandler() {
                @Override public int getSlots() { return 1; }
                @Override public @NotNull ItemStack getStackInSlot(int slot) { return output.getStackInSlot(0); }
                @Override public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate) {
                    return isDrive(stack) ? drive.insertItem(0, stack, simulate) : input.insertItem(0, stack, simulate);
                }
                @Override public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate) {
                    return output.extractItem(0, amount, simulate);
                }
                @Override public int getSlotLimit(int slot) { return 64; }
                @Override public boolean isItemValid(int slot, @NotNull ItemStack stack) { return true; }
            }).cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public InternalInventory getInternalInventory() { return inv; }

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
        if (tag.contains("inv")) this.inv.readFromNBT(tag, "inv");
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        this.inv.writeToNBT(tag, "inv");
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 5, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (level == null || level.isClientSide) return TickRateModulation.IDLE;

        if (active == null) {
            active = findStartableRecipe();
            progress = 0;
            if (active == null) return TickRateModulation.IDLE;
        }

        if (!stillValid(active)) {
            cancelWork();
            return TickRateModulation.IDLE;
        }

        progress++;
        if (progress >= FIXED_DURATION) {
            finish(active);
            cancelWork();
            return TickRateModulation.IDLE;
        }

        setChanged();
        return TickRateModulation.URGENT;
    }

    private void cancelWork() {
        active = null;
        progress = 0;
        setChanged();
    }

    private void finish(FabricationRecipe r) {
        ItemStack in = input.getStackInSlot(0).copy();
        in.shrink(r.getInputCount());
        input.setItemDirect(0, in.isEmpty() ? ItemStack.EMPTY : in);

        ItemStack outWant = r.getOutput().copy();
        ItemStack outCur  = output.getStackInSlot(0);
        if (outCur.isEmpty()) {
            output.setItemDirect(0, outWant);
        } else {
            outCur.grow(outWant.getCount());
            output.setItemDirect(0, outCur);
        }
        setChanged();
    }

    @Nullable
    private FabricationRecipe findStartableRecipe() {
        if (level == null) return null;
        ItemStack in = input.getStackInSlot(0);
        if (in.isEmpty() && CrazyConfig.COMMON.ResearchRequired.get()) return null;

        var list = level.getRecipeManager().getAllRecipesFor(CrazyRecipes.FABRICATION_TYPE.get());
        for (FabricationRecipe r : list) {
            if (!r.matches(new SimpleContainer(in), level)) continue;
            if (in.getCount() < r.getInputCount()) continue;

            if (CrazyConfig.COMMON.ResearchRequired.get() && r.getRequiredKey() != null && !driveHasKey(r.getRequiredKey())) {
                continue;
            }

            if (!hasOutputSpaceFor(r)) continue;
            return r;
        }
        return null;
    }


    private boolean stillValid(FabricationRecipe r) {
        ItemStack in = input.getStackInSlot(0);
        if (in.isEmpty() || in.getCount() < r.getInputCount()) return false;
        if (!r.matches(new SimpleContainer(in), level)) return false;

        if (CrazyConfig.COMMON.ResearchRequired.get() && r.getRequiredKey() != null && !driveHasKey(r.getRequiredKey())) {
            return false;
        }

        return hasOutputSpaceFor(r);
    }


    private boolean hasOutputSpaceFor(FabricationRecipe r) {
        ItemStack out = output.getStackInSlot(0);
        ItemStack want = r.getOutput();
        if (out.isEmpty()) return want.getCount() <= want.getMaxStackSize();
        if (!ItemStack.isSameItemSameTags(out, want)) return false;
        return out.getCount() + want.getCount() <= out.getMaxStackSize();
    }

    private boolean driveHasKey(@NotNull String key) {
        ItemStack d = drive.getStackInSlot(0);
        if (d.isEmpty()) return false;
        CompoundTag tag = d.getTag();
        if (tag == null || !tag.contains(NBT_KEYS, Tag.TAG_LIST)) return false;
        ListTag list = tag.getList(NBT_KEYS, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) if (key.equals(list.getString(i))) return true;
        return false;
    }

    private boolean isDrive(ItemStack st) {
        CompoundTag tag = st.getTag();
        return tag != null && tag.contains(NBT_KEYS, Tag.TAG_LIST);
    }

    public int getProgress() { return progress; }
    public int getDuration() { return FIXED_DURATION; }

    @Override
    public Component getDisplayName() {
        return Component.literal("Recipe Fabricator");
    }
}
