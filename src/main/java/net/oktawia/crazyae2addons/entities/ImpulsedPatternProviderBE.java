package net.oktawia.crazyae2addons.entities;

import appeng.api.stacks.AEItemKey;
import appeng.blockentity.crafting.PatternProviderBlockEntity;
import appeng.helpers.patternprovider.PatternProviderLogic;
import appeng.menu.ISubMenu;
import appeng.menu.MenuOpener;
import appeng.menu.locator.MenuLocator;
import appeng.menu.locator.MenuLocators;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyMenuRegistrar;
import net.oktawia.crazyae2addons.defs.regs.CrazyBlockEntityRegistrar;

public class ImpulsedPatternProviderBE extends PatternProviderBlockEntity {

    public ImpulsedPatternProviderBE(BlockPos pos, BlockState blockState) {
        super(CrazyBlockEntityRegistrar.IMPULSED_PATTERN_PROVIDER_BE.get(), pos, blockState);
        this.getMainNode().setVisualRepresentation(CrazyBlockRegistrar.IMPULSED_PATTERN_PROVIDER_BLOCK.get());
    }

    public void openMenu(Player player, MenuLocator locator) {
        MenuOpener.open(CrazyMenuRegistrar.IMPULSED_PATTERN_PROVIDER_MENU.get(), player, locator);
    }

    protected PatternProviderLogic createLogic() {
        return new PatternProviderLogic(this.getMainNode(), this, 9);
    }

    @Override
    public void returnToMainMenu(Player player, ISubMenu subMenu) {
        MenuOpener.returnTo(CrazyMenuRegistrar.IMPULSED_PATTERN_PROVIDER_MENU.get(), player, MenuLocators.forBlockEntity(this));
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