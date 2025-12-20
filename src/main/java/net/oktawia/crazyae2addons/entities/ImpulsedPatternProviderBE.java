package net.oktawia.crazyae2addons.entities;

import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.logic.ImpulsedPatternProviderLogic;

public class ImpulsedPatternProviderBE extends PatternProviderBlockEntity {

    private ImpulsedPatternProviderLogic impulsedLogic;
    private boolean lastRedstone = false;

    public ImpulsedPatternProviderBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.IMPULSED_PATTERN_PROVIDER_BE.get(), pos, blockState);
        this.getMainNode().setVisualRepresentation(CrazyBlockRegistrar.IMPULSED_PATTERN_PROVIDER_BLOCK.get());
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.IMPULSED_PATTERN_PROVIDER_MENU.get(), player, locator);
    }

    @Override
    protected PatternProviderLogic createLogic() {
        this.impulsedLogic = new ImpulsedPatternProviderLogic(this.getMainNode(), this, 9 * 4);
        return this.impulsedLogic;
    }

    public ImpulsedPatternProviderLogic getImpulsedLogic() {
        return this.impulsedLogic;
    }

    public void onRedstoneUpdate(boolean powered) {
        if (this.level == null || this.level.isClientSide) {
            return;
        }

        if (!this.lastRedstone && powered && this.impulsedLogic != null) {
            this.impulsedLogic.repeat();
        }
        this.lastRedstone = powered;
    }

    @Override
    public void onLoad() {
        super.onLoad();

        if (this.level == null || this.level.isClientSide) {
            return;
        }

        boolean poweredNow = this.level.hasNeighborSignal(this.worldPosition);
        if (!this.lastRedstone && poweredNow && this.impulsedLogic != null) {
            this.impulsedLogic.repeat();
        }
        this.lastRedstone = poweredNow;
    }

    @Override
    public void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putBoolean("lastRedstone", this.lastRedstone);
    }

    @Override
    public void loadTag(CompoundTag tag) {
        super.loadTag(tag);
        this.lastRedstone = tag.getBoolean("lastRedstone");
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(
                CrazyMenuRegistrar.IMPULSED_PATTERN_PROVIDER_MENU.get(),
                player,
                MenuLocators.forBlockEntity(this)
        );
    }

    @Override
    public AEItemKey getTerminalIcon() {
        return AEItemKey.of(CrazyBlockRegistrar.IMPULSED_PATTERN_PROVIDER_BLOCK.get().asItem().getDefaultInstance());
    }

    @Override
    public ItemStack getMainMenuIcon() {
        return CrazyBlockRegistrar.IMPULSED_PATTERN_PROVIDER_BLOCK.get().asItem().getDefaultInstance();
    }
}
