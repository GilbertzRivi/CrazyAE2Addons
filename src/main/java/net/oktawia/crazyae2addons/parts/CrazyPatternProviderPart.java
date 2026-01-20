package net.oktawia.crazyae2addons.parts;

import java.util.Arrays;
import java.util.List;

import appeng.api.inventories.ISegmentedInventory;
import appeng.api.inventories.InternalInventory;
import appeng.api.upgrades.IUpgradeInventory;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.api.upgrades.UpgradeInventories;
import appeng.core.AppEngBase;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.parts.crafting.PatternProviderPart;
import appeng.util.inv.AppEngInternalInventory;
import com.mojang.logging.LogUtils;
import net.minecraft.network.chat.Component;
import net.minecraftforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.CrazyAddons;
import net.oktawia.crazyae2addons.CrazyConfig;
import net.oktawia.crazyae2addons.defs.regs.CrazyItemRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IProviderLogicResizable;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.SyncBlockClientPacket;
import org.jetbrains.annotations.Nullable;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import appeng.api.parts.IPartItem;
import appeng.api.parts.IPartModel;
import appeng.api.stacks.AEItemKey;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.items.parts.PartModels;
import appeng.menu.locator.MenuLocators;
import appeng.parts.PartModel;
import appeng.util.SettingsFrom;

public class CrazyPatternProviderPart extends PatternProviderPart implements IUpgradeableObject {

    public static List<ResourceLocation> MODELS = Arrays.asList(
            CrazyAddons.makeId("part/crazy_pattern_provider_part"),
            ResourceLocation.fromNamespaceAndPath(AppEngBase.MOD_ID, "part/interface_on"),
            ResourceLocation.fromNamespaceAndPath(AppEngBase.MOD_ID, "part/interface_off"),
            ResourceLocation.fromNamespaceAndPath(AppEngBase.MOD_ID, "part/interface_has_channel")
    );
    @PartModels
    public static final PartModel MODELS_OFF = new PartModel(MODELS.get(0), MODELS.get(2));
    @PartModels
    public static final PartModel MODELS_ON = new PartModel(MODELS.get(0), MODELS.get(1));
    @PartModels
    public static final PartModel MODELS_HAS_CHANNEL = new PartModel(MODELS.get(0), MODELS.get(3));

    @Override
    public IPartModel getStaticModels() {
        if (isActive() && isPowered()) {
            return MODELS_HAS_CHANNEL;
        } else if (isPowered()) {
            return MODELS_ON;
        } else {
            return MODELS_OFF;
        }
    }

    private int added = 0;
    public IUpgradeInventory upgrades = UpgradeInventories.forMachine(CrazyItemRegistrar.CRAZY_PATTERN_PROVIDER_PART.get(), 1, this::saveChanges);

    public CrazyPatternProviderPart(IPartItem<?> partItem) {
        super(partItem);
    }

    public void upgradeOnce() {
        this.added ++;
        LogUtils.getLogger().info("{}, {}", isClientSide(), getLogic().getPatternInv().size());
        ((IProviderLogicResizable) getLogic()).setSize(8 * 9 + 9 * added);
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(),
                new SyncBlockClientPacket(getHost().getBlockEntity().getBlockPos(), added, getSide()));
    }

    public void setAdded(int amt) {
        if (amt != this.added) {
            this.added = amt;
            ((IProviderLogicResizable) getLogic()).setSize(8 * 9 + 9 * added);
        }
    }

    public int getAdded() {
        return this.added;
    }

    @Override
    public boolean onPartActivate(Player player, InteractionHand hand, Vec3 pos) {
        NetworkHandler.INSTANCE.send(PacketDistributor.ALL.noArg(), new SyncBlockClientPacket(getHost().getBlockEntity().getBlockPos(), added, getSide()));

        var heldItem = player.getItemInHand(hand);

        if (!heldItem.isEmpty() && heldItem.getItem() == CrazyItemRegistrar.CRAZY_UPGRADE.get().asItem()) {
            int maxAdd = CrazyConfig.COMMON.CrazyProviderMaxAddRows.get();
            int cur = getAdded();

            if (maxAdd != -1 && cur >= maxAdd) {
                player.displayClientMessage(Component.translatable("gui.crazyae2addons.provider_max"), true);
                return true;
            }
            heldItem.shrink(1);
            upgradeOnce();
            return true;
        }
        if (!player.getCommandSenderWorld().isClientSide()) {
            MenuOpener.open(CrazyMenuRegistrar.CRAZY_PATTERN_PROVIDER_MENU.get(), player, MenuLocators.forPart(this));
        }
        return true;
    }

    @Override
    public void writeToNBT(CompoundTag data) {
        super.writeToNBT(data);
        data.putInt("added", added);
        ((AppEngInternalInventory) getLogic().getPatternInv()).writeToNBT(data, "dainv");
        this.upgrades.writeToNBT(data, "upgrades");
    }

    @Override
    public void readFromNBT(CompoundTag data) {
        super.readFromNBT(data);
        added = data.getInt("added");
        if (data.contains("upgrades")) {
            this.upgrades.readFromNBT(data, "upgrades");
        }
        ((IProviderLogicResizable) getLogic()).setSize(8 * 9 + 9 * added);
        ((AppEngInternalInventory) getLogic().getPatternInv()).readFromNBT(data, "dainv");
    }


    @Nullable
    @Override
    public InternalInventory getSubInventory(ResourceLocation id) {
        if (id.equals(ISegmentedInventory.UPGRADES)) {
            return this.upgrades;
        }
        return super.getSubInventory(id);
    }

    @Override
    public IUpgradeInventory getUpgrades() {
        return this.upgrades;
    }

    @Override
    public PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, 9 * 8 + (this.added * 9));
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
    public void exportSettings(SettingsFrom mode, CompoundTag output) {
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            this.writeToNBT(output);
            return;
        }

        super.exportSettings(mode, output);
    }

    @Override
    public void importSettings(SettingsFrom mode, CompoundTag input, @Nullable Player player) {
        if (mode == SettingsFrom.DISMANTLE_ITEM) {
            this.readFromNBT(input);
            this.getLogic().updatePatterns();
            this.saveChanges();
            var host = this.getHost();
            if (host != null) {
                host.markForUpdate();
            }
            return;
        }

        super.importSettings(mode, input, player);
    }

    @Override
    public void addAdditionalDrops(List<ItemStack> drops, boolean wrenched) {
    }
}
