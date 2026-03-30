package net.oktawia.crazyae2addons.parts;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.api.ids.AEComponents;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.items.parts.PartModels;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.SettingsFrom;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.LangDefs;
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

    private int added = 0;

    public final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get(),
            IsModLoaded.isAppFluxLoaded() ? 2 : 1,
            this::saveChanges
    );

    public CrazyPatternProviderPart(IPartItem<?> partItem) {
        super(partItem);
    }

    @Override
    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, BASE_SIZE);
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int newAdded) {
        if (newAdded == this.added) return;
        this.added = newAdded;
        var level = getLevel();
        if (level != null) {
            applySize(level.registryAccess());
        }
    }

    private void applySize(HolderLookup.Provider registries) {
        ((IProviderLogicResizable) getLogic()).setSize(BASE_SIZE + ROW_SIZE * added, registries);
    }

    public void upgradeOnce() {
        this.added++;
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
                        new SyncBlockClientPacket(be.getBlockPos(), added, getSide())
                );
            }
        }
    }

    @Override
    public boolean onUseItemOn(ItemStack heldItem, Player player, InteractionHand hand, Vec3 pos) {
        if (heldItem.getItem() == CrazyItemRegistrar.CRAZY_UPGRADE.get().asItem()) {
            if (!isClientSide()) {
                int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
                if (maxAdd != -1 && added >= maxAdd) {
                    player.displayClientMessage(Component.translatable(LangDefs.PROVIDER_MAX.getTranslationKey()), true);
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
                        new SyncBlockClientPacket(be.getBlockPos(), added, getSide())
                );
            }
            MenuOpener.open(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void writeToNBT(CompoundTag data, HolderLookup.Provider registries) {
        super.writeToNBT(data, registries);
        data.putInt("added", added);
        upgrades.writeToNBT(data, "upgrades", registries);
    }

    @Override
    public void readFromNBT(CompoundTag data, HolderLookup.Provider registries) {
        added = data.getInt("added");
        applySize(registries);
        super.readFromNBT(data, registries);
        if (data.contains("upgrades")) {
            upgrades.readFromNBT(data, "upgrades", registries);
        }
    }

    @Override
    public void exportSettings(SettingsFrom mode, DataComponentMap.Builder builder) {
        super.exportSettings(mode, builder);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var patternContents = ((AppEngInternalInventory) getLogic().getPatternInv()).toItemContainerContents();
            builder.set(AEComponents.EXPORTED_PATTERNS, patternContents);
            int filled = (int) patternContents.nonEmptyStream().count();
            var tag = new CompoundTag();
            tag.putInt("added", added);
            tag.putInt("filled", filled);
            builder.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        }
    }

    @Override
    public void importSettings(SettingsFrom mode, DataComponentMap input, @Nullable Player player) {
        super.importSettings(mode, input, player);
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            var customData = input.get(DataComponents.CUSTOM_DATA);
            if (customData != null && customData.contains("added")) {
                added = customData.copyTag().getInt("added");
                var level = getLevel();
                if (level != null) applySize(level.registryAccess());
            }
            var patterns = input.getOrDefault(AEComponents.EXPORTED_PATTERNS, ItemContainerContents.EMPTY);
            ((AppEngInternalInventory) getLogic().getPatternInv()).fromItemContainerContents(patterns);
        }
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
        for (int i = 0; i < upgrades.size(); i++) {
            var stack = upgrades.getStackInSlot(i);
            if (!stack.isEmpty()) drops.add(stack);
        }
    }

    @Override
    @Nullable
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) return upgrades;
        return super.getSubInventory(id);
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return upgrades;
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
        if (isActive() && isPowered()) return MODELS_HAS_CHANNEL;
        if (isPowered()) return MODELS_ON;
        return MODELS_OFF;
    }
}