package net.oktawia.crazyae2addons.entities;

import appeng.api.config.AccessRestriction;
import appeng.api.config.Actionable;
import appeng.api.config.PowerMultiplier;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.energy.IAEPowerStorage;
import appeng.api.networking.energy.IEnergyService;
import appeng.api.networking.events.GridPowerStorageStateChanged;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.util.AECableType;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.blockentity.networking.ControllerBlockEntity;
import appeng.me.energy.StoredEnergyAmount;
import appeng.me.service.EnergyService;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.EnergyStorageControllerMenu;
import net.oktawia.crazyae2addons.misc.CachedBlockInfo;
import net.oktawia.crazyae2addons.misc.EnergyStoragePreviewRenderer;
import net.oktawia.crazyae2addons.misc.EnergyStorageValidator;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

public class EnergyStorageControllerBE extends AENetworkBlockEntity implements MenuProvider, IGridTickable, IAEPowerStorage {

    private boolean replace;
    public EnergyStorageValidator validator;
    public double energy;
    public boolean active;
    public double maxEnergy;
    public EnergyStorageControllerMenu menu;
    public StoredEnergyAmount stored;

    public List<CachedBlockInfo> ghostCache = null;

    public boolean preview = false;

    public static final Set<EnergyStorageControllerBE> CLIENT_INSTANCES = new java.util.HashSet<>();

    public EnergyStorageControllerBE(BlockPos pos, BlockState blockState){
        this(pos, blockState, 0, 0, false, false);
    }

    public EnergyStorageControllerBE(BlockPos pos, BlockState blockState, double energy, double maxEnergy, boolean active, boolean replace) {
        super(CrazyBlockEntityRegistrar.ENERGY_STORAGE_CONTROLLER_BE.get(), pos, blockState);
        validator = new EnergyStorageValidator();
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .addService(IAEPowerStorage.class, this)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.ENERGY_STORAGE_CONTROLLER_BLOCK.get().asItem())
                );
        this.maxEnergy = maxEnergy;
        this.energy = energy;
        this.active = active;
        this.replace = replace;
        this.stored = new StoredEnergyAmount(this.active ? this.energy : 0, this.maxEnergy, this::emitPowerEvent);
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
    public void onReady(){
        super.onReady();
        validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this);
        if (this.getMainNode().getGrid() != null){
            ((EnergyService) this.getMainNode().getGrid().getService(IEnergyService.class)).addNode(this.getGridNode(), null);
            emitPowerEvent(GridPowerStorageStateChanged.PowerEventType.PROVIDE_POWER);
        }
    }

    private void emitPowerEvent(GridPowerStorageStateChanged.PowerEventType type) {
        getMainNode().ifPresent(
                grid -> grid.postEvent(new GridPowerStorageStateChanged(this, type)));
        this.setChanged();
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        if (data.contains("maxenergy")) {
            this.maxEnergy = data.getDouble("maxenergy");
            this.stored.setMaximum(maxEnergy);
        }
        if (data.contains("energy")) {
            this.energy = data.getDouble("energy");
            this.stored.setStored(energy);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putDouble("energy", this.stored.getAmount());
        data.putDouble("maxenergy", this.stored.getMaximum());
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new EnergyStorageControllerMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Energy Storage Controller");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.ENERGY_STORAGE_CONTROLLER_MENU.get(), player, locator);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(10, 10, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (isClientSide() || getLevel() == null) return TickRateModulation.IDLE;

        boolean validStructure = validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this) &&
                getMainNode().getGrid() != null &&
                !getMainNode().getGrid().getMachines(ControllerBlockEntity.class).isEmpty();
        boolean wasActive = this.active;

        if (getMenu() != null) {
            getMenu().maxEnergy = (long) this.maxEnergy;
            getMenu().energy = (long) this.energy;
        }

        if (!validStructure && wasActive || !replace) {
            this.active = false;
            this.replace = true;

            getMainNode().destroy();
            getLevel().removeBlock(getBlockPos(), false);
            getLevel().removeBlockEntity(getBlockPos());
            getLevel().setBlockAndUpdate(getBlockPos(), getBlockState());
            getLevel().setBlockEntity(new EnergyStorageControllerBE(getBlockPos(), getBlockState(), this.energy, 0, false, true));
            setChanged();
            return TickRateModulation.IDLE;
        }

        if (!wasActive && validStructure) {
            long storage1k    = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:energy_storage_1k") * 8 * 1024 * 1024;
            long storage4k    = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:energy_storage_4k") * 4 * 8 * 1024 * 1024;
            long storage16k   = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:energy_storage_16k") * 16 * 8 * 1024 * 1024;
            long storage64k   = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:energy_storage_64k") * 64 * 8 * 1024 * 1024;
            long storage256k  = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:energy_storage_256k") * 256 * 8 * 1024 * 1024;
            long denseStorage1k    = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:dense_energy_storage_1k") * 8 * 1024 * 1024 * 1024;
            long denseStorage4k    = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:dense_energy_storage_4k") * 4 * 8 * 1024 * 1024 * 1024;
            long denseStorage16k   = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:dense_energy_storage_16k") * 16 * 8 * 1024 * 1024 * 1024;
            long denseStorage64k   = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:dense_energy_storage_64k") * 64 * 8 * 1024 * 1024 * 1024;
            long denseStorage256k  = (long) validator.countBlockInStructure(getLevel(), getBlockPos(), getBlockState(), "crazyae2addons:dense_energy_storage_256k") * 256 * 8 * 1024 * 1024 * 1024;
            this.maxEnergy = storage1k + storage4k + storage16k + storage64k + storage256k + denseStorage1k + denseStorage4k + denseStorage16k + denseStorage64k + denseStorage256k;
            this.stored.setMaximum(this.maxEnergy);

            if (this.energy > this.maxEnergy && this.maxEnergy != 0) {
                this.energy = this.maxEnergy;
            }

            this.stored.setStored(this.energy);

            this.active = true;

            getMainNode().destroy();
            getLevel().removeBlock(getBlockPos(), false);
            getLevel().removeBlockEntity(getBlockPos());
            getLevel().setBlockAndUpdate(getBlockPos(), getBlockState());
            getLevel().setBlockEntity(new EnergyStorageControllerBE(getBlockPos(), getBlockState(), this.energy, this.maxEnergy, true, true));
            setChanged();
        }
        emitPowerEvent(GridPowerStorageStateChanged.PowerEventType.PROVIDE_POWER);
        if (getMainNode().getGrid() != null){
            ((EnergyService)getMainNode().getGrid().getEnergyService()).refreshPower();
        }
        return TickRateModulation.IDLE;
    }


    @Override
    public AECableType getCableConnectionType(Direction dir) {
        return AECableType.COVERED;
    }

    public void updateMenu(){
        if (getMenu() != null){
            getMenu().energy = (long) this.energy;
        }
    }

    @Override
    public double injectAEPower(double amt, Actionable mode) {
        double space = this.maxEnergy - this.energy;
        double toInsert = Math.min(amt, space);

        if (mode == Actionable.MODULATE) {
            this.energy += toInsert;
            this.stored.insert(toInsert, true);
        }

        updateMenu();

        return amt - toInsert;
    }

    @Override
    public double getAEMaxPower() {
        return this.stored.getMaximum();
    }

    @Override
    public double getAECurrentPower() {
        return this.stored.getAmount();
    }

    @Override
    public boolean isAEPublicPowerStorage() {
        return true;
    }

    @Override
    public AccessRestriction getPowerFlow() {
        return this.active ? AccessRestriction.READ_WRITE : AccessRestriction.NO_ACCESS;
    }

    @Override
    public double extractAEPower(double amt, Actionable mode, PowerMultiplier usePowerMultiplier) {
        double extracted = Math.min(amt, this.energy);
        if (mode == Actionable.MODULATE) {
            this.energy -= extracted;
            this.stored.extract(extracted, true);
        }
        updateMenu();
        return extracted;
    }

    public void setMenu(EnergyStorageControllerMenu menu){
        this.menu = menu;
    }

    public EnergyStorageControllerMenu getMenu(){
        return this.menu;
    }
}
