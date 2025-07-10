package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.storage.StorageCells;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.grid.AENetworkInvBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.PenroseControllerMenu;
import net.oktawia.crazyae2addons.misc.*;
import net.oktawia.crazyae2addons.renderer.preview.PreviewInfo;
import net.oktawia.crazyae2addons.renderer.preview.Previewable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class PenroseControllerBE extends AENetworkInvBlockEntity implements Previewable, MenuProvider, IUpgradeableObject, IGridTickable {
    public static final Set<PenroseControllerBE> CLIENT_INSTANCES = new HashSet<>();
    public boolean energyMode = false;
    public boolean preview = false;
    public int previewTier = 0;
    private int myticks = 0;
    public EnergyStorage energyStorage = new EnergyStorage(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, 0);

    public int cachedTier = -1;

    private PreviewInfo previewInfo = null;

    @Override
    public PreviewInfo getPreviewInfo() {
        return previewInfo;
    }

    @Override
    public void setPreviewInfo(PreviewInfo info) {
        this.previewInfo = info;
    }

    private final LazyOptional<IEnergyStorage> energyCap = LazyOptional.of(() -> new IEnergyStorage() {
        @Override public int getEnergyStored() {
            return energyStorage.getEnergyStored();
        }
        @Override public int getMaxEnergyStored() {
            return energyStorage.getMaxEnergyStored();
        }
        @Override public boolean canExtract() {
            return energyStorage.canExtract();
        }
        @Override public int extractEnergy(int maxExtract, boolean simulate) {
            return energyStorage.extractEnergy(maxExtract, simulate);
        }
        @Override public boolean canReceive() {
            return false;
        }
        @Override public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }
    });

    public PenroseValidatorT0 validatorT0;
    public PenroseValidatorT1 validatorT1;
    public PenroseValidatorT2 validatorT2;
    public PenroseValidatorT3 validatorT3;

    public int tier = 0;

    public AppEngInternalInventory diskInv = new AppEngInternalInventory(this, 4, 1, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            if (slot > tier) return false;
            if (stack.getItem() == AEItems.ITEM_CELL_1K.asItem()) {
                var cellInv = StorageCells.getCellInventory(stack, null);
                if (cellInv == null) return false;
                var stacks = cellInv.getAvailableStacks();
                if (stacks.isEmpty()) return true;
                return stacks.size() == 1 && Objects.equals(stacks.getFirstKey(), AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
            }
            return false;
        }
    });

    public AppEngInternalInventory inputInv = new AppEngInternalInventory(this, 1, 16384, new IAEItemFilter() {
        @Override
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.getItem() == CrazyItemRegistrar.SUPER_SINGULARITY.get();
        }
    });

    public ConfigInventory config = ConfigInventory.configTypes(
            key -> key instanceof AEItemKey itemkey && itemkey.toStack().getItem() != CrazyItemRegistrar.SUPER_SINGULARITY.get(), 1, () -> {});
    private boolean formed;

    public PenroseControllerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.PENROSE_CONTROLLER_BE.get(), pos, blockState);
        validatorT0 = new PenroseValidatorT0();
        validatorT1 = new PenroseValidatorT1();
        validatorT2 = new PenroseValidatorT2();
        validatorT3 = new PenroseValidatorT3();
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PENROSE_CONTROLLER.get().asItem())
                );
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        drops.add(this.inputInv.getStackInSlot(0));
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        this.config.readFromChildTag(data, "config");
        this.inputInv.readFromNBT(data, "inputinv");
        if (data.contains("mode")){
            this.energyMode = data.getBoolean("mode");
        }
        if (data.contains("energy")){
            this.energyStorage = new EnergyStorage(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, data.getInt("energy"));
        }
        if (data.contains("tier")){
            this.tier = data.getInt("tier");
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && level.isClientSide) {
            CLIENT_INSTANCES.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        CLIENT_INSTANCES.remove(this);
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        this.config.writeToChildTag(data, "config");
        this.inputInv.writeToNBT(data, "inputinv");
        data.putBoolean("mode", this.energyMode);
        data.putInt("energy", this.energyStorage.getEnergyStored());
        data.putInt("tier", this.tier);
    }

    public static long energyGenerated(int count, int tier) {
        long maxCount;
        long maxEnergy = switch (tier) {
            case 1 -> {
                maxCount = 16384;
                yield (1L << 26) / 64;
            }
            case 2 -> {
                maxCount = 24576;
                yield (1L << 28) / 64;
            }
            case 3 -> {
                maxCount = 32768;
                yield (1L << 30) / 64;
            }
            default -> {
                maxCount = 8192;
                yield (1L << 24) / 64;
            }
        };

        if (count <= 0) {
            return 0L;
        }
        if (count >= maxCount) {
            return maxEnergy;
        }

        return (long) count * maxEnergy / maxCount;
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PenroseControllerMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Penrose Controller");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PENROSE_CONTROLLER_MENU.get(), player, locator);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(1, 1, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        myticks++;
        if (myticks >= 20){
            this.formed = false;
            this.tier = 0;
             if (validatorT3.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this)) {
                this.tier = 3;
                this.formed = true;
            } else if (validatorT2.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this)) {
                 this.tier = 2;
                 this.formed = true;
            } else if (validatorT1.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this)) {
                this.tier = 1;
                this.formed = true;
            } else if (validatorT0.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this)){
                this.tier = 0;
                this.formed = true;
            }

            myticks = 0;
        }
        if (this.formed){
            var grid = getMainNode().getGrid();
            if (grid == null) return TickRateModulation.IDLE;
            var inv = grid.getStorageService().getInventory();
            var disk = StorageCells.getCellInventory(diskInv.getStackInSlot(0), null);
            if (disk == null) return TickRateModulation.IDLE;
            if (config.getStack(0) == null) return TickRateModulation.IDLE;
            var extracted = inv.extract(this.config.getKey(0), 1, Actionable.MODULATE, IActionSource.ofMachine(this));
            var disk0 = StorageCells.getCellInventory(diskInv.getStackInSlot(0), null);
            var disk1 = StorageCells.getCellInventory(diskInv.getStackInSlot(1), null);
            var disk2 = StorageCells.getCellInventory(diskInv.getStackInSlot(2), null);
            var disk3 = StorageCells.getCellInventory(diskInv.getStackInSlot(3), null);
            long generated;
            int count = 0;
            if (disk0 != null && tier >= 0) {
                count += (int) disk0.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
            } if (disk1 != null && tier >= 1) {
                count += (int) disk1.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
            } if (disk2 != null && tier >= 2) {
                count += (int) disk2.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
            } if (disk3 != null && tier >= 3) {
                count += (int) disk3.getAvailableStacks().get(AEItemKey.of(CrazyItemRegistrar.SUPER_SINGULARITY.get()));
            }

            generated = PenroseControllerBE.energyGenerated(count, tier) * extracted;

            if (AEItems.MATTER_BALL.isSameAs(((AEItemKey) config.getStack(0).what()).toStack())){
                generated *= 8;
            } else if (AEItems.SINGULARITY.isSameAs(((AEItemKey) config.getStack(0).what()).toStack())){
                generated *= 64;
            }
            if (!energyMode){
                this.energyStorage = new EnergyStorage(Integer.MAX_VALUE, 0, Integer.MAX_VALUE, (int) Math.min((Integer.MAX_VALUE), ((long)this.energyStorage.getEnergyStored()) + generated));
            } else {
                generated /= 2;
                grid.getEnergyService().injectPower(generated, Actionable.MODULATE);
            }
        }
        return TickRateModulation.IDLE;
    }

    @Override
    public InternalInventory getInternalInventory() {
        return diskInv;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {}

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap, Direction side) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        } else if (cap == ForgeCapabilities.ITEM_HANDLER){
            return LazyOptional.empty();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public <T> @NotNull LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        if (cap == ForgeCapabilities.ENERGY) {
            return energyCap.cast();
        } else if (cap == ForgeCapabilities.ITEM_HANDLER){
            return LazyOptional.empty();
        }
        return super.getCapability(cap);
    }
}
