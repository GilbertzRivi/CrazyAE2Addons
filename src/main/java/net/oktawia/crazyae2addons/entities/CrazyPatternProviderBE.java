package net.oktawia.crazyae2addons.entities;

import appeng.api.ids.AEComponents;
import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuHostLocator;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import com.lowdragmc.lowdraglib2.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib2.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib2.syncdata.holder.blockentity.ISyncPersistRPCBlockEntity;
import com.lowdragmc.lowdraglib2.syncdata.storage.FieldManagedStorage;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.components.CrazyProviderDisplayData;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IProviderLogicResizable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrazyPatternProviderBE extends PatternProviderBlockEntity
        implements IUpgradeableObject, ISyncPersistRPCBlockEntity {

    private static final int BASE_SIZE = 8 * 9;
    private static final int ROW_SIZE = 9;

    @Getter
    private final FieldManagedStorage syncStorage;

    @Getter
    @Persisted
    public final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(),
            IsModLoaded.isAppFluxLoaded() ? 2 : 1,
            this::saveChanges
    );

    @Getter
    @Persisted
    @DescSynced
    @UpdateListener(methodName = "onAddedSynced")
    private int added = 0;

    public CrazyPatternProviderBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.CRAZY_PATTERN_PROVIDER_BE.get(), pos, blockState);
        this.syncStorage = new FieldManagedStorage(this);
        this.getMainNode().setVisualRepresentation(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get().asItem());
    }

    private void applySize(HolderLookup.Provider registries) {
        ((IProviderLogicResizable) getLogic()).setSize(BASE_SIZE + ROW_SIZE * added, registries);
    }

    private void onAddedSynced(int oldValue, int newValue) {
        if (level != null) {
            applySize(level.registryAccess());
            getLogic().updatePatterns();
        }
    }

    public void setAdded(int newAdded) {
        if (newAdded == this.added) return;
        this.added = newAdded;
        if (level != null) {
            applySize(level.registryAccess());
            if (!level.isClientSide) {
                getLogic().updatePatterns();
                setChanged();
                markDirty("added");
                sync(false);
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        var managed = data.getCompound("managed");
        if (managed.contains("added")) {
            this.added = managed.getInt("added");
            applySize(registries);
        }
        super.loadTag(data, registries);
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    public PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, BASE_SIZE);
    }

    @Override
    public void openMenu(Player player, MenuHostLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), player, subMenu.getLocator());
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get().asItem().getDefaultInstance();
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder, @Nullable Player player) {
        super.exportSettings(mode, builder, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var patternContents = ((AppEngInternalInventory) getLogic().getPatternInv()).toItemContainerContents();
            builder.set(AEComponents.EXPORTED_PATTERNS, patternContents);

            int filled = (int) patternContents.nonEmptyStream().count();
            builder.set(
                    CrazyDataComponents.CRAZY_PROVIDER_DISPLAY.get(),
                    new CrazyProviderDisplayData(added, filled)
            );
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var display = input.get(CrazyDataComponents.CRAZY_PROVIDER_DISPLAY.get());
            if (display != null) {
                this.added = display.added();

                if (level != null) {
                    applySize(level.registryAccess());
                }
            }

            var patterns = input.getOrDefault(AEComponents.EXPORTED_PATTERNS, net.minecraft.world.item.component.ItemContainerContents.EMPTY);
            ((AppEngInternalInventory) getLogic().getPatternInv()).fromItemContainerContents(patterns);
        }
    }

    @Override
    public void addAdditionalDrops(net.minecraft.world.level.Level level, BlockPos pos, List<ItemStack> drops) {
    }
}