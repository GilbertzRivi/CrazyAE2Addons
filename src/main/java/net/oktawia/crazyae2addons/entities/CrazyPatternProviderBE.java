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
import appeng.menu.locator.MenuHostLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;
import net.oktawia.crazyae2addons.IsModLoaded;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.interfaces.IProviderLogicResizable;
import net.oktawia.crazyae2addons.network.packets.SyncBlockClientPacket;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CrazyPatternProviderBE extends PatternProviderBlockEntity implements IUpgradeableObject {

    private static final int BASE_SIZE = 8 * 9;
    private static final int ROW_SIZE = 9;

    private int added = 0;

    public final IUpgradeInventory upgrades = UpgradeInventories.forMachine(
            CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get(),
            IsModLoaded.isAppFluxLoaded() ? 2 : 1,
            this::saveChanges
    );

    public CrazyPatternProviderBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.CRAZY_PATTERN_PROVIDER_BE.get(), pos, blockState);
        this.getMainNode().setVisualRepresentation(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get().asItem());
    }

    private void applySize(HolderLookup.Provider registries) {
        ((IProviderLogicResizable) getLogic()).setSize(BASE_SIZE + ROW_SIZE * added, registries);
    }

    public void syncAddedToClients() {
        if (level == null || level.isClientSide) return;
        PacketDistributor.sendToPlayersTrackingChunk(
                (ServerLevel) level,
                new ChunkPos(getBlockPos()),
                new SyncBlockClientPacket(getBlockPos(), added)
        );
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int newAdded) {
        if (newAdded == this.added) return;
        this.added = newAdded;
        if (level != null) {
            applySize(level.registryAccess());
            if (!level.isClientSide) {
                getLogic().updatePatterns();
                setChanged();
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
                syncAddedToClients();
            }
        }
    }

    @Override
    public void saveAdditional(CompoundTag data, HolderLookup.Provider registries) {
        super.saveAdditional(data, registries);
        data.putInt("added", added);
        upgrades.writeToNBT(data, "upgrades", registries);
    }

    @Override
    public void loadTag(CompoundTag data, HolderLookup.Provider registries) {
        this.added = data.getInt("added");
        applySize(registries);
        super.loadTag(data, registries);
        if (data.contains("upgrades")) {
            upgrades.readFromNBT(data, "upgrades", registries);
        }
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
    public IUpgradeInventory getUpgrades() {
        return upgrades;
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
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {}
}