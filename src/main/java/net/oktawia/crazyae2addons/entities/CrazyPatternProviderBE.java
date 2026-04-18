package net.oktawia.crazyae2addons.entities;

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
import appeng.menu.locator.MenuLocator;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.annotation.UpdateListener;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IProviderLogicResizable;
import net.oktawia.crazyae2addons.util.IManagedBEHelper;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Getter
public class CrazyPatternProviderBE extends PatternProviderBlockEntity
        implements IUpgradeableObject, IManagedBEHelper {

    private static final int BASE_SIZE = 8 * 9;
    private static final int ROW_SIZE = 9;
    private static final String NBT_PATTERNS = "crazy_patterns";

    protected static final ManagedFieldHolder MANAGED_FIELD_HOLDER =
            new ManagedFieldHolder(CrazyPatternProviderBE.class);

    private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

    @Persisted
    @LazyManaged
    private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(),
            IsModLoaded.APP_FLUX ? 2 : 1,
            this::onUpgradesChanged
    );

    @Persisted
    @DescSynced
    @UpdateListener(methodName = "onAddedSynced")
    private int added = 0;

    public CrazyPatternProviderBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.CRAZY_PATTERN_PROVIDER_BE.get(), pos, blockState);
        this.getMainNode().setVisualRepresentation(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get().asItem());
    }

    @Override
    public ManagedFieldHolder getFieldHolder() {
        return MANAGED_FIELD_HOLDER;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        onValid();

        if (level != null) {
            applySize();
        }
    }

    @Override
    public void setRemoved() {
        onInValid();
        super.setRemoved();
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        saveManagedData(tag);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        loadManagedData(tag);
        applySize();
        super.loadTag(tag);
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output, @Nullable Player player) {
        super.exportSettings(mode, output, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            saveManagedData(output);
            ((AppEngInternalInventory) getLogic().getPatternInv()).writeToNBT(output, NBT_PATTERNS);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            loadManagedData(input);
            applySize();

            ((AppEngInternalInventory) getLogic().getPatternInv()).readFromNBT(input, NBT_PATTERNS);
            getLogic().updatePatterns();
        }
    }

    public void setAdded(int newAdded) {
        if (this.added == newAdded) {
            return;
        }

        this.added = newAdded;
        applySize();
        getLogic().updatePatterns();
        syncManaged();
    }

    private void onAddedSynced(int newValue, int oldValue) {
        if (level != null) {
            applySize();
            getLogic().updatePatterns();
        }
    }

    private void onUpgradesChanged() {
        setChanged();
    }

    private void applySize() {
        ((IProviderLogicResizable) getLogic()).crazyAE2Addons$setSize(BASE_SIZE + ROW_SIZE * added);
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {}

    @Override
    public void clearContent() {
        super.clearContent();

        for (int i = 0; i < upgrades.size(); i++) {
            upgrades.setItemDirect(i, ItemStack.EMPTY);
        }
    }

    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (ISegmentedInventory.UPGRADES.equals(id)) {
            return upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, BASE_SIZE);
    }

    @Override
    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), player, locator);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), player, subMenu.getLocator());
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(getBlockState().getBlock().asItem().getDefaultInstance());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return getBlockState().getBlock().asItem().getDefaultInstance();
    }
}