package net.oktawia.crazyae2addons.parts;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.items.parts.PartModels;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import com.lowdragmc.lowdraglib.syncdata.AccessorOp;
import com.lowdragmc.lowdraglib.syncdata.IManaged;
import com.lowdragmc.lowdraglib.syncdata.IManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.accessor.IManagedAccessor;
import com.lowdragmc.lowdraglib.syncdata.annotation.DescSynced;
import com.lowdragmc.lowdraglib.syncdata.annotation.LazyManaged;
import com.lowdragmc.lowdraglib.syncdata.annotation.Persisted;
import com.lowdragmc.lowdraglib.syncdata.field.FieldManagedStorage;
import com.lowdragmc.lowdraglib.syncdata.field.ManagedFieldHolder;
import com.lowdragmc.lowdraglib.syncdata.payload.NbtTagPayload;
import lombok.Getter;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.interfaces.IProviderLogicResizable;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrazyPatternProviderPart extends PatternProviderPart implements IUpgradeableObject {

    private static final int BASE_SIZE = 8 * 9;
    private static final int ROW_SIZE = 9;

    private static final String NBT_STATE = "crazy_state";
    private static final String NBT_PATTERNS = "crazy_patterns";
    private static final String NBT_ADDED = "added";

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(
            CrazyAddons.makeId("part/crazy_pattern_provider_part"),
            new ResourceLocation("ae2", "part/interface_off")
    );

    @PartModels
    public static final PartModel MODELS_ON = new PartModel(
            CrazyAddons.makeId("part/crazy_pattern_provider_part"),
            new ResourceLocation("ae2", "part/interface_on")
    );

    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(
            CrazyAddons.makeId("part/crazy_pattern_provider_part"),
            new ResourceLocation("ae2", "part/interface_has_channel")
    );

    private final PartState state = new PartState(this);

    public CrazyPatternProviderPart(IPartItem<?> partItem) {
        super(partItem);
    }

    public int getAdded() {
        return state.getAdded();
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return state.getUpgrades();
    }

    @Override
    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, BASE_SIZE);
    }

    public void setAdded(int newAdded) {
        if (newAdded == state.getAdded()) {
            return;
        }

        state.setAdded(newAdded);
        applySize();
        getLogic().updatePatterns();
        markForSync();
    }

    public void upgradeOnce() {
        setAdded(state.getAdded() + 1);
    }

    private void applySize() {
        ((IProviderLogicResizable) getLogic()).crazyAE2Addons$setSize(BASE_SIZE + ROW_SIZE * state.getAdded());
    }

    private void markForSync() {
        saveChanges();
        if (getHost() != null) {
            getHost().markForUpdate();
        }
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        ItemStack heldItem = player.getItemInHand(hand);

        if (heldItem.getItem() == CrazyItemRegistrar.CRAZY_UPGRADE.get().asItem()) {
            if (!player.level().isClientSide) {
                int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
                if (maxAdd != -1 && state.getAdded() >= maxAdd) {
                    player.displayClientMessage(
                            Component.translatable(LangDefs.PROVIDER_MAX.getTranslationKey()),
                            true
                    );
                    return true;
                }

                heldItem.shrink(1);
                upgradeOnce();
            }
            return true;
        }

        if (!player.level().isClientSide) {
            MenuOpener.open(
                    CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(),
                    player,
                    MenuLocators.forPart(this)
            );
        }

        return true;
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.put(NBT_STATE, state.savePersisted());
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        if (data.contains(NBT_STATE, Tag.TAG_COMPOUND)) {
            state.loadPersisted(data.getCompound(NBT_STATE));
            applySize();
        }

        super.readFromNBT(data);
    }

    @Override
    public void writeToStream(FriendlyByteBuf data) {
        super.writeToStream(data);
        data.writeNbt(state.saveSync(true));
    }

    @Override
    public boolean readFromStream(FriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);

        int oldAdded = state.getAdded();
        CompoundTag syncTag = data.readNbt();

        if (syncTag != null) {
            state.loadSync(syncTag);
        }

        if (oldAdded != state.getAdded()) {
            applySize();
            getLogic().updatePatterns();
        }

        return changed || oldAdded != state.getAdded();
    }

    @Override
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        super.exportSettings(mode, output);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            output.put(NBT_STATE, state.savePersisted());
            ((AppEngInternalInventory) getLogic().getPatternInv()).writeToNBT(output, NBT_PATTERNS);
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            if (input.contains(NBT_STATE, Tag.TAG_COMPOUND)) {
                state.loadPersisted(input.getCompound(NBT_STATE));
                applySize();
            }

            ((AppEngInternalInventory) getLogic().getPatternInv()).readFromNBT(input, NBT_PATTERNS);
            getLogic().updatePatterns();
            markForSync();
        }
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {}

    @Override
    public void clearContent() {
        super.clearContent();

        for (int i = 0; i < state.getUpgrades().size(); i++) {
            state.getUpgrades().setItemDirect(i, ItemStack.EMPTY);
        }
    }

    @Override
    @Nullable
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (ISegmentedInventory.UPGRADES.equals(id)) {
            return state.getUpgrades();
        }
        return super.getSubInventory(id);
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
        return AEItemKey.of(CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get().asItem().getDefaultInstance();
    }

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        }
        if (isPowered()) {
            return MODELS_ON;
        }
        return MODELS_OFF;
    }

    private static final class PartState implements IManaged {
        private static final ManagedFieldHolder MANAGED_FIELD_HOLDER = new ManagedFieldHolder(PartState.class);

        private final CrazyPatternProviderPart owner;
        private final FieldManagedStorage syncStorage = new FieldManagedStorage(this);

        @Getter
        @Persisted
        @DescSynced
        private int added = 0;

        @Getter
        @Persisted
        @LazyManaged
        private final IUpgradeInventory upgrades;

        private PartState(CrazyPatternProviderPart owner) {
            this.owner = owner;
            this.upgrades = UpgradeInventories.forMachine(
                    owner.getPartItem(),
                    IsModLoaded.APP_FLUX ? 2 : 1,
                    this::onUpgradesChanged
            );
        }

        public void setAdded(int added) {
            this.added = added;
            markFieldDirty("added");
        }

        public CompoundTag savePersisted() {
            return IManagedAccessor.readManagedFields(this, new CompoundTag());
        }

        public void loadPersisted(CompoundTag tag) {
            IManagedAccessor.writePersistedFields(tag, getSyncStorage().getPersistedFields());
        }

        public CompoundTag saveSync(boolean force) {
            return IManagedAccessor.readSyncedFields(this, new CompoundTag(), force);
        }

        public void loadSync(CompoundTag tag) {
            new IManagedAccessor().writeToReadonlyField(
                    AccessorOp.SYNCED,
                    this,
                    NbtTagPayload.of(tag)
            );
        }

        private void onUpgradesChanged() {
            owner.saveChanges();
        }

        private void markFieldDirty(String name) {
            getSyncStorage().getFieldByKey(getFieldHolder().getSyncedFieldIndex(name)).markAsDirty();
        }

        @Override
        public ManagedFieldHolder getFieldHolder() {
            return MANAGED_FIELD_HOLDER;
        }

        @Override
        public IManagedStorage getSyncStorage() {
            return syncStorage;
        }

        @Override
        public void onChanged() {
            owner.saveChanges();
        }
    }
}