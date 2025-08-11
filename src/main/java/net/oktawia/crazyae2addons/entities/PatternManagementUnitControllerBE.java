package net.oktawia.crazyae2addons.entities;

import appeng.api.inventories.InternalInventory;
import appeng.api.networking.GridFlags;
import appeng.api.networking.IGridNode;
import appeng.api.networking.ticking.IGridTickable;
import appeng.api.networking.ticking.TickRateModulation;
import appeng.api.networking.ticking.TickingRequest;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.blockentity.grid.AENetworkBlockEntity;
import appeng.core.definitions.AEItems;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import appeng.util.inv.InternalInventoryHost;
import appeng.util.inv.filter.IAEItemFilter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.menus.PatternManagementUnitControllerMenu;
import net.oktawia.crazyae2addons.misc.PatternManagementUnitPreviewRenderer;
import net.oktawia.crazyae2addons.misc.PatternManagementUnitValidator;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PatternManagementUnitControllerBE extends AENetworkBlockEntity implements IGridTickable, InternalInventoryHost, MenuProvider {

    public PatternManagementUnitControllerBE controller;
    public PatternManagementUnitValidator validator;
    public boolean valid = false;
    public AppEngInternalInventory inv = new AppEngInternalInventory(this, CrazyConfig.COMMON.PatternUnitCapacity.get() * 9, 1, new IAEItemFilter(){
        public boolean allowInsert(InternalInventory inv, int slot, ItemStack stack) {
            return stack.getItem().asItem() == AEItems.CRAFTING_PATTERN.asItem().asItem();
        }
    });
    public boolean preview = false;
    public boolean init;
    public boolean active;

    public List<PatternManagementUnitPreviewRenderer.CachedBlockInfo> ghostCache = null;

    public static final Set<PatternManagementUnitControllerBE> CLIENT_INSTANCES = new HashSet<>();

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

    public PatternManagementUnitControllerBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_BE.get(), pos, blockState);
        this.validator = new PatternManagementUnitValidator();
        this.init = false;
        this.active = false;
        this.getMainNode()
                .setIdlePowerUsage(2.0F)
                .addService(IGridTickable.class, this)
                .setFlags(GridFlags.REQUIRE_CHANNEL)
                .setVisualRepresentation(
                        new ItemStack(CrazyBlockRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_BLOCK.get().asItem())
                );
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        inv.writeToNBT(data, "inv");
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        inv.readFromNBT(data, "inv");
    }

    @Override
    public TickingRequest getTickingRequest(IGridNode node) {
        return new TickingRequest(20, 20, false, false);
    }

    @Override
    public TickRateModulation tickingRequest(IGridNode node, int ticksSinceLastCall) {
        if (getLevel() != null){
            this.valid = this.validator.matchesStructure(getLevel(), getBlockPos(), getBlockState(), this);
            if (!valid && active){
                active = false;
                refreshPatterns();
            }
            if (valid && !active){
                this.active = true;
                refreshPatterns();
            }
            if (!init && getMainNode().getGrid() != null){
                refreshPatterns();
                init = true;
            }
        }
        return TickRateModulation.IDLE;
    }

    @Override
    public void onChangeInventory(InternalInventory inv, int slot) {
        refreshPatterns();
    }

    public void refreshPatterns(){
        setChanged();
        if (getMainNode().getGrid() != null){
            getMainNode().getGrid().getNodes().forEach(node -> {
                if (node.getOwner() instanceof PatternProviderBlockEntity pp){
                    pp.getLogic().updatePatterns();
                }
            });
        }
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new PatternManagementUnitControllerMenu(i, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Pattern Management Unit");
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.PATTERN_MANAGEMENT_UNIT_CONTROLLER_MENU.get(), player, locator);
    }

}
