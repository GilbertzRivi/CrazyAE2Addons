package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.CalculationStrategy;
import appeng.api.networking.crafting.ICraftingLink;
import appeng.api.networking.crafting.ICraftingPlan;
import appeng.api.networking.crafting.ICraftingRequester;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.AEItemKey;
import appeng.api.stacks.AEKey;
import appeng.api.storage.StorageHelper;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.ConfigInventory;
import com.google.common.collect.ImmutableSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.CraftingSchedulerMenu;

import java.util.concurrent.Future;

public class CraftingSchedulerBE extends AENetworkedBlockEntity implements MenuProvider, ICraftingRequester, IGridTickable {

    public ConfigInventory inv = ConfigInventory.configTypes(1).build();
    public int amount = 0;

    @Nullable
    private ICraftingLink activeJob;

    @Nullable
    private Future<ICraftingPlan> pendingPlan;

    public CraftingSchedulerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.CRAFTING_SCHEDULER_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(1.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.CRAFTING_SCHEDULER_BLOCK.get().asItem()));
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CraftingSchedulerMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return this.getBlockState().getBlock().getName();
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return this.activeJob != null ? ImmutableSet.of(this.activeJob) : ImmutableSet.of();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        var grid = this.getMainNode().getGrid();
        if (grid == null) return 0;

        var energy = grid.getEnergyService();
        var storage = grid.getStorageService().getInventory();
        return StorageHelper.poweredInsert(energy, storage, what, amount, IActionSource.ofMachine(this), mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        if (this.activeJob != null && this.activeJob.getCraftingID().equals(link.getCraftingID())) {
            this.activeJob = null;
            this.setChanged();
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        super.loadTag(data, registries);

        if (data.contains("config")) {
            this.inv.readFromChildTag(data, "config", registries);
        }
        if (data.contains("amount")) {
            this.amount = data.getInt("amount");
        }

        if (data.contains("activeJob")) {
            try {
                var tag = data.getCompound("activeJob");
                var loaded = StorageHelper.loadCraftingLink(tag, this);
                if (loaded != null && !loaded.isDone() && !loaded.isCanceled()) {
                    this.activeJob = loaded;
                } else {
                    this.activeJob = null;
                }
            } catch (Throwable ignored) {
                this.activeJob = null;
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);

        this.inv.writeToChildTag(data, "config", registries);
        data.putInt("amount", this.amount);

        if (this.activeJob != null && !this.activeJob.isDone() && !this.activeJob.isCanceled()) {
            CompoundTag jobTag = new CompoundTag();
            this.activeJob.writeToNBT(jobTag);
            data.put("activeJob", jobTag);
        } else {
            data.remove("activeJob");
        }
    }

    public void doWork() {
        if (this.pendingPlan != null || this.activeJob != null) return;

        var grid = this.getMainNode().getGrid();
        if (grid == null) return;

        if (this.amount <= 0) return;

        var key = this.inv.getKey(0);
        if (key == null) return;

        var crafting = grid.getCraftingService();

        if (!crafting.isCraftable(key)) return;

        boolean hasFreeCpu = crafting.getCpus().stream().anyMatch(cpu -> !cpu.isBusy());
        if (!hasFreeCpu) return;

        this.pendingPlan = crafting.beginCraftingCalculation(
                getLevel(),
                () -> IActionSource.ofMachine(this),
                key,
                this.amount,
                CalculationStrategy.REPORT_MISSING_ITEMS
        );

        this.setChanged();
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (this.pendingPlan == null) {
            return TickRateModulation.IDLE;
        }

        if (!this.pendingPlan.isDone()) {
            return TickRateModulation.IDLE;
        }

        try {
            var grid = node.getGrid();
            if (grid == null) {
                this.pendingPlan = null;
                return TickRateModulation.IDLE;
            }

            var plan = this.pendingPlan.get();
            this.pendingPlan = null;

            if (this.activeJob != null) return TickRateModulation.IDLE;

            var result = grid.getCraftingService().submitJob(plan, this, null, true, IActionSource.ofMachine(this));
            if (result.successful() && result.link() != null) {
                this.activeJob = result.link();
                this.setChanged();
            }
        } catch (Throwable ignored) {
            this.pendingPlan = null;
        }

        return TickRateModulation.IDLE;
    }

    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.CRAFTING_SCHEDULER_MENU.get(), player, locator);
    }
}
