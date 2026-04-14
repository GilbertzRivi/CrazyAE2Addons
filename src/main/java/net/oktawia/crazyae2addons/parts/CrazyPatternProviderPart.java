package net.oktawia.crazyae2addons.parts;

import appeng.api.ids.AEComponents;
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
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import com.lowdragmc.lowdraglib2.syncdata.IPersistedSerializable;
import com.lowdragmc.lowdraglib2.syncdata.annotation.Persisted;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.LangDefs;
import net.oktawia.crazyae2addons.defs.components.CrazyProviderDisplayData;
import net.oktawia.crazyae2addons.defs.regs.CrazyDataComponents;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IProviderLogicResizable;
import net.oktawia.crazyae2addons.network.packets.SyncBlockClientPacket;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrazyPatternProviderPart extends PatternProviderPart implements IUpgradeableObject {

    private static final int BASE_SIZE = 8 * 9;
    private static final int ROW_SIZE = 9;

    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(
            CrazyAddons.makeId("part/crazy_pattern_provider_part"),
            ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_off")
    );

    @PartModels
    public static final PartModel MODELS_ON = new PartModel(
            CrazyAddons.makeId("part/crazy_pattern_provider_part"),
            ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_on")
    );

    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(
            CrazyAddons.makeId("part/crazy_pattern_provider_part"),
            ResourceLocation.fromNamespaceAndPath("ae2", "part/interface_has_channel")
    );

    private final PartState persisted = new PartState();

    public CrazyPatternProviderPart(IPartItem<?> partItem) {
        super(partItem);
    }

    public int getAdded() {
        return persisted.added;
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return persisted.upgrades;
    }

    @Override
    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, BASE_SIZE);
    }

    public void setAdded(int newAdded) {
        if (newAdded == persisted.added) {
            return;
        }

        persisted.added = newAdded;

        var level = getLevel();
        if (level != null) {
            applySize(level.registryAccess());
        }
    }

    private void applySize(HolderLookup.Provider registries) {
        ((IProviderLogicResizable) getLogic()).setSize(BASE_SIZE + ROW_SIZE * persisted.added, registries);
    }

    public void upgradeOnce() {
        persisted.added++;

        var level = getLevel();
        var be = getHost().getBlockEntity();

        if (level != null) {
            applySize(level.registryAccess());
            getLogic().updatePatterns();
            saveChanges();

            if (!level.isClientSide) {
                PacketDistributor.sendToPlayersTrackingChunk(
                        (ServerLevel) level,
                        new ChunkPos(be.getBlockPos()),
                        new SyncBlockClientPacket(be.getBlockPos(), persisted.added, getSide())
                );
            }
        }
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, Player player, InteractionHand hand, Vec3 pos) {
        if (heldItem.getItem() == CrazyItemRegistrar.CRAZY_UPGRADE.get().asItem()) {
            if (!isClientSide()) {
                int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
                if (maxAdd != -1 && persisted.added >= maxAdd) {
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

        return false;
    }

    @Override
    public boolean onUseWithoutItem(Player player, Vec3 pos) {
        if (!isClientSide()) {
            var be = getHost().getBlockEntity();
            var level = getLevel();

            if (level != null) {
                PacketDistributor.sendToPlayersTrackingChunk(
                        (ServerLevel) level,
                        new ChunkPos(be.getBlockPos()),
                        new SyncBlockClientPacket(be.getBlockPos(), persisted.added, getSide())
                );
            }

            MenuOpener.open(
                    CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(),
                    player,
                    MenuLocators.forPart(this)
            );
        }
        return true;
    }

    @Override
    public void writeToNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        data.put("crazy_state", persisted.serializeNBT(registries));
    }

    @Override
    public void readFromNBT(CompoundTag data, HolderLookup.Provider registries) {
        if (data.contains("crazy_state", Tag.TAG_COMPOUND)) {
            CompoundTag stateTag = data.getCompound("crazy_state");

            if (stateTag.contains("added", Tag.TAG_INT)) {
                persisted.added = stateTag.getInt("added");
                applySize(registries);
            }
        }

        super.readFromNBT(data, registries);

        if (data.contains("crazy_state", Tag.TAG_COMPOUND)) {
            persisted.deserializeNBT(registries, data.getCompound("crazy_state"));
        }
    }

    @Override
    public boolean readFromStream(RegistryFriendlyByteBuf data) {
        boolean changed = super.readFromStream(data);

        int oldAdded = persisted.added;
        persisted.readFromBuff(data);

        if (oldAdded != persisted.added) {
            var level = getLevel();
            if (level != null) {
                applySize(level.registryAccess());
                getLogic().updatePatterns();
            }
        }

        return changed || oldAdded != persisted.added;
    }

    @Override
    public void writeToStream(RegistryFriendlyByteBuf data) {
        super.writeToStream(data);
        persisted.writeToBuff(data);
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder) {
        super.exportSettings(mode, builder);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var patternContents = ((AppEngInternalInventory) getLogic().getPatternInv()).toItemContainerContents();
            builder.set(AEComponents.EXPORTED_PATTERNS, patternContents);

            int filled = (int) patternContents.nonEmptyStream().count();
            builder.set(
                    CrazyDataComponents.CRAZY_PROVIDER_DISPLAY.get(),
                    new CrazyProviderDisplayData(persisted.added, filled)
            );
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);

        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var display = input.get(CrazyDataComponents.CRAZY_PROVIDER_DISPLAY.get());
            if (display != null) {
                persisted.added = display.added();

                var level = getLevel();
                if (level != null) {
                    applySize(level.registryAccess());
                }
            }

            var patterns = input.getOrDefault(AEComponents.EXPORTED_PATTERNS, ItemContainerContents.EMPTY);
            ((AppEngInternalInventory) getLogic().getPatternInv()).fromItemContainerContents(patterns);
        }
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        for (int i = 0; i < persisted.upgrades.size(); i++) {
            var stack = persisted.upgrades.getStackInSlot(i);
            if (!stack.isEmpty()) {
                drops.add(stack);
            }
        }
    }

    @Override
    @Nullable
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return persisted.upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    public void openMenu(Player player, appeng.menu.locator.MenuHostLocator locator) {
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

    private final class PartState implements IPersistedSerializable {
        @Persisted
        private int added = 0;

        @Persisted
        private final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
                CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get(),
                IsModLoaded.isAppFluxLoaded() ? 2 : 1,
                CrazyPatternProviderPart.this::saveChanges
        );
    }
}