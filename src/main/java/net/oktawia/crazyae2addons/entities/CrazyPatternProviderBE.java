package net.oktawia.crazyae2addons.entities;

import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.util.inv.AppEngInternalInventory;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.mixins.PatternProviderBlockEntityAccessor;
import net.oktawia.crazyae2addons.network.NetworkHandler;
import net.oktawia.crazyae2addons.network.SyncBlockClientPacket;

import java.util.List;

public class CrazyPatternProviderBE extends PatternProviderBlockEntity {

    private int added = 0;
    private CompoundTag nbt;

    public CrazyPatternProviderBE(BlockPos pos, BlockState blockState) {
        this(pos, blockState, 9 * 8);
    }

    public CrazyPatternProviderBE(BlockPos pos, BlockState blockState, int patternSize) {
        super(CrazyBlockEntityRegistrar.CRAZY_PATTERN_PROVIDER_BE.get(), pos, blockState);
        this.getMainNode().setVisualRepresentation(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get().asItem());
        ((PatternProviderBlockEntityAccessor) this).setLogic(new PatternProviderLogic(getMainNode(), this, patternSize));
    }

    public CrazyPatternProviderBE refreshLogic(int added) {
        CompoundTag snap = new CompoundTag();
        this.getLogic().writeToNBT(snap);
        var oldInv = (AppEngInternalInventory) this.getLogic().getPatternInv();
        oldInv.writeToNBT(snap, "dainv");

        BlockPos pos = this.getBlockPos();
        BlockState state = this.getBlockState();
        level.removeBlockEntity(pos);
        level.setBlockEntity(new CrazyPatternProviderBE(pos, state, 8 * 9 + 9 * added));

        var newBE = (CrazyPatternProviderBE) level.getBlockEntity(pos);
        if (newBE == null) return this;

        newBE.added = added;

        newBE.getLogic().readFromNBT(snap);

        var newInv = (AppEngInternalInventory) newBE.getLogic().getPatternInv();
        newInv.readFromNBT(snap, "dainv");

        newBE.setChanged();
        if (!level.isClientSide) {
            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
            NetworkHandler.INSTANCE.send(
                    net.minecraftforge.network.PacketDistributor.ALL.noArg(),
                    new SyncBlockClientPacket(pos, added)
            );
        }

        return newBE;
    }

    public int getAdded() {
        return added;
    }

    public void setAdded(int amt) {
        if (amt != this.added) {
            this.added = amt;
            this.refreshLogic(amt);
        }
    }

    @Override
    public void saveAdditional(CompoundTag data) {
        super.saveAdditional(data);
        data.putInt("added", added);
        getLogic().writeToNBT(data);
        ((AppEngInternalInventory) getLogic().getPatternInv()).writeToNBT(data, "dainv");
    }

    @Override
    public void loadTag(CompoundTag data) {
        super.loadTag(data);
        added = data.getInt("added");
        this.nbt = data;
    }

    @Override
    public void onReady() {
        super.onReady();
        int expected = 8 * 9 + 9 * added;
        if (this.getLogic().getPatternInv().size() != expected) {
            var be = refreshLogic(added);
            be.added = added;
            be.getLogic().readFromNBT(this.nbt);
            ((AppEngInternalInventory) (be.getLogic().getPatternInv())).readFromNBT(this.nbt, "dainv");
        }
    }

    @Override
    public PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, 9 * 8 + (this.getAdded() * 9));
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
        return AEItemKey.of(CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.CRAZY_PATTERN_PROVIDER_BLOCK.get().asItem().getDefaultInstance();
    }

    @Override
    public void addAdditionalDrops(Level level, BlockPos pos, List<ItemStack> drops) {
    }
}
