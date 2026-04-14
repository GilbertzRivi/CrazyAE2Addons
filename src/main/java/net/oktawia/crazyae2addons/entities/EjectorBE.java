package net.oktawia.crazyae2addons.entities;

import appeng.api.config.Actionable;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.crafting.*;
import appeng.api.networking.security.IActionSource;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.api.stacks.*;
import appeng.api.storage.StorageHelper;
import appeng.blockentity.grid.AENetworkedBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.helpers.patternprovider.PatternProviderTarget;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.ConfigInventory;
import appeng.util.inv.AppEngInternalInventory;
import com.google.common.collect.ImmutableSet;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.blocks.EjectorBlock;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.buffer.ManagedBuffer;
import net.oktawia.crazyae2addons.menus.EjectorMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class EjectorBE extends AENetworkedBlockEntity implements MenuProvider, IGridTickable, PatternProviderLogicHost, ICraftingRequester, ISyncPersistRPCBlockEntity {

    @Getter
    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);
    @Persisted
    public final ConfigInventory config = ConfigInventory.configStacks(36).build();
    @Persisted
    public final AppEngInternalInventory pattern = new AppEngInternalInventory(1);
    public GenericStack cantCraft = null;
    public EjectorMenu menu = null;
    private boolean isCrafting = false;

    @Persisted
    private final ManagedBuffer buffer = new ManagedBuffer(
            getMainNode(), this, this, this::setChanged, this::ejectFromBuffer, () -> isCrafting
    );

    public EjectorBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.EJECTOR_BE.get(), pos, blockState);
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .addService(IGridTickable.class, this)
                .addService(ICraftingRequester.class, this)
                .setVisualRepresentation(new ItemStack(CrazyBlockRegistrar.EJECTOR_BLOCK.get().asItem()));
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            buffer.onLoad();
        }
    }

    @Override
    public PatternProviderLogic getLogic() {
        return buffer.getLogic();
    }

    @Override
    public EnumSet<Direction> getTargets() {
        return EnumSet.of(getBlockState().getValue(EjectorBlock.FACING));
    }

    @Override
    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        return buffer.getRequestedJobs();
    }

    @Override
    public long insertCraftedItems(ICraftingLink link, AEKey what, long amount, Actionable mode) {
        return buffer.insertCraftedItems(what, amount, mode);
    }

    @Override
    public void jobStateChange(ICraftingLink link) {
        buffer.jobStateChange(link);
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(5, 5, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        var missing = buffer.tick(ticksSinceLastCall);
        if (missing != null) {
            cantCraft = missing;
            if (menu != null) menu.cantCraft = String.format(
                    "%sx %s",
                    missing.what().formatAmount(missing.amount(), AmountFormat.SLOT),
                    missing.what()
            );
        }
        boolean crafting = buffer.hasActiveCrafting();
        if (isCrafting != crafting) {
            isCrafting = crafting;
            if (getLevel() != null) {
                getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, crafting));
            }
        }
        return (crafting || buffer.isFlushPending()) ? TickRateModulation.URGENT : TickRateModulation.IDLE;
    }

    public void doWork() {
        if (getGridNode() == null || getGridNode().getGrid() == null || !getMainNode().isActive() || isCrafting) return;
        cantCraft = null;
        if (menu != null) menu.cantCraft = "nothing";

        List<GenericStack> required = new ArrayList<>();
        for (int slot = 0; slot < config.size(); slot++) {
            var gs = config.getStack(slot);
            if (gs == null || gs.amount() <= 0) continue;
            required.add(gs);
        }
        if (required.isEmpty()) return;

        if (buffer.request(required)) {
            isCrafting = true;
            if (getLevel() != null) {
                getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, true));
            }
        }
    }

    private void ejectFromBuffer() {
        isCrafting = false;
        cantCraft = null;
        if (menu != null) menu.cantCraft = "nothing";

        var level = getLevel();
        var grid = getGrid();
        if (level == null || grid == null) {
            if (!buffer.isEmpty()) buffer.beginFlush();
            if (getLevel() != null) {
                getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, false));
            }
            return;
        }

        var direction = getBlockState().getValue(EjectorBlock.FACING);
        var src = IActionSource.ofMachine(this);
        var energy = grid.getEnergyService();
        var storageInv = grid.getStorageService().getInventory();

        List<GenericStack> toEject = new ArrayList<>();
        for (int slot = 0; slot < config.size(); slot++) {
            var gs = config.getStack(slot);
            if (gs == null || gs.amount() <= 0 || gs.what() == null) continue;
            toEject.add(gs);
        }

        var targetPos = getBlockPos().relative(direction);
        var target = PatternProviderTarget.get(
                level, targetPos, level.getBlockEntity(targetPos), direction.getOpposite(), src);

        if (target == null) {
            for (var gs : toEject) {
                long amount = buffer.extract(gs.what(), gs.amount());
                if (amount > 0) StorageHelper.poweredInsert(energy, storageInv, gs.what(), amount, src, Actionable.MODULATE);
            }
            if (!buffer.isEmpty()) buffer.beginFlush();
            getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, false));
            return;
        }

        for (var gs : toEject) {
            if (target.insert(gs.what(), gs.amount(), Actionable.SIMULATE) < gs.amount()) {
                if (!buffer.isEmpty()) buffer.beginFlush();
                getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, false));
                return;
            }
        }

        for (var gs : toEject) {
            long amount = buffer.extract(gs.what(), gs.amount());
            if (amount <= 0) continue;
            long leftover = amount - target.insert(gs.what(), amount, Actionable.MODULATE);
            if (leftover > 0) StorageHelper.poweredInsert(energy, storageInv, gs.what(), leftover, src, Actionable.MODULATE);
        }

        if (!buffer.isEmpty()) buffer.beginFlush();
        getLevel().setBlockAndUpdate(getBlockPos(), getBlockState().setValue(EjectorBlock.ISCRAFTING, false));
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
        super.addAdditionalDrops(level, pos, drops);
        drops.add(pattern.getStackInSlot(0));
        for (var gs : buffer.toData().entries()) {
            if (gs.what() instanceof AEItemKey key) {
                drops.add(key.toStack((int) Math.min(gs.amount(), Integer.MAX_VALUE)));
            }
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new EjectorMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.EJECTOR_MENU.get(), player, locator);
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(CrazyBlockRegistrar.EJECTOR_BLOCK.get());
    }

    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.EJECTOR_BLOCK.get().asItem().getDefaultInstance();
    }
}
